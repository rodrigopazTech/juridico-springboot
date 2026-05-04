package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.repository.procesal.AudienciaDesahogadaRepository;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
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

        if (actual.getRol().name().equals("GERENTE") && actual.getGerencia() != null) {
            filtroRoleGerenciaId = actual.getGerencia().getId();
        } else if (actual.getRol().name().equals("JEFE_DEPTO") && !actual.getMaterias().isEmpty()) {
            filtroRoleMateriaIds = actual.getMaterias().stream().map(m -> m.getId()).toList();
        }

        // Si las fechas son nulas sugerir rango de siglo por defecto para evitar NULL
        if (fechaInicio == null) fechaInicio = LocalDate.of(1900, 1, 1);
        if (fechaFin == null) fechaFin = LocalDate.of(2100, 12, 31);

        return audienciaRepository.buscarParaHistorial(
                keyword,
                EstatusAudiencia.CONCLUIDA,
                fechaInicio, fechaFin,
                gerenciaId, materiaId,
                filtroRoleGerenciaId, filtroRoleMateriaIds,
                pageable);
    }

    public Page<Termino> getTerminosPorFiltro(String keyword, LocalDate fechaInicio, LocalDate fechaFin, Integer gerenciaId, Integer materiaId, int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Usuario actual = obtenerUsuarioActual();

        List<EstatusTermino> estatusFinales = Arrays.asList(EstatusTermino.PRESENTADO, EstatusTermino.CONCLUIDO);

        Integer filtroRoleGerenciaId = null;
        List<Integer> filtroRoleMateriaIds = null;

        if (actual.getRol().name().equals("GERENTE") && actual.getGerencia() != null) {
            filtroRoleGerenciaId = actual.getGerencia().getId();
        } else if (actual.getRol().name().equals("JEFE_DEPTO") && !actual.getMaterias().isEmpty()) {
            filtroRoleMateriaIds = actual.getMaterias().stream().map(m -> m.getId()).toList();
        }

        if (fechaInicio == null) fechaInicio = LocalDate.of(1900, 1, 1);
        if (fechaFin == null) fechaFin = LocalDate.of(2100, 12, 31);

        return terminoRepository.buscarParaHistorial(
                keyword,
                estatusFinales,
                fechaInicio, fechaFin,
                gerenciaId, materiaId,
                filtroRoleGerenciaId, filtroRoleMateriaIds,
                null,
                pageable);
    }
}