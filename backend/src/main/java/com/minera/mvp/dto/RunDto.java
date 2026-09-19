package com.minera.mvp.dto;

public record RunDto(
        Long id,
        Long uploadId,
        String originalName,
        String status,
        String runType,
        int totalRows,
        int validRows,
        int invalidRows,
        Double totalTonnage,
        Double avgGrade,
        String kpis,
        String logs,
        String excelFileName,
        String pdfFileName,
        Long durationMs,
        String executedByName,
        String startedAt,
        String finishedAt) {
}