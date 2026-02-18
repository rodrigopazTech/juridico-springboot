package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.ColaboradorExpedienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SecurityService {

    @Autowired
    private ColaboradorExpedienteRepository colaboradorRepository;

    /**
     * Verifica si un usuario tiene permiso de LECTURA sobre un expediente.
     */
    public boolean tieneAccesoLectura(Usuario usuario, Expediente expediente) {
        if (usuario == null || expediente == null)
            return false;

        // 1. DIRECCION y SUBDIRECCION tienen acceso total
        if (usuario.getRol() == RolUsuario.DIRECCION || usuario.getRol() == RolUsuario.SUBDIRECCION) {
            return true;
        }

        // 2. Si es de otra Gerencia, denegar (mismo comportamiento que el filtro del
        // index)
        if (usuario.getGerencia() != null && expediente.getGerencia() != null) {
            if (!usuario.getGerencia().getId().equals(expediente.getGerencia().getId())) {
                return false;
            }
        }

        // 3. Casos de acceso permitido para cualquier rol (siempre dentro de la misma
        // gerencia o si no hay gerencia)

        // A. Si es GERENTE de la misma Gerencia
        if (usuario.getRol() == RolUsuario.GERENTE) {
            return true;
        }

        // B. Si es el Abogado Responsable
        if (expediente.getAbogadoResponsable() != null &&
                expediente.getAbogadoResponsable().getId().equals(usuario.getId())) {
            return true;
        }

        // C. Si tiene la Materia asignada
        if (usuario.getMaterias() != null && expediente.getMateria() != null) {
            boolean tieneMateria = usuario.getMaterias().stream()
                    .anyMatch(m -> m.getId().equals(expediente.getMateria().getId()));
            if (tieneMateria)
                return true;
        }

        // D. Si es colaborador activo (ACL temporal)
        return colaboradorRepository.existsByExpedienteIdAndUsuarioIdAndFechaExpiracionAfter(
                expediente.getId(),
                usuario.getId(),
                LocalDateTime.now());
    }

    /**
     * Verifica si un usuario tiene permiso de ESCRITURA sobre un expediente.
     */
    public boolean tieneAccesoEscritura(Usuario usuario, Expediente expediente) {
        if (usuario == null || expediente == null)
            return false;

        // 1. DIRECCION y SUBDIRECCION tienen acceso total
        if (usuario.getRol() == RolUsuario.DIRECCION || usuario.getRol() == RolUsuario.SUBDIRECCION) {
            return true;
        }

        // 2. Si es de otra Gerencia, denegar
        if (usuario.getGerencia() != null && expediente.getGerencia() != null) {
            if (!usuario.getGerencia().getId().equals(expediente.getGerencia().getId())) {
                return false;
            }
        }

        // 3. Si es GERENTE de la misma Gerencia (tiene permisos de supervisión/edición)
        if (usuario.getRol() == RolUsuario.GERENTE) {
            return true;
        }

        // 4. Si es el Abogado Responsable
        if (expediente.getAbogadoResponsable() != null &&
                expediente.getAbogadoResponsable().getId().equals(usuario.getId())) {
            return true;
        }

        // 5. Si tiene la Materia asignada
        if (usuario.getMaterias() != null && expediente.getMateria() != null) {
            boolean tieneMateria = usuario.getMaterias().stream()
                    .anyMatch(m -> m.getId().equals(expediente.getMateria().getId()));
            if (tieneMateria)
                return true;
        }

        return false;
    }
}
