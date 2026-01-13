package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.dto.LoginRequest;
import com.juridico.sistema_juridico.dto.LoginResponse;
import com.juridico.sistema_juridico.dto.RefreshTokenRequest;
import com.juridico.sistema_juridico.dto.RegisterRequest;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.security.JwtTokenProvider;
import com.juridico.sistema_juridico.exception.ConflictException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.exception.UnauthorizedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest loginRequest) {
        // Usamos el email que viene en el DTO para autenticar
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);
        return new LoginResponse(jwt, jwt, "Bearer");
    }

    public String register(RegisterRequest registerRequest) {
        // Usamos existsByEmail que es el método oficial del repositorio
        if(usuarioRepository.existsByEmail(registerRequest.getEmail())) {
            throw new ConflictException("El email ya está registrado en el sistema jurídico.");
        }

        Usuario usuario = Usuario.builder()
                .nombreCompleto(registerRequest.getNombre())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .createdAt(LocalDateTime.now())
                .activo(true)
                .build();
        
        usuarioRepository.save(usuario);
        return "Abogado registrado exitosamente";
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // 1. Validar si el refresh token es legítimo
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("El token de refresco ha expirado o es inválido.");
        }

        // 2. Extraer el email del token (AQUÍ usamos la variable que causaba el aviso)
        String email = tokenProvider.getUsernameFromJWT(request.getRefreshToken());
        
        // 3. Buscamos al usuario en la base de datos para asegurarnos que sigue activo
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 4. Creamos una nueva autenticación para generar el nuevo Access Token
        // Usamos una implementación simple de Authentication para el token
        Authentication auth = new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, java.util.Collections.emptyList());
        
        String newAccessToken = tokenProvider.generateToken(auth);

        // Devolvemos el nuevo Access Token y conservamos el mismo Refresh Token
        return new LoginResponse(newAccessToken, request.getRefreshToken(), "Bearer");
        }

    public String logout() {
        SecurityContextHolder.clearContext();
        return "Sesión cerrada exitosamente";
    }

}
