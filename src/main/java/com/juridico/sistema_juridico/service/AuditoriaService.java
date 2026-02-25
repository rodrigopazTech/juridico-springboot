package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.ActividadExpedienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditoriaService {

    @Autowired
    private ActividadExpedienteRepository actividadRepository;

    @Transactional
    @SuppressWarnings("null")
    public void registrar(Expediente expediente, String titulo, String descripcion, String tipoIcono, Usuario usuario) {
        ActividadExpediente actividad = ActividadExpediente.builder()
                .expediente(expediente)
                .titulo(titulo)
                .descripcion(descripcion)
                .tipoIcono(tipoIcono)
                .usuario(usuario)
                .fechaRegistro(LocalDateTime.now())
                .build();
        actividadRepository.save(actividad);
    }
}
