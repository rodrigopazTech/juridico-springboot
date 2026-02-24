package com.juridico.sistema_juridico.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.juridico.sistema_juridico.service.procesal.TerminoImportService;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.juridico.sistema_juridico.service.procesal.TerminoImportService; // IMPORTAR EL SERVICIO
import org.springframework.stereotype.Controller;

@Controller
public class AdminImportController {

    @Autowired
    private TerminoImportService terminoImportService;

    @GetMapping("/admin/importar")
    public String mostrarPaginaImportar(Model model) {
        model.addAttribute("activePage", "importar");
        return "views/admin/importar"; 
    }

    @PostMapping("/admin/importar")
    public String procesarImportacion(
            @RequestParam("file") MultipartFile file, 
            @RequestParam("tipoDato") String tipoDato, 
            RedirectAttributes redirectAttributes) {
        
        try {
            if (file.isEmpty()) throw new Exception("El archivo está vacío.");

            // 2. Conectamos el Switch con el servicio
            switch (tipoDato) {
                case "TERMINOS":
                    terminoImportService.importar(file); // MAGIA ACTIVADA
                    break;
                case "AUDIENCIAS":
                    // audienciaImportService.importar(file); (Próximamente)
                    break;
                default:
                    throw new Exception("Tipo de dato no válido.");
            }
            
            redirectAttributes.addFlashAttribute("mensaje", "Se han importado los datos de " + tipoDato + " correctamente. Los expedientes nuevos fueron creados.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar: " + e.getMessage());
        }
        
        return "redirect:/admin/importar"; 
    }

    // ==========================================================
    // RUTAS PARA DESCARGAR LAS PLANTILLAS DE EXCEL (ENRIQUECIDAS)
    // ==========================================================

    @GetMapping("/admin/plantilla/terminos")
    public ResponseEntity<ByteArrayResource> descargarPlantillaTerminos() {
        // Se agregaron las columnas maestras: Materia, OrganoJurisdiccional, EmailAbogadoResponsable
        String encabezados = "NumeroExpediente,Materia,OrganoJurisdiccional,EmailAbogadoResponsable,Actuacion,FechaVencimiento,Prioridad";
        return generarPlantilla("plantilla_terminos.xlsx", encabezados);
    }

    @GetMapping("/admin/plantilla/audiencias")
    public ResponseEntity<ByteArrayResource> descargarPlantillaAudiencias() {
        // Se agregaron las columnas maestras
        String encabezados = "NumeroExpediente,Materia,OrganoJurisdiccional,EmailAbogadoResponsable,TipoAudiencia,Fecha,Hora,Sala,EsEnLinea,EnlaceReunion";
        return generarPlantilla("plantilla_audiencias.xlsx", encabezados);
    }

    private ResponseEntity<ByteArrayResource> generarPlantilla(String nombreArchivo, String contenidoSimulado) {
        ByteArrayResource resource = new ByteArrayResource(contenidoSimulado.getBytes());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + nombreArchivo)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }
}