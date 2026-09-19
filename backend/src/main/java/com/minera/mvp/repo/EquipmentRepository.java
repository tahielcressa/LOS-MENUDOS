package com.minera.mvp.repo;

import com.minera.mvp.model.Company;
import com.minera.mvp.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByCompanyOrderByCode(Company company);
    Optional<Equipment> findByCompanyAndCode(Company company, String code);
}