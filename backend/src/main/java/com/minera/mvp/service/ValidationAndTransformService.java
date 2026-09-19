package com.minera.mvp.service;

import com.minera.mvp.model.Company;
import com.minera.mvp.model.Equipment;
import com.minera.mvp.repo.EquipmentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Valida cada fila, transforma valores (norm, mayusculas, numeros con punto o coma)
 * y cruza con el catalogo de equipos de la empresa.
 */
@Service
public class ValidationAndTransformService {

    private final EquipmentRepository equipmentRepo;

    public ValidationAndTransformService(EquipmentRepository equipmentRepo) {
        this.equipmentRepo = equipmentRepo;
    }

    public record ValidatedRow(
            int rowNumber,
            LocalDate fecha,
            String turno,
            String zona,
            String equipmentCode,
            String equipmentName,
            String equipmentArea,
            String equipoTipo,
            String estado,
            BigDecimal tonelaje,
            BigDecimal leyCu,
            boolean valid,
            String error) {
    }

    public List<ValidatedRow> validateAll(List<Map<String, String>> rows, Company company) {
        List<ValidatedRow> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            out.add(validate(rows.get(i), company, i + 2));
        }
        return out;
    }

    public ValidatedRow validate(Map<String, String> row, Company company, int rowNumber) {
        List<String> errors = new ArrayList<>();

        String fechaRaw = value(row, "fecha", "date", "fecha_oper", "dia");
        LocalDate fecha = parseDate(fechaRaw);
        if (fecha == null) errors.add("Fecha invalida '" + blank(fechaRaw) + "'");

        String turno = normalizeTurno(value(row, "turno", "shift"));
        if (turno == null) errors.add("Turno invalido (usar A, B o C)");

        String zona = normalizeZona(value(row, "zona", "area", "sector", "block"));
        if (zona.isEmpty()) errors.add("Zona vacia");

        String estado = normalizeEstado(value(row, "estado", "status", "condicion"));
        if (estado == null) errors.add("Estado invalido (usar OK, EN_PROCESO o DETENIDO)");

        String tonelajeRaw = value(row, "tonelaje", "ton", "tonnage", "tms", "peso");
        BigDecimal tonelaje = parseNumber(tonelajeRaw);
        if (tonelaje == null || tonelaje.signum() <= 0 || tonelaje.compareTo(new BigDecimal("100000")) > 0) {
            errors.add("Tonelaje invalido '" + blank(tonelajeRaw) + "' (mayor a 0 y menor a 100000)");
        }

        String leyRaw = value(row, "ley_cu", "ley", "ley_cobre", "cu", "grade", "grado");
        BigDecimal ley = parseNumber(leyRaw);
        if (ley == null || ley.signum() < 0 || ley.compareTo(new BigDecimal("100")) > 0) {
            errors.add("Ley de cobre invalida '" + blank(leyRaw) + "' (0 a 100)");
        }

        String code = normalizeCode(value(row, "equipo", "equipment", "equipo_codigo", "codigo_equipo", "maquina"));
        Equipment equipment = null;
        if (code.isEmpty()) {
            errors.add("Codigo de equipo vacio");
        } else {
            equipment = equipmentRepo.findByCompanyAndCode(company, code).orElse(null);
            if (equipment == null) {
                errors.add("Equipo '" + code + "' no existe en el catalogo de la empresa");
            }
        }

        boolean valid = errors.isEmpty();
        String error = valid ? null : String.join(" | ", errors);

        return new ValidatedRow(
                rowNumber,
                fecha,
                turno,
                zona,
                code,
                equipment != null ? equipment.getName() : null,
                equipment != null ? equipment.getArea() : null,
                equipment != null ? equipment.getType() : null,
                estado,
                tonelaje,
                ley,
                valid,
                error);
    }

    // ---- Helpers de lectura y transformacion ----

    private String value(Map<String, String> row, String... aliases) {
        for (String a : aliases) {
            for (Map.Entry<String, String> e : row.entrySet()) {
                if (e.getKey().trim().toLowerCase().replaceAll("_", " ")
                        .equals(a.toLowerCase().replaceAll("_", " "))) {
                    String v = e.getValue() == null ? "" : e.getValue().trim();
                    if (!v.isEmpty()) return v;
                }
            }
        }
        return "";
    }

    private static String blank(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }

    private String normalizeCode(String raw) {
        return raw.toUpperCase().trim();
    }

    private String normalizeZona(String raw) {
        return raw.toUpperCase().trim();
    }

    private String normalizeTurno(String raw) {
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "A", "1", "DIA", "MAÑANA", "MANANA", "TURNO A" -> "A";
            case "B", "2", "TARDE", "TURNO B" -> "B";
            case "C", "3", "NOCHE", "TURNO C" -> "C";
            default -> null;
        };
    }

    private String normalizeEstado(String raw) {
        String v = raw.trim().toUpperCase();
        return switch (v) {
            case "OK", "OPERATIVO", "OPERANDO", "PRODUCCION", "EN_PROCESO", "EN PROCESO", "ACTIVO" -> "EN_PROCESO";
            case "DETENIDO", "DETENIDA", "MANTENCION", "MANTENIMIENTO", "INACTIVO", "STOP" -> "DETENIDO";
            default -> null;
        };
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.trim();
        for (String pattern : new String[]{"dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "yyyy/MM/dd", "dd.MM.yyyy"}) {
            try {
                return LocalDate.parse(v, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // probar siguiente patron
            }
        }
        return null;
    }

    /** Acepta "1.234,56", "1234.56", "12,34", espacios. Normaliza a punto decimal. */
    private BigDecimal parseNumber(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.replace(" ", "").replace("$", "").replace("%", "");
        boolean hasComma = v.contains(",");
        boolean hasDot = v.contains(".");
        String normalized;
        if (hasComma && hasDot) {
            if (v.indexOf('.') < v.indexOf(',')) {
                normalized = v.replace(".", "").replace(",", ".");
            } else {
                normalized = v.replace(",", "");
            }
        } else if (hasComma) {
            int commaCount = v.length() - v.replace(",", "").length();
            if (commaCount == 1) {
                normalized = v.replace(",", ".");
            } else {
                normalized = v.replace(",", "");
            }
        } else {
            normalized = v;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Para exponer el resultado de cruce tambien como Optional (uso en reporte). */
    public Optional<Equipment> findEquipment(Company company, String code) {
        return equipmentRepo.findByCompanyAndCode(company, code);
    }
}