package com.juridico.sistema_juridico.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String path = request.getRequestURI();

            // Allow static assets, error pages, logout functionality and the password
            // change endpoints
            if (userDetails.isForcePasswordChange() &&
                    !path.startsWith("/css/") &&
                    !path.startsWith("/js/") &&
                    !path.startsWith("/img/") &&
                    !path.startsWith("/webjars/") &&
                    !path.startsWith("/lib/") &&
                    !path.equals("/cambiar-password") &&
                    !path.equals("/logout") &&
                    !path.equals("/error")) {

                response.sendRedirect(request.getContextPath() + "/cambiar-password");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
