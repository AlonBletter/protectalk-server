package com.protectalk.security.model;

import java.util.Map;

public record FirebasePrincipal(String uid,
                                String email,
                                Map<String, Object> claims) {

    public boolean hasRole(String role) {
        Object r = claims.get("role");
        return r != null && r.toString().equalsIgnoreCase(role);
    }
}
