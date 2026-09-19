package com.minera.mvp.repo;

import com.minera.mvp.model.Company;
import com.minera.mvp.model.Upload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadRepository extends JpaRepository<Upload, Long> {
    List<Upload> findByCompanyOrderByCreatedAtDesc(Company company);
}