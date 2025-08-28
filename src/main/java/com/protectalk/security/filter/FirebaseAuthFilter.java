package com.protectalk.security.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.protectalk.security.model.FirebasePrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.substring(7);
            try {
                FirebaseToken decoded = firebaseAuth.verifyIdToken(idToken, true);
                UserRecord userRecord = firebaseAuth.getUser(decoded.getUid());

                FirebasePrincipal principal = new FirebasePrincipal(
                        decoded.getUid(),
                        userRecord != null ? userRecord.getEmail() : decoded.getEmail(),
                        decoded.getClaims()
                );

                var authorities = authoritiesFromClaims(decoded.getClaims());

                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (FirebaseAuthException e) {
                System.err.println("Firebase token verification failed: " + e.getMessage());
                System.err.println("Error code: " + e.getAuthErrorCode());
                System.err.println("Error details: " + e.getErrorCode());

                // Provide specific guidance based on error type
                if (e.getAuthErrorCode() != null) {
                    switch (e.getAuthErrorCode().toString()) {
                        case "INVALID_ARGUMENT":
                            System.err.println("HINT: Token format is invalid. Check if you're sending the correct Firebase ID token.");
                            break;
                        case "EXPIRED_ID_TOKEN":
                            System.err.println("HINT: Token has expired. Get a fresh token from your client.");
                            break;
                        case "REVOKED_ID_TOKEN":
                            System.err.println("HINT: Token has been revoked. User needs to re-authenticate.");
                            break;
                        case "INVALID_ID_TOKEN":
                            System.err.println("HINT: Token signature/format is invalid. Check project ID mismatch.");
                            break;
                    }
                }

                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid Firebase token: " + e.getMessage() + "\"}");
                response.setContentType("application/json");
                return;
            } catch (Exception e) {
                System.err.println("Unexpected error during token verification: " + e.getMessage());
                e.printStackTrace();
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private Collection<GrantedAuthority> authoritiesFromClaims(Map<String, Object> claims) {
        List<GrantedAuthority> list = new ArrayList<>();
        Object role = claims.get("role");
        if (role != null) {
            list.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        // optional: handle claims like scopes[] or permissions[]
        return list;
    }
}