package com.juridico.sistema_juridico.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username; // O email, según como lo manejes en el front
    private String password;
}