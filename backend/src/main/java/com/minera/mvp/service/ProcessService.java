package com.minera.mvp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minera.mvp.model.AppUser;
import com.minera.mvp.model.ResultRow;
import com.minera.mvp.model.Upload;
import com.minera.mvp.model.ProcessRun;
import com.minera.mvp.repo.ResultRowRepository;
import com.minera.mvp.repo.ProcessRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta el proceso completo: leer archivo, validar, transformar, cruzar con el catalogo,
 * guardar historial, generar Excel/PDF y notificar por email.
 */
@Service
public class ProcessService {

    private static final Logger log = LoggerFactory.getLogger(ProcessService.class);

    private final FileParserService parser;
    private final ValidationAndTransformService validator;
    private final ReportService reportService;
    private final MailService mailService;
    private final ResultRowRepository rowRepo;
    private final ProcessRunRepository runRepo;
    private final ObjectMapper json;
    private final Path outputDir;

    public ProcessService(FileParserService parser,
                          ValidationAndTransformService validator,
                          ReportService reportService,
                          MailService mailService,
                          ResultRowRepository rowRepo,
                          ProcessRunRepository runRepo,
                          ObjectMapper json,
                          @Value("${app.storage.output:./outputs}") String outputDir) {
        this.parser = parser;
        this.validator = validator;
        this.reportService = reportService;
        this.mailService = mailService;
        this.rowRepo = rowRepo;
        this.runRepo = runRepo;
        this.json = json;
        this.outputDir = Path.of(outputDir);
    }

