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
        if (usuario == null) {
            return; // Evitar null pointer
        }
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

        // 6. NOTIFICAR A SUPERIORES (Jerarquía de Gerencia)
        notificarASuperiores(usuario, titulo, mensaje, tipo, prioridad, entidadId);
    }

    private void notificarASuperiores(Usuario subordinado, String titulo, String mensaje, String tipo, CatPrioridad prioridad, String entidadId) {
        if (subordinado.getGerencia() == null || subordinado.getRol() == null) {
            return;
        }

        List<RolUsuario> rolesSuperiores = new ArrayList<>();
        RolUsuario rolActual = subordinado.getRol();

        // Definir quiénes son "jefes" según el rol del que crea/recibe la acción
        if (rolActual == RolUsuario.ABOGADO) {
            rolesSuperiores.add(RolUsuario.JEFE_DEPTO);
            rolesSuperiores.add(RolUsuario.GERENTE);
            rolesSuperiores.add(RolUsuario.DIRECCION);
            rolesSuperiores.add(RolUsuario.SUBDIRECCION);
        } else if (rolActual == RolUsuario.JEFE_DEPTO) {
            rolesSuperiores.add(RolUsuario.GERENTE);
            rolesSuperiores.add(RolUsuario.DIRECCION);
            rolesSuperiores.add(RolUsuario.SUBDIRECCION);
        } else if (rolActual == RolUsuario.GERENTE) {
            rolesSuperiores.add(RolUsuario.DIRECCION);
            rolesSuperiores.add(RolUsuario.SUBDIRECCION);
        }

        if (rolesSuperiores.isEmpty()) {
            return;
        }

        // Buscar a los jefes de la MISMA gerencia
        List<Usuario> jefes = usuarioRepository.findByGerenciaIdAndRolInAndActivoTrue(
                subordinado.getGerencia().getId(), rolesSuperiores);
        String nombreSubordinado = subordinado.getNombreCompleto();

        String tituloJefe = "[" + rolActual + "]"+titulo;
        //String mensajeJefe = "Actividad de " + nombreSubordinado + ": " + mensaje;
        String mensajeJefe = "";

        if (tipo == "TERMINO") {
            mensajeJefe = nombreSubordinado + " " + " Ha creado un témino nuevo";
        }
        if (tipo == "AUDIENCIA") {
            mensajeJefe = nombreSubordinado + " " + " Ha asignado una audiencia nueva";
        }
        if (tipo == "ASIGNACIÓN") {
            mensajeJefe = nombreSubordinado + " " + " Ha hecho una asignación nueva";
        }

        for (Usuario jefe : jefes) {
            // Evitar duplicados si el jefe ya es el usuario asignado (aunque la lógica ya lo separa)
            if (jefe.getId().equals(subordinado.getId())) {
                continue;
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
