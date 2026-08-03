package com.juridico.sistema_juridico.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.juridico.sistema_juridico.security.CustomUserDetailsService;
import com.juridico.sistema_juridico.security.ForcePasswordChangeFilter;
import com.juridico.sistema_juridico.security.JwtAuthenticationEntryPoint;
import com.juridico.sistema_juridico.security.JwtAuthenticationFilter; // <-- Importación necesaria
import com.juridico.sistema_juridico.security.JwtTokenProvider;
import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final ForcePasswordChangeFilter forcePasswordChangeFilter;
    private final com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository usuarioRepository;
    private final com.juridico.sistema_juridico.service.AuditoriaService auditoriaService;
    private final AuditoriaTecnicaService auditoriaTecnicaService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            ForcePasswordChangeFilter forcePasswordChangeFilter,
            com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository usuarioRepository,
            com.juridico.sistema_juridico.service.AuditoriaService auditoriaService,
            AuditoriaTecnicaService auditoriaTecnicaService,
            JwtTokenProvider jwtTokenProvider) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.forcePasswordChangeFilter = forcePasswordChangeFilter;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.auditoriaTecnicaService = auditoriaTecnicaService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                jwtAuthenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            auditoriaTecnicaService.registrarAccesoDenegado(request, accessDeniedException.getMessage());
                            if (isJsonRequest(request)) {
                                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Acceso denegado\"}");
                            } else {
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
                            }
                        }))
                .authorizeHttpRequests(auth -> auth
                        // 1. Recursos Públicos
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**", "/lib/**", "/manifest.json",
                                "/sw.js")
                        .permitAll()
                        .requestMatchers("/", "/login", "/register", "/auth/**", "/api/auth/**", "/error", "/cambiar-password",
                                "/ws-realtime/**")
                        .permitAll()

                        // 2. NIVEL ALTO
                        .requestMatchers("/agenda", "/agenda/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/catalogos/**")
                        .hasAnyAuthority("ADMIN", "DIRECTOR_PRINCIPAL", "DIRECTOR", "SUBDIRECTOR", "GERENTE", "JEFE_DEPTO")
                        .requestMatchers(HttpMethod.DELETE, "/catalogos/**")
                        .hasAnyAuthority("ADMIN", "DIRECTOR_PRINCIPAL", "DIRECTOR", "SUBDIRECTOR", "GERENTE", "JEFE_DEPTO")
                        .requestMatchers(HttpMethod.GET, "/catalogos/*/toggle/**")
                        .hasAnyAuthority("ADMIN", "DIRECTOR_PRINCIPAL", "DIRECTOR", "SUBDIRECTOR", "GERENTE", "JEFE_DEPTO")
                        .requestMatchers("/catalogos", "/catalogos/**")
                        .hasAnyAuthority("ADMIN", "DIRECTOR_PRINCIPAL", "DIRECTOR", "SUBDIRECTOR", "GERENTE", "JEFE_DEPTO", "ABOGADO",
                                "PERM_CATALOGOS_VER")
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/usuarios", "/usuarios/**")
                        .hasAnyAuthority("ADMIN", "DIRECTOR_PRINCIPAL", "DIRECTOR", "SUBDIRECTOR", "GERENTE", "JEFE_DEPTO")

                        // 3. NIVEL MEDIO
                        .requestMatchers("/dashboard", "/dashboard/**")
                        .hasAnyAuthority("ADMIN", "DIRECTOR_PRINCIPAL", "DIRECTOR", "SUBDIRECTOR", "GERENTE", "JEFE_DEPTO", "ABOGADO")

                        // 4. NIVEL OPERATIVO
                        .requestMatchers("/expedientes", "/expedientes/**", "/terminos", "/terminos/**", "/audiencias",
                                "/audiencias/**")
                        .authenticated()

                        // 5. API REST
                        .requestMatchers("/api/documentos/**", "/api/carpetas/**").authenticated()

                        // 6. API MIGRACIÓN (n8n)
                        .requestMatchers("/api/migration/**").permitAll()
                        // 7. Resto bloqueado
                        .anyRequest().authenticated())

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(forcePasswordChangeFilter, UsernamePasswordAuthenticationFilter.class)

                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/perform_login")
                        .successHandler((request, response, authentication) -> {
                            String email = authentication.getName();
                            usuarioRepository.findByEmail(email).ifPresent(u -> {
                                try {
                                    auditoriaService.registrarActividadGeneral(u, "Inicio de Sesión",
                                            "El usuario ha ingresado al sistema.", "CLOCK");
                                    auditoriaTecnicaService.registrarLoginExitoso(u, request);
                                } catch (Exception auditError) {
                                    // La auditoria no debe bloquear el inicio de sesion.
                                }
                            });
                            // Generar cookie JWT para sincronizar sesión con peticiones /api/**
                            try {
                                String jwt = jwtTokenProvider.generateToken(authentication);
                                jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("JWT_TOKEN", jwt);
                                jwtCookie.setHttpOnly(true);
                                jwtCookie.setPath("/");
                                jwtCookie.setMaxAge((int) (jwtExpirationMs / 1000));
                                response.addCookie(jwtCookie);
                            } catch (Exception jwtError) {
                                // La sesion web puede continuar aunque no se emita cookie JWT.
                            }
                            // Redirigir a la URL solicitada originalmente o al dashboard
                            org.springframework.security.web.savedrequest.HttpSessionRequestCache requestCache = new org.springframework.security.web.savedrequest.HttpSessionRequestCache();
                            org.springframework.security.web.savedrequest.SavedRequest savedRequest = requestCache
                                    .getRequest(request, response);
                            if (savedRequest != null) {
                                String redirectUrl = savedRequest.getRedirectUrl();
                                if (redirectUrl.contains(".well-known") ||
                                        redirectUrl.contains("favicon") ||
                                        redirectUrl.contains("manifest.json") ||
                                        redirectUrl.contains("sw.js") ||
                                        redirectUrl.contains("/error") ||
                                        redirectUrl.contains("/api/")) {
                                    response.sendRedirect("/dashboard");
                                } else {
                                    response.sendRedirect(redirectUrl);
                                }
                            } else {
                                response.sendRedirect("/dashboard");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            auditoriaTecnicaService.registrarLoginFallido(request.getParameter("username"), request,
                                    exception.getMessage());
                            response.sendRedirect("/?error=true");
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication != null) {
                                String email = authentication.getName();
                                usuarioRepository.findByEmail(email).ifPresent(u -> {
                                    auditoriaService.registrarActividadGeneral(u, "Cierre de Sesión",
                                            "El usuario ha salido del sistema.", "CLOCK");
                                    auditoriaTecnicaService.registrarLogout(u, request);
                                });
                            }
                            // Limpiar cookie JWT al cerrar sesión
                            jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("JWT_TOKEN", "");
                            jwtCookie.setMaxAge(0);
                            jwtCookie.setPath("/");
                            response.addCookie(jwtCookie);
                        })
                        .logoutSuccessHandler((request, response, authentication) -> {
                            request.getSession().setAttribute("logoutSuccess", true);
                            response.sendRedirect("/");
                        })
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .invalidSessionUrl("/?timeout=true")
                        .maximumSessions(3)
                        .expiredUrl("/?timeout=true"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    private boolean isJsonRequest(jakarta.servlet.http.HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        return request.getRequestURI().startsWith("/api/")
                || (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }
}
