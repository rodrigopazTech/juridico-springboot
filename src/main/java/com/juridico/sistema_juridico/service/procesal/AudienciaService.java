package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.audiencias.AudienciaRequest;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaDesahogadaRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.service.NotificacionService;
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;

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
            List<Usuario> directivos = usuarioRepository.findAll().stream()
                    .filter(u -> u.getRol().name().equals("DIRECCION") || u.getRol().name().equals("SUBDIRECCION"))
                    .filter(Usuario::getActivo)
                    .collect(Collectors.toList());

            // Aquí usamos el expediente que ya viene cargado porque es una actualización
            String expediente = (audiencia.getExpediente() != null) ? audiencia.getExpediente().getNumero()
                    : "Desconocido";

            String nombreAbogado;
            if (audiencia.getAbogadoComparece() != null) {
                nombreAbogado = audiencia.getAbogadoComparece().getNombreCompleto();
            } else {
                nombreAbogado = audiencia.getExpediente().getAbogadoResponsable().getNombreCompleto();
            }

            String linkRedireccion = "/audiencias?keyword=" + audiencia.getId();

            for (Usuario directivo : directivos) {
                notificacionService.crearNotificacion(
                        directivo,
                        "Acta Disponible: " + expediente,
                        "El abogado " + nombreAbogado + " ha subido el acta. Requiere validación.",
                        "AUDIENCIA",
                        Prioridad.ALTA,
                        linkRedireccion);
            }
        } catch (Exception e) {
            System.err.println("Error al enviar notificación de acta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 2. PASO FINAL: CONCLUIR (Cambia a CONCLUIDA y Mueve a Histórico)
    @Transactional
    @SuppressWarnings("null")
    public void concluirAudiencia(Integer id, String observaciones) {
        Audiencia audiencia = audienciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

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

        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        String fechaFormat = LocalDate.now().toString();
        registrarActividad(audiencia.getExpediente(), "Audiencia Concluida",
                "La audiencia ha sido marcada como concluida: " + tipoConcluida + ". Acción realizada por: "
                        + usuarioActual + " el " + fechaFormat + ".",
                "CHECK");
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

            // 4. NOTIFICAR AL RESPONSABLE (Dueño del expediente)
            if (responsable != null && !responsable.getEmail().equalsIgnoreCase(emailCreador)) {
                notificacionService.crearNotificacion(
                        responsable,
                        tituloBase,
                        mensajeBase,
                        "AUDIENCIA",
                        Prioridad.MEDIA,
                        entidadId);
            }

            // 5. NOTIFICAR AL QUE COMPARECE (El asignado)
            if (comparece != null && !comparece.getEmail().equalsIgnoreCase(emailCreador)) {

                if (responsable == null || !comparece.getId().equals(responsable.getId())) {
                    notificacionService.crearNotificacion(
                            comparece,
                            "Asignación de Comparecencia",
                            "Has sido asignado para asistir a la audiencia del expediente " + numeroExpediente,
                            "AUDIENCIA",
                            Prioridad.ALTA,
                            entidadId);
                }
            }

            String tipoProg = (a.getTipoAudiencia() != null) ? a.getTipoAudiencia().getNombre() : "Desconocida";

            String fechaFormat = LocalDate.now().toString();
            registrarActividad(a.getExpediente(), "Audiencia Creada",
                    "Se ha programado una nueva audiencia: " + tipoProg + " para el " + a.getFechaAudiencia()
                            + ". Acción realizada por: " + emailCreador + " el " + fechaFormat + ".",
                    "PLUS");

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
}
