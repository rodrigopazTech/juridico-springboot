package com.juridico.sistema_juridico.security;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /** Política: la contraseña vence a los 3 meses (92 días). */
    private static final int DIAS_VIGENCIA = 92;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String path = request.getRequestURI();

            // Rutas siempre permitidas (assets estáticos, logout, cambio de contraseña)
            // IMPORTANTE: /perfil/password/actualizar debe estar aquí para que usuarios con
            // contraseña vencida puedan cambiarla también desde el apartado de Seguridad.
            boolean esRutaPermitida = path.startsWith("/css/")
                    || path.startsWith("/js/")
                    || path.startsWith("/img/")
                    || path.startsWith("/fonts/")
                    || path.startsWith("/webjars/")
                    || path.startsWith("/lib/")
                    || path.equals("/cambiar-password")
                    || path.startsWith("/perfil/password/") // ← permite el POST de cambio desde perfil
                    || path.equals("/logout")
                    || path.equals("/error");

            if (esRutaPermitida) {
                filterChain.doFilter(request, response);
                return;
            }

            // --- A. Cambio OBLIGATORIO por admin (primera vez) ---
            if (userDetails.isForcePasswordChange()) {
                response.sendRedirect(request.getContextPath() + "/cambiar-password?razon=primera-vez");
                return;
            }

            // --- B. Contraseña VENCIDA por tiempo (> 3 meses) ---
            Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (usuario != null) {
                LocalDateTime referencia = (usuario.getPasswordUpdatedAt() != null)
                        ? usuario.getPasswordUpdatedAt()
                        : (usuario.getCreatedAt() != null ? usuario.getCreatedAt() : null);

                if (referencia != null) {
                    boolean estaVencida = referencia.plusDays(DIAS_VIGENCIA).isBefore(LocalDateTime.now());
                    if (estaVencida) {
                        response.sendRedirect(request.getContextPath() + "/cambiar-password?razon=vencida");
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
