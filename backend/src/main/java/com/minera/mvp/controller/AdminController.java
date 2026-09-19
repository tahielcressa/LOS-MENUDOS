package com.minera.mvp.controller;

import com.minera.mvp.dto.CreateUserRequest;
import com.minera.mvp.dto.EquipmentDto;
import com.minera.mvp.dto.EquipmentRequest;
import com.minera.mvp.dto.UserDto;
import com.minera.mvp.model.AppUser;
import com.minera.mvp.model.Company;
import com.minera.mvp.model.Equipment;
import com.minera.mvp.repo.CompanyRepository;
import com.minera.mvp.repo.EquipmentRepository;
import com.minera.mvp.repo.UserRepository;
import com.minera.mvp.security.PrincipalInfo;
import com.minera.mvp.service.BadRequestException;
import com.minera.mvp.service.CurrentUser;
import com.minera.mvp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository users;
    private final UserService userService;
    private final EquipmentRepository equipments;
    private final CompanyRepository companies;
    private final PasswordEncoder encoder;

    public AdminController(UserRepository users, UserService userService,
                           EquipmentRepository equipments, CompanyRepository companies,
                           PasswordEncoder encoder) {
        this.users = users;
        this.userService = userService;
        this.equipments = equipments;
        this.companies = companies;
        this.encoder = encoder;
    }

    private Company currentCompany() {
        PrincipalInfo p = CurrentUser.principal();
        return companies.findById(p.companyId()).orElseThrow();
    }

    @GetMapping("/company")
    public Map<String, Object> company() {
        Company c = currentCompany();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("code", c.getCode());
        m.put("name", c.getName());
        m.put("country", c.getCountry());
        m.put("users", users.findByCompanyOrderById(c).size());
        m.put("equipments", equipments.findByCompanyOrderByCode(c).size());
        return m;
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return users.findByCompanyOrderById(currentCompany()).stream().map(userService::toDto).toList();
    }

    @PostMapping("/users")
    public UserDto createUser(@Valid @RequestBody CreateUserRequest req) {
        Company company = currentCompany();
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new BadRequestException("Ya existe un usuario con ese email");
        if (!req.role().equalsIgnoreCase(AppUser.ROLE_ADMIN)
                && !req.role().equalsIgnoreCase(AppUser.ROLE_OPERATOR)) {
            throw new BadRequestException("Rol no valido (ADMIN u OPERATOR)");
        }
        AppUser u = new AppUser();
        u.setCompany(company);
        u.setEmail(email);
        u.setFullName(req.fullName());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(req.role().toUpperCase());
        return userService.toDto(users.save(u));
    }

    @GetMapping("/equipments")
    public List<EquipmentDto> equipments() {
        return equipments.findByCompanyOrderByCode(currentCompany()).stream()
                .map(e -> new EquipmentDto(e.getId(), e.getCode(), e.getName(), e.getArea(), e.getType(), e.isActive()))
                .toList();
    }

    @PostMapping("/equipments")
    public EquipmentDto createEquipment(@Valid @RequestBody EquipmentRequest req) {
        Company company = currentCompany();
        String code = req.code().trim().toUpperCase();
        if (equipments.findByCompanyAndCode(company, code).isPresent()) {
            throw new BadRequestException("El equipo " + code + " ya existe");
        }
        Equipment e = new Equipment();
        e.setCompany(company);
        e.setCode(code);
        e.setName(req.name());
        e.setArea(req.area());
        e.setType(req.type());
        var saved = equipments.save(e);
        return new EquipmentDto(saved.getId(), saved.getCode(), saved.getName(),
                saved.getArea(), saved.getType(), saved.isActive());
    }
}