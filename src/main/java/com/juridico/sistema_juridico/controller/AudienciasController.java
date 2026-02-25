package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String gerencia,
            @RequestParam(required = false) String materia,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(required = false) String periodo) {

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

        // 3. FECHAS CENTINELA
        LocalDate fechaInicio = LocalDate.of(1900, 1, 1);
        LocalDate fechaFin = LocalDate.of(2100, 12, 31);

        if (periodo != null && !periodo.isEmpty()) {
            LocalDate hoy = LocalDate.now();
            switch (periodo) {
                case "HOY":
                    fechaInicio = hoy;
                    fechaFin = hoy;
                    break;
                case "MANANA":
                    fechaInicio = hoy.plusDays(1);
                    fechaFin = hoy.plusDays(1);
                    break;
                case "SEMANA":
                    fechaInicio = hoy;
                    fechaFin = hoy.plusDays(7);
                    break;
                case "MES":
                    fechaInicio = hoy.withDayOfMonth(1);
                    fechaFin = hoy.withDayOfMonth(hoy.lengthOfMonth());
                    break;
            }
        }

        // 4. CONSULTA
        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaAudiencia").ascending());

        Page<Audiencia> audiencias = audienciaRepository.buscarConFiltros(
                keyword, tipo, gerencia, materia, estatus,
                abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds,
                pageable);

        model.addAttribute("audiencias", audiencias);
        model.addAttribute("pageTitle", "Gestión de Audiencias");
        model.addAttribute("activePage", "audiencias");

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
                            !"CONCLUIDA".equals(a.getEstatusAudiencia())) // <--- AQUÍ ESTÁ EL CANDADO FANTASMA
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

        // Listas generales
        model.addAttribute("listaTiposAudiencia", tipoAudienciaRepository.findAll());
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        model.addAttribute("tipos", tipoAudienciaRepository.findAll());

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
        model.addAttribute("paramEstatus", estatus);
        model.addAttribute("paramAbogadoId", abogadoId);
        model.addAttribute("paramPeriodo", periodo);
        model.addAttribute("usuarioActual", usuario);

        return "views/audiencias/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Audiencia audiencia, RedirectAttributes redirectAttrs) {
        // NOTA: Ya no necesitamos el try-catch envolvente.
        // Si algo falla, el GlobalExceptionHandler se encarga.

        boolean esNueva = (audiencia.getId() == null);

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
            if (audiencia.getObservaciones() == null) {
                audiencia.setObservaciones(audienciaExistente.getObservaciones());
            }
        } else {
            // Lógica de CREACIÓN
            audiencia.setEstatusAudiencia("PENDIENTE");
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
        }

        // Mensaje de Éxito (Solo definimos el camino feliz)
        redirectAttrs.addFlashAttribute("mensaje", "Audiencia guardada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");

        return "redirect:/audiencias";
    }

    @GetMapping("/obtener/{id}")
    @ResponseBody
    public ResponseEntity<Audiencia> obtenerPorId(@PathVariable Integer id) {
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
                redirectAttrs.addFlashAttribute("mensaje",
                        "Acceso denegado: Solo Dirección o Gerencia pueden validar y concluir audiencias.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/audiencias";
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

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttrs) {

        // 1. VALIDACIÓN LIMPIA
        // Si no existe, lanzamos la excepción y el GlobalHandler se encarga del resto.
        if (!audienciaRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se puede eliminar: La audiencia con ID " + id + " no existe.");
        }

        // 2. LÓGICA FELIZ (Sin try-catch)
        // Si hay un error de base de datos aquí (ej. llave foránea), el GlobalHandler
        // (Exception.class) lo atrapará.
        audienciaRepository.deleteById(id);

        redirectAttrs.addFlashAttribute("mensaje", "Audiencia eliminada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");

        return "redirect:/audiencias";
    }

    @GetMapping("/descargar-acta/{id}")
    public ResponseEntity<Resource> descargarActa(@PathVariable Integer id) {
        try {
            Audiencia audiencia = audienciaRepository.findById(id).orElse(null);

            if (audiencia == null || audiencia.getActaDocumento() == null) {
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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String gerencia,
            @RequestParam(required = false) String materia,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) Integer abogadoId,
            @RequestParam(required = false) String periodo) throws IOException {

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

        if (periodo != null && !periodo.isEmpty()) {
            LocalDate hoy = LocalDate.now();
            switch (periodo) {
                case "HOY":
                    fechaInicio = hoy;
                    fechaFin = hoy;
                    break;
                case "MANANA":
                    fechaInicio = hoy.plusDays(1);
                    fechaFin = hoy.plusDays(1);
                    break;
                case "SEMANA":
                    fechaInicio = hoy;
                    fechaFin = hoy.plusDays(7);
                    break;
                case "MES":
                    fechaInicio = hoy.withDayOfMonth(1);
                    fechaFin = hoy.withDayOfMonth(hoy.lengthOfMonth());
                    break;
            }
        }

        // 3. LLAMAR AL REPOSITORIO
        List<Audiencia> listado = audienciaRepository.listarParaExcel(
                keyword, tipo, gerencia, materia, estatus,
                abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds);

        AudienciaExcelExporter excelExporter = new AudienciaExcelExporter(listado);
        excelExporter.export(response);
    }

    // --- API: BUSCAR ABOGADOS DISPONIBLES (PARA SUSTITUCIÓN EN AUDIENCIAS) ---
    @GetMapping("/api/abogados-disponibles/{expedienteId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerAbogadosDisponibles(@PathVariable String expedienteId) {
        try {
            // 1. Obtener datos del expediente
            Expediente exp = expedienteRepository.findById(UUID.fromString(expedienteId))
                    .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));

            Integer gerenciaId = exp.getGerencia().getId();
            Integer materiaId = exp.getMateria().getId();

            // 2. Buscar abogados que tengan esa materia (usamos el repo de usuarios)
            List<Usuario> abogadosConMateria = usuarioRepository.findByRolAndMateriaId(RolUsuario.ABOGADO, materiaId);

            // 3. Filtrar por Gerencia en memoria (para asegurar que sean del mismo equipo)
            List<Map<String, Object>> resultado = abogadosConMateria.stream()
                    .filter(u -> u.getGerencia() != null && u.getGerencia().getId().equals(gerenciaId))
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