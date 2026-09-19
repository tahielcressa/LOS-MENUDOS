package com.minera.mvp.service;

import com.minera.mvp.dto.RunDto;
import com.minera.mvp.dto.UploadDto;
import com.minera.mvp.model.AppUser;
import com.minera.mvp.model.Company;
import com.minera.mvp.model.ProcessRun;
import com.minera.mvp.model.Upload;
import com.minera.mvp.repo.ProcessRunRepository;
import com.minera.mvp.repo.UploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final UploadRepository uploadRepo;
    private final ProcessRunRepository runRepo;
    private final ProcessService processService;
    private final Path uploadsDir;

    public UploadService(UploadRepository uploadRepo, ProcessRunRepository runRepo,
                         ProcessService processService,
                         @Value("${app.storage.output:./outputs}") String outputDir) {
        this.uploadRepo = uploadRepo;
        this.runRepo = runRepo;
        this.processService = processService;
        this.uploadsDir = Path.of(outputDir).resolve("uploads");
    }

    public Upload store(Company company, AppUser user, MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "archivo" : file.getOriginalFilename();
        if (file.isEmpty()) throw new BadRequestException("El archivo esta vacio");

        try {
            Files.createDirectories(uploadsDir);
            Path dest = uploadsDir.resolve(System.currentTimeMillis() + "_" + sanitize(name));
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            Upload upload = new Upload();
            upload.setCompany(company);
            upload.setUploadedBy(user);
            upload.setOriginalName(name);
            upload.setStoredPath(dest.toString());
            upload.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            upload.setSizeBytes(file.getSize());
            upload.setStatus(Upload.STATUS_RECEIVED);
            return uploadRepo.save(upload);

        } catch (IOException ex) {
            log.error("No se pudo guardar el archivo", ex);
            throw new BadRequestException("No se pudo guardar el archivo: " + ex.getMessage());
        }
    }

    public List<Upload> list(Company company) {
        return uploadRepo.findByCompanyOrderByCreatedAtDesc(company);
    }

    public long countUploads(Company company) {
        return uploadRepo.findByCompanyOrderByCreatedAtDesc(company).size();
    }

    public Upload getOwned(Long id, Company company) {
        return uploadRepo.findById(id)
                .filter(u -> u.getCompany().getId().equals(company.getId()))
                .orElseThrow(() -> new BadRequestException("Subida no encontrada"));
    }

    public ProcessRun run(Upload upload, AppUser user) {
        return processService.process(upload, user);
    }

    public List<ProcessRun> runs(Company company) {
        return runRepo.findByCompanyOrderByStartedAtDesc(company);
    }

    public ProcessRun getRunOwned(Long id, Company company) {
        return runRepo.findById(id)
                .filter(r -> r.getCompany().getId().equals(company.getId()))
                .orElseThrow(() -> new BadRequestException("Proceso no encontrado"));
    }

    public Path runDir(ProcessRun run) {
        return uploadsDir.getParent().resolve("runs").resolve(String.valueOf(run.getId()));
    }

    public UploadDto toDto(Upload u) {
        return new UploadDto(
                u.getId(), u.getOriginalName(), u.getStatus(),
                u.getTotalRows(), u.getValidRows(), u.getInvalidRows(),
                u.getSizeBytes(),
                u.getCreatedAt() == null ? "" : u.getCreatedAt().toString(),
                u.getUploadedBy() == null ? "" : u.getUploadedBy().getFullName());
    }

    public RunDto toRunDto(ProcessRun r) {
        Double ton = r.getTotalTonnage() == null ? null : r.getTotalTonnage().setScale(2, RoundingMode.HALF_UP).doubleValue();
        Double avg = r.getAvgGrade() == null ? null : r.getAvgGrade().setScale(2, RoundingMode.HALF_UP).doubleValue();
        return new RunDto(
                r.getId(),
                r.getUpload() == null ? null : r.getUpload().getId(),
                r.getUpload() == null ? "" : r.getUpload().getOriginalName(),
                r.getStatus(),
                r.getRunType(),
                r.getTotalRows(), r.getValidRows(), r.getInvalidRows(),
                ton, avg,
                r.getKpis(),
                r.getLogs(),
                r.getExcelFileName(),
                r.getPdfFileName(),
                r.getDurationMs(),
                r.getExecutedBy() == null ? "" : r.getExecutedBy().getFullName(),
                r.getStartedAt() == null ? "" : r.getStartedAt().toString(),
                r.getFinishedAt() == null ? null : r.getFinishedAt().toString());
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}