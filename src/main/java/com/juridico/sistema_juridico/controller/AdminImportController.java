package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.LoteMigracion;
import com.juridico.sistema_juridico.Entity.procesal.CatalogoMigracion;
import com.juridico.sistema_juridico.service.procesal.LoteMigracionService;
import com.juridico.sistema_juridico.repository.procesal.CatalogoMigracionRepository;
import com.juridico.sistema_juridico.repository.Catalogo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Controller
@RequestMapping("/admin/importar")
public class AdminImportController {

    @Autowired private LoteMigracionService loteMigracionService;
    @Autowired private CatalogoMigracionRepository catalogoMigracionRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private TipoAudienciaRepository tipoAudienciaRepository;

    @GetMapping
    public String mostrarWizard(Model model) {
        model.addAttribute("activePage", "importar");

        LoteMigracion loteActivo = loteMigracionService.obtenerLoteActivoOpcional();

        if (loteActivo == null || !loteActivo.getEstatus().equals("PENDIENTE")) {
            // No hay migración en curso: Mostramos pantalla para subir Excel o esperar a n8n
            model.addAttribute("modo", "INICIO");
        } else {
            // Hay una migración pausada o iniciada: Mostramos el Wizard
            model.addAttribute("modo", "WIZARD");
            model.addAttribute("lote", loteActivo);
            model.addAttribute("pasoActual", loteActivo.getPasoActual());

            // Dependiendo del paso, mandamos información a la vista
            switch (loteActivo.getPasoActual()) {
               case 1:
                    // 1. Forzamos la extracción de textos del JSON
                    loteMigracionService.extraerCatalogosDeLote(loteActivo.getId());
                    
                    // 2. Traemos la lista de textos extraídos y los agrupamos por tipo (Gerencia, Materia, etc.)
                    List<CatalogoMigracion> catalogosExtraidos = catalogoMigracionRepository.findByLoteIdAndTipoCatalogo(loteActivo.getId(), "GERENCIA"); // Haremos esto más dinámico pero sirve para agrupar
                    List<CatalogoMigracion> todosCatalogos = catalogoMigracionRepository.findAll().stream().filter(c-> c.getLote().getId().equals(loteActivo.getId())).collect(Collectors.toList());
                    
                    Map<String, List<CatalogoMigracion>> catalogosAgrupados = todosCatalogos.stream()
                        .collect(Collectors.groupingBy(CatalogoMigracion::getTipoCatalogo));
                    
                    model.addAttribute("catalogosAgrupados", catalogosAgrupados);

                    // 3. Pasamos los catálogos oficiales para llenar los "Selects" del usuario
                    model.addAttribute("gerenciasDB", gerenciaRepository.findAll());
                    model.addAttribute("materiasDB", materiaRepository.findAll());
                    model.addAttribute("organosDB", organoRepository.findAll());
                    model.addAttribute("tiposAudDB", tipoAudienciaRepository.findAll());
                    break;
                    
                case 2:
                    // TODO: Lógica para listar Expedientes nuevos/existentes
                    break;
                case 3:
                    // TODO: Lógica para validar Audiencias
                    break;
                case 4:
                    // TODO: Lógica para validar Términos y estatus histórico
                    break;
            }
        }
        return "views/admin/importar";
    }

    // Botón para simular el inicio de un lote manualmente (por Excel)
    @PostMapping("/iniciar")
    public String iniciarLoteManual(RedirectAttributes redirectAttributes) {
        loteMigracionService.obtenerOCrearLoteActivo();
        redirectAttributes.addFlashAttribute("mensaje", "Lote de migración iniciado correctamente.");
        return "redirect:/admin/importar";
    }

    // Navegación del Wizard: Avanzar al siguiente paso
    @PostMapping("/avanzar")
    public String avanzarPaso(@RequestParam("loteId") Long loteId, RedirectAttributes redirectAttributes) {
        LoteMigracion lote = loteMigracionService.avanzarPaso(loteId);
        if(lote.getEstatus().equals("COMPLETADO")) {
            redirectAttributes.addFlashAttribute("mensaje", "¡Migración completada e inyectada con éxito!");
        }
        return "redirect:/admin/importar";
    }
    
    // Navegación del Wizard: Mandar a Papelera
    @PostMapping("/cancelar")
    public String cancelarLote(@RequestParam("loteId") Long loteId, RedirectAttributes redirectAttributes) {
        loteMigracionService.cancelarLote(loteId);
        redirectAttributes.addFlashAttribute("error", "Lote de migración cancelado y enviado a la papelera.");
        return "redirect:/admin/importar";
    }

    // ==========================================================
    // PLANTILLAS DE EXCEL (RECICLADAS)
    // ==========================================================
    @GetMapping("/plantilla/terminos")
    public ResponseEntity<ByteArrayResource> descargarPlantillaTerminos() {
        String encabezados = "NumeroExpediente,Materia,OrganoJurisdiccional,EmailAbogadoResponsable,Actuacion,FechaVencimiento,Prioridad";
        return generarPlantilla("plantilla_terminos.xlsx", encabezados);
    }

    @GetMapping("/plantilla/audiencias")
    public ResponseEntity<ByteArrayResource> descargarPlantillaAudiencias() {
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

    // Procesar el formulario de la Etapa 1
    @PostMapping("/mapear-catalogos")
    public String mapearCatalogos(@RequestParam("loteId") Long loteId, @RequestParam Map<String, String> formData, RedirectAttributes redirectAttributes) {
        loteMigracionService.guardarMapeoCatalogos(loteId, formData);
        redirectAttributes.addFlashAttribute("mensaje", "Catálogos homologados correctamente. Iniciando validación de expedientes.");
        return "redirect:/admin/importar";
    }
}