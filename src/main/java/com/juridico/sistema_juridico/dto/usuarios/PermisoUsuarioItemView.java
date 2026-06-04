package com.juridico.sistema_juridico.dto.usuarios;

import com.juridico.sistema_juridico.Entity.enums.PermisoSistema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermisoUsuarioItemView {
    private final PermisoSistema permiso;
    private final String titulo;
    private final String descripcion;
    private final boolean habilitado;
    private final boolean baseRol;
    private final boolean otorgable;
}
