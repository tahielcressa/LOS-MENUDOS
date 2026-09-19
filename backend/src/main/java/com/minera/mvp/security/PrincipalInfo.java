package com.minera.mvp.security;

public record PrincipalInfo(Long userId, String email, Long companyId, String role, String fullName) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}