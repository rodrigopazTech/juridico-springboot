package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.expediente.AuditoriaExpediente;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.AuditoriaExpedienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaExpedienteRepository auditoriaRepository;

    @Transactional
    public void registrarAccion(Expediente expediente, Usuario usuario, String accion, String detalle) {
        AuditoriaExpediente auditoria = AuditoriaExpediente.builder()
                .expediente(expediente)
                .usuario(usuario)
                .accion(accion)
                .detalle(detalle)
                .createdAt(LocalDateTime.now())
                .build();
        auditoriaRepository.save(auditoria);
    }

    public List<AuditoriaExpediente> obtenerHistorial(UUID expedienteId) {
        return auditoriaRepository.findByExpedienteIdOrderByCreatedAtDesc(expedienteId);
    }
}
