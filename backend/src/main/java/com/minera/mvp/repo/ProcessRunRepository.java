package com.minera.mvp.repo;

import com.minera.mvp.model.Company;
import com.minera.mvp.model.ProcessRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessRunRepository extends JpaRepository<ProcessRun, Long> {
    List<ProcessRun> findByCompanyOrderByStartedAtDesc(Company company);
    List<ProcessRun> findTop5ByCompanyOrderByStartedAtDesc(Company company);
}