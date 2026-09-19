package com.minera.mvp.service;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera reportes Excel (.xlsx) y PDF con el detalle validado y los KPIs del proceso.
 */
@Service
public class ReportService {

    private static final String[] EXCEL_HEADERS = {
            "Fila", "Fecha", "Turno", "Zona", "Equipo", "NombreEquipo", "Area", "Tipo",
            "Estado", "Tonelaje", "LeyCu", "Validacion", "Errores"};

    public record ReportOutput(Path excelPath, String excelName, Path pdfPath, String pdfName) {
    }

    public ReportOutput generate(Path dir, String companyName, String originalName,
                                 List<ValidationAndTransformService.ValidatedRow> rows,
                                 Map<String, Object> kpis) throws IOException {
        String base = sanitize(originalName);
        Path excel = dir.resolve(base + "_resultado.xlsx");
        Path pdf = dir.resolve(base + "_reporte.pdf");

        writeExcel(excel, companyName, originalName, rows, kpis);
        writePdf(pdf, companyName, originalName, rows, kpis);

        return new ReportOutput(excel, excel.getFileName().toString(), pdf, pdf.getFileName().toString());
    }

    private void writeExcel(Path file, String companyName, String originalName,
                            List<ValidationAndTransformService.ValidatedRow> rows,
                            Map<String, Object> kpis) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet detail = wb.createSheet("Resultados");
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font hf = wb.createFont();
            hf.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            hf.setBold(true);
            headerStyle.setFont(hf);

