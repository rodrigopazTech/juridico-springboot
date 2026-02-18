package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.repository.procesal.AudienciaDesahogadaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class AgendaService {

    @Autowired
    private AudienciaDesahogadaRepository desahogadaRepository;
    @Autowired
    private TerminoRepository terminoRepository;

    // Aceptamos el número de página (page) y devolvemos Page<>
    // MODIFIED: Added gerenciaId, materiaIds, and usuarioId for RBAC filtering
    public Page<AudienciaDesahogada> getAudienciasPorFiltro(PeriodoFiltro filtro, String mes, Integer anio, int page,
            Integer gerenciaId, List<Integer> materiaIds, Integer usuarioId) {
        LocalDate[] rango = PeriodoFiltro.calcularRango(filtro, mes, anio);
        Pageable pageable = PageRequest.of(page, 10); // 10 registros por página

        if (gerenciaId != null) {
            return desahogadaRepository
                    .findByFechaDesahogoBetweenAndAudiencia_Expediente_Gerencia_IdOrderByFechaDesahogoDesc(
                            rango[0], rango[1], gerenciaId, pageable);
        } else if (materiaIds != null && !materiaIds.isEmpty()) {
            return desahogadaRepository
                    .findByFechaDesahogoBetweenAndAudiencia_Expediente_Materia_IdInOrderByFechaDesahogoDesc(
                            rango[0], rango[1], materiaIds, pageable);
        } else if (usuarioId != null) {
            // ROD-45: Filtro por Abogado (Solo lo suyo)
            return desahogadaRepository
                    .findByFechaDesahogoBetweenAndAudiencia_Expediente_AbogadoResponsable_IdOrderByFechaDesahogoDesc(
                            rango[0], rango[1], usuarioId, pageable);
        } else {
            return desahogadaRepository.findByFechaDesahogoBetweenOrderByFechaDesahogoDesc(rango[0], rango[1],
                    pageable);
        }
    }

    // MODIFIED: Added gerenciaId, materiaIds, and usuarioId for RBAC filtering
    public Page<Termino> getTerminosPorFiltro(PeriodoFiltro filtro, String mes, Integer anio, int page,
            Integer gerenciaId,
            List<Integer> materiaIds,
            Integer usuarioId) {
        LocalDate[] rango = PeriodoFiltro.calcularRango(filtro, mes, anio);
        List<EstatusTermino> estatusFinales = Arrays.asList(EstatusTermino.PRESENTADO, EstatusTermino.CONCLUIDO);
        Pageable pageable = PageRequest.of(page, 10); // 10 registros por página

        if (gerenciaId != null) {
            return terminoRepository
                    .findByEstatusTerminoInAndFechaPresentacionBetweenAndExpediente_Gerencia_IdOrderByFechaPresentacionDesc(
                            estatusFinales, rango[0], rango[1], gerenciaId, pageable);
        } else if (materiaIds != null && !materiaIds.isEmpty()) {
            return terminoRepository
                    .findByEstatusTerminoInAndFechaPresentacionBetweenAndExpediente_Materia_IdInOrderByFechaPresentacionDesc(
                            estatusFinales, rango[0], rango[1], materiaIds, pageable);
        } else if (usuarioId != null) {
            // ROD-45: Filtro por Abogado (Solo lo suyo)
            return terminoRepository
                    .findByEstatusTerminoInAndFechaPresentacionBetweenAndAbogadoResponsable_IdOrderByFechaPresentacionDesc(
                            estatusFinales, rango[0], rango[1], usuarioId, pageable);
        } else {
            return terminoRepository.findByEstatusTerminoInAndFechaPresentacionBetweenOrderByFechaPresentacionDesc(
                    estatusFinales, rango[0], rango[1], pageable);
        }
    }
}
