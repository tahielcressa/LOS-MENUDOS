package com.minera.mvp.service;

import com.minera.mvp.security.PrincipalInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static PrincipalInfo principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PrincipalInfo p) {
            return p;
        }
        throw new IllegalStateException("Usuario no autenticado");
    }
}