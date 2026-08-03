package com.juridico.sistema_juridico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String username;
    private String email;
    private String password;

    public String loginIdentifier() {
        return username != null && !username.isBlank() ? username : email;
    }
}
