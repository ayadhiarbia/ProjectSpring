package com.mycompany.platforme_telemedcine.Controllers;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestAuthController {

    private final AuthenticationProvider authenticationProvider;

    public TestAuthController(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    @GetMapping("/test-auth")
    public String testAuthentication() {
        try {
            Authentication authRequest = new UsernamePasswordAuthenticationToken(
                    "patient@gmail.com",
                    "patient01"
            );

            Authentication authResult = authenticationProvider.authenticate(authRequest);

            return String.format(
                    "✅ Authentication SUCCESSFUL!<br>" +
                            "Authenticated: %s<br>" +
                            "Principal: %s<br>" +
                            "Authorities: %s",
                    authResult.isAuthenticated(),
                    authResult.getPrincipal().getClass().getName(),
                    authResult.getAuthorities()
            );

        } catch (Exception e) {
            return "❌ Authentication FAILED: " + e.getMessage();
        }
    }
}