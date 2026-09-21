package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.auditoria.AuditoriaEvento;
import com.juridico.sistema_juridico.Entity.auditoria.AuditoriaSesion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.auditoria.AuditoriaEventoRepository;
import com.juridico.sistema_juridico.repository.auditoria.AuditoriaSesionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaTecnicaService {

    public static final String SESSION_USUARIO_ID = "AUDITORIA_USUARIO_ID";
    private static final List<String> RUTAS_RUIDO_OPERATIVO = List.of(
            "/",
            "/api/webpush/public-key",
            "/api/webpush/subscribe",
            "/notificaciones/api/recientes"
    );

    private final AuditoriaSesionRepository sesionRepository;
    private final AuditoriaEventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.audit.active-session-window:30m}")
    private java.time.Duration activeSessionWindow;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLoginExitoso(Usuario usuario, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_USUARIO_ID, usuario.getId());
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        cerrarSesionesPreviasDelUsuario(usuario, session.getId());

        AuditoriaSesion sesion = AuditoriaSesion.builder()
                .usuario(usuario)
                .email(usuario.getEmail())
                .rol(usuario.getRol() != null ? usuario.getRol().name() : null)
                .sessionId(session.getId())
                .ip(resolveIp(request))
                .userAgent(resolveUserAgent(request))
                .estado("ACTIVA")
                .fechaInicio(LocalDateTime.now())
                .fechaUltimaActividad(LocalDateTime.now())
                .build();
        sesionRepository.save(sesion);

        registrarEventoInterno(usuario, request, "SEGURIDAD", "LOGIN_EXITOSO", "EXITOSO", 200,
                "Inicio de sesion correcto.", null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLoginFallido(String identifier, HttpServletRequest request, String motivo) {
        String login = identifier != null ? identifier.trim() : "";
        Optional<Usuario> usuarioOpt = login.isBlank() ? Optional.empty() : usuarioRepository.findByLoginIdentifier(login);
        Usuario usuario = usuarioOpt.orElse(null);
        AuditoriaEvento evento = eventoBase(request, usuario, "SEGURIDAD", "LOGIN_FALLIDO", "FALLIDO", 401);
        if (usuario == null) {
            evento.setEmail(login);
            evento.setDescripcion("No se encontro un usuario activo con el identificador escrito: " + login + ".");
            evento.setEntidadTipo("LOGIN");
            evento.setEntidadId(login);
        } else if (!Boolean.TRUE.equals(usuario.getActivo())) {
            evento.setDescripcion("La cuenta existe, pero esta inactiva. El usuario no pudo iniciar sesion.");
            evento.setEntidadTipo("USUARIO");
            evento.setEntidadId(String.valueOf(usuario.getId()));
        } else {
            evento.setDescripcion("La contrasena escrita no coincide con la cuenta registrada.");
            evento.setEntidadTipo("USUARIO");
            evento.setEntidadId(String.valueOf(usuario.getId()));
        }
        eventoRepository.save(evento);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLogout(Usuario usuario, HttpServletRequest request) {
        cerrarSesion(request.getSession(false) != null ? request.getSession(false).getId() : null, "LOGOUT");
        registrarEventoInterno(usuario, request, "SEGURIDAD", "LOGOUT", "EXITOSO", 200,
                "Cierre de sesion solicitado por el usuario.", null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cerrarSesion(String sessionId, String motivo) {
        if (sessionId == null) {
            return;
        }
        Optional<AuditoriaSesion> sesionOpt = sesionRepository.findTopBySessionIdOrderByFechaInicioDesc(sessionId);
        if (sesionOpt.isEmpty()) {
            return;
        }
        AuditoriaSesion sesion = sesionOpt.get();
        if (!"ACTIVA".equals(sesion.getEstado())) {
            return;
        }
        sesion.setEstado("CERRADA");
        sesion.setMotivoCierre(motivo);
        sesion.setFechaCierre(LocalDateTime.now());
        sesion.setFechaUltimaActividad(LocalDateTime.now());
        sesionRepository.save(sesion);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void actualizarUltimaActividad(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Optional<AuditoriaSesion> sesionOpt = sesionRepository.findTopBySessionIdOrderByFechaInicioDesc(session.getId());
        if (sesionOpt.isEmpty()) {
            usuarioActual().ifPresent(usuario -> crearSesionRecuperada(usuario, request, session));
            return;
        }
        sesionOpt.ifPresent(sesion -> {
            if ("ACTIVA".equals(sesion.getEstado())) {
                sesion.setFechaUltimaActividad(LocalDateTime.now());
                sesionRepository.save(sesion);
            } else {
                sesion.setEstado("ACTIVA");
                sesion.setMotivoCierre(null);
                sesion.setFechaCierre(null);
                sesion.setFechaUltimaActividad(LocalDateTime.now());
                sesionRepository.save(sesion);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarActividadHttp(HttpServletRequest request, int status) {
        Usuario usuario = usuarioActual().orElse(null);
        String metodo = request.getMethod();
        String accion = "GET".equalsIgnoreCase(metodo)
                ? (request.getRequestURI().startsWith("/api/") || request.getRequestURI().contains("/api/")
                        ? "CONSULTA_API"
                        : "ACCESO_PANTALLA")
                : "ACCION_HTTP";
        String resultado = status >= 400 ? "FALLIDO" : "EXITOSO";
        registrarEventoInterno(usuario, request, moduloDesdeRuta(request.getRequestURI()), accion, resultado, status,
                descripcionHttp(request), entidadDesdeRuta(request.getRequestURI()), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAccesoDenegado(HttpServletRequest request, String motivo) {
        Usuario usuario = usuarioActual().orElse(null);
        registrarEventoInterno(usuario, request, moduloDesdeRuta(request.getRequestURI()), "ACCESO_DENEGADO",
                "DENEGADO", 403, motivo, entidadDesdeRuta(request.getRequestURI()), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarError(HttpServletRequest request, Exception ex) {
        Usuario usuario = usuarioActual().orElse(null);
        String descripcion = ex.getClass().getSimpleName() + ": " + safe(ex.getMessage(), 900);
        registrarEventoInterno(usuario, request, moduloDesdeRuta(request.getRequestURI()), "ERROR_SISTEMA",
                "ERROR", 500, descripcion, entidadDesdeRuta(request.getRequestURI()), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEventoSeguridad(Usuario usuario, String email, HttpServletRequest request, String accion,
            String resultado, Integer codigoHttp, String descripcion, String entidadTipo, String entidadId) {
        AuditoriaEvento evento = eventoBase(request, usuario, "SEGURIDAD", accion, resultado, codigoHttp);
        if (usuario == null) {
            evento.setEmail(email);
        }
        evento.setDescripcion(descripcion);
        evento.setEntidadTipo(entidadTipo);
        evento.setEntidadId(entidadId);
        eventoRepository.save(evento);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarRecuperacionManual(Usuario actor, Usuario destino, HttpServletRequest request,
            String accion, String resultado, Integer codigoHttp, String descripcion) {
        AuditoriaEvento evento = eventoBase(request, actor, "SEGURIDAD", accion, resultado, codigoHttp);
        evento.setDescripcion(descripcion);
        evento.setEntidadTipo("USUARIO");
        evento.setEntidadId(String.valueOf(destino.getId()));
        eventoRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public boolean existeFalloRecienteDeCorreo(Integer usuarioId, java.time.Duration ventana) {
        LocalDateTime limite = LocalDateTime.now().minus(ventana);
        return eventoRepository.existsByAccionAndEntidadIdAndFechaAfter(
                "PASSWORD_RESET_ADMIN_EMAIL_ERROR", String.valueOf(usuarioId), limite);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cerrarSesionesActivasUsuario(Usuario usuario, String motivo) {
        if (usuario == null || usuario.getId() == null) {
            return;
        }
        for (AuditoriaSesion sesion : sesionRepository.findByEstadoAndUsuarioId("ACTIVA", usuario.getId())) {
            sesion.setEstado("CERRADA");
            sesion.setMotivoCierre(motivo);
            sesion.setFechaCierre(LocalDateTime.now());
            sesion.setFechaUltimaActividad(LocalDateTime.now());
            sesionRepository.save(sesion);
        }
    }

    @Transactional
    public Map<String, Object> obtenerResumen() {
        cerrarSesionesInactivas();
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay();
        return Map.of(
                "sesionesActivas", sesionRepository.countByEstadoAndFechaUltimaActividadAfter("ACTIVA", limiteSesionActiva()),
                "loginsHoy", eventoRepository.countByAccionAndFechaBetween("LOGIN_EXITOSO", inicio, fin),
                "logoutsHoy", sesionRepository.countByFechaCierreBetween(inicio, fin),
                "eventosHoy", eventoRepository.countByFechaBetween(inicio, fin),
                "loginFallidosHoy", eventoRepository.countByAccionAndFechaBetween("LOGIN_FALLIDO", inicio, fin),
                "denegadosHoy", eventoRepository.countByResultadoAndFechaBetween("DENEGADO", inicio, fin),
                "erroresHoy", eventoRepository.countByResultadoAndFechaBetween("ERROR", inicio, fin)
        );
    }

    @Transactional
    public List<AuditoriaSesion> sesionesRecientes() {
        cerrarSesionesInactivas();
        return sesionRepository.findTop12ByEstadoAndFechaUltimaActividadAfterOrderByFechaUltimaActividadDesc(
                "ACTIVA", limiteSesionActiva());
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEvento> eventosRecientes() {
        return eventoRepository.findTop20Operativos(RUTAS_RUIDO_OPERATIVO, PageRequest.of(0, 20));
    }

    @Transactional
    public Page<AuditoriaSesion> sesiones(Pageable pageable) {
        cerrarSesionesInactivas();
        return sesionRepository.findByEstadoAndFechaUltimaActividadAfterOrderByFechaUltimaActividadDesc(
                "ACTIVA", limiteSesionActiva(), pageable);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cerrarSesionesInactivas() {
        LocalDateTime limite = limiteSesionActiva();
        List<AuditoriaSesion> inactivas = sesionRepository.findByEstadoAndFechaUltimaActividadBefore("ACTIVA", limite);
        for (AuditoriaSesion sesion : inactivas) {
            sesion.setEstado("CERRADA");
            sesion.setMotivoCierre("INACTIVIDAD");
            sesion.setFechaCierre(LocalDateTime.now());
            sesionRepository.save(sesion);
        }
        cerrarSesionesDuplicadasActivas();
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaEvento> eventos(Pageable pageable) {
        return eventoRepository.findOperativos(RUTAS_RUIDO_OPERATIVO, pageable);
    }

    public boolean esRutaRuidoOperativo(String ruta) {
        return ruta != null && RUTAS_RUIDO_OPERATIVO.contains(ruta);
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaEvento> eventosPorResultado(List<String> resultados, Pageable pageable) {
        return eventoRepository.findByResultadoInOrderByFechaDesc(resultados, pageable);
    }

    private void registrarEventoInterno(Usuario usuario, HttpServletRequest request, String modulo, String accion,
            String resultado, Integer codigoHttp, String descripcion, String entidadTipo, String entidadId) {
        AuditoriaEvento evento = eventoBase(request, usuario, modulo, accion, resultado, codigoHttp);
        evento.setDescripcion(descripcion);
        evento.setEntidadTipo(entidadTipo);
        evento.setEntidadId(entidadId);
        eventoRepository.save(evento);
    }

    private void cerrarSesionesPreviasDelUsuario(Usuario usuario, String sessionIdActual) {
        if (usuario == null || usuario.getId() == null || sessionIdActual == null) {
            return;
        }
        List<AuditoriaSesion> previas = sesionRepository.findByEstadoAndUsuarioIdAndSessionIdNot(
                "ACTIVA", usuario.getId(), sessionIdActual);
        for (AuditoriaSesion previa : previas) {
            previa.setEstado("CERRADA");
            previa.setMotivoCierre("NUEVO_LOGIN");
            previa.setFechaCierre(LocalDateTime.now());
            previa.setFechaUltimaActividad(LocalDateTime.now());
            sesionRepository.save(previa);
        }
    }

    private void crearSesionRecuperada(Usuario usuario, HttpServletRequest request, HttpSession session) {
        cerrarSesionesPreviasDelUsuario(usuario, session.getId());
        AuditoriaSesion sesion = AuditoriaSesion.builder()
                .usuario(usuario)
                .email(usuario.getEmail())
                .rol(usuario.getRol() != null ? usuario.getRol().name() : null)
                .sessionId(session.getId())
                .ip(resolveIp(request))
                .userAgent(resolveUserAgent(request))
                .estado("ACTIVA")
                .fechaInicio(LocalDateTime.now())
                .fechaUltimaActividad(LocalDateTime.now())
                .build();
        sesionRepository.save(sesion);
    }

    private void cerrarSesionesDuplicadasActivas() {
        List<AuditoriaSesion> activas = sesionRepository.findActivasOrdenadasParaNormalizar("ACTIVA");
        Set<Integer> usuariosVistos = new HashSet<>();
        for (AuditoriaSesion sesion : activas) {
            if (sesion.getUsuario() == null || sesion.getUsuario().getId() == null) {
                continue;
            }
            Integer usuarioId = sesion.getUsuario().getId();
            if (usuariosVistos.add(usuarioId)) {
                continue;
            }
            sesion.setEstado("CERRADA");
            sesion.setMotivoCierre("SESION_DUPLICADA");
            sesion.setFechaCierre(LocalDateTime.now());
            sesionRepository.save(sesion);
        }
    }

    private AuditoriaEvento eventoBase(HttpServletRequest request, Usuario usuario, String modulo, String accion,
            String resultado, Integer codigoHttp) {
        return AuditoriaEvento.builder()
                .usuario(usuario)
                .email(usuario != null ? usuario.getEmail() : null)
                .rol(usuario != null && usuario.getRol() != null ? usuario.getRol().name() : null)
                .sessionId(request.getSession(false) != null ? request.getSession(false).getId() : null)
                .ip(resolveIp(request))
                .userAgent(resolveUserAgent(request))
                .fecha(LocalDateTime.now())
                .modulo(modulo)
                .accion(accion)
                .ruta(request.getRequestURI())
                .metodoHttp(request.getMethod())
                .resultado(resultado)
                .codigoHttp(codigoHttp)
                .build();
    }

    private Optional<Usuario> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmail(auth.getName());
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return safe(request.getHeader("User-Agent"), 1000);
    }

    private String moduloDesdeRuta(String ruta) {
        if (ruta == null || "/".equals(ruta)) {
            return "AUTH";
        }
        String limpia = ruta.startsWith("/") ? ruta.substring(1) : ruta;
        int slash = limpia.indexOf('/');
        return (slash >= 0 ? limpia.substring(0, slash) : limpia).toUpperCase();
    }

    private String entidadDesdeRuta(String ruta) {
        String modulo = moduloDesdeRuta(ruta);
        if ("EXPEDIENTES".equals(modulo) || "AUDIENCIAS".equals(modulo) || "TERMINOS".equals(modulo)
                || "USUARIOS".equals(modulo) || "CATALOGOS".equals(modulo)) {
            return modulo;
        }
        return null;
    }

    private String descripcionHttp(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }

    private LocalDateTime limiteSesionActiva() {
        java.time.Duration ventana = activeSessionWindow != null ? activeSessionWindow : java.time.Duration.ofMinutes(30);
        return LocalDateTime.now().minus(ventana);
    }

    private String safe(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
