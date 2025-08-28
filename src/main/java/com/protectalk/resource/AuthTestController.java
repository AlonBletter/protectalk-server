package com.protectalk.resource;

import com.protectalk.security.model.FirebasePrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class AuthTestController {

    @GetMapping("/auth")
    public Map<String, Object> testAuth(@AuthenticationPrincipal FirebasePrincipal principal) {
        if (principal == null) {
            return Map.of("status", "error", "message", "No authentication found");
        }
        
        return Map.of(
            "status", "success",
            "uid", principal.uid(),
            "email", principal.email(),
            "claims", principal.claims()
        );
    }
}
