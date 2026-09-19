package com.minera.mvp.service;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsea archivos CSV (coma o punto y coma) y Excel (.xlsx/.xls) a filas con encabezados.
 */
@Service
public class FileParserService {

    public record ParsedFile(String fileName, List<String> headers, List<Map<String, String>> rows) {
        public int totalRows() {
            return rows.size();
        }
    }

    public ParsedFile parse(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return parse(file.getOriginalFilename(), in);
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    /** Version sin MultipartFile: permite releer archivos guardados en disco por nombre. */
    public ParsedFile parse(String fileName, InputStream in) {
        String name = fileName == null ? "" : fileName.toLowerCase();

        if (name.endsWith(".csv")) {
            return parseCsv(fileName, in);
        }
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return parseExcel(fileName, in);
        }
        throw new BadRequestException("Formato no soportado. Usa CSV o Excel (.xlsx)");
    }

    private ParsedFile parseCsv(String fileName, InputStream in) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) lines.add(line);
            }
            if (lines.isEmpty()) {
                throw new BadRequestException("El archivo CSV esta vacio");
            }

            String delimiter = detectDelimiter(lines.getFirst());
            List<String> headers = splitCsv(lines.getFirst(), delimiter);
            if (headers.stream().allMatch(String::isBlank)) {
                throw new BadRequestException("El archivo no tiene encabezados validos");
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                List<String> values = splitCsv(lines.get(i), delimiter);
                Map<String, String> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    String key = headers.get(c).trim();
                    String val = c < values.size() ? values.get(c).trim() : "";
                    map.put(key, val);
                }
                if (map.values().stream().anyMatch(v -> !v.isEmpty())) {
                    rows.add(map);
                }
            }
            if (rows.isEmpty()) {
                throw new BadRequestException("El CSV no tiene filas de datos");
            }
            return new ParsedFile(fileName, headers, rows);

        } catch (BadRequestException bex) {
            throw bex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo leer el CSV: " + ex.getMessage());
        }
    }

    private ParsedFile parseExcel(String fileName, InputStream in) {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new BadRequestException("El Excel no tiene hojas");

            DataFormatter fmt = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) throw new BadRequestException("El Excel no tiene encabezados");

            List<String> headers = new ArrayList<>();
            for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
                headers.add(fmt.formatCellValue(headerRow.getCell(c)).trim());
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                boolean empty = true;
                for (int c = 0; c < headers.size(); c++) {
                    String val = fmt.formatCellValue(row.getCell(c)).trim();
                    if (!val.isEmpty()) empty = false;
                    map.put(headers.get(c), val);
                }
                if (!empty) rows.add(map);
            }
            if (rows.isEmpty()) throw new BadRequestException("El Excel no tiene filas de datos");
            return new ParsedFile(fileName, headers, rows);

        } catch (BadRequestException bex) {
            throw bex;
        } catch (Exception ex) {
            throw new BadRequestException("No se pudo leer el Excel: " + ex.getMessage());
        }
    }

    /** Detecta si el CSV usa ";" o "," como separador segun la primera linea. */
    private String detectDelimiter(String firstLine) {
        int semis = countOutsideQuotes(firstLine, ';');
        int commas = countOutsideQuotes(firstLine, ',');
        return semis > commas ? ";" : ",";
    }

    private int countOutsideQuotes(String line, char target) {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') inQuotes = !inQuotes;
            else if (ch == target && !inQuotes) count++;
        }
        return count;
    }

    /** Divide una linea CSV respetando comillas dobles (estado simple y robusto). */
    private List<String> splitCsv(String line, String delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char sep = delimiter.charAt(0);

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == sep && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString().trim());
        return result;
    }
}