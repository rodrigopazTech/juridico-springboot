package com.juridico.sistema_juridico.dto;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String nombreCompleto;
    private String email;
    private String password;
    private RolUsuario rol;
}