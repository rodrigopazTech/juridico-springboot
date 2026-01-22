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

    // LÓGICA PARA CONCLUIR AUDIENCIA
    @Transactional
    public void concluirAudiencia(Integer id, String observaciones, MultipartFile archivoActa) throws IOException {
        Audiencia audiencia = audienciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

        // 1. Guardar el archivo del acta (Si existe)
        if (archivoActa != null && !archivoActa.isEmpty()) {
            String carpeta = "uploads/audiencias/";
            Path ruta = Paths.get(carpeta);
            if (!Files.exists(ruta)) Files.createDirectories(ruta);

            String nombreArchivo = id + "_ACTA_" + archivoActa.getOriginalFilename();
            Files.copy(archivoActa.getInputStream(), ruta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
            
            audiencia.setActaDocumento(nombreArchivo);
        }

        // 2. Actualizar estatus
        audiencia.setEstatusAudiencia("CONCLUIDA");
        audiencia.setFechaDesahogo(LocalDate.now());
        // Aquí podrías guardar las observaciones en algún campo de la entidad Audiencia si lo tuvieras, 
        // o se van directo al histórico.

        audienciaRepository.save(audiencia);

        // 3. Crear registro en Histórico (AudienciaDesahogada)
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