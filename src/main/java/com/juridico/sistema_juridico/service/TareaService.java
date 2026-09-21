package com.juridico.sistema_juridico.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.recordatorios.RecordatorioRequest;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;

@Service
public class TareaService {

    public static final int MINUTOS_ANTICIPACION = 15;

    private final RecordatorioRepository recordatorioRepository;
    private final CatPrioridadRepository prioridadRepository;
    private final NotificacionRepository notificacionRepository;

    public TareaService(
            RecordatorioRepository recordatorioRepository,
            CatPrioridadRepository prioridadRepository,
            NotificacionRepository notificacionRepository) {
        this.recordatorioRepository = recordatorioRepository;
        this.prioridadRepository = prioridadRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional(readOnly = true)
    public List<CatPrioridad> listarPrioridadesActivas() {
        return prioridadRepository.findAll().stream()
                .filter(prioridad -> Boolean.TRUE.equals(prioridad.getActivo()))
                .toList();
    }

    @Transactional
    public Recordatorio crear(RecordatorioRequest request, Usuario usuario) {
        validarProgramacion(request);

        LocalDateTime ahora = LocalDateTime.now();
        Recordatorio recordatorio = Recordatorio.builder()
                .usuario(usuario)
                .titulo(request.getTitulo().trim())
                .detalles(normalizarDetalles(request.getDetalles()))
                .fechaRecordatorio(request.getFechaRecordatorio())
                .horaRecordatorio(request.getHoraRecordatorio())
                .prioridad(obtenerPrioridadActiva(request.getPrioridadId()))
                .completado(false)
                .recordatorioRevision(0)
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();
        return recordatorioRepository.save(recordatorio);
    }

    @Transactional
    public Recordatorio actualizar(Integer id, RecordatorioRequest request, Usuario usuario) {
        validarProgramacion(request);

        Recordatorio recordatorio = recordatorioRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada."));
        if (Boolean.TRUE.equals(recordatorio.getCompletado())) {
            throw new BadRequestException("No se puede editar una tarea completada.");
        }

        boolean cambioProgramacion = !request.getFechaRecordatorio().equals(recordatorio.getFechaRecordatorio())
                || !request.getHoraRecordatorio().equals(recordatorio.getHoraRecordatorio());

        recordatorio.setTitulo(request.getTitulo().trim());
        recordatorio.setDetalles(normalizarDetalles(request.getDetalles()));
        recordatorio.setFechaRecordatorio(request.getFechaRecordatorio());
        recordatorio.setHoraRecordatorio(request.getHoraRecordatorio());
        recordatorio.setPrioridad(obtenerPrioridadActiva(request.getPrioridadId()));
        recordatorio.setUpdatedAt(LocalDateTime.now());

        if (cambioProgramacion) {
            archivarNotificacionesAnteriores(recordatorio, usuario);
            int revisionActual = recordatorio.getRecordatorioRevision() == null
                    ? 0
                    : recordatorio.getRecordatorioRevision();
            recordatorio.setRecordatorioRevision(revisionActual + 1);
        }

        return recordatorioRepository.save(recordatorio);
    }

    private void validarProgramacion(RecordatorioRequest request) {
        if (request == null || request.getFechaRecordatorio() == null || request.getHoraRecordatorio() == null) {
            throw new BadRequestException("La fecha y la hora son obligatorias.");
        }
        if (request.getTitulo() == null || request.getTitulo().trim().isEmpty()) {
            throw new BadRequestException("El título de la tarea es obligatorio.");
        }
        LocalDateTime programada = request.getFechaRecordatorio().atTime(request.getHoraRecordatorio());
        if (!programada.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("La fecha y hora de la tarea deben estar en el futuro.");
        }
    }

    private CatPrioridad obtenerPrioridadActiva(Integer prioridadId) {
        if (prioridadId == null) {
            throw new BadRequestException("La prioridad es obligatoria.");
        }
        CatPrioridad prioridad = prioridadRepository.findById(prioridadId)
                .orElseThrow(() -> new BadRequestException("La prioridad seleccionada no existe."));
        if (!Boolean.TRUE.equals(prioridad.getActivo())) {
            throw new BadRequestException("La prioridad seleccionada no está activa.");
        }
        return prioridad;
    }

    private String normalizarDetalles(String detalles) {
        if (detalles == null || detalles.trim().isEmpty()) {
            return null;
        }
        return detalles.trim();
    }

    private void archivarNotificacionesAnteriores(Recordatorio recordatorio, Usuario usuario) {
        String idBase = String.valueOf(recordatorio.getId());
        List<Notificacion> anteriores = new ArrayList<>();
        anteriores.addAll(notificacionRepository.findByUsuarioAndEventoAndLeidaFalseAndEntidadId(
                usuario, NotificacionEvento.TAREA_RECORDATORIO, idBase));
        anteriores.addAll(notificacionRepository.findByUsuarioAndEventoAndLeidaFalseAndEntidadIdStartingWith(
                usuario, NotificacionEvento.TAREA_RECORDATORIO, idBase + ":"));

        if (anteriores.isEmpty()) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        anteriores.forEach(notificacion -> {
            notificacion.setLeida(true);
            notificacion.setFechaLeida(ahora);
        });
        notificacionRepository.saveAll(anteriores);
    }
}
