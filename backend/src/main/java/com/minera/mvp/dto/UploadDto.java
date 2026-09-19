package com.minera.mvp.dto;

public record UploadDto(
        Long id,
        String originalName,
        String status,
        int totalRows,
        int validRows,
        int invalidRows,
        long sizeBytes,
        String createdAt,
        String uploadedByName) {
}