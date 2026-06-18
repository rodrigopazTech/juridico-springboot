package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.audiencias.AudienciaRequest;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaDesahogadaRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.NotificacionEvento;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import java.time.LocalTime;

@Service
public class AudienciaService {

    @Autowired
    private AudienciaRepository audienciaRepository;

    @Autowired
    private AudienciaDesahogadaRepository desahogadaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private TipoAudienciaRepository tipoAudienciaRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private CatPrioridadRepository catPrioridadRepository;

    @Autowired
    private com.juridico.sistema_juridico.repository.Expediente.ColaboradorExpedienteRepository colaboradorExpedienteRepository;

    // 1. PASO INTERMEDIO: SUBIR ACTA (Cambia a CON_ACTA)
    @Transactional
    @SuppressWarnings("null")
    public void subirActa(Integer id, MultipartFile archivo) throws IOException {
        Audiencia audiencia = audienciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

        if (!archivo.isEmpty()) {
            String carpeta = "uploads/audiencias/";
            Path ruta = Paths.get(carpeta);
            if (!Files.exists(ruta))
                Files.createDirectories(ruta);

            String nombreArchivo = id + "_ACTA_" + archivo.getOriginalFilename();
            Files.copy(archivo.getInputStream(), ruta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);

            audiencia.setActaDocumento(nombreArchivo);
            // Lógica de Estatus: Si estaba pendiente, ahora tiene acta
            if (EstatusAudiencia.PENDIENTE.equals(audiencia.getEstatusAudiencia())) {
                audiencia.setEstatusAudiencia(EstatusAudiencia.CON_ACTA);
                notificarSubidaActa(audiencia);
            }
            audienciaRepository.save(audiencia);

            registrarActividad(audiencia.getExpediente(), "ARCHIVO CARGADO",
                    "Acta: " + archivo.getOriginalFilename(),
                    "UPLOAD");
        }
    }

