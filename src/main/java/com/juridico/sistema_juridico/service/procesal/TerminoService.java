package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.dto.request.terminos.TerminoRequest;
import com.juridico.sistema_juridico.dto.response.terminos.TerminoResponse;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoPresentadoRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TerminoService {

    private final TerminoRepository terminoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final TerminoPresentadoRepository presentadoRepository;

    public Page<TerminoResponse> listarPaginado(Pageable pageable) {
        return terminoRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public TerminoResponse crear(TerminoRequest request) {
        // Corrección de tipos: Buscamos por UUID que es lo que espera tu
        // ExpedienteRepository
        Expediente exp = expedienteRepository.findById(UUID.fromString(request.getAsuntoId().toString()))
                .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));

        Termino termino = Termino.builder()
                .expediente(exp)
                .actuacion(request.getAsunto()) // En tu entidad se llama actuacion
                .fechaVencimiento(request.getFechaVencimiento())
                .estatusTermino(EstatusTermino.PROYECTISTA) // En tu entidad se llama estatusTermino
                .fechaIngreso(LocalDate.now())
                .build();

        return mapToResponse(terminoRepository.save(termino));
    }

    @Transactional
    public void marcarComoPresentado(Integer id, String observaciones) {
        Termino termino = terminoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Término no encontrado"));

        termino.setEstatusTermino(EstatusTermino.PRESENTADO);
        terminoRepository.save(termino);

        // Lógica de Histórico: Creamos el registro en la tabla de presentados
        TerminoPresentado historico = TerminoPresentado.builder()
                .termino(termino)
                .expedienteNumero(termino.getExpediente().getNumero())
                .fechaPresentacion(LocalDate.now())
                .acuseDocumento(observaciones) // Guardamos el comentario como acuse inicial
                .build();

        presentadoRepository.save(historico);
    }

    @Transactional
    public void cambiarEstado(Integer id, EstatusTermino nuevoEstado) { // Cambiado a Integer
        Termino termino = terminoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Término no encontrado"));
        termino.setEstatusTermino(nuevoEstado);
        terminoRepository.save(termino);
    }

    // Para el endpoint de próximos a vencer (alertas de 7 días)
    public List<TerminoResponse> listarProximosAVencer() {
        LocalDate limite = LocalDate.now().plusDays(7);
        return terminoRepository.findByFechaVencimientoBeforeAndEstatusTerminoNot(limite, EstatusTermino.CONCLUIDO)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TerminoResponse mapToResponse(Termino entity) {
        TerminoResponse res = new TerminoResponse();
        res.setId(Long.valueOf(entity.getId()));
        res.setActuación(entity.getActuacion());
        res.setFechaVencimiento(entity.getFechaVencimiento());

        if (entity.getExpediente() != null) {
            res.setExpediente(entity.getExpediente().getNumero());
        }

        // Agregamos lógica para el abogado responsable
        if (entity.getAbogadoResponsable() != null) {
            res.setAbogadoResponsable(entity.getAbogadoResponsable().getNombreCompleto());
        }

        return res;
    }

    // 1. Obtener términos por expediente (Requisito: GET
    // /api/terminos/expediente/{expedienteId})
    public List<TerminoResponse> listarPorExpediente(UUID expedienteId) {
        return terminoRepository.findByExpedienteId(expedienteId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 2. Obtener términos vencidos (Requisito: GET /api/terminos/vencidos)
    public List<TerminoResponse> listarVencidos() {
        return terminoRepository.findByFechaVencimientoBeforeAndEstatusTerminoNot(
                LocalDate.now(), EstatusTermino.CONCLUIDO)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. Lógica de Alertas (Sistema de 7, 3, 1 días)
    public String calcularNivelAlerta(LocalDate fechaVencimiento) {
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
        if (diasRestantes <= 1)
            return "CRÍTICA (1 día)";
        if (diasRestantes <= 3)
            return "ALTA (3 días)";
        if (diasRestantes <= 7)
            return "MEDIA (7 días)";
        return "NORMAL";
    }

}