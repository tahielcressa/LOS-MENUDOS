package com.minera.mvp.controller;

import com.minera.mvp.dto.DashboardDto;
import com.minera.mvp.model.Company;
import com.minera.mvp.repo.CompanyRepository;
import com.minera.mvp.security.PrincipalInfo;
import com.minera.mvp.service.CurrentUser;
import com.minera.mvp.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CompanyRepository companies;

    public DashboardController(DashboardService dashboardService, CompanyRepository companies) {
        this.dashboardService = dashboardService;
        this.companies = companies;
    }

    @GetMapping
    public DashboardDto dashboard() {
        PrincipalInfo p = CurrentUser.principal();
        Company company = companies.findById(p.companyId()).orElseThrow();
        return dashboardService.build(company);
    }
}