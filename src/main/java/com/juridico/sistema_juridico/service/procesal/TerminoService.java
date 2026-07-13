package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.dto.request.terminos.TerminoRequest;
import com.juridico.sistema_juridico.dto.response.terminos.TerminoResponse;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoPresentadoRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
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
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final CatPrioridadRepository catPrioridadRepository;


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
                .asunto(request.getAsunto()) // En tu entidad se llama asunto
                .fechaVencimiento(request.getFechaVencimiento())
                .estatusTermino(EstatusTermino.PROYECTISTA) // En tu entidad se llama estatusTermino
                .fechaIngreso(LocalDate.now())
                .build();

        Termino guardado = terminoRepository.save(termino);

        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        String fechaFormat = LocalDate.now().toString();
        String actuacionStr = (termino.getAsunto() != null && !termino.getAsunto().isEmpty())
                ? termino.getAsunto()
                : "Sin Especificar";

        registrarActividad(exp, "Término Creado",
                "Se ha registrado un nuevo término: " + actuacionStr + ". Acción realizada por: "
                        + usuarioActual + " el " + fechaFormat + ".",
                "PLUS");

        return mapToResponse(guardado);
    }

    @Transactional
    public TerminoResponse guardarTermino(TerminoRequest request) {
        return crear(request); // Llama al método que ya escribiste
    }

    @Transactional
    public void marcarComoPresentado(Integer id, String observaciones) {
        Termino termino = terminoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Término no encontrado"));

        termino.setEstatusTermino(EstatusTermino.PRESENTADO);
        termino.setFechaPresentacion(LocalDate.now());
        terminoRepository.save(termino);

        // Lógica de Histórico: Creamos el registro en la tabla de presentados
        TerminoPresentado historico = TerminoPresentado.builder()
                .termino(termino)
                .expedienteNumero(termino.getNumeroReferencia())
                .fechaPresentacion(LocalDate.now())
                .acuseDocumento(observaciones) // Guardamos el comentario como acuse inicial
                .build();

        presentadoRepository.save(historico);

        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        String fechaFormat = LocalDate.now().toString();
        if (termino.getExpediente() != null) {
            registrarActividad(termino.getExpediente(), "Término Concluido",
                "El término ha sido marcado como presentado. Acción realizada por: " + usuarioActual + " el "
                        + fechaFormat + ".",
                "CHECK");
        } else {
            Usuario usuarioActor = usuarioRepository.findByEmail(usuarioActual).orElse(null);
            auditoriaService.registrarActividadGeneral(usuarioActor, "Término Concluido",
                "El término '" + termino.getAsunto() + "' (sin expediente) ha sido marcado como presentado. Acción realizada por: " + usuarioActual + " el " + fechaFormat + ".",
                "CHECK");
        }

        // Notificar al abogado si fue concluido por un superior
        Usuario abogadoResponsable = termino.getExpediente() != null ? termino.getExpediente().getAbogadoResponsable() : null;
        Usuario responsableTermino = termino.getAbogadoResponsable(); // Si existe
        Usuario usuarioActor = usuarioRepository.findByEmail(usuarioActual).orElse(null);

        String descExpediente = (termino.getExpediente() != null) 
                ? "del expediente " + termino.getExpediente().getNumero() 
                : "(sin expediente)";

        if (abogadoResponsable != null && usuarioActor != null && !abogadoResponsable.getId().equals(usuarioActor.getId())) {
            notificacionService.crearNotificacion(
                    abogadoResponsable,
                    usuarioActor,
                    NotificacionEvento.TERMINO_VALIDADO_CONCLUIDO,
                    "Termino Validado y Concluido",
                    "El término '" + termino.getAsunto() + "' " + descExpediente + " ha sido marcado como presentado y concluido.",

                    catPrioridadRepository.findByNombreIgnoreCase("MEDIA").orElse(null),
                    termino.getId().toString(),
                    true
            );
        }
        if (responsableTermino != null && usuarioActor != null && !responsableTermino.getId().equals(usuarioActor.getId()) && (abogadoResponsable == null || !responsableTermino.getId().equals(abogadoResponsable.getId()))) {
            notificacionService.crearNotificacion(
                    responsableTermino,
                    usuarioActor,
                    NotificacionEvento.TERMINO_VALIDADO_CONCLUIDO,
                    "Termino Validado y Concluido",
                    "El término '" + termino.getAsunto() + "' " + descExpediente + " ha sido marcado como presentado y concluido.",

                    catPrioridadRepository.findByNombreIgnoreCase("MEDIA").orElse(null),
                    termino.getId().toString(),
                    true
            );
        }
    }

    @Transactional
    public void cambiarEstado(Integer id, EstatusTermino nuevoEstado) { // Cambiado a Integer
        Termino termino = terminoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Término no encontrado"));
        EstatusTermino estadoAnterior = termino.getEstatusTermino();
        termino.setEstatusTermino(nuevoEstado);
        terminoRepository.save(termino);

        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        String fechaFormat = LocalDate.now().toString();

        if (termino.getExpediente() != null) {
            registrarActividad(termino.getExpediente(), "Cambio de Estado (Término)",
                "Cambio de estatus de Término a " + nuevoEstado.getDescripcion() + ". Acción realizada por: "
                        + usuarioActual + " el "
                        + fechaFormat + ".",
                    "EDIT");
        }
    }

    private void registrarActividad(Expediente expediente, String titulo, String descripcion, String icono) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        auditoriaService.registrar(expediente, titulo, descripcion, icono, usuario);
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
        res.setActuación(entity.getAsunto());
        res.setFechaVencimiento(entity.getFechaVencimiento());

        res.setExpediente(entity.getNumeroReferencia());

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
