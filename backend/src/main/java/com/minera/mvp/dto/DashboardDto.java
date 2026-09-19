package com.minera.mvp.dto;

import java.util.List;

public record DashboardDto(
        long totalUploads,
        long totalRuns,
        int totalRowsProcessed,
        int validRows,
        int invalidRows,
        Double totalTonnage,
        Double avgGrade,
        List<NameValue> perZone,
        List<NameValue> perTurno,
        List<NameValue> perEstado,
        List<RunDto> recentRuns) {

    public record NameValue(String name, double value) {
    }
}