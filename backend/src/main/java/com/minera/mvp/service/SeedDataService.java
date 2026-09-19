package com.minera.mvp.service;

import com.minera.mvp.model.AppUser;
import com.minera.mvp.model.Company;
import com.minera.mvp.model.Equipment;
import com.minera.mvp.repo.CompanyRepository;
import com.minera.mvp.repo.EquipmentRepository;
import com.minera.mvp.repo.UserRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Al primer arranque crea empresas, usuarios y catalogo de equipos de demostracion,
 * y genera archivos CSV y Excel de ejemplo en ../sample-data.
 */
@Component
public class SeedDataService {

    private static final Logger log = LoggerFactory.getLogger(SeedDataService.class);

    private final CompanyRepository companies;
    private final UserRepository users;
    private final EquipmentRepository equipments;
    private final PasswordEncoder encoder;
    private final Path samplesDir;

    public SeedDataService(CompanyRepository companies, UserRepository users,
                           EquipmentRepository equipments, PasswordEncoder encoder,
                           @Value("${app.storage.samples:../sample-data}") String samplesDir) {
        this.companies = companies;
        this.users = users;
        this.equipments = equipments;
        this.encoder = encoder;
        this.samplesDir = Path.of(samplesDir);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (companies.count() > 0) {
            generateSamples();
            return;
        }

        Company andina = company("ANDINA", "Minera Andina SAC", "PERU");
        Company delSur = company("DELSUR", "Mina del Sur Ltda", "CHILE");

        user(andina, "admin@mineraandina.com", "Ana Jefa de Operaciones", "admin123", AppUser.ROLE_ADMIN);
        user(andina, "operador@mineraandina.com", "Pablo Operador", "oper123", AppUser.ROLE_OPERATOR);
        user(delSur, "admin@minadelsur.com", "Carla Gerente Mina", "admin123", AppUser.ROLE_ADMIN);

        equipment(andina, "CAM-01", "Camión 785C-01", "CHACRA", "CAMION");
        equipment(andina, "CAM-02", "Camión 785C-02", "CHACRA", "CAMION");
        equipment(andina, "CAM-03", "Camión 793F-03", "NORESTE", "CAMION");
        equipment(andina, "CAR-01", "Cargador WA900-01", "CHACRA", "CARGADOR");
        equipment(andina, "CAR-02", "Cargador R9800-02", "SUR", "CARGADOR");
        equipment(andina, "PER-01", "Perforadora RHP-01", "PIT", "PERFORADORA");

        equipment(delSur, "CL-01", "Camión Komatsu-01", "CHACRA", "CAMION");
        equipment(delSur, "CL-02", "Camión Komatsu-02", "NORTE", "CAMION");
        equipment(delSur, "CP-01", "Cargador P&H-01", "SUR", "CARGADOR");

        generateSamples();

        log.info("SeedData: empresas, usuarios y catalogo creados. Usuarios demo: admin@mineraandina.com/admin123");
    }

    private Company company(String code, String name, String country) {
        Company c = new Company();
        c.setCode(code);
        c.setName(name);
        c.setCountry(country);
        companies.save(c);
        return c;
    }

    private void user(Company company, String email, String fullName, String password, String role) {
        AppUser u = new AppUser();
        u.setCompany(company);
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        users.save(u);
    }

    private void equipment(Company company, String code, String name, String area, String type) {
        Equipment e = new Equipment();
        e.setCompany(company);
        e.setCode(code);
        e.setName(name);
        e.setArea(area);
        e.setType(type);
        equipments.save(e);
    }

    private void generateSamples() {
        try {
            Files.createDirectories(samplesDir);

            Path csv = samplesDir.resolve("muestra_operacion.csv");
            if (!Files.exists(csv)) {
                String content = """
                        fecha;turno;zona;equipo;tonelaje;ley_cu;estado
                        12/09/2026;A;CHACRA;CAM-01;1234,5;0,62;OK
                        12/09/2026;A;CHACRA;CAR-01;987,25;0,58;OPERANDO
                        12/09/2026;B;NORESTE;CAM-03;1543,75;0,71;EN PROCESO
                        13/09/2026;B;CHACRA;CAM-02;1100,00;0,55;OK
                        13/09/2026;C;SUR;CAR-02;876,4;0,49;MANTENCION
                        13/09/2026;A;PIT;PER-01;0;0,0;DETENIDO
                        14/09/2026;X;SUR;CAM-01;1500;0,6;OK
                        14/09/2026;A;CHACRA;ZZ-999;200;0,5;OK
                        15/09/2026;B;CHACRA;CAM-01;1250,5;0,66;OK
                        15/09/2026;C;CHACRA;CAM-02;980;0,59;OPERATIVO
                        """;
                Files.writeString(csv, content, StandardCharsets.UTF_8);
                log.info("Sample CSV generado en {}", csv);
            }

            Path xlsx = samplesDir.resolve("muestra_operacion.xlsx");
            if (!Files.exists(xlsx)) {
                try (XSSFWorkbook wb = new XSSFWorkbook()) {
                    var sheet = wb.createSheet("Operacion");
                    String[] headers = {"fecha", "turno", "zona", "equipo", "tonelaje", "ley_cu", "estado"};
                    var hr = sheet.createRow(0);
                    for (int i = 0; i < headers.length; i++) hr.createCell(i).setCellValue(headers[i]);
                    String[][] data = {
                            {"12/09/2026", "A", "CHACRA", "CAM-01", "1234,5", "0,62", "OK"},
                            {"12/09/2026", "A", "CHACRA", "CAR-01", "987,25", "0,58", "OPERANDO"},
                            {"12/09/2026", "B", "NORESTE", "CAM-03", "1543,75", "0,71", "EN PROCESO"},
                            {"13/09/2026", "B", "CHACRA", "CAM-02", "1100", "0,55", "OK"},
                            {"13/09/2026", "C", "SUR", "CAR-02", "876,4", "0,49", "MANTENCION"},
                            {"14/09/2026", "A", "CHACRA", "ZZ-999", "200", "0,5", "OK"},
                            {"15/09/2026", "B", "CHACRA", "CAM-01", "1250,5", "0,66", "OK"}
                    };
                    for (int i = 0; i < data.length; i++) {
                        var row = sheet.createRow(i + 1);
                        for (int c = 0; c < data[i].length; c++) row.createCell(c).setCellValue(data[i][c]);
                    }
                    for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
                    try (FileOutputStream out = new FileOutputStream(xlsx.toFile())) {
                        wb.write(out);
                    }
                }
                log.info("Sample Excel generado en {}", xlsx);
            }

        } catch (Exception ex) {
            log.warn("No se pudieron generar los archivos de ejemplo: {}", ex.getMessage());
        }
    }
}