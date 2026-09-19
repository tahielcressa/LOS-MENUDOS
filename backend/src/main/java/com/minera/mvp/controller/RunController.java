package com.minera.mvp.controller;

import com.minera.mvp.dto.RunDto;
import com.minera.mvp.model.Company;
import com.minera.mvp.model.ProcessRun;
import com.minera.mvp.repo.CompanyRepository;
import com.minera.mvp.security.PrincipalInfo;
import com.minera.mvp.service.BadRequestException;
import com.minera.mvp.service.CurrentUser;
import com.minera.mvp.service.UploadService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final UploadService uploadService;
    private final CompanyRepository companies;

    public RunController(UploadService uploadService, CompanyRepository companies) {
        this.uploadService = uploadService;
        this.companies = companies;
    }

    private Company currentCompany() {
        PrincipalInfo p = CurrentUser.principal();
        return companies.findById(p.companyId()).orElseThrow();
    }

    @GetMapping
    public List<RunDto> listRuns() {
        return uploadService.runs(currentCompany()).stream().map(uploadService::toRunDto).toList();
    }

    @GetMapping("/{id}")
    public RunDto detail(@PathVariable Long id) {
        return uploadService.toRunDto(uploadService.getRunOwned(id, currentCompany()));
    }

    @GetMapping("/{id}/download/{type}")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable String type) {
        ProcessRun run = uploadService.getRunOwned(id, currentCompany());
        Path dir = uploadService.runDir(run);

        String fileName;
        switch (type.toLowerCase()) {
            case "excel", "xlsx" -> {
                if (run.getExcelFileName() == null) {
                    throw new BadRequestException("No hay Excel para este proceso");
                }
                fileName = run.getExcelFileName();
            }
            case "pdf" -> {
                if (run.getPdfFileName() == null) {
                    throw new BadRequestException("No hay PDF para este proceso");
                }
                fileName = run.getPdfFileName();
            }
            default -> throw new BadRequestException("Tipo de archivo no valido (excel o pdf)");
        }

        Path file = dir.resolve(fileName);
        if (!Files.exists(file)) {
            throw new BadRequestException("El archivo ya no existe en el servidor");
        }

        Resource resource = new FileSystemResource(file);
        String contentType = type.equalsIgnoreCase("pdf") ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}