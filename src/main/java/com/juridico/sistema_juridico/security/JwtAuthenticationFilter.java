package com.juridico.sistema_juridico.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
                                        
        // Si ya está autenticado previamente, lo dejamos pasar
        if (SecurityContextHolder.getContext().getAuthentication() != null && 
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // =========================================================
        // 0. NUEVO: REVISAR SI TRAE LA LLAVE MAESTRA (X-API-KEY)
        // =========================================================
        String apiKey = request.getHeader("X-API-KEY");
        if ("n8nAgendaLegalF1N4813NGDSA6D54".equals(apiKey)) {
            // Creamos un usuario "fantasma" con rol de ADMIN para n8n/ThunderClient
            UsernamePasswordAuthenticationToken apiAuth = new UsernamePasswordAuthenticationToken(
                    "n8n_system", 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ADMIN"))
            );
            // Lo registramos en el contexto de seguridad y lo dejamos pasar
            SecurityContextHolder.getContext().setAuthentication(apiAuth);
            filterChain.doFilter(request, response);
            return; // Detenemos la ejecución aquí, no hace falta buscar JWT
        }

        // =========================================================
        // 1. Obtener el token de la petición (Header Authorization)
        // =========================================================
        String jwt = getJwtFromRequest(request);

        // 2. Validar el token
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            String username = tokenProvider.getUsernameFromJWT(jwt);

            // 3. Cargar el usuario asociado al token
            UserDetails userDetails = username.contains("@")
                    ? userDetailsService.loadUserByEmail(username)
                    : userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 4. Establecer la autenticación en el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Intentar obtener del Header (para Postman o apps móviles)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Intentar obtener de las Cookies (para el Navegador/Thymeleaf)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
