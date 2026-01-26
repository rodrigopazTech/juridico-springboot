package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import com.juridico.sistema_juridico.repository.procesal.AudienciaDesahogadaRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AudienciaService {

    @Autowired
    private AudienciaRepository audienciaRepository;

    @Autowired
    private AudienciaDesahogadaRepository desahogadaRepository;

    // 1. PASO INTERMEDIO: SUBIR ACTA (Cambia a CON_ACTA)
    @Transactional
    public void subirActa(Integer id, MultipartFile archivo) throws IOException {
        Audiencia audiencia = audienciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

        if (!archivo.isEmpty()) {
            String carpeta = "uploads/audiencias/";
            Path ruta = Paths.get(carpeta);
            if (!Files.exists(ruta)) Files.createDirectories(ruta);

            String nombreArchivo = id + "_ACTA_" + archivo.getOriginalFilename();
            Files.copy(archivo.getInputStream(), ruta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
            
            audiencia.setActaDocumento(nombreArchivo);
            
            // Lógica de Estatus: Si estaba pendiente, ahora tiene acta
            if ("PENDIENTE".equals(audiencia.getEstatusAudiencia())) {
                audiencia.setEstatusAudiencia("CON_ACTA");
            }
            audienciaRepository.save(audiencia);
        }
    }

    // 2. PASO FINAL: CONCLUIR (Cambia a CONCLUIDA y Mueve a Histórico)
    @Transactional
    public void concluirAudiencia(Integer id, String observaciones) {
        Audiencia audiencia = audienciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

        // Validamos que tenga acta antes de concluir (Opcional, según tu regla de negocio)
        // if (audiencia.getActaDocumento() == null) throw new RuntimeException("Debes subir el acta antes de concluir.");

        audiencia.setEstatusAudiencia("CONCLUIDA");
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
    }
}