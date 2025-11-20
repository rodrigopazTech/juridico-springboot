package com.juridico.sistema_juridico.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Recursos estáticos públicos (CSS, JS, imágenes)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // Páginas de autenticación públicas
                .requestMatchers("/login", "/register").permitAll()
                // Todas las demás rutas requieren autenticación
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Página personalizada de login
                .loginPage("/login")
                // Redirección después de login exitoso
                .defaultSuccessUrl("/dashboard", true)
                // Redirección después de login fallido
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                // URL para cerrar sesión
                .logoutUrl("/logout")
                // Redirección después de logout
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // Temporal para desarrollo

        return http.build();
    }
}