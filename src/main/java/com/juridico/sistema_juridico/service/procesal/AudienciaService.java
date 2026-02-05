package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.Prioridad; 
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaDesahogadaRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.service.NotificacionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AudienciaService {

    @Autowired
    private AudienciaRepository audienciaRepository;

    @Autowired
    private AudienciaDesahogadaRepository desahogadaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacionService notificacionService;

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
                notificarSubidaActa(audiencia);
            }
            audienciaRepository.save(audiencia);
        }
    }

    // Método privado para enviar la alerta
    private void notificarSubidaActa(Audiencia audiencia) {
        try {
            List<Usuario> directivos = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol().name().equals("DIRECCION") || u.getRol().name().equals("SUBDIRECCION"))
                .filter(Usuario::getActivo)
                .collect(Collectors.toList());

            String expediente = audiencia.getExpediente().getNumero();
            
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
                    linkRedireccion  
                );
            }
        } catch (Exception e) {
            System.err.println("Error al enviar notificación de acta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 2. PASO FINAL: CONCLUIR (Cambia a CONCLUIDA y Mueve a Histórico)
    @Transactional
    public void concluirAudiencia(Integer id, String observaciones) {
        Audiencia audiencia = audienciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

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