package com.minera.mvp.repo;

import com.minera.mvp.model.Company;
import com.minera.mvp.model.ProcessRun;
import com.minera.mvp.model.ResultRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultRowRepository extends JpaRepository<ResultRow, Long> {
    List<ResultRow> findByRun(ProcessRun run);
    List<ResultRow> findTop500ByCompanyOrderByIdDesc(Company company);
}