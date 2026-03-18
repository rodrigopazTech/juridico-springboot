package com.juridico.sistema_juridico.config;

import com.juridico.sistema_juridico.security.CustomUserDetailsService;
import com.juridico.sistema_juridico.security.JwtAuthenticationFilter;
import com.juridico.sistema_juridico.security.JwtAuthenticationEntryPoint;
import com.juridico.sistema_juridico.security.ForcePasswordChangeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // <-- Importación necesaria
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final ForcePasswordChangeFilter forcePasswordChangeFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            ForcePasswordChangeFilter forcePasswordChangeFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.forcePasswordChangeFilter = forcePasswordChangeFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                jwtAuthenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**")))
                .authorizeHttpRequests(auth -> auth
                        // 1. Recursos Públicos
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**", "/lib/**").permitAll()
                        .requestMatchers("/", "/login", "/register", "/api/auth/**", "/error", "/cambiar-password")
                        .permitAll()

                        // 2. NIVEL ALTO
                        .requestMatchers("/agenda", "/agenda/**")
                        .hasAnyAuthority("ADMIN", "DIRECCION", "SUBDIRECCION", "GERENTE", "JEFE_DEPTO")
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/usuarios", "/usuarios/**")
                        .hasAnyAuthority("ADMIN", "DIRECCION", "SUBDIRECCION", "GERENTE")

                        // 3. NIVEL MEDIO
                        .requestMatchers("/dashboard", "/dashboard/**")
                        .hasAnyAuthority("ADMIN", "DIRECCION", "SUBDIRECCION", "GERENTE", "JEFE_DEPTO", "ABOGADO")

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
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .invalidSessionUrl("/?timeout=true")
                        .maximumSessions(1)
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
}