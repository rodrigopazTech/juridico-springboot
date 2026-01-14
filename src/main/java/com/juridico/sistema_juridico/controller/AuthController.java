package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.dto.LoginRequest;
import com.juridico.sistema_juridico.dto.LoginResponse;
import com.juridico.sistema_juridico.dto.RegisterRequest;
import com.juridico.sistema_juridico.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // Endpoint para LOGIN
    // URL: http://localhost:8080/api/auth/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    // Endpoint para REGISTRO
    // URL: http://localhost:8080/api/auth/register
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest) {
        String mensaje = authService.register(registerRequest);
        return ResponseEntity.ok(mensaje);
    }
}