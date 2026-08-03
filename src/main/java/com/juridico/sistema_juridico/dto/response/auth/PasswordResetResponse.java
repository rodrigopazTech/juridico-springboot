package com.juridico.sistema_juridico.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordResetResponse {

    private String status;
    private String message;
}
