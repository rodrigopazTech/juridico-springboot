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

    // Aceptamos el número de página (page) y devolvemos Page<>
    public Page<Audiencia> getAudienciasPorFiltro(String filtro, String mes, Integer anio, int page) {
        LocalDate[] rango = calcularRango(filtro, mes, anio);
        Pageable pageable = PageRequest.of(page, 10); // 10 registros por página

        Usuario actual = obtenerUsuarioActual();
        if (actual.getRol().name().equals("GERENTE") && actual.getGerencia() != null) {
            return audienciaRepository.findByEstatusGerencia(EstatusAudiencia.CONCLUIDA, rango[0], rango[1],
                    actual.getGerencia().getId().longValue(), pageable);
        } else if (actual.getRol().name().equals("JEFE_DEPTO") && !actual.getMaterias().isEmpty()) {
            List<Integer> materiaIds = actual.getMaterias().stream().map(m -> m.getId()).toList();
            return audienciaRepository.findByEstatusMaterias(EstatusAudiencia.CONCLUIDA, rango[0], rango[1], materiaIds,
                    pageable);
        }

        return audienciaRepository.findByEstatusAudienciaAndFechaDesahogoBetweenOrderByFechaDesahogoDesc(
                EstatusAudiencia.CONCLUIDA,
                rango[0],
                rango[1],
                pageable);
    }

    public Page<Termino> getTerminosPorFiltro(String filtro, String mes, Integer anio, int page) {
        LocalDate[] rango = calcularRango(filtro, mes, anio);
        List<EstatusTermino> estatusFinales = Arrays.asList(EstatusTermino.PRESENTADO, EstatusTermino.CONCLUIDO);
        Pageable pageable = PageRequest.of(page, 10); // 10 registros por página

        Usuario actual = obtenerUsuarioActual();
        if (actual.getRol().name().equals("GERENTE") && actual.getGerencia() != null) {
            return terminoRepository.findByEstatusGerencia(estatusFinales, rango[0], rango[1],
                    actual.getGerencia().getId().longValue(), pageable);
        } else if (actual.getRol().name().equals("JEFE_DEPTO") && !actual.getMaterias().isEmpty()) {
            List<Integer> materiaIds = actual.getMaterias().stream().map(m -> m.getId()).toList();
            return terminoRepository.findByEstatusMaterias(estatusFinales, rango[0], rango[1], materiaIds, pageable);
        }

        return terminoRepository.findByEstatusTerminoInAndFechaPresentacionBetweenOrderByFechaPresentacionDesc(
                estatusFinales,
                rango[0],
                rango[1],
                pageable);
    }

    private LocalDate[] calcularRango(String filtro, String mes, Integer anio) {
        // ... (El código de calcularRango se queda IGUAL que como lo tenías ayer) ...
        int year = (anio != null) ? anio : LocalDate.now().getYear();
        LocalDate baseDate = LocalDate.of(year, LocalDate.now().getMonth(), LocalDate.now().getDayOfMonth());

        if (anio != null && anio != LocalDate.now().getYear()) {
            baseDate = LocalDate.of(year, 1, 1);
        } else {
            baseDate = LocalDate.now();
        }

        LocalDate inicio = baseDate;
        LocalDate fin = baseDate;

        if ("semana".equals(filtro)) {
            inicio = baseDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            fin = baseDate.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        } else if ("mes".equals(filtro)) {
            inicio = baseDate.with(TemporalAdjusters.firstDayOfMonth());
            fin = baseDate.with(TemporalAdjusters.lastDayOfMonth());
        } else if ("otro-mes".equals(filtro) && mes != null) {
            int mesInt = Integer.parseInt(mes);
            YearMonth anioMes = YearMonth.of(year, mesInt);
            inicio = anioMes.atDay(1);
            fin = anioMes.atEndOfMonth();
        } else if ("anio".equals(filtro)) {
            inicio = LocalDate.of(year, 1, 1);
            fin = LocalDate.of(year, 12, 31);
        }
        // "hoy" es el default

        return new LocalDate[] { inicio, fin };
    }
}