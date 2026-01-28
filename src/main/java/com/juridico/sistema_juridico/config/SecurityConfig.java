package com.juridico.sistema_juridico.config;

import com.juridico.sistema_juridico.security.CustomUserDetailsService;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

   @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth
                // 1. Recursos Públicos
                .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**", "/lib/**").permitAll()
                .requestMatchers("/", "/login", "/register", "/api/auth/**").permitAll()

                // 2. NIVEL ALTO: Usuarios y Agenda General (Solo Dirección y Subdirección)
                .requestMatchers("/usuarios/**", "/agenda/**").hasAnyAuthority("DIRECCION", "SUBDIRECCION")

                // 3. NIVEL MEDIO: Dashboard (Dirección, Subdirección, Gerentes y Jefes)
                // Nota: Los Abogados NO entran aquí.
                .requestMatchers("/dashboard/**").hasAnyAuthority("DIRECCION", "SUBDIRECCION", "GERENTE", "JEFE_DEPTO")

                // 4. NIVEL OPERATIVO: Expedientes, Términos, Audiencias (TODOS los autenticados)
                .requestMatchers("/expedientes/**", "/terminos/**", "/audiencias/**").authenticated()

                // 5. Resto bloqueado
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/")
                .loginProcessingUrl("/perform_login")
                .successHandler((request, response, authentication) -> {
                    // LÓGICA DE REDIRECCIÓN INTELIGENTE SEGÚN ROL
                    String role = authentication.getAuthorities().iterator().next().getAuthority();
                    if (role.equals("ABOGADO")) {
                        response.sendRedirect("/expedientes");
                    } else {
                        response.sendRedirect("/dashboard");
                    }
                })
                .failureUrl("/?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/?logout")
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

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