package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AgendaService {

    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private TerminoRepository terminoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public Page<Audiencia> getAudienciasPorFiltro(String keyword, LocalDate fechaInicio, LocalDate fechaFin, Integer gerenciaId, Integer materiaId, int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Usuario actual = obtenerUsuarioActual();

        Integer filtroRoleGerenciaId = null;
        List<Integer> filtroRoleMateriaIds = null;
        Integer filtroRoleUsuarioId = null;

        if (actual.getRol() == RolUsuario.ABOGADO) {
            filtroRoleUsuarioId = actual.getId();
        } else if (actual.getRol() == RolUsuario.GERENTE) {
            filtroRoleGerenciaId = actual.getGerencia() != null ? actual.getGerencia().getId() : -1;
        } else if (actual.getRol() == RolUsuario.JEFE_DEPTO) {
            filtroRoleMateriaIds = actual.getMaterias() != null && !actual.getMaterias().isEmpty()
                    ? actual.getMaterias().stream().map(m -> m.getId()).toList()
                    : List.of(-1);
        }

        // Si las fechas son nulas sugerir rango de siglo por defecto para evitar NULL
        if (fechaInicio == null) fechaInicio = LocalDate.of(1900, 1, 1);
        if (fechaFin == null) fechaFin = LocalDate.of(2100, 12, 31);

        return audienciaRepository.buscarParaHistorial(
                keyword,
                EstatusAudiencia.CONCLUIDA,
                fechaInicio, fechaFin,
                gerenciaId, materiaId,
                filtroRoleGerenciaId, filtroRoleMateriaIds, filtroRoleUsuarioId,
                pageable);
    }

    public Page<Termino> getTerminosPorFiltro(String keyword, LocalDate fechaInicio, LocalDate fechaFin, Integer gerenciaId, Integer materiaId, int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Usuario actual = obtenerUsuarioActual();

        Integer filtroRoleGerenciaId = null;
        List<Integer> filtroRoleMateriaIds = null;
        Integer filtroRoleUsuarioId = null;

        if (actual.getRol() == RolUsuario.ABOGADO) {
            filtroRoleUsuarioId = actual.getId();
        } else if (actual.getRol() == RolUsuario.GERENTE) {
            filtroRoleGerenciaId = actual.getGerencia() != null ? actual.getGerencia().getId() : -1;
        } else if (actual.getRol() == RolUsuario.JEFE_DEPTO) {
            filtroRoleMateriaIds = actual.getMaterias() != null && !actual.getMaterias().isEmpty()
                    ? actual.getMaterias().stream().map(m -> m.getId()).toList()
                    : List.of(-1);
        }

        if (fechaInicio == null) fechaInicio = LocalDate.of(1900, 1, 1);
        if (fechaFin == null) fechaFin = LocalDate.of(2100, 12, 31);
        LocalDateTime inicioConclusion = fechaInicio.atStartOfDay();
        LocalDateTime finConclusion = fechaFin.atTime(LocalTime.MAX);

        return terminoRepository.buscarParaHistorial(
                keyword,
                EstatusTermino.CONCLUIDO,
                inicioConclusion, finConclusion,
                gerenciaId, materiaId,
                filtroRoleGerenciaId, filtroRoleMateriaIds,
                filtroRoleUsuarioId,
                pageable);
    }
}