    // Método privado para enviar la alerta
    private void notificarSubidaActa(Audiencia audiencia) {
        try {
            // 1. Obtener destinatarios: Dirección, Subdirección
            List<Usuario> destinatarios = usuarioRepository.findAll().stream()
                    .filter(u -> u.getActivo())
                    .filter(u -> u.getRol() == RolUsuario.DIRECCION || u.getRol() == RolUsuario.SUBDIRECCION)
                    .collect(Collectors.toList());

            // 2. Agregar Gerentes de la misma Gerencia
            if (audiencia.getExpediente().getGerencia() != null) {
                Integer gerenciaId = audiencia.getExpediente().getGerencia().getId();
                List<Usuario> gerentes = usuarioRepository.findAll().stream()
                        .filter(u -> u.getActivo() && u.getRol() == RolUsuario.GERENTE)
                        .filter(u -> u.getGerencia() != null && u.getGerencia().getId().equals(gerenciaId))
                        .collect(Collectors.toList());
                destinatarios.addAll(gerentes);
            }

            // 3. Agregar Jefes de Departamento de la misma Materia
            if (audiencia.getExpediente().getMateria() != null) {
                Integer materiaId = audiencia.getExpediente().getMateria().getId();
                List<Usuario> jefes = usuarioRepository.findAll().stream()
                        .filter(u -> u.getActivo() && u.getRol() == RolUsuario.JEFE_DEPTO)
                        .filter(u -> u.tieneMateria(materiaId))
                        .collect(Collectors.toList());
                destinatarios.addAll(jefes);
            }

            // Eliminar duplicados si los hay
            destinatarios = destinatarios.stream().distinct().collect(Collectors.toList());

            String expediente = (audiencia.getExpediente() != null) ? audiencia.getExpediente().getNumero()
                    : "Desconocido";

            String nombreAbogado;
            if (audiencia.getAbogadoComparece() != null) {
                nombreAbogado = audiencia.getAbogadoComparece().getNombreCompleto();
            } else {
                nombreAbogado = audiencia.getExpediente().getAbogadoResponsable().getNombreCompleto();
            }

            String entidadId = audiencia.getId().toString();

            String emailActor = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actor = usuarioRepository.findByEmail(emailActor).orElse(null);

            for (Usuario destinatario : destinatarios) {
                notificacionService.crearNotificacion(
                        destinatario,
                        actor,
                        NotificacionEvento.AUDIENCIA_ACTA_DISPONIBLE,
                        "Acta Disponible: " + expediente,
                        "El abogado " + nombreAbogado + " ha subido el acta. Requiere validación.",

                        catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null),
                        entidadId,
                        true);
            }
        } catch (Exception e) {
            System.err.println("Error al enviar notificación de acta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 2. PASO FINAL: CONCLUIR (Cambia a CONCLUIDA y Mueve a Histórico)
    @Transactional
    @SuppressWarnings("null")
    public void concluirAudiencia(Integer id, String observaciones, Usuario usuarioActual) {
        Audiencia audiencia = audienciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

        // --- SEGURIDAD: Validar permisos de conclusión ---
        validarPermisoConclusion(audiencia, usuarioActual);

        audiencia.setEstatusAudiencia(EstatusAudiencia.CONCLUIDA);
        audiencia.setFechaDesahogo(LocalDate.now());
        audiencia.setObservaciones(observaciones);

        audienciaRepository.save(audiencia);

        // Crear registro en Histórico
        AudienciaDesahogada historico = AudienciaDesahogada.builder()
                .audiencia(audiencia)
                .expedienteNumero(audiencia.getExpediente().getNumero())
                .fechaAudiencia(audiencia.getFechaAudiencia())
                .horaAudiencia(audiencia.getHoraAudiencia())
                .fechaDesahogo(LocalDate.now())
                .sincronizadoAt(LocalDateTime.now())
                .build();

        desahogadaRepository.save(historico);
        String tipoConcluida = (audiencia.getTipoAudiencia() != null) ? audiencia.getTipoAudiencia().getNombre()
                : "Desconocida";

        String fechaFormat = LocalDate.now().toString();

        String desc = "La audiencia ha sido marcada como concluida: " + tipoConcluida + ".";
        if (observaciones != null && !observaciones.trim().isEmpty()) {
            desc += " - Observaciones: " + observaciones;
        }
        desc += " Acción realizada por: " + usuarioActual.getNombreCompleto() + " el " + fechaFormat + ".";

        registrarActividad(audiencia.getExpediente(), "Audiencia Concluida", desc, "CHECK");

        // Notificar al abogado responsable si la audiencia fue concluida por un superior
        Usuario abogadoResponsable = audiencia.getExpediente().getAbogadoResponsable();
        Usuario abogadoComparece = audiencia.getAbogadoComparece(); // Si existe

        if (abogadoComparece != null && !abogadoComparece.getId().equals(usuarioActual.getId())) {
            notificacionService.crearNotificacion(
                    abogadoComparece,
                    usuarioActual,
                    NotificacionEvento.AUDIENCIA_VALIDADA_CONCLUIDA,
                    "Audiencia Validada y Concluida",
                    "Tu audiencia del expediente " + audiencia.getExpediente().getNumero() + " ha sido validada y concluida.",

                    catPrioridadRepository.findByNombreIgnoreCase("MEDIA").orElse(null),
                    audiencia.getId().toString(),
                    true
            );
        }
        if (abogadoResponsable != null && !abogadoResponsable.getId().equals(usuarioActual.getId()) && (abogadoComparece == null || !abogadoResponsable.getId().equals(abogadoComparece.getId()))) {
            notificacionService.crearNotificacion(
                    abogadoResponsable,
                    usuarioActual,
                    NotificacionEvento.AUDIENCIA_VALIDADA_CONCLUIDA,
                    "Audiencia Validada y Concluida",
                    "La audiencia del expediente " + audiencia.getExpediente().getNumero() + " ha sido validada y concluida.",

                    catPrioridadRepository.findByNombreIgnoreCase("MEDIA").orElse(null),
                    audiencia.getId().toString(),
                    true
            );
        }

    }

    private void validarPermisoConclusion(Audiencia audiencia, Usuario usuario) {
        String rol = usuario.getRol().name();

        // 1. Admin y Dirección pueden concluir todo
        if (rol.equals("ADMIN") || rol.equals("DIRECCION") || rol.equals("SUBDIRECCION")) {
            return;
        }

        // 2. Abogados pueden concluir si son responsables del caso, comparecientes o colaboradores activos
        if (rol.equals("ABOGADO")) {
            boolean tienePermiso = false;
            if (audiencia.getExpediente() != null) {
                Expediente exp = audiencia.getExpediente();
                if (exp.getAbogadoResponsable() != null && exp.getAbogadoResponsable().getId().equals(usuario.getId())) {
                    tienePermiso = true;
                } else if (colaboradorExpedienteRepository.existsByExpedienteIdAndUsuarioIdAndFechaExpiracionAfter(
                        exp.getId(), usuario.getId(), LocalDateTime.now())) {
                    tienePermiso = true;
                }
            }
            if (audiencia.getAbogadoComparece() != null && audiencia.getAbogadoComparece().getId().equals(usuario.getId())) {
                tienePermiso = true;
            }
            
            if (!tienePermiso) {
                throw new com.juridico.sistema_juridico.exception.ForbiddenException(
                        "Acceso denegado: Solo el abogado responsable, compareciente o colaborador activo puede concluir esta audiencia.");
            }
            return;
        }

        // 3. Gerentes deben ser de la misma Gerencia
        if (rol.equals("GERENTE")) {
            if (usuario.getGerencia() == null || audiencia.getExpediente().getGerencia() == null ||
                    !usuario.getGerencia().getId().equals(audiencia.getExpediente().getGerencia().getId())) {
                throw new com.juridico.sistema_juridico.exception.ForbiddenException(
                        "Acceso denegado: Solo el Gerente del área correspondiente puede concluir esta audiencia.");
            }
            return;
        }

        // 4. Jefes de Departamento deben tener la materia asignada
        if (rol.equals("JEFE_DEPTO")) {
            if (audiencia.getExpediente().getMateria() == null ||
                    !usuario.tieneMateria(audiencia.getExpediente().getMateria().getId())) {
                throw new com.juridico.sistema_juridico.exception.ForbiddenException(
                        "Acceso denegado: Solo el Jefe de Departamento de la materia correspondiente puede concluir esta audiencia.");
            }
            return;
        }

        // Por defecto, denegar
        throw new com.juridico.sistema_juridico.exception.ForbiddenException("Acceso denegado para realizar esta acción.");
    }

    @Transactional
    @SuppressWarnings("null")
    public void notificarCreacionAudiencia(Audiencia audiencia) {
        try {
            // 1. Obtener la Audiencia Fresca
            Audiencia a = audienciaRepository.findById(audiencia.getId())
                    .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

            // 2. RECUPERAR DATOS FALTANTES (Corrección del NULL)

            // A) Expediente: Forzamos la búsqueda del número real
            String numeroExpediente = "Desconocido";
            if (a.getExpediente() != null) {
                numeroExpediente = expedienteRepository.findById(a.getExpediente().getId())
                        .map(Expediente::getNumero)
                        .orElse("Desconocido");
            }

            // B) Usuarios: Recuperación explícita
            Usuario responsable = null;
            if (a.getExpediente() != null && a.getExpediente().getAbogadoResponsable() != null) {
                responsable = usuarioRepository.findById(a.getExpediente().getAbogadoResponsable().getId())
                        .orElse(null);
            }

            Usuario comparece = null;
            if (a.getAbogadoComparece() != null) {
                comparece = usuarioRepository.findById(a.getAbogadoComparece().getId()).orElse(null);
            }

            // 3. Preparar datos de la notificación
            String emailCreador = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            String tituloBase = "Nueva Audiencia: " + numeroExpediente;
            String mensajeBase = "Programada para el " + a.getFechaAudiencia() + " a las " + a.getHoraAudiencia();
            String entidadId = a.getId().toString();

            Usuario actor = usuarioRepository.findByEmail(emailCreador).orElse(null);

            // 4. NOTIFICAR AL RESPONSABLE (Dueño del expediente)
            if (responsable != null && !responsable.getEmail().equalsIgnoreCase(emailCreador)) {
                notificacionService.crearNotificacion(
                        responsable,
                        actor,
                        NotificacionEvento.AUDIENCIA_NUEVA,
                        tituloBase,
                        mensajeBase,

                        catPrioridadRepository.findByNombreIgnoreCase("MEDIA").orElse(null),
                        entidadId,
                        false);
            }

            // 5. NOTIFICAR AL QUE COMPARECE (El asignado)
            if (comparece != null && !comparece.getEmail().equalsIgnoreCase(emailCreador)) {

                if (responsable == null || !comparece.getId().equals(responsable.getId())) {
                    notificacionService.crearNotificacion(
                            comparece,
                            actor,
                            NotificacionEvento.AUDIENCIA_COMPARECENCIA_ASIGNADA,
                            "Asignacion de Comparecencia",
                            "Has sido asignado para asistir a la audiencia del expediente " + numeroExpediente,

                            catPrioridadRepository.findByNombreIgnoreCase("ALTA").orElse(null),
                            entidadId,
                            false);
                }
            }

            String tipoProg = (a.getTipoAudiencia() != null) ? a.getTipoAudiencia().getNombre() : "Desconocida";

            String fechaFormat = LocalDate.now().toString();
            String desc = "Se ha programado una nueva audiencia: " + tipoProg + " para el " + a.getFechaAudiencia();
            if (a.getObservaciones() != null && !a.getObservaciones().isEmpty()) {
                desc += " - Nota: " + a.getObservaciones();
            }
            desc += ". Acción realizada por: " + emailCreador + " el " + fechaFormat + ".";

            registrarActividad(a.getExpediente(), "Audiencia Creada", desc, "PLUS");

        } catch (Exception e) {
            System.err.println("CRÍTICO - Error al notificar creación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public Audiencia guardarAudiencia(AudienciaRequest request) {
        // 1. Conversión de ID a UUID
        Expediente exp = expedienteRepository.findById(UUID.fromString(request.getAsuntoId().toString()))
                .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));

        // 2. Buscar el objeto TipoAudiencia del catálogo
        com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia tipo = tipoAudienciaRepository.findAll().stream()
                .filter(t -> t.getNombre().equalsIgnoreCase(request.getTipoAudiencia()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Tipo de audiencia no válido"));

        // 3. Conversión de String a LocalTime para la hora
        LocalTime horaFormateada = (request.getHora() != null) ? LocalTime.parse(request.getHora()) : null;

        Audiencia audiencia = Audiencia.builder()
                .expediente(exp)
                .tipoAudiencia(tipo)
                .fechaAudiencia(request.getFecha())
                .horaAudiencia(horaFormateada)
                .salaLugar(request.getSala())
                .estatusAudiencia(EstatusAudiencia.PENDIENTE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return audienciaRepository.save(audiencia);
    }

    private void registrarActividad(Expediente expediente, String titulo, String descripcion, String tipoIcono) {
        if (expediente == null)
            return;

        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        auditoriaService.registrar(expediente, titulo, descripcion, tipoIcono, usuario);
    }

    @Transactional
    public void registrarEdicionAudiencia(Audiencia a) {
        try {
            String emailCreador = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            String tipoProg = (a.getTipoAudiencia() != null) ? a.getTipoAudiencia().getNombre() : "Desconocida";
            String fechaFormat = LocalDate.now().toString();

            String desc = "Se han guardado cambios en la audiencia: " + tipoProg;
            if (a.getObservaciones() != null && !a.getObservaciones().isEmpty()) {
                desc += " - Nota: " + a.getObservaciones();
            }
            desc += ". Acción realizada por: " + emailCreador + " el " + fechaFormat + ".";

            registrarActividad(a.getExpediente(), "Audiencia Editada", desc, "EDIT");
        } catch (Exception e) {
            System.err.println("Error al registrar auditoria edicion: " + e.getMessage());
        }
    }
}
