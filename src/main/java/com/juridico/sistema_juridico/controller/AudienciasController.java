package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ForbiddenException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.service.procesal.AudienciaService;
//import com.juridico.sistema_juridico.repository.Expediente.ColaboradorExpedienteRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.util.AudienciaExcelExporter;

import jakarta.servlet.http.HttpServletResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.UUID;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

@Controller
@RequestMapping("/audiencias")
public class AudienciasController {

    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private ExpedienteRepository expedienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AudienciaService audienciaService;

    // @Autowired private ColaboradorExpedienteRepository colaboradorRepository;
    @Autowired
    private GerenciaRepository gerenciaRepository;
    @Autowired
    private MateriaRepository materiaRepository;
    @Autowired
    private TipoAudienciaRepository tipoAudienciaRepository;

    @GetMapping
    public String index(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "gerencia", required = false) String gerencia,
            @RequestParam(value = "materia", required = false) String materia,
            @RequestParam(value = "estatus", required = false) EstatusAudiencia estatus,
            @RequestParam(value = "abogadoId", required = false) Integer abogadoId,
            @RequestParam(value = "periodo", required = false) PeriodoFiltro periodo,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "anio", required = false) Integer anio) {

        if (keyword != null && keyword.contains("?keyword=")) {
            // Nos quedamos solo con lo que esté después del último igual (=)
            keyword = keyword.substring(keyword.lastIndexOf("=") + 1);
        }

        // 1. OBTENER USUARIO ACTUAL
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        String rol = usuario.getRol().name();

        // 2. CONFIGURAR FILTROS DE SEGURIDAD
        Integer filtroUsuarioId = null;
        Integer filtroGerenciaId = null;
        List<Integer> filtroMateriaIds = null;

        switch (rol) {
            case "ABOGADO":
                filtroUsuarioId = usuario.getId();
                break;
            case "GERENTE":
            case "JEFE_DEPTO":
                if (usuario.getGerencia() != null)
                    filtroGerenciaId = usuario.getGerencia().getId();
                break;
        }

        // 3. FECHAS CENTINELA
        LocalDate fechaInicio = LocalDate.of(1900, 1, 1);
        LocalDate fechaFin = LocalDate.of(2100, 12, 31);

        String searchKeyword = keyword;
        if (keyword != null && keyword.matches("\\d{4}-\\d{2}-\\d{2}")) {
            // 1. Prioridad: Fecha exacta desde mini-calendario
            LocalDate fecha = LocalDate.parse(keyword);
            fechaInicio = fecha;
            fechaFin = fecha;
            searchKeyword = null; // No buscar la fecha como texto en otros campos
        } else if (periodo != null) {
            // 2. Periodos relativos (Hoy, Semana, Mes actual)
            LocalDate[] rango = periodo.calcularRango();
            fechaInicio = rango[0];
            fechaFin = rango[1];
        } else if (mes != null && anio != null) {
            // 3. Navegación por mes en mini-calendario
            fechaInicio = LocalDate.of(anio, mes, 1);
            fechaFin = fechaInicio.withDayOfMonth(fechaInicio.lengthOfMonth());
        }

        // 4. CONSULTA
        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaAudiencia").ascending());

        Page<Audiencia> audiencias = audienciaRepository.buscarConFiltros(
                id, searchKeyword, tipo, gerencia, materia, estatus,
                abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds,
                pageable);

        model.addAttribute("audiencias", audiencias);
        model.addAttribute("pageTitle", "Gestión de Audiencias");
        model.addAttribute("activePage", "audiencias");
        model.addAttribute("paramPeriodo", periodo != null ? periodo.name() : null);
        model.addAttribute("mesActual", mes != null ? mes : LocalDate.now().getMonthValue());
        model.addAttribute("anioActual", anio != null ? anio : LocalDate.now().getYear());

        // 5. DROPDOWN INTELIGENTE (MODO ANTI-FANTASMA)
        List<Expediente> expedientesParaDropdown;

        if (rol.equals("ABOGADO")) {
            // A) Expedientes donde soy el Dueño
            List<Expediente> misExpedientes = expedienteRepository.findAll().stream()
                    .filter(e -> e.getAbogadoResponsable() != null
                            && e.getAbogadoResponsable().getId().equals(usuario.getId()))
                    .collect(Collectors.toList());

            // B) Expedientes donde soy Invitado ACTIVO (Audiencia NO Concluida)
            List<Expediente> expedientesPorAudiencia = audienciaRepository.findAll().stream()
                    .filter(a -> a.getAbogadoComparece() != null &&
                            a.getAbogadoComparece().getId().equals(usuario.getId()) &&
                            EstatusAudiencia.CONCLUIDA != a.getEstatusAudiencia()) // <--- AQUÍ ESTÁ EL CANDADO FANTASMA
                    .map(Audiencia::getExpediente)
                    .collect(Collectors.toList());

            // Unir listas sin duplicados
            Set<Expediente> conjunto = new HashSet<>(misExpedientes);
            conjunto.addAll(expedientesPorAudiencia);
            expedientesParaDropdown = new ArrayList<>(conjunto);

        } else if (rol.equals("GERENTE")) {
            expedientesParaDropdown = expedienteRepository.findAll().stream()
                    .filter(e -> e.getGerencia() != null && usuario.getGerencia() != null &&
                            e.getGerencia().getId().equals(usuario.getGerencia().getId()))
                    .collect(Collectors.toList());
        } else if (rol.equals("JEFE_DEPTO")) {
            expedientesParaDropdown = expedienteRepository.findAll().stream()
                    .filter(e -> e.getMateria() != null && usuario.getMaterias().stream()
                            .anyMatch(m -> m.getId().equals(e.getMateria().getId())))
                    .collect(Collectors.toList());
        } else {
            expedientesParaDropdown = expedienteRepository.findAll();
        }
        model.addAttribute("expedientesList", expedientesParaDropdown);

        // Listas generales — solo valores activos
        model.addAttribute("listaTiposAudiencia", tipoAudienciaRepository.findByActivoTrue());
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("tipos", tipoAudienciaRepository.findByActivoTrue());

        // --- FILTRO DE ABOGADOS EN MODAL ---
        // (Aunque esto no estaba en el requerimiento del fantasma, lo alineamos para
        // que sea coherente)
        List<Usuario> listaAbogados;
        if (rol.equals("ABOGADO")) {
            listaAbogados = Collections.singletonList(usuario);
        } else if (rol.equals("GERENTE") || rol.equals("JEFE_DEPTO")) {
            listaAbogados = usuarioRepository.findAll().stream()
                    .filter(u -> u.getGerencia() != null && usuario.getGerencia() != null &&
                            u.getGerencia().getId().equals(usuario.getGerencia().getId()))
                    .collect(Collectors.toList());
        } else {
            listaAbogados = usuarioRepository.findAll();
        }
        model.addAttribute("abogados", listaAbogados);
        // ------------------------------------

        // Filtros en vista
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramTipo", tipo);
        model.addAttribute("paramGerencia", gerencia);
        model.addAttribute("paramMateria", materia);
        model.addAttribute("paramEstatus", estatus != null ? estatus.name() : null);
        model.addAttribute("paramAbogadoId", filtroUsuarioId != null ? filtroUsuarioId : abogadoId);
        model.addAttribute("paramPeriodo", periodo);
        model.addAttribute("usuarioActual", usuario);

        // --- 6. CONTEOS PARA KPIs (NUEVO) ---
        List<Object[]> rawCounts = audienciaRepository.contarPorEstatusConFiltros(
                keyword, tipo, gerencia, materia, abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds);

        Map<EstatusAudiencia, Long> kpisAudit = new EnumMap<>(EstatusAudiencia.class);
        for (EstatusAudiencia e : EstatusAudiencia.values()) {
            kpisAudit.put(e, 0L);
        }
        long totalKpi = 0;
        for (Object[] row : rawCounts) {
            if (row[0] != null) {
                EstatusAudiencia e = (EstatusAudiencia) row[0];
                long count = (long) row[1];
                kpisAudit.put(e, count);
                totalKpi += count;
            }
        }
        model.addAttribute("kpisAudiencias", kpisAudit);
        model.addAttribute("totalAudiencias", totalKpi);

        return "views/audiencias/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Audiencia audiencia, RedirectAttributes redirectAttrs) {
        // NOTA: Ya no necesitamos el try-catch envolvente.
        // Si algo falla, el GlobalExceptionHandler se encarga.

        boolean esNueva = (audiencia.getId() == null);

        if (audiencia.getFechaAudiencia() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de la audiencia es obligatoria.");
        }
        if (audiencia.getTipoAudiencia() == null || audiencia.getTipoAudiencia().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de audiencia es obligatorio.");
        }

        if (!esNueva) {
            // Lógica de EDICIÓN:
            // Buscamos la audiencia. Si no existe, lanzamos la excepción y el sistema
            // redirige al usuario con la alerta roja.
            Audiencia audienciaExistente = audienciaRepository.findById(audiencia.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se puede editar: La audiencia con ID " + audiencia.getId() + " no existe."));

            // Preservamos los datos que no vienen en el formulario simple
            audiencia.setEstatusAudiencia(audienciaExistente.getEstatusAudiencia());
            audiencia.setActaDocumento(audienciaExistente.getActaDocumento());

            // Preservar observaciones si no se enviaron nuevas
            if (audiencia.getObservaciones() == null && audienciaExistente.getObservaciones() != null) {
                audiencia.setObservaciones(audienciaExistente.getObservaciones());
            }
            if (audiencia.getExpediente() == null && audienciaExistente.getExpediente() != null) {
                audiencia.setExpediente(audienciaExistente.getExpediente());
            }
        } else {
            // Lógica de CREACIÓN
            audiencia.setEstatusAudiencia(EstatusAudiencia.PENDIENTE);
        }

        // Corrección para evitar conflictos con Hibernate si el ID del abogado viene
        // vacío
        if (audiencia.getAbogadoComparece() != null && audiencia.getAbogadoComparece().getId() == null) {
            audiencia.setAbogadoComparece(null);
        }

        // GUARDADO (Si falla la BD, se va al ExceptionHandler genérico)
        Audiencia guardada = audienciaRepository.save(audiencia);

        // Notificación (Solo si es nueva)
        if (esNueva) {
            audienciaService.notificarCreacionAudiencia(guardada);
        } else {
            audienciaService.registrarEdicionAudiencia(guardada);
        }

        // Mensaje de Éxito (Solo definimos el camino feliz)
        redirectAttrs.addFlashAttribute("mensaje", "Audiencia guardada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");

        return "redirect:/audiencias";
    }

    @GetMapping("/obtener/{id}")
    @ResponseBody
    public ResponseEntity<Audiencia> obtenerPorId(@PathVariable("id") Integer id) {
        return audienciaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/subir-acta")
    public String subirActa(@RequestParam("id") Integer id,
            @RequestParam("archivo") MultipartFile archivo,
            RedirectAttributes redirectAttrs) {
        try {
            audienciaService.subirActa(id, archivo);
            redirectAttrs.addFlashAttribute("mensaje", "Acta subida correctamente. Estatus actualizado.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al subir acta: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }

    @PostMapping("/concluir")
    public String concluir(@RequestParam("id") Integer id,
            @RequestParam("observaciones") String observaciones,
            RedirectAttributes redirectAttrs) {
        try {
            // 1. OBTENER USUARIO ACTUAL
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // 2. VALIDAR PERMISOS (Solo Jefes hacia arriba pueden concluir)
            if (usuario.getRol().name().equals("ABOGADO")) {
                throw new ForbiddenException(
                        "Acceso denegado: Solo Dirección o Jefatura pueden validar y concluir audiencias.");
            }

            // 3. EJECUTAR LÓGICA
            audienciaService.concluirAudiencia(id, observaciones);

            redirectAttrs.addFlashAttribute("mensaje", "Audiencia validada y concluida exitosamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }

    @PostMapping("/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> eliminar(@PathVariable("id") Integer id) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            // 1. VALIDACIÓN LIMPIA
            if (!audienciaRepository.existsById(id)) {
                response.put("success", false);
                response.put("message", "La audiencia no existe.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 2. LÓGICA FELIZ (Sin try-catch interno)
            audienciaRepository.deleteById(id);

            response.put("success", true);
            response.put("message", "Audiencia eliminada correctamente.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar la audiencia.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/descargar-acta/{id}")
    public ResponseEntity<Resource> descargarActa(@PathVariable("id") Integer id) {
        try {
            Audiencia audiencia = audienciaRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audiencia no encontrada"));

            if (audiencia.getActaDocumento() == null) {
                return ResponseEntity.notFound().build();
            }

            java.nio.file.Path rutaArchivo = java.nio.file.Paths.get("uploads/audiencias")
                    .resolve(audiencia.getActaDocumento());
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + recurso.getFilename() + "\"")
                        .body(recurso);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar-excel")
    public void exportarExcel(HttpServletResponse response,
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "gerencia", required = false) String gerencia,
            @RequestParam(value = "materia", required = false) String materia,
            @RequestParam(value = "estatus", required = false) EstatusAudiencia estatus,
            @RequestParam(value = "abogadoId", required = false) Integer abogadoId,
            @RequestParam(value = "periodo", required = false) PeriodoFiltro periodo) throws IOException {

        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Audiencias_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);

        // 1. SEGURIDAD DE ROLES
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        String rol = usuario.getRol().name();

        Integer filtroUsuarioId = null;
        Integer filtroGerenciaId = null;
        List<Integer> filtroMateriaIds = null;

        switch (rol) {
            case "ABOGADO":
                filtroUsuarioId = usuario.getId();
                break;
            case "GERENTE":
                if (usuario.getGerencia() != null)
                    filtroGerenciaId = usuario.getGerencia().getId();
                break;
            case "JEFE_DEPTO":
                if (usuario.getMaterias() != null && !usuario.getMaterias().isEmpty()) {
                    filtroMateriaIds = usuario.getMaterias().stream().map(Materia::getId).collect(Collectors.toList());
                } else {
                    filtroMateriaIds = new ArrayList<>();
                    filtroMateriaIds.add(-1);
                }
                break;
        }

        // 2. LÓGICA DE FECHAS "CENTINELA"
        LocalDate fechaInicio = LocalDate.of(1900, 1, 1);
        LocalDate fechaFin = LocalDate.of(2100, 12, 31);

        if (periodo != null) {
            LocalDate[] rango = periodo.calcularRango();
            fechaInicio = rango[0];
            fechaFin = rango[1];
        }

        // 3. LLAMAR AL REPOSITORIO
        List<Audiencia> listado = audienciaRepository.listarParaExcel(
                id, keyword, tipo, gerencia, materia, estatus,
                abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds);

        AudienciaExcelExporter excelExporter = new AudienciaExcelExporter(listado);
        excelExporter.export(response);

    }

    // --- API: BUSCAR ABOGADOS DISPONIBLES (PARA SUSTITUCIÓN EN AUDIENCIAS) ---
    @GetMapping("/api/abogados-disponibles/{expedienteId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerAbogadosDisponibles(
            @PathVariable("expedienteId") String expedienteId) {
        try {
            // 1. Obtener datos del expediente
            Expediente exp = expedienteRepository.findById(UUID.fromString(expedienteId))
                    .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));

            Integer gerenciaId = exp.getGerencia().getId();
            // materiaId no se requiere para filtrar abogados en audiencias según ROD-146

            // 2. Buscamos todos los usuarios activos de la gerencia (sin filtrar por
            // materia para audiencias)
            List<Usuario> abogadosGerencia = usuarioRepository.findAll().stream()
                    .filter(u -> u.getActivo() != null && u.getActivo()
                            && u.getGerencia() != null && u.getGerencia().getId().equals(gerenciaId))
                    .filter(u -> u.getRol() == RolUsuario.ABOGADO || u.getRol() == RolUsuario.GERENTE
                            || u.getRol() == RolUsuario.JEFE_DEPTO)
                    .collect(Collectors.toList());

            // 3. Preparamos el JSON
            List<Map<String, Object>> resultado = abogadosGerencia.stream()
                    .map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("nombre", u.getNombreCompleto());

                        // Marcamos quién es el titular original para distinguirlo visualmente
                        boolean esTitular = exp.getAbogadoResponsable() != null
                                && u.getId().equals(exp.getAbogadoResponsable().getId());
                        map.put("esTitular", esTitular);

                        return map;
                    })
                    .sorted((a, b) -> {
                        // Ordenar: Titular primero, luego alfabéticamente
                        if ((boolean) a.get("esTitular"))
                            return -1;
                        if ((boolean) b.get("esTitular"))
                            return 1;
                        return ((String) a.get("nombre")).compareTo((String) b.get("nombre"));
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}