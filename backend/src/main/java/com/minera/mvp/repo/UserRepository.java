package com.minera.mvp.repo;

import com.minera.mvp.model.AppUser;
import com.minera.mvp.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    List<AppUser> findByCompanyOrderById(Company company);
}