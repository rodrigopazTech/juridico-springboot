package com.juridico.sistema_juridico.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetConfirmRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String nuevaContrasena;

    @NotBlank
    private String confirmacionContrasena;
}
