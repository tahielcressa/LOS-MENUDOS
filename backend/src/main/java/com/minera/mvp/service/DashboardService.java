package com.minera.mvp.service;

import com.minera.mvp.dto.DashboardDto;
import com.minera.mvp.dto.RunDto;
import com.minera.mvp.model.Company;
import com.minera.mvp.model.ResultRow;
import com.minera.mvp.repo.ProcessRunRepository;
import com.minera.mvp.repo.ResultRowRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ResultRowRepository rowRepo;
    private final ProcessRunRepository runRepo;
    private final UploadService uploadService;

    public DashboardService(ResultRowRepository rowRepo, ProcessRunRepository runRepo,
                            UploadService uploadService) {
        this.rowRepo = rowRepo;
        this.runRepo = runRepo;
        this.uploadService = uploadService;
    }

    public DashboardDto build(Company company) {
        List<ResultRow> rows = rowRepo.findTop500ByCompanyOrderByIdDesc(company);

        Map<String, Double> perZone = new LinkedHashMap<>();
        Map<String, Double> perTurno = new LinkedHashMap<>();
        Map<String, Long> perEstado = new LinkedHashMap<>();
        BigDecimal tonnage = BigDecimal.ZERO;
        BigDecimal gradeAcc = BigDecimal.ZERO;
        long gradeCount = 0;
        long valid = 0;
        long invalid = 0;

        for (ResultRow r : rows) {
            if (r.isValid()) valid++;
            else invalid++;

            if (!r.isValid()) continue;
            double t = r.getTonelaje() == null ? 0 : r.getTonelaje().doubleValue();
            tonnage = tonnage.add(r.getTonelaje() == null ? BigDecimal.ZERO : r.getTonelaje());
            if (r.getLeyCu() != null) {
                gradeAcc = gradeAcc.add(r.getLeyCu());
                gradeCount++;
            }
            perZone.merge(str(r.getZona()), t, Double::sum);
            perTurno.merge(str(r.getTurno()), t, Double::sum);
        }
        for (ResultRow r : rows) {
            if (r.isValid()) perEstado.merge(str(r.getEstado()), 1L, Long::sum);
        }

        List<DashboardDto.NameValue> zoneList = perZone.entrySet().stream()
                .map(e -> new DashboardDto.NameValue(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        List<DashboardDto.NameValue> turnoList = perTurno.entrySet().stream()
                .map(e -> new DashboardDto.NameValue(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        List<DashboardDto.NameValue> estadoList = perEstado.entrySet().stream()
                .map(e -> new DashboardDto.NameValue(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        List<RunDto> recent = runRepo.findTop5ByCompanyOrderByStartedAtDesc(company).stream()
                .map(uploadService::toRunDto)
                .toList();

        return new DashboardDto(
                uploadService.countUploads(company),
                runRepo.findByCompanyOrderByStartedAtDesc(company).size(),
                rows.size(),
                (int) valid,
                (int) invalid,
                tonnage.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                gradeCount == 0 ? 0.0
                        : gradeAcc.divide(BigDecimal.valueOf(gradeCount), 4, RoundingMode.HALF_UP).doubleValue(),
                zoneList,
                turnoList,
                estadoList,
                recent);
    }

    private String str(String s) {
        return s == null || s.isBlank() ? "SIN_DATO" : s;
    }
}