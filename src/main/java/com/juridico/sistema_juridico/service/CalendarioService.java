package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.dto.response.calendario.EventoResponse;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalendarioService {

    @Autowired
    private AudienciaRepository audienciaRepository;

    @Autowired
    private TerminoRepository terminoRepository;

    @Autowired
    private RecordatorioRepository recordatorioRepository;

    public List<EventoResponse> obtenerEventosCalendario() {
        List<EventoResponse> eventos = new ArrayList<>();

        // 1. Mapear Audiencias (Confirmado: getFechaAudiencia)
        eventos.addAll(audienciaRepository.findAll().stream()
                .<EventoResponse>map(this::mapToAudienciaResponse)
                .collect(Collectors.toList()));

        // 2. Mapear Términos (Confirmado: getFechaVencimiento)
        eventos.addAll(terminoRepository.findAll().stream()
                .<EventoResponse>map(this::mapToTerminoResponse)
                .collect(Collectors.toList()));

        // 3. Mapear Recordatorios (Confirmado: getFechaRecordatorio)
        eventos.addAll(recordatorioRepository.findAll().stream()
                .<EventoResponse>map(this::mapToRecordatorioResponse)
                .collect(Collectors.toList()));

        return eventos;
    }

    private EventoResponse mapToAudienciaResponse(Audiencia a) {
        return EventoResponse.builder()
                .id(a.getId() != null ? a.getId().longValue() : null)
                .titulo("Audiencia: " + (a.getExpediente() != null ? a.getExpediente().getNumero() : "S/N"))
                .tipo("audiencia")
                .fecha(a.getFechaAudiencia() != null ? a.getFechaAudiencia().toString() : "")
                .hora(a.getHoraAudiencia() != null ? a.getHoraAudiencia().toString() : "")
                .expediente(a.getExpediente() != null ? a.getExpediente().getNumero() : "")
                .build();
    }

    private EventoResponse mapToTerminoResponse(Termino t) {
        return EventoResponse.builder()
                .id(t.getId() != null ? t.getId().longValue() : null)
                .titulo("Término: " + t.getActuacion())
                .tipo("termino")
                .fecha(t.getFechaVencimiento() != null ? t.getFechaVencimiento().toString() : "")
                .hora("23:59")
                .expediente(t.getExpediente() != null ? t.getExpediente().getNumero() : "N/A")
                .build();
    }

    private EventoResponse mapToRecordatorioResponse(Recordatorio r) {
        return EventoResponse.builder()
                .id(r.getId() != null ? r.getId().longValue() : null)
                .titulo(r.getTitulo())
                .tipo("recordatorio")
                .fecha(r.getFechaRecordatorio() != null ? r.getFechaRecordatorio().toString() : "")
                .hora(r.getHoraRecordatorio() != null ? r.getHoraRecordatorio().toString() : "")
                .detalles(r.getDetalles())
                .build();
    }
}