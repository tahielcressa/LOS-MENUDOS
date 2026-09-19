package com.minera.mvp.service;

import com.minera.mvp.dto.AuthRequest;
import com.minera.mvp.dto.AuthResponse;
import com.minera.mvp.dto.RegisterRequest;
import com.minera.mvp.dto.UserDto;
import com.minera.mvp.model.AppUser;
import com.minera.mvp.model.Company;
import com.minera.mvp.repo.CompanyRepository;
import com.minera.mvp.repo.UserRepository;
import com.minera.mvp.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public UserService(UserRepository users, CompanyRepository companies,
                       PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.companies = companies;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse login(AuthRequest req) {
        AppUser user = users.findByEmail(req.email().trim().toLowerCase())
                .filter(AppUser::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }
        return toResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }
        if (companies.findByCode(req.companyCode().trim().toUpperCase()).isPresent()) {
            throw new BadRequestException("Codigo de empresa ya en uso");
        }

        Company company = new Company();
        company.setCode(req.companyCode().trim().toUpperCase());
        company.setName(req.companyName().trim());
        company.setCountry(req.country());
        companies.save(company);

        AppUser admin = new AppUser();
        admin.setEmail(email);
        admin.setFullName(req.fullName());
        admin.setPasswordHash(encoder.encode(req.password()));
        admin.setRole(AppUser.ROLE_ADMIN);
        admin.setCompany(company);
        users.save(admin);

        return toResponse(admin);
    }

    @Transactional(readOnly = true)
    public UserDto toDto(AppUser user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.getCompany().getId(), user.getCompany().getName());
    }

    private AuthResponse toResponse(AppUser user) {
        String token = jwt.generateToken(user.getId(), user.getEmail(),
                user.getCompany().getId(), user.getRole(), user.getFullName());
        return new AuthResponse(token, toDto(user));
    }
}