            Row header = detail.createRow(0);
            for (int c = 0; c < EXCEL_HEADERS.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(EXCEL_HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }
            detail.createFreezePane(0, 1);

            CellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setFont(wb.createFont());

            int r = 1;
            for (ValidationAndTransformService.ValidatedRow v : rows) {
                Row row = detail.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(v.rowNumber());
                row.createCell(c++).setCellValue(v.fecha() == null ? "" : v.fecha().toString());
                row.createCell(c++).setCellValue(v.turno() == null ? "" : v.turno());
                row.createCell(c++).setCellValue(v.zona() == null ? "" : v.zona());
                row.createCell(c++).setCellValue(v.equipmentCode() == null ? "" : v.equipmentCode());
                row.createCell(c++).setCellValue(v.equipmentName() == null ? "" : v.equipmentName());
                row.createCell(c++).setCellValue(v.equipmentArea() == null ? "" : v.equipmentArea());
                row.createCell(c++).setCellValue(v.equipoTipo() == null ? "" : v.equipoTipo());
                row.createCell(c++).setCellValue(v.estado() == null ? "" : v.estado());
                if (v.tonelaje() != null) row.createCell(c).setCellValue(v.tonelaje().doubleValue());
                c++;
                if (v.leyCu() != null) row.createCell(c).setCellValue(v.leyCu().doubleValue());
                c++;
                row.createCell(c++).setCellValue(v.valid() ? "OK" : "INVALIDO");
                row.createCell(c++).setCellValue(v.error() == null ? "" : v.error());
                for (int col = 0; col < c; col++) row.getCell(col).setCellStyle(bodyStyle);
            }
            for (int c = 0; c < EXCEL_HEADERS.length; c++) detail.autoSizeColumn(c);

            Sheet kpiSheet = wb.createSheet("KPIs");
            int kr = 0;
            kpiSheet.createRow(kr++).createCell(0).setCellValue("Resumen de proceso");
            row(kpiSheet, kr++, "Empresa", companyName);
            row(kpiSheet, kr++, "Archivo original", originalName);
            row(kpiSheet, kr++, "Fecha proceso", LocalDate.now().toString());
            row(kpiSheet, kr++, "Total filas", String.valueOf(kpis.get("totalRows")));
            row(kpiSheet, kr++, "Validas", String.valueOf(kpis.get("validRows")));
            row(kpiSheet, kr++, "Invalidas", String.valueOf(kpis.get("invalidRows")));
            row(kpiSheet, kr++, "Tonelaje total (t)", num(kpis.get("totalTonnage")));
            row(kpiSheet, kr++, "Ley media de cobre (%)", num(kpis.get("avgGrade")));

            kr++;
            row(kpiSheet, kr++, "Tonelaje por zona", "");
            for (Map.Entry<String, Object> e : safeMap(kpis.get("perZone")).entrySet()) {
                row(kpiSheet, kr++, "  " + e.getKey(), num(((Number) e.getValue()).doubleValue()));
            }
            kr++;
            row(kpiSheet, kr++, "Tonelaje por turno", "");
            for (Map.Entry<String, Object> e : safeMap(kpis.get("perTurno")).entrySet()) {
                row(kpiSheet, kr++, "  " + e.getKey(), num(((Number) e.getValue()).doubleValue()));
            }
            kr++;
            row(kpiSheet, kr++, "Filas por estado", "");
            for (Map.Entry<String, Object> e : safeMap(kpis.get("perEstado")).entrySet()) {
                row(kpiSheet, kr++, "  " + e.getKey(), String.valueOf(((Number) e.getValue()).longValue()));
            }

            try (FileOutputStream out = new FileOutputStream(file.toFile())) {
                wb.write(out);
            }
        }
    }

    private void row(Sheet sheet, int r, String label, String value) {
        sheet.createRow(r).createCell(0).setCellValue(label);
        if (value != null) sheet.getRow(r).createCell(1).setCellValue(value);
    }

    private Map<String, Object> safeMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return Map.of();
    }

    private String num(Object o) {
        if (o instanceof Double d) return String.format("%.2f", d);
        if (o instanceof BigDecimal bd) return bd.toPlainString();
        if (o instanceof Number n) return n.toString();
        return String.valueOf(o);
    }

    private void writePdf(Path file, String companyName, String originalName,
                          List<ValidationAndTransformService.ValidatedRow> rows,
                          Map<String, Object> kpis) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        try (FileOutputStream out = new FileOutputStream(file.toFile())) {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Reporte de proceso - " + companyName,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            doc.add(new Paragraph("Archivo: " + originalName + "   |   Generado: " + LocalDate.now(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9)));
            doc.add(new Paragraph(" "));

            PdfPTable sum = new PdfPTable(8);
            sum.setWidthPercentage(100);
            sum.addCell(kpiCell("Total filas", String.valueOf(kpis.get("totalRows"))));
            sum.addCell(kpiCell("Validas", String.valueOf(kpis.get("validRows"))));
            sum.addCell(kpiCell("Invalidas", String.valueOf(kpis.get("invalidRows"))));
            sum.addCell(kpiCell("Tonelaje (t)", num(kpis.get("totalTonnage"))));
            sum.addCell(kpiCell("Ley media Cu (%)", num(kpis.get("avgGrade"))));
            sum.addCell(kpiCell("Zonas", String.valueOf(safeMap(kpis.get("perZone")).size())));
            sum.addCell(kpiCell("Turnos", String.valueOf(safeMap(kpis.get("perTurno")).size())));
            sum.addCell(kpiCell("Estados", String.valueOf(safeMap(kpis.get("perEstado")).size())));
            doc.add(sum);
            doc.add(new Paragraph(" "));

            PdfPTable detail = new PdfPTable(8);
            detail.setWidthPercentage(100);
            String[] headers = {"Fila", "Fecha", "Turno", "Zona", "Equipo", "Tonelaje", "LeyCu", "Validacion"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                cell.setGrayFill(0.9f);
                detail.addCell(cell);
            }
            for (ValidationAndTransformService.ValidatedRow v : rows) {
                if (!v.valid()) continue;
                detail.addCell(String.valueOf(v.rowNumber()));
                detail.addCell(v.fecha() == null ? "-" : v.fecha().toString());
                detail.addCell(v.turno() == null ? "-" : v.turno());
                detail.addCell(v.zona() == null ? "-" : v.zona());
                detail.addCell(v.equipmentCode() == null ? "-" : v.equipmentCode());
                detail.addCell(v.tonelaje() == null ? "-" : num(v.tonelaje()));
                detail.addCell(v.leyCu() == null ? "-" : num(v.leyCu()));
                detail.addCell("OK");
            }
            doc.add(detail);
            doc.close();
        }
    }

    private PdfPCell kpiCell(String label, String value) {
        PdfPTable inner = new PdfPTable(1);
        inner.addCell(new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8))));
        inner.addCell(new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 9))));
        return new PdfPCell(inner);
    }

    private String num(BigDecimal bd) {
        return bd == null ? "-" : bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String sanitize(String name) {
        String base = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        int dot = base.lastIndexOf('.');
        return dot > 0 ? base.substring(0, dot) : base;
    }
}