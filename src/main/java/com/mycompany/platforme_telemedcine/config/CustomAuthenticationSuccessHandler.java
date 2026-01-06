package com.mycompany.platforme_telemedcine.config;

import com.mycompany.platforme_telemedcine.Models.CustomUserDetails;
import com.mycompany.platforme_telemedcine.Models.UserStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import java.io.IOException;

public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication ) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // This check preserves your user approval workflow!
        if (userDetails.getStatus() != UserStatus.APPROVED) {
            // If not approved, reject the login and redirect with a message
            getRedirectStrategy().sendRedirect(request, response, "/login?pending=true");
            return;
        }

        // If approved, redirect the user based on their role
        String targetUrl = determineTargetUrl(authentication);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                return "/admin/dashboard";
            } else if (authority.getAuthority().equals("ROLE_MEDECIN")) {
                return "/medecin/dashboard";
            } else if (authority.getAuthority().equals("ROLE_PATIENT")) {
                return "/patient/dashboard";
            }
        }
        // This should never happen if roles are set correctly
        throw new IllegalStateException("No role found for user, cannot determine target URL.");
    }
}
