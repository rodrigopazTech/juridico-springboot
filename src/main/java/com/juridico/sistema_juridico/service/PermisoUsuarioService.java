package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.enums.PermisoSistema;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.usuarios.PermisoCategoriaView;
import com.juridico.sistema_juridico.dto.usuarios.PermisoUsuarioItemView;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PermisoUsuarioService {

    private final UsuarioRepository usuarioRepository;

    private static final EnumMap<RolUsuario, EnumSet<PermisoSistema>> PERMISOS_BASE = new EnumMap<>(RolUsuario.class);

    static {
        PERMISOS_BASE.put(RolUsuario.ABOGADO, EnumSet.of(
                PermisoSistema.DASHBOARD_VER,
                PermisoSistema.EXPEDIENTES_VER,
                PermisoSistema.TERMINOS_VER,
                PermisoSistema.AUDIENCIAS_VER));

        PERMISOS_BASE.put(RolUsuario.JEFE_DEPTO, EnumSet.of(
                PermisoSistema.DASHBOARD_VER,
                PermisoSistema.AGENDA_VER,
                PermisoSistema.CATALOGOS_VER,
                PermisoSistema.USUARIOS_VER,
                PermisoSistema.EXPEDIENTES_VER,
                PermisoSistema.EXPEDIENTES_SUPERVISION,
                PermisoSistema.TERMINOS_VER,
                PermisoSistema.TERMINOS_SUPERVISION,
                PermisoSistema.AUDIENCIAS_VER,
                PermisoSistema.AUDIENCIAS_SUPERVISION,
                PermisoSistema.ALERTAS_CONFIGURAR));

        PERMISOS_BASE.put(RolUsuario.GERENTE, EnumSet.of(
                PermisoSistema.DASHBOARD_VER,
                PermisoSistema.AGENDA_VER,
                PermisoSistema.CATALOGOS_VER,
                PermisoSistema.USUARIOS_VER,
                PermisoSistema.EXPEDIENTES_VER,
                PermisoSistema.EXPEDIENTES_SUPERVISION,
                PermisoSistema.TERMINOS_VER,
                PermisoSistema.TERMINOS_SUPERVISION,
                PermisoSistema.AUDIENCIAS_VER,
                PermisoSistema.AUDIENCIAS_SUPERVISION,
                PermisoSistema.DOCUMENTOS_SUPERVISION,
                PermisoSistema.ALERTAS_CONFIGURAR));

        PERMISOS_BASE.put(RolUsuario.DIRECTOR, EnumSet.of(
                PermisoSistema.DASHBOARD_VER,
                PermisoSistema.AGENDA_VER,
                PermisoSistema.CATALOGOS_VER,
                PermisoSistema.USUARIOS_VER,
                PermisoSistema.EXPEDIENTES_VER,
                PermisoSistema.EXPEDIENTES_SUPERVISION,
                PermisoSistema.TERMINOS_VER,
                PermisoSistema.TERMINOS_SUPERVISION,
                PermisoSistema.AUDIENCIAS_VER,
                PermisoSistema.AUDIENCIAS_SUPERVISION,
                PermisoSistema.DOCUMENTOS_SUPERVISION,
                PermisoSistema.ALERTAS_CONFIGURAR));

        PERMISOS_BASE.put(RolUsuario.SUBDIRECTOR, EnumSet.copyOf(PERMISOS_BASE.get(RolUsuario.DIRECTOR)));
        PERMISOS_BASE.put(RolUsuario.ADMIN, EnumSet.allOf(PermisoSistema.class));
    }

    public PermisoUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public EnumSet<PermisoSistema> obtenerPermisosBase(RolUsuario rol) {
        return PERMISOS_BASE.containsKey(rol)
                ? EnumSet.copyOf(PERMISOS_BASE.get(rol))
                : EnumSet.noneOf(PermisoSistema.class);
    }

    public EnumSet<PermisoSistema> obtenerPermisosEfectivos(Usuario usuario) {
        EnumSet<PermisoSistema> efectivos = obtenerPermisosBase(usuario.getRol());
        if (usuario.getPermisosAdicionales() != null) {
            efectivos.addAll(usuario.getPermisosAdicionales());
        }
        return efectivos;
    }

    public List<PermisoCategoriaView> construirVistaPermisos(Usuario actor, Usuario objetivo) {
        EnumSet<PermisoSistema> baseObjetivo = obtenerPermisosBase(objetivo.getRol());
        EnumSet<PermisoSistema> efectivosObjetivo = obtenerPermisosEfectivos(objetivo);
        EnumSet<PermisoSistema> permisosActor = obtenerPermisosEfectivos(actor);

        Map<String, List<PermisoUsuarioItemView>> agrupados = new LinkedHashMap<>();

        for (PermisoSistema permiso : PermisoSistema.values()) {
            agrupados.computeIfAbsent(permiso.getCategoria(), key -> new ArrayList<>())
                    .add(PermisoUsuarioItemView.builder()
                            .permiso(permiso)
                            .titulo(permiso.getTitulo())
                            .descripcion(permiso.getDescripcion())
                            .habilitado(efectivosObjetivo.contains(permiso))
                            .baseRol(baseObjetivo.contains(permiso))
                            .otorgable(puedeOtorgar(actor, permiso, permisosActor))
                            .build());
        }

        List<PermisoCategoriaView> vista = new ArrayList<>();
        agrupados.forEach((categoria, permisos) -> {
            permisos.sort(Comparator.comparing(PermisoUsuarioItemView::getTitulo));
            vista.add(PermisoCategoriaView.builder()
                    .categoria(categoria)
                    .permisos(permisos)
                    .build());
        });
        return vista;
    }

    public boolean puedeOtorgar(Usuario actor, PermisoSistema permiso) {
        return puedeOtorgar(actor, permiso, obtenerPermisosEfectivos(actor));
    }

    private boolean puedeOtorgar(Usuario actor, PermisoSistema permiso, Set<PermisoSistema> permisosActor) {
        if (actor.getRol() == RolUsuario.ADMIN) {
            return true;
        }
        return permisosActor.contains(permiso);
    }

    @Transactional
    public void guardarPermisosAdicionales(Integer usuarioId, List<PermisoSistema> permisosSolicitados, Usuario actor) {
        Usuario objetivo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        EnumSet<PermisoSistema> permisosBase = obtenerPermisosBase(objetivo.getRol());
        EnumSet<PermisoSistema> nuevosExtras = EnumSet.noneOf(PermisoSistema.class);

        if (permisosSolicitados != null) {
            for (PermisoSistema permiso : permisosSolicitados) {
                if (!permisosBase.contains(permiso) && puedeOtorgar(actor, permiso)) {
                    nuevosExtras.add(permiso);
                }
            }
        }

        objetivo.getPermisosAdicionales().clear();
        objetivo.getPermisosAdicionales().addAll(nuevosExtras);
        usuarioRepository.save(objetivo);
    }
}
