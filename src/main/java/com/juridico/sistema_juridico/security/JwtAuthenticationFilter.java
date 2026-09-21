package com.juridico.sistema_juridico.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${migration.api.key:}")
    private String migrationApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
                                        
        // Las sesiones existentes también deben respetar una suspensión administrativa.
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!"n8n_system".equals(currentUser)) {
                var currentUsuario = usuarioRepository.findByEmail(currentUser);
                if (currentUsuario.isEmpty() || !Boolean.TRUE.equals(currentUsuario.get().getActivo())) {
                    rejectAccess(request, response);
                    return;
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        // =========================================================
        // 0. NUEVO: REVISAR SI TRAE LA LLAVE MAESTRA (X-API-KEY)
        // =========================================================
        String apiKey = request.getHeader("X-API-KEY");
        if (esApiKeyValida(apiKey)) {
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

            var usuario = usuarioRepository.findByEmail(username).orElse(null);
            if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())
                    || tokenProvider.getSecurityVersionFromJWT(jwt)
                    != (usuario.getSecurityVersion() == null ? 0 : usuario.getSecurityVersion())) {
                rejectAccess(request, response);
                return;
            }

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

    private void rejectAccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        Cookie jwtCookie = new Cookie("JWT_TOKEN", "");
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        jwtCookie.setHttpOnly(true);
        response.addCookie(jwtCookie);
        if (request.getRequestURI().startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "La sesion ya no es valida.");
        } else {
            response.sendRedirect("/?disabled=true");
        }
    }

    private boolean esApiKeyValida(String apiKey) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(migrationApiKey)) {
            return false;
        }
        return MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                migrationApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
