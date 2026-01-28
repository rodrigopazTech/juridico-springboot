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

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        return new LoginResponse(accessToken, refreshToken, "Bearer");
    }

    public String register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("El email ya está registrado.");
        }

        Usuario usuario = Usuario.builder()
                .nombreCompleto(request.getNombreCompleto())
                .email(request.getEmail())
                // CAMBIO: .password en lugar de .passwordHash
                .password(passwordEncoder.encode(request.getPassword())) 
                .rol(request.getRol())
                .activo(true)
                .build();

        usuarioRepository.save(usuario);
        return "Abogado registrado exitosamente";
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("El token de refresco ha expirado o es inválido.");
        }

        String email = tokenProvider.getUsernameFromJWT(request.getRefreshToken());
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Authentication auth = new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, java.util.Collections.emptyList());
        
        String newAccessToken = tokenProvider.generateToken(auth);

        return new LoginResponse(newAccessToken, request.getRefreshToken(), "Bearer");
    }

    public String logout() {
        SecurityContextHolder.clearContext();
        return "Sesión cerrada exitosamente";
    }
}