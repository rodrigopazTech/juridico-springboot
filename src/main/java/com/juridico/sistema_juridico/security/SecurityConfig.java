package com.juridico.sistema_juridico.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de Seguridad para el Sistema Jurídico
 * 
 * Esta configuración está OPTIMIZADA PARA DESARROLLO.
 * Permite acceso abierto a todos los endpoints para facilitar el desarrollo.
 * 
 * TODO: Configurar roles y permisos antes de producción
 * TODO: Implementar JWT filter chain
 * TODO: Habilitar CSRF para producción
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Bean de Password Encoder usando BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuración de la cadena de filtros de seguridad
     * CONFIGURACIÓN DE DESARROLLO - TODO: Ajustar para producción
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF para desarrollo (habilitar en producción)
            .csrf(csrf -> csrf.disable())
            
            // Configurar CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configurar manejo de sesiones (stateless para API REST)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configurar autorización de requests
            .authorizeHttpRequests(authz -> authz
                // Swagger/OpenAPI - público
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                
                // Endpoints de autenticación - público
                .requestMatchers("/api/auth/**").permitAll()
                
                // Recursos estáticos - público
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                
                // Health check - público
                .requestMatchers("/actuator/health").permitAll()
                
                // TODO: Configurar roles específicos antes de producción
                // Por ahora DESARROLLO: Todos los endpoints son públicos
                .anyRequest().permitAll()
                
                /* CONFIGURACIÓN PARA PRODUCCIÓN (descomentar):
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/gerencia/**").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers("/api/usuario/**").hasAnyRole("ADMIN", "GERENTE", "USUARIO")
                .anyRequest().authenticated()
                */
            );

        return http.build();
    }

    /**
     * Configuración de CORS para permitir requests desde el frontend
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Orígenes permitidos (frontend)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Headers permitidos
        configuration.setAllowedHeaders(List.of("*"));
        
        // Permitir credenciales (cookies, tokens)
        configuration.setAllowCredentials(true);
        
        // Headers expuestos al frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        // Tiempo de cache de preflight request
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}