    @Transactional
    public ProcessRun process(Upload upload, AppUser user) {
        List<String> logs = new ArrayList<>();
        logs.add("Inicio del proceso: " + upload.getOriginalName());

        ProcessRun run = new ProcessRun();
        run.setCompany(upload.getCompany());
        run.setUpload(upload);
        run.setRunType("VALIDA_TRANSFORMA_CRUZA");
        run.setStatus(ProcessRun.STATUS_RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setExecutedBy(user);
        runRepo.save(run);
        runRepo.flush();

        long start = System.currentTimeMillis();
        try {
            FileParserService.ParsedFile parsed;
            try (FileInputStream in = new FileInputStream(upload.getStoredPath())) {
                parsed = parser.parse(upload.getOriginalName(), in);
            }
            logs.add("Archivo leido: " + parsed.totalRows() + " filas, columnas: " + parsed.headers());

            List<ValidationAndTransformService.ValidatedRow> validated =
                    validator.validateAll(parsed.rows(), upload.getCompany());
            long valid = validated.stream().filter(ValidationAndTransformService.ValidatedRow::valid).count();
            long invalid = validated.size() - valid;
            logs.add("Validacion/cruce completado: " + valid + " filas validas, " + invalid + " invalidas");

            Map<String, Object> kpis = computeKpis(validated);
            logs.add("KPIs calculados: tonelaje total=" + kpis.get("totalTonnage")
                    + ", ley media=" + kpis.get("avgGrade"));

            run.setTotalRows(validated.size());
            run.setValidRows((int) valid);
            run.setInvalidRows((int) invalid);
            run.setTotalTonnage(toBd(kpis.get("totalTonnage")));
            run.setAvgGrade(toBd(kpis.get("avgGrade")));
            run.setKpis(json.writeValueAsString(kpis));

            Path runDir = outputDir.resolve("runs").resolve(String.valueOf(run.getId()));
            Files.createDirectories(runDir);

            ReportService.ReportOutput output = reportService.generate(
                    runDir, upload.getCompany().getName(), upload.getOriginalName(), validated, kpis);
            run.setExcelFileName(output.excelName());
            run.setPdfFileName(output.pdfName());
            logs.add("Reportes generados: " + output.excelName() + " y " + output.pdfName());

            List<ResultRow> entities = new ArrayList<>();
            for (ValidationAndTransformService.ValidatedRow v : validated) {
                ResultRow r = new ResultRow();
                r.setRun(run);
                r.setCompany(upload.getCompany());
                r.setRowNumber(v.rowNumber());
                r.setFecha(v.fecha());
                r.setTurno(v.turno());
                r.setZona(v.zona());
                r.setEquipmentCode(v.equipmentCode());
                r.setEquipmentName(v.equipmentName());
                r.setEquipmentArea(v.equipmentArea());
                r.setEquipoTipo(v.equipoTipo());
                r.setEstado(v.estado());
                r.setTonelaje(v.tonelaje());
                r.setLeyCu(v.leyCu());
                r.setValid(v.valid());
                r.setErrors(v.error());
                entities.add(r);
            }
            rowRepo.saveAll(entities);
            logs.add("Historial guardado: " + entities.size() + " filas");

            upload.setStatus(Upload.STATUS_PROCESSED);
            upload.setTotalRows(validated.size());
            upload.setValidRows((int) valid);
            upload.setInvalidRows((int) invalid);
            upload.setErrors(String.join(" | ", entities.stream()
                    .filter(er -> !er.isValid())
                    .limit(20)
                    .map(ResultRow::getErrors)
                    .distinct()
                    .toList()));

            run.setLogs(String.join("\n", logs));
            run.setStatus(ProcessRun.STATUS_OK);
            run.setFinishedAt(LocalDateTime.now());
            run.setDurationMs(System.currentTimeMillis() - start);
            run = runRepo.save(run);

            mailService.sendRunSummary(
                    user.getEmail(),
                    "Proceso completado: " + upload.getOriginalName(),
                    buildEmailBody(run),
                    List.of(runDir.resolve(run.getExcelFileName()).toFile(),
                            runDir.resolve(run.getPdfFileName()).toFile()));

            log.info("Proceso {} completado en {} ms", run.getId(), run.getDurationMs());
            return run;

        } catch (Exception ex) {
            log.error("Error en el proceso", ex);
            run.setStatus(ProcessRun.STATUS_ERROR);
            run.setFinishedAt(LocalDateTime.now());
            run.setDurationMs(System.currentTimeMillis() - start);
            logs.add("ERROR: " + ex.getMessage());
            run.setLogs(String.join("\n", logs));
            upload.setStatus(Upload.STATUS_ERROR);
            return runRepo.save(run);
        }
    }

    private String buildEmailBody(ProcessRun run) {
        return "Hola,\n\nEl proceso sobre '" + run.getUpload().getOriginalName() + "' terminó.\n\n"
                + "Estado: " + run.getStatus() + "\n"
                + "Filas totales: " + run.getTotalRows() + "\n"
                + "Filas validas: " + run.getValidRows() + "\n"
                + "Filas invalidas: " + run.getInvalidRows() + "\n"
                + "Tonelaje total: " + run.getTotalTonnage() + " t\n"
                + "Ley media de cobre: " + run.getAvgGrade() + " %\n\n"
                + "Adjuntos: Excel y PDF con los resultados.\n\nSaludos,\nSistema Minera MVP";
    }

    private Map<String, Object> computeKpis(List<ValidationAndTransformService.ValidatedRow> rows) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        Map<String, Double> perZone = new LinkedHashMap<>();
        Map<String, Double> perTurno = new LinkedHashMap<>();
        Map<String, Long> perEstado = new LinkedHashMap<>();
        BigDecimal tonnage = BigDecimal.ZERO;
        BigDecimal gradeAcc = BigDecimal.ZERO;
        long gradeCount = 0;

        for (ValidationAndTransformService.ValidatedRow v : rows) {
            if (!v.valid()) continue;
            double t = v.tonelaje() == null ? 0 : v.tonelaje().doubleValue();
            tonnage = tonnage.add(v.tonelaje() == null ? BigDecimal.ZERO : v.tonelaje());
            if (v.leyCu() != null) {
                gradeAcc = gradeAcc.add(v.leyCu());
                gradeCount++;
            }
            perZone.merge(v.zona() == null ? "SIN_ZONA" : v.zona(), t, Double::sum);
            perTurno.merge(v.turno() == null ? "-" : v.turno(), t, Double::sum);
        }
        for (ValidationAndTransformService.ValidatedRow v : rows) {
            if (!v.valid()) continue;
            perEstado.merge(v.estado() == null ? "SIN_ESTADO" : v.estado(), 1L, Long::sum);
        }

        kpis.put("totalRows", rows.size());
        kpis.put("validRows", rows.stream().filter(ValidationAndTransformService.ValidatedRow::valid).count());
        kpis.put("invalidRows", rows.stream().filter(r -> !r.valid()).count());
        kpis.put("totalTonnage", tonnage.setScale(2, RoundingMode.HALF_UP).doubleValue());
        kpis.put("avgGrade", gradeCount == 0 ? 0.0
                : gradeAcc.divide(BigDecimal.valueOf(gradeCount), 4, RoundingMode.HALF_UP).doubleValue());
        kpis.put("perZone", perZone);
        kpis.put("perTurno", perTurno);
        kpis.put("perEstado", perEstado);
        return kpis;
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(((Number) o).doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }
}