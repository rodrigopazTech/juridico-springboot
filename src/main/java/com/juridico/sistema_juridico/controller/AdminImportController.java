package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Controller
public class AdminImportController {

    @GetMapping("/admin/importar")
    public String mostrarPaginaImportar(Model model) {
        model.addAttribute("activePage", "importar");
        return "views/admin/importar"; 
    }

    @PostMapping("/admin/importar")
    public String procesarImportacion(
            @RequestParam("file") MultipartFile file, 
            @RequestParam("tipoDato") String tipoDato, // <-- Recibimos qué estamos importando
            RedirectAttributes redirectAttributes) {
        
        try {
            if (file.isEmpty()) {
                throw new Exception("El archivo está vacío.");
            }

            // Aquí conectaremos la lógica real de lectura de Excel dependiendo del tipo
            switch (tipoDato) {
                case "EXPEDIENTES":
                    // expedienteImportService.importar(file);
                    System.out.println("Procesando Excel de Expedientes...");
                    break;
                case "TERMINOS":
                    // terminoImportService.importar(file);
                    System.out.println("Procesando Excel de Términos...");
                    break;
                case "AUDIENCIAS":
                    // audienciaImportService.importar(file);
                    System.out.println("Procesando Excel de Audiencias...");
                    break;
                default:
                    throw new Exception("Tipo de dato no válido.");
            }
            
            redirectAttributes.addFlashAttribute("mensaje", "Se han importado los datos de " + tipoDato + " correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar: " + e.getMessage());
        }
        
        return "redirect:/admin/importar"; 
    }

    // ==========================================================
    // RUTAS PARA DESCARGAR LAS PLANTILLAS DE EXCEL (PROVISIONAL)
    // ==========================================================
    
    @GetMapping("/admin/plantilla/expedientes")
    public ResponseEntity<ByteArrayResource> descargarPlantillaExpedientes() {
        return generarPlantilla("plantilla_expedientes.xlsx", "Numero,Materia,Juzgado,Abogado");
    }

    @GetMapping("/admin/plantilla/terminos")
    public ResponseEntity<ByteArrayResource> descargarPlantillaTerminos() {
        return generarPlantilla("plantilla_terminos.xlsx", "Expediente,Actuacion,FechaVencimiento,Prioridad");
    }

    @GetMapping("/admin/plantilla/audiencias")
    public ResponseEntity<ByteArrayResource> descargarPlantillaAudiencias() {
        return generarPlantilla("plantilla_audiencias.xlsx", "Expediente,TipoAudiencia,Fecha,Hora,Sala");
    }

    // Método auxiliar para no romper la vista (Se reemplazará con Apache POI para hacer Excels reales)
    private ResponseEntity<ByteArrayResource> generarPlantilla(String nombreArchivo, String contenidoSimulado) {
        ByteArrayResource resource = new ByteArrayResource(contenidoSimulado.getBytes());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + nombreArchivo)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }
}