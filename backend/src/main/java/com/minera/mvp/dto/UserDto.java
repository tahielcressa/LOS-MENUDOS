package com.minera.mvp.dto;

public record UserDto(Long id, String email, String fullName, String role, Long companyId, String companyName) {
}