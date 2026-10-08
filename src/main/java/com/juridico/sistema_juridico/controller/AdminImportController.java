package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.LoteMigracion;
import com.juridico.sistema_juridico.Entity.procesal.CatalogoMigracion;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.service.procesal.LoteMigracionService;
import com.juridico.sistema_juridico.repository.procesal.CatalogoMigracionRepository;
import com.juridico.sistema_juridico.repository.Catalogo.*;
import com.juridico.sistema_juridico.repository.Usuarios.*;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Controller
@RequestMapping("/admin/importar")
public class AdminImportController {

    @Autowired private LoteMigracionService loteMigracionService;
    @Autowired private CatalogoMigracionRepository catalogoMigracionRepository;
    @Autowired private com.juridico.sistema_juridico.repository.procesal.RegistroMigracionRepository registroRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private TipoAudienciaRepository tipoAudienciaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CatPrioridadRepository catPrioridadRepository;
    @Autowired private EstadoRepository estadoRepository;
    @Autowired private com.juridico.sistema_juridico.repository.procesal.LoteMigracionRepository loteRepository;

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
                boolean necesitaGlobal = loteMigracionService.requiereGerenciaGlobal(loteActivo.getId());

                if (necesitaGlobal && loteActivo.getGerenciaGlobal() == null) {
                    model.addAttribute("requiereGerenciaGlobal", true);
                    model.addAttribute("todasGerencias", gerenciaRepository.findAll());
                    return "views/admin/importar";
                } else {
                    model.addAttribute("requiereGerenciaGlobal", false);
                }
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

            // Catálogos Oficiales — solo valores activos disponibles para nuevas inserciones
            model.addAttribute("gerenciasDB", gerenciaRepository.findAll());
            model.addAttribute("materiasDB", materiaRepository.findByActivoTrueOrderByNombreAsc());
            model.addAttribute("organosDB", organoRepository.findByActivoTrue());
            model.addAttribute("tiposAudDB", tipoAudienciaRepository.findByActivoTrue());
            model.addAttribute("usuariosDB", usuarioRepository.findAll());
            model.addAttribute("prioridadesDB", catPrioridadRepository.findAll());
            model.addAttribute("sedesDB", estadoRepository.findByActivoTrue());
            model.addAttribute("etapas", EtapaProcesal.values());

