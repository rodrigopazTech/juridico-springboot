package com.juridico.sistema_juridico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken; // Si usas refresh token
    private String tokenType = "Bearer";
}