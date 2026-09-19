package com.minera.mvp.controller;

import com.minera.mvp.dto.RunDto;
import com.minera.mvp.dto.UploadDto;
import com.minera.mvp.model.AppUser;
import com.minera.mvp.model.Company;
import com.minera.mvp.model.ProcessRun;
import com.minera.mvp.model.Upload;
import com.minera.mvp.repo.CompanyRepository;
import com.minera.mvp.repo.UserRepository;
import com.minera.mvp.security.PrincipalInfo;
import com.minera.mvp.service.CurrentUser;
import com.minera.mvp.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final UploadService uploadService;
    private final CompanyRepository companies;
    private final UserRepository users;

    public UploadController(UploadService uploadService, CompanyRepository companies, UserRepository users) {
        this.uploadService = uploadService;
        this.companies = companies;
        this.users = users;
    }

    private AppUser currentUser() {
        PrincipalInfo p = CurrentUser.principal();
        return users.findById(p.userId()).orElseThrow();
    }

    private Company currentCompany() {
        PrincipalInfo p = CurrentUser.principal();
        return companies.findById(p.companyId()).orElseThrow();
    }

    @PostMapping("/uploads")
    public UploadDto upload(@RequestParam("file") MultipartFile file) {
        Upload upload = uploadService.store(currentCompany(), currentUser(), file);
        return uploadService.toDto(upload);
    }

    @GetMapping("/uploads")
    public List<UploadDto> listUploads() {
        return uploadService.list(currentCompany()).stream().map(uploadService::toDto).toList();
    }

    @PostMapping("/uploads/{id}/run")
    public RunDto run(@PathVariable Long id) {
        Upload upload = uploadService.getOwned(id, currentCompany());
        ProcessRun run = uploadService.run(upload, currentUser());
        return uploadService.toRunDto(run);
    }
}