package com.juridico.sistema_juridico.dto.usuarios;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PermisoCategoriaView {
    private final String categoria;
    private final List<PermisoUsuarioItemView> permisos;
}
