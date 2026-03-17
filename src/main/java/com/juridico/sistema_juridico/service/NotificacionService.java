package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.usuario.Notificacion;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.usuario.PreferenciaNotificacion;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.PreferenciaNotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    // 1. Inyectamos el repositorio de preferencias
    @Autowired
    private PreferenciaNotificacionRepository preferenciaRepository;

    public void crearNotificacion(Usuario usuario, String titulo, String mensaje, String tipo, Prioridad prioridad,
            String entidadId) {
        if (usuario == null)
            return; // Evitar null pointer

        // 2. Obtener preferencias del usuario (si no existen, crea unas por defecto)
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
                // Diferenciamos por el título si es un término nuevo o uno por vencer
                if (tituloUpper.contains("NUEVO")) {
                    debeNotificar = prefs.isNotificarNuevoTermino();
                } else if (tituloUpper.contains("VENCER") || tituloUpper.contains("VENCIDO")
                        || tituloUpper.contains("CRÍTICA")) {
                    debeNotificar = prefs.isNotificarTerminoVencer();
                }
                break;
        }

        // 4. Si la preferencia está apagada, abortamos silenciosamente
        if (!debeNotificar) {
            return;
        }

        // 5. Si está encendida, procedemos a guardar
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
    }

    public Page<Notificacion> listarMisNotificaciones(Usuario usuario, Pageable pageable) {
        return notificacionRepository.findByUsuarioOrderByCreatedAtDesc(usuario, pageable);
    }

    public long contarNoLeidas(Usuario usuario) {
        return notificacionRepository.countByUsuarioAndLeidaFalse(usuario);
    }
}