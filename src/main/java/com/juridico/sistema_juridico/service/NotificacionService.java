package com.juridico.sistema_juridico.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.PreferenciaNotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    // 1. Inyectamos el repositorio de preferencias
    @Autowired
    private PreferenciaNotificacionRepository preferenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public void crearNotificacion(Usuario usuario, String titulo, String mensaje, String tipo, CatPrioridad prioridad,
            String entidadId) {
        crearNotificacion(usuario, null, titulo, mensaje, tipo, prioridad, entidadId, false);
    }

    public void crearNotificacion(Usuario usuario, Usuario actor, String titulo, String mensaje, String tipo, CatPrioridad prioridad,
            String entidadId, boolean silenciarSuperiores) {
        if (usuario == null) {
            return; // Evitar null pointer
        }
        // 2. Obtener preferencias del usuario
        PreferenciaNotificacion prefs = preferenciaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> PreferenciaNotificacion.builder().usuario(usuario).build());

        // 3. Evaluar si el usuario quiere recibir esta notificación
        boolean debeNotificar = true;
        String tipoNotif = (tipo != null) ? tipo.toUpperCase() : "";
        String tituloUpper = (titulo != null) ? titulo.toUpperCase() : "";

        switch (tipoNotif) {
            case "AUDIENCIA":
                debeNotificar = prefs.isNotificarNuevaAudiencia();
                break;
            case "ASIGNACION":
                debeNotificar = prefs.isNotificarReasignacion();
                break;
            case "TERMINO":
                if (tituloUpper.contains("NUEVO")) {
                    debeNotificar = prefs.isNotificarNuevoTermino();
                } else if (tituloUpper.contains("VENCER") || tituloUpper.contains("VENCIDO")
                        || tituloUpper.contains("CRÍTICA")) {
                    debeNotificar = prefs.isNotificarTerminoVencer();
                }
                break;
        }

        if (!debeNotificar) {
            return;
        }

        // 5. Guardar notificación principal
        Notificacion notif = Notificacion.builder()
                .usuario(usuario)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(tipo)
                .prioridad(prioridad)
                .entidadTipo(tipo)
                .entidadId(entidadId)
                .notificarEn(LocalDateTime.now())
                .leida(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificacionRepository.save(notif);

        // 6. NOTIFICAR A SUPERIORES con contexto del autor
        if (!silenciarSuperiores) {
            notificarASuperiores(usuario, actor, titulo, mensaje, tipo, prioridad, entidadId);
        }
    }

    private void notificarASuperiores(Usuario subordinado, Usuario actor, String titulo, String mensaje, String tipo, CatPrioridad prioridad, String entidadId) {
        if (subordinado.getGerencia() == null || subordinado.getRol() == null) {
            return;
        }

        List<RolUsuario> rolesSuperiores = new ArrayList<>();
        RolUsuario rolDestinatario = subordinado.getRol();

        if (rolDestinatario == RolUsuario.ABOGADO) {
            rolesSuperiores.addAll(List.of(RolUsuario.JEFE_DEPTO, RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        } else if (rolDestinatario == RolUsuario.JEFE_DEPTO) {
            rolesSuperiores.addAll(List.of(RolUsuario.GERENTE, RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        } else if (rolDestinatario == RolUsuario.GERENTE) {
            rolesSuperiores.addAll(List.of(RolUsuario.DIRECCION, RolUsuario.SUBDIRECCION));
        }

        if (rolesSuperiores.isEmpty()) {
            return;
        }

        List<Usuario> jefes = new ArrayList<>();
        if (subordinado.getGerencia() != null) {
            jefes.addAll(usuarioRepository.findByGerenciaIdAndRolInAndActivoTrue(
                    subordinado.getGerencia().getId(), rolesSuperiores));
        }

        jefes = jefes.stream().distinct().toList();

        // Título mejorado para el Jefe
        String etiquetaAutor = (actor != null) ? actor.getRol().toString() : "SISTEMA";
        String tituloJefe = "[" + etiquetaAutor + "] Asignación a " + rolDestinatario;

        // Mensaje detallado: "¿Quién le asignó a quién?"
        String mensajeJefe = "";
        String numExpediente = (entidadId != null && !entidadId.trim().isEmpty()) ? entidadId : "S/N";
        
        if (actor != null && !actor.getId().equals(subordinado.getId())) {
            // Caso: Alguien más le asignó al subordinado
            mensajeJefe = String.format("El %s %s ha asignado un %s al %s %s en el expediente: %s",
                    actor.getRol(), actor.getNombreCompleto(), tipo.toLowerCase(),
                    rolDestinatario, subordinado.getNombreCompleto(), numExpediente);
        } else {
            // Caso: El mismo usuario creó la actividad o es proceso de sistema
            String nombreAutor = (actor != null) ? actor.getNombreCompleto() : "El sistema";
            mensajeJefe = String.format("%s ha generado actividad de %s para %s en el expediente: %s",
                    nombreAutor, tipo.toLowerCase(), subordinado.getNombreCompleto(), numExpediente);
        }

        for (Usuario jefe : jefes) {
            if (jefe.getId().equals(subordinado.getId()) || (actor != null && jefe.getId().equals(actor.getId()))) {
                continue; // No notificar al receptor ni al autor
            }

            Notificacion notifJefe = Notificacion.builder()
                    .usuario(jefe)
                    .titulo(tituloJefe)
                    .mensaje(mensajeJefe)
                    .tipo(tipo)
                    .prioridad(prioridad)
                    .entidadTipo(tipo)
                    .entidadId(entidadId)
                    .notificarEn(LocalDateTime.now())
                    .leida(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificacionRepository.save(notifJefe);
        }
    }

    public Page<Notificacion> listarMisNotificaciones(Usuario usuario, Pageable pageable) {
        return notificacionRepository.findByUsuarioOrderByCreatedAtDesc(usuario, pageable);
    }

    public long contarNoLeidas(Usuario usuario) {
        return notificacionRepository.countByUsuarioAndLeidaFalse(usuario);
    }
}
// NotificacionService - Version VScode en antigravity - lineas 188