            switch (loteActivo.getPasoActual()) {
                case 1:
                    // Calcular gerencias activas en este lote para filtrar los optgroups
                    List<Integer> gerenciaIds = todosCatalogos.stream()
                        .filter(c -> "GERENCIA".equals(c.getTipoCatalogo()) && c.getIdMapeado() != null)
                        .map(c -> Integer.valueOf(c.getIdMapeado().toString()))
                        .distinct()
                        .collect(Collectors.toList());
                    
                    List<Gerencia> gerenciasActivasLote = gerenciaIds.isEmpty() 
                        ? gerenciaRepository.findAll() 
                        : gerenciaRepository.findAllById(gerenciaIds);

                    model.addAttribute("catalogosAgrupados", catalogosAgrupados);
                    model.addAttribute("gerenciasActivasLote", gerenciasActivasLote);
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
            redirectAttributes.addFlashAttribute("mensaje", "Los datos se importaron correctamente.");
        }
        return "redirect:/admin/importar";
    }

    // Navegación del Wizard: Retroceder al paso anterior
    @PostMapping("/retroceder")
    public String retrocederPaso(@RequestParam("loteId") Long loteId) {
        loteMigracionService.retrocederPaso(loteId);
        return "redirect:/admin/importar";
    }

    // Configurar gerencia global del lote
    @PostMapping("/guardar-gerencia-global")
    public String guardarGerenciaGlobal(@RequestParam("loteId") Long loteId, @RequestParam("gerenciaId") Integer gerenciaId, RedirectAttributes redirectAttributes) {
        com.juridico.sistema_juridico.Entity.procesal.LoteMigracion lote = loteMigracionService.obtenerOCrearLoteActivo();
        if (lote.getId().equals(loteId)) {
            Gerencia gerencia = gerenciaRepository.findById(gerenciaId).orElse(null);
            lote.setGerenciaGlobal(gerencia);
            loteRepository.save(lote);
            
            if (gerencia != null) {
                // Verificar si ya existe este catálogo de gerencia (puede pasar si la vista se recarga o si json tiene un campo con el mismo nombre)
                boolean yaExiste = catalogoMigracionRepository.findAll().stream()
                    .anyMatch(c -> c.getLote().getId().equals(loteId) && "GERENCIA".equals(c.getTipoCatalogo()) 
                                && c.getValorOriginal().equalsIgnoreCase(gerencia.getNombre()));
                
                if (!yaExiste) {
                    // Crear el registro en CatalogoMigracion para que aparezca en la Etapa 1
                    CatalogoMigracion cat = new CatalogoMigracion();
                    cat.setLote(lote);
                    cat.setTipoCatalogo("GERENCIA");
                    cat.setValorOriginal(gerencia.getNombre());
                    cat.setIdMapeado(gerencia.getId().longValue());
                    cat.setEstatus("MAPEADO");
                    catalogoMigracionRepository.save(cat);
                }
            }
            
            redirectAttributes.addFlashAttribute("mensaje", "Gerencia del lote establecida correctamente.");
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

    // Procesar la confirmación de la Etapa 2
    @PostMapping("/validar-expedientes")
    public String validarExpedientes(@RequestParam("loteId") Long loteId, RedirectAttributes redirectAttributes) {
        
        // VERIFICACIÓN: Impedir el paso si hay expedientes críticos (sin materia)
        boolean hayCriticos = loteMigracionService.obtenerResumenExpedientes(loteId).stream()
                .anyMatch(exp -> Boolean.TRUE.equals(exp.get("esCritico")));
        if (hayCriticos) {
            redirectAttributes.addFlashAttribute("error", "No puedes avanzar. Hay expedientes sin Materia o con errores críticos. Debes asignarles una materia primero.");
            return "redirect:/admin/importar";
        }
        
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
            // Corrección automática post-inyección: garantizar que ningún expediente
            // quede con materia "Por Asignar" si su gerencia permite inferirla.
            int corregidos = loteMigracionService.corregirMateriasPorAsignar();
            String msg = "Los datos del lote se importaron correctamente.";
            if (corregidos > 0) msg += " Se asignó una materia predeterminada a " + corregidos + " expediente(s).";
            redirectAttributes.addFlashAttribute("mensaje", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No pudimos importar el lote. Revisa los datos e inténtalo de nuevo.");
        }
        return "redirect:/admin/importar";
    }

    /**
     * Endpoint de corrección manual: asigna materia por defecto a TODOS los
     * expedientes existentes que tengan materia nula o "Por Asignar".
     * Disponible en /admin/importar/corregir-materias (POST).
     */
    @PostMapping("/corregir-materias")
    public String corregirMateriasPorAsignar(RedirectAttributes redirectAttributes) {
        try {
            int corregidos = loteMigracionService.corregirMateriasPorAsignar();
            redirectAttributes.addFlashAttribute("mensaje",
                "Corrección completada: " + corregidos + " expediente(s) actualizados con materia por defecto según su gerencia.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No pudimos actualizar las materias. Inténtalo de nuevo.");
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
            
            // LIMPIAR CACHE PARA REFLEJAR CAMBIOS AL INSTANTE
            com.juridico.sistema_juridico.Entity.procesal.RegistroMigracion reg = registroRepository.findById(registroId).orElse(null);
            if (reg != null && reg.getLote() != null) {
                loteMigracionService.limpiarCacheLote(reg.getLote().getId());
            }
            
            return ResponseEntity.ok(Map.of("status", "ok", "mensaje", "Registro actualizado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No pudimos actualizar el registro. Inténtalo de nuevo."));
        }
    }

    @GetMapping("/api/buscar-expedientes-similares")
    @ResponseBody
    public List<String> buscarExpedientesSimilares(@RequestParam("term") String term, @RequestParam("loteId") Long loteId) {
        return loteMigracionService.buscarExpedientesSimilares(term, loteId);
    }

    /**
     * Persiste el texto editado ("lápiz") de un catálogo en la Etapa 1.
     * Actualiza el nuevoValor del CatalogoMigracion y lo marca como NUEVO,
     * de modo que al regresar a Etapa 1 el valor editado persiste.
     */
    @PostMapping("/api/actualizar-catalogo-texto")
    @ResponseBody
    public ResponseEntity<?> actualizarCatalogoTexto(
            @RequestParam("catId") Long catId,
            @RequestParam("nuevoTexto") String nuevoTexto) {
        try {
            CatalogoMigracion cat = catalogoMigracionRepository.findById(catId)
                .orElseThrow(() -> new RuntimeException("Catálogo no encontrado: " + catId));
            String textoLimpio = nuevoTexto.trim();
            if (textoLimpio.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El texto no puede estar vacío"));
            }
            cat.setNuevoValor(textoLimpio);
            cat.setEstatus("NUEVO");
            cat.setIdMapeado(null); // limpiar mapeo anterior si lo había
            catalogoMigracionRepository.save(cat);
            return ResponseEntity.ok(Map.of("status", "ok", "catId", catId, "nuevoTexto", textoLimpio));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No pudimos actualizar el catálogo. Inténtalo de nuevo."));
        }
    }

    // --- API ENDPOINTS PARA FILTRADO DINÁMICO ---

    private boolean perteneceAGerencia(String tipoCatalogo, String valorOriginal, String nuevoValor, String gerenciaVal, List<Map<String, Object>> resumenExp, List<Map<String, Object>> resumenAud) {
        if (resumenExp == null) return true;
        
        Long gerId = null;
        try { gerId = Long.parseLong(gerenciaVal); } catch (NumberFormatException e) {}
        
        for (Map<String, Object> req : resumenExp) {
            String valorRelevante = null;
            if (tipoCatalogo.equals("MATERIA")) valorRelevante = (String) req.get("materia");
            if (tipoCatalogo.equals("ORGANO")) valorRelevante = (String) req.get("organo");
            
            if (valorRelevante != null && (valorRelevante.trim().equalsIgnoreCase(valorOriginal.trim()) || 
                (nuevoValor != null && valorRelevante.trim().equalsIgnoreCase(nuevoValor.trim())))) {
                if (gerId != null && req.get("gerenciaId") != null && req.get("gerenciaId").toString().equals(gerId.toString())) return true;
                if (gerId == null && req.get("gerencia") != null && req.get("gerencia").toString().equalsIgnoreCase(gerenciaVal)) return true;
            }
        }
        
        if (resumenAud != null && (tipoCatalogo.equals("TIPO_AUDIENCIA") || tipoCatalogo.equals("ORGANO"))) {
            for (Map<String, Object> req : resumenAud) {
                String valorRelevante = null;
                if (tipoCatalogo.equals("TIPO_AUDIENCIA")) valorRelevante = (String) req.get("tipoAudiencia");
                if (tipoCatalogo.equals("ORGANO")) valorRelevante = (String) req.get("organo");
                
                if (valorRelevante != null && (valorRelevante.trim().equalsIgnoreCase(valorOriginal.trim()) || 
                    (nuevoValor != null && valorRelevante.trim().equalsIgnoreCase(nuevoValor.trim())))) {
                    if (gerId != null && req.get("gerenciaId") != null && req.get("gerenciaId").toString().equals(gerId.toString())) return true;
                    if (gerId == null && req.get("gerencia") != null && req.get("gerencia").toString().equalsIgnoreCase(gerenciaVal)) return true;
                }
            }
        }
        return false;
    }

    @GetMapping("/api/materias-por-gerencia/{gerenciaId}")
    @ResponseBody
    public List<Map<String, Object>> getMateriasByGerencia(@PathVariable("gerenciaId") String gerenciaVal, @RequestParam(required = false) Long loteId) {
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        try {
            Integer gerId = Integer.parseInt(gerenciaVal);
            res.addAll(materiaRepository.findAll().stream()
                .filter(m -> m.getGerencia() != null && m.getGerencia().getId().equals(gerId) && m.getActivo())
                .map(m -> Map.of("id", (Object)m.getId(), "nombre", m.getNombre()))
                .collect(Collectors.toList()));
        } catch(NumberFormatException e) { }
        
        if (loteId != null) {
            List<Map<String, Object>> resExp = loteMigracionService.obtenerResumenExpedientes(loteId);
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "MATERIA".equals(c.getTipoCatalogo()) && "NUEVO".equals(c.getEstatus()))
                .filter(c -> perteneceAGerencia("MATERIA", c.getValorOriginal(), c.getNuevoValor(), gerenciaVal, resExp, null))
                .forEach(c -> res.add(Map.of("id", c.getNuevoValor(), "nombre", c.getNuevoValor())));
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
            // materiaVal is a string (a NEW materia), so we just return NEW ones further down
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
            // gerenciaVal is a string (a NEW gerencia), no db results possible yet
        }
        if (loteId != null) {
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "USUARIO".equals(c.getTipoCatalogo()) && "NUEVO".equals(c.getEstatus()))
                .forEach(c -> res.add(Map.of("id", c.getNuevoValor(), "nombre", c.getNuevoValor())));
        }
        return res;
    }

    @GetMapping("/api/organos-por-gerencia/{gerenciaId}")
    @ResponseBody
    public List<Map<String, Object>> getOrganosByGerencia(@PathVariable("gerenciaId") String gerenciaVal, @RequestParam(required = false) Long loteId) {
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        try {
            Integer gerId = Integer.parseInt(gerenciaVal);
            res.addAll(organoRepository.findByGerenciaIdAndActivoTrue(gerId).stream()
                    .map(o -> Map.of("id", (Object)o.getId(), "nombre", o.getNombre()))
                    .collect(Collectors.toList()));
        } catch(NumberFormatException e) {
            // gerenciaVal is a string (a NEW gerencia), no db results possible yet
        }
        if (loteId != null) {
            List<Map<String, Object>> resExp = loteMigracionService.obtenerResumenExpedientes(loteId);
            List<Map<String, Object>> resAud = loteMigracionService.obtenerResumenAudiencias(loteId);
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "ORGANO".equals(c.getTipoCatalogo()) && "NUEVO".equals(c.getEstatus()))
                .filter(c -> perteneceAGerencia("ORGANO", c.getValorOriginal(), c.getNuevoValor(), gerenciaVal, resExp, resAud))
                .forEach(c -> res.add(Map.of("id", c.getNuevoValor(), "nombre", c.getNuevoValor())));
        }
        return res;
    }

    @GetMapping("/api/tipos-audiencia-por-gerencia/{gerenciaId}")
    @ResponseBody
    public List<Map<String, Object>> getTiposAudienciaByGerencia(@PathVariable("gerenciaId") String gerenciaVal, @RequestParam(required = false) Long loteId) {
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        try {
            Integer gerId = Integer.parseInt(gerenciaVal);
            res.addAll(tipoAudienciaRepository.findByGerenciaIdAndActivoTrue(gerId).stream()
                    .map(t -> Map.of("id", (Object)t.getId(), "nombre", t.getNombre()))
                    .collect(Collectors.toList()));
        } catch(NumberFormatException e) {
            // gerenciaVal is a string (a NEW gerencia), no db results possible yet
        }
        
        if (loteId != null) {
            List<Map<String, Object>> resExp = loteMigracionService.obtenerResumenExpedientes(loteId);
            List<Map<String, Object>> resAud = loteMigracionService.obtenerResumenAudiencias(loteId);
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "TIPO_AUDIENCIA".equals(c.getTipoCatalogo()) && "NUEVO".equals(c.getEstatus()))
                .filter(c -> perteneceAGerencia("TIPO_AUDIENCIA", c.getValorOriginal(), c.getNuevoValor(), gerenciaVal, resExp, resAud))
                .forEach(c -> res.add(Map.of("id", c.getNuevoValor(), "nombre", c.getNuevoValor())));
        }
        return res;
    }

    @GetMapping("/api/estado-lote")
    @ResponseBody
    public Map<String, Object> checkEstadoLote() {
        LoteMigracion loteActivo = loteMigracionService.obtenerLoteActivoOpcional();
        boolean hayLote = loteActivo != null && "PENDIENTE".equals(loteActivo.getEstatus());
        
        Map<String, Object> response = new HashMap<>();
        response.put("hayLotePendiente", hayLote);
        if (hayLote) {
            response.put("loteId", loteActivo.getId());
            response.put("totalRegistros", registroRepository.countByLoteId(loteActivo.getId()));
        }
        return response;
    }

    /**
     * Retorna TODOS los abogados activos con sus IDs de materias asignadas.
     * El frontend usa esto para saber si el abogado seleccionado pertenece
     * a la materia elegida, sin restringir la lista mostrada al usuario.
     */
    @GetMapping("/api/todos-abogados")
    @ResponseBody
    public List<Map<String, Object>> getTodosAbogados(@RequestParam(required = false) Long loteId) {
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        
        // 1. Abogados existentes en la BD
        res.addAll(usuarioRepository.findAll().stream()
            .filter(u -> u.getActivo() != null && u.getActivo())
            .map(u -> {
                List<Integer> materiaIds = u.getMaterias().stream()
                    .map(m -> m.getId())
                    .collect(Collectors.toList());
                Map<String, Object> m = new HashMap<>();
                m.put("id",        u.getId());
                m.put("nombre",    u.getNombreCompleto());
                m.put("gerenciaId", u.getGerencia() != null ? u.getGerencia().getId() : null);
                m.put("materiaIds", materiaIds);
                return m;
            })
            .collect(Collectors.toList()));
            
        // 2. Abogados "NUEVO" del catálogo de migración
        if (loteId != null) {
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "USUARIO".equals(c.getTipoCatalogo()) && "NUEVO".equals(c.getEstatus()) && c.getNuevoValor() != null)
                .forEach(c -> {
                    String nombreNuevo = c.getNuevoValor();
                    // CLAVE: usar valorOriginal (nombre como vino del JSON) para inferir materias,
                    // ya que los registros del lote contienen el nombre original, no el editado.
                    String nombreParaBuscar = c.getValorOriginal() != null ? c.getValorOriginal() : nombreNuevo;
                    List<Integer> materiaIdsInf = loteMigracionService.getMateriasInferredForAbogadoNuevos(loteId, nombreParaBuscar);
                    // Inferir gerencia desde las materias del abogado en el lote
                    Long gerenciaIdInf = loteMigracionService.getGerenciaIdInferredForAbogado(loteId, nombreParaBuscar);
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", nombreNuevo); // ID será su nombre editado en formato string
                    m.put("nombre", nombreNuevo);
                    m.put("gerenciaId", gerenciaIdInf); // Gerencia inferida desde las materias del lote
                    m.put("materiaIds", materiaIdsInf);
                    res.add(m);
                });
            
            // 3. Abogados "MAPEADO" a un usuario existente: combinar materias DB + inferidas del lote
            catalogoMigracionRepository.findByLoteId(loteId).stream()
                .filter(c -> "USUARIO".equals(c.getTipoCatalogo()) 
                          && "MAPEADO".equals(c.getEstatus()) 
                          && c.getIdMapeado() != null)
                .forEach(c -> {
                    // Si la entrada ya está en res como usuario existente, enriquecemos sus materiaIds
                    Long idMapeado = c.getIdMapeado();
                    String nombreParaBuscar = c.getValorOriginal() != null ? c.getValorOriginal() : "";
                    List<Integer> materiasInferidas = loteMigracionService.getMateriasInferredForAbogadoNuevos(loteId, nombreParaBuscar);
                    
                    if (!materiasInferidas.isEmpty()) {
                        // Enriquecer la entrada existente en res con las materias inferidas del lote
                        res.stream()
                            .filter(entry -> idMapeado.intValue() == Integer.parseInt(String.valueOf(entry.get("id"))))
                            .findFirst()
                            .ifPresent(entry -> {
                                @SuppressWarnings("unchecked")
                                List<Integer> existentes = (List<Integer>) entry.get("materiaIds");
                                java.util.Set<Integer> combinadas = new java.util.HashSet<>(existentes);
                                combinadas.addAll(materiasInferidas);
                                entry.put("materiaIds", new java.util.ArrayList<>(combinadas));
                            });
                    }
                });
        }

        
        return res;
    }

    /**
     * Asigna una materia adicional a un abogado existente.
     * Se llama cuando el usuario confirma asignar una materia "ajena" al abogado.
     */
    @PostMapping("/api/asignar-materia-abogado")
    @ResponseBody
    public ResponseEntity<?> asignarMateriaAbogado(
            @RequestParam("abogadoId") String abogadoIdStr,
            @RequestParam("materiaId") Integer materiaId) {
        try {
            Integer abogadoId;
            try {
                abogadoId = Integer.parseInt(abogadoIdStr);
            } catch (NumberFormatException e) {
                // Es un abogado NUEVO en proceso de migración (ID es un String como "Juan Perez")
                return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "mensaje", "La materia será asignada automáticamente en la inyección para: " + abogadoIdStr
                ));
            }

            Usuario usuario =
                usuarioRepository.findById(abogadoId)
                    .orElseThrow(() -> new RuntimeException("Abogado no encontrado con id: " + abogadoId));
            Materia materia =
                materiaRepository.findById(materiaId)
                    .orElseThrow(() -> new RuntimeException("Materia no encontrada con id: " + materiaId));

            // Añadir solo si no la tiene ya
            boolean yaAsignada = usuario.getMaterias().stream()
                .anyMatch(m -> m.getId().equals(materiaId));
            if (!yaAsignada) {
                usuario.getMaterias().add(materia);
                usuarioRepository.save(usuario);
            }
            return ResponseEntity.ok(Map.of(
                "status", "ok",
                "mensaje", "Materia \"" + materia.getNombre() + "\" asignada a " + usuario.getNombreCompleto()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No pudimos guardar los cambios. Inténtalo de nuevo."));
        }
    }
}
