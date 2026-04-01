package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.LoteMigracion;
import com.juridico.sistema_juridico.Entity.procesal.CatalogoMigracion;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.service.procesal.LoteMigracionService;
import com.juridico.sistema_juridico.repository.procesal.CatalogoMigracionRepository;
import com.juridico.sistema_juridico.repository.Catalogo.*;
import com.juridico.sistema_juridico.repository.Usuarios.*;
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
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CatPrioridadRepository catPrioridadRepository;
    @Autowired private EstadoRepository estadoRepository;

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
            if (loteActivo.getPasoActual() == 1) {
                loteMigracionService.extraerCatalogosDeLote(loteActivo.getId());
            }

            // Obtenemos los mapeos de este lote
            List<CatalogoMigracion> todosCatalogos = catalogoMigracionRepository.findAll().stream()
                .filter(c -> c.getLote().getId().equals(loteActivo.getId()))
                .collect(Collectors.toList());
            
            Map<String, List<CatalogoMigracion>> catalogosAgrupados = todosCatalogos.stream()
                .collect(Collectors.groupingBy(CatalogoMigracion::getTipoCatalogo));

            // Filtramos solo los que se marcaron como "NUEVO" para mostrarlos en los selects de pasos posteriores
            Map<String, List<CatalogoMigracion>> mapeosNuevos = todosCatalogos.stream()
                .filter(c -> "NUEVO".equals(c.getEstatus()))
                .collect(Collectors.groupingBy(CatalogoMigracion::getTipoCatalogo));
            
            model.addAttribute("mapeosNuevos", mapeosNuevos);

            // Catálogos Oficiales siempre disponibles para los pasos de edición
            model.addAttribute("gerenciasDB", gerenciaRepository.findAll());
            model.addAttribute("materiasDB", materiaRepository.findAll());
            model.addAttribute("organosDB", organoRepository.findAll());
            model.addAttribute("tiposAudDB", tipoAudienciaRepository.findAll());
            model.addAttribute("usuariosDB", usuarioRepository.findAll());
            model.addAttribute("prioridadesDB", catPrioridadRepository.findAll());
            model.addAttribute("sedesDB", estadoRepository.findAll());
            model.addAttribute("etapas", EtapaProcesal.values());

            switch (loteActivo.getPasoActual()) {
                case 1:
                    model.addAttribute("catalogosAgrupados", catalogosAgrupados);
                    break;
                case 2:
                    model.addAttribute("expedientesResumen", loteMigracionService.obtenerResumenExpedientes(loteActivo.getId()));
                    break;
                case 3:
                    model.addAttribute("audienciasResumen", loteMigracionService.obtenerResumenAudiencias(loteActivo.getId()));
                    break;
                case 4:
                    model.addAttribute("terminosResumen", loteMigracionService.obtenerResumenTerminos(loteActivo.getId()));
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

    // Navegación del Wizard: Retroceder al paso anterior
    @PostMapping("/retroceder")
    public String retrocederPaso(@RequestParam("loteId") Long loteId) {
        loteMigracionService.retrocederPaso(loteId);
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

    // Procesar la confirmación de la Etapa 2
    @PostMapping("/validar-expedientes")
    public String validarExpedientes(@RequestParam("loteId") Long loteId, RedirectAttributes redirectAttributes) {
        // En un futuro aquí podrías excluir expedientes que el usuario no quiera migrar
        loteMigracionService.avanzarPaso(loteId);
        redirectAttributes.addFlashAttribute("mensaje", "Expedientes validados correctamente. Pasando a revisión de Audiencias.");
        return "redirect:/admin/importar";
    }

    // Procesar la confirmación de la Etapa 3
    @PostMapping("/validar-audiencias")
    public String validarAudiencias(@RequestParam("loteId") Long loteId, RedirectAttributes redirectAttributes) {
        loteMigracionService.avanzarPaso(loteId);
        redirectAttributes.addFlashAttribute("mensaje", "Audiencias validadas correctamente. Pasando al último paso: Términos.");
        return "redirect:/admin/importar";
    }

    // Procesar la Inyección Final a la Base de Datos
    @PostMapping("/inyectar")
    public String inyectarLoteFinal(@RequestParam("loteId") Long loteId, RedirectAttributes redirectAttributes) {
        try {
            loteMigracionService.inyectarLoteOficial(loteId);
            redirectAttributes.addFlashAttribute("mensaje", "¡Inyección Exitosa! Todos los datos del lote han sido migrados a la base de datos oficial.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error durante la inyección: " + e.getMessage());
        }
        return "redirect:/admin/importar";
    }

    // AJAX: Editar un registro individual (Expediente, Audiencia o Término)
    @PostMapping("/editar-registro")
    @ResponseBody
    public ResponseEntity<?> editarRegistro(@RequestParam("registroId") Long registroId,
                                              @RequestParam Map<String, String> campos) {
        try {
            campos.remove("registroId"); // quitar el propio id de los campos a editar
            loteMigracionService.editarRegistro(registroId, campos);
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Registro actualizado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // --- API ENDPOINTS PARA FILTRADO DINÁMICO ---
    
    @GetMapping("/api/materias-por-gerencia/{gerenciaId}")
    @ResponseBody
    public List<Map<String, Object>> getMateriasByGerencia(@PathVariable Integer gerenciaId, @RequestParam(required = false) Long loteId) {
        List<Map<String, Object>> res = new java.util.ArrayList<>(materiaRepository.findAll().stream()
                .filter(m -> m.getGerencia() != null && m.getGerencia().getId().equals(gerenciaId))
                .map(m -> Map.of("id", (Object)m.getId(), "nombre", m.getNombre()))
                .collect(Collectors.toList()));
        if (loteId != null) {
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "MATERIA".equals(c.getTipoCatalogo()) && "NUEVO".equals(c.getEstatus()))
                .forEach(c -> res.add(Map.of("id", c.getNuevoValor(), "nombre", c.getNuevoValor()))); // using name as id since it's unpersisted
        }
        return res;
    }

    @GetMapping("/api/usuarios-por-materia/{materiaId}")
    @ResponseBody
    public List<Map<String, Object>> getUsuariosByMateria(@PathVariable("materiaId") String materiaVal, @RequestParam(required = false) Long loteId) {
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        try {
            Integer matId = Integer.parseInt(materiaVal);
            res.addAll(usuarioRepository.findAll().stream()
                    .filter(u -> u.getMaterias().stream().anyMatch(m -> m.getId().equals(matId)))
                    .map(u -> Map.of("id", (Object)u.getId(), "nombre", u.getNombreCompleto()))
                    .collect(Collectors.toList()));
        } catch(NumberFormatException e) {
            // materiaVal is a string (a NEW materia), so we just return ALL db users for now, 
            // plus the new ones, because we can't filter by a non-existent materia ID.
            res.addAll(usuarioRepository.findAll().stream()
                    .map(u -> Map.of("id", (Object)u.getId(), "nombre", u.getNombreCompleto()))
                    .collect(Collectors.toList()));
        }
        
        if (loteId != null) {
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "USUARIO".equals(c.getTipoCatalogo()) && "NUEVO".equals(c.getEstatus()))
                .forEach(c -> res.add(Map.of("id", c.getNuevoValor(), "nombre", c.getNuevoValor())));
        }
        return res;
    }

    @GetMapping("/api/usuarios-por-gerencia/{gerenciaId}")
    @ResponseBody
    public List<Map<String, Object>> getUsuariosByGerencia(@PathVariable("gerenciaId") String gerenciaVal, @RequestParam(required = false) Long loteId) {
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        try {
            Integer gerId = Integer.parseInt(gerenciaVal);
            res.addAll(usuarioRepository.findAll().stream()
                    .filter(u -> u.getGerencia() != null && u.getGerencia().getId().equals(gerId))
                    .map(u -> Map.of("id", (Object)u.getId(), "nombre", u.getNombreCompleto()))
                    .collect(Collectors.toList()));
        } catch(NumberFormatException e) {
            res.addAll(usuarioRepository.findAll().stream()
                    .map(u -> Map.of("id", (Object)u.getId(), "nombre", u.getNombreCompleto()))
                    .collect(Collectors.toList()));
        }
        if (loteId != null) {
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "USUARIO".equals(c.getTipoCatalogo()) && "NUEVO".equals(c.getEstatus()))
                .forEach(c -> res.add(Map.of("id", c.getNuevoValor(), "nombre", c.getNuevoValor())));
        }
        return res;
    }
}