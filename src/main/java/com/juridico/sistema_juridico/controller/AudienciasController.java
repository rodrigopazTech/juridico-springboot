package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.service.procesal.AudienciaService;
import com.juridico.sistema_juridico.repository.Expediente.ColaboradorExpedienteRepository;
import com.juridico.sistema_juridico.Entity.expediente.ColaboradorExpediente;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;

import com.juridico.sistema_juridico.service.SecurityService;
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

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.util.AudienciaExcelExporter;

import jakarta.servlet.http.HttpServletResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.io.IOException;
import java.util.List;
import java.net.MalformedURLException;
import java.util.ArrayList;

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

    @Autowired
    private ColaboradorExpedienteRepository colaboradorRepository;
    @Autowired
    private SecurityService securityService;
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
        // 3. FECHAS CENTINELA
        // Refactor ROD-22: Usar PeriodoFiltro
        com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro periodoEnum = com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro.TODOS;

        if (periodo != null && !periodo.isEmpty()) {
            try {
                periodoEnum = com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro.valueOf(periodo.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Si el parametro no coincide, ignoramos y usamos TODOS
            }
        }

        LocalDate[] rango = com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro.calcularRango(periodoEnum, null,
                null);
        LocalDate fechaInicio = rango[0];
        LocalDate fechaFin = rango[1];

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

        // 5. DROPDOWN INTELIGENTE
        List<Expediente> expedientesParaDropdown;
        if (rol.equals("ABOGADO")) {
            expedientesParaDropdown = expedienteRepository.findAll().stream()
                    .filter(e -> e.getAbogadoResponsable() != null
                            && e.getAbogadoResponsable().getId().equals(usuario.getId()))
                    .collect(Collectors.toList());
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

        // ROD-16: Filtrar Abogados por Gerencia
        List<Usuario> listaAbogados;
        if (rol.equals("DIRECCION") || rol.equals("SUBDIRECCION")) {
            listaAbogados = usuarioRepository.findAll(); // O findByActivoTrue() si se prefiere
        } else {
            // Gerentes, Jefes y Abogados solo ven gente de su gerencia
            if (usuario.getGerencia() != null) {
                listaAbogados = usuarioRepository.findByGerenciaAndActivoTrue(usuario.getGerencia());
            } else {
                listaAbogados = new ArrayList<>(); // Caso borde: usuario sin gerencia asignada
            }
        }
        model.addAttribute("abogados", listaAbogados);

        // Filtros en vista
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramTipo", tipo);
        model.addAttribute("paramGerencia", gerencia);
        model.addAttribute("paramMateria", materia);
        model.addAttribute("paramEstatus", estatus);
        model.addAttribute("paramAbogadoId", abogadoId);
        model.addAttribute("paramPeriodo", periodo);

        return "views/audiencias/index";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Audiencia audienciaForm,
            @RequestParam("expedienteId") UUID expedienteId,
            @RequestParam("tipoAudienciaId") Integer tipoAudienciaId,
            RedirectAttributes redirectAttrs) {
        try {
            Audiencia audienciaFinal;

            if (audienciaForm.getId() != null) {
                Audiencia existente = audienciaRepository.findById(audienciaForm.getId())
                        .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

                existente.setFechaAudiencia(audienciaForm.getFechaAudiencia());
                existente.setHoraAudiencia(audienciaForm.getHoraAudiencia());
                existente.setSalaLugar(audienciaForm.getSalaLugar());
                existente.setEsVirtual(audienciaForm.getEsVirtual());
                existente.setUrlReunion(audienciaForm.getUrlReunion());
                existente.setAbogadoComparece(audienciaForm.getAbogadoComparece());

                audienciaFinal = existente;
            } else {
                audienciaFinal = audienciaForm;
                audienciaFinal.setEstatusAudiencia(EstatusAudiencia.PENDIENTE);
                audienciaFinal.setCreatedAt(LocalDateTime.now());
            }

            Expediente exp = expedienteRepository.findById(expedienteId)
                    .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));

            // VALIDACIÓN ROD-19
            Usuario actor = usuarioRepository
                    .findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow();
            if (!securityService.tieneAccesoEscritura(actor, exp)) {
                redirectAttrs.addFlashAttribute("mensaje",
                        "Acceso denegado: No tiene permisos de escritura para este expediente.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/audiencias";
            }

            audienciaFinal.setExpediente(exp);

            TipoAudiencia tipo = tipoAudienciaRepository.findById(tipoAudienciaId)
                    .orElseThrow(() -> new RuntimeException("Tipo de audiencia no encontrado"));
            audienciaFinal.setTipoAudiencia(tipo);

            audienciaFinal.setUpdatedAt(LocalDateTime.now());

            Audiencia guardada = audienciaRepository.save(audienciaFinal);

            if (guardada.getAbogadoComparece() != null &&
                    !guardada.getAbogadoComparece().getId().equals(exp.getAbogadoResponsable().getId())) {

                ColaboradorExpediente colaborador = colaboradorRepository
                        .findByExpedienteIdAndUsuarioId(exp.getId(), guardada.getAbogadoComparece().getId())
                        .stream().findFirst().orElse(new ColaboradorExpediente());

                colaborador.setExpediente(exp);
                colaborador.setUsuario(guardada.getAbogadoComparece());
                colaborador
                        .setPermisoNivel(com.juridico.sistema_juridico.Entity.enums.PermisoColaborador.LECTURA_TOTAL);
                colaborador.setMotivo("Comparecencia en Audiencia: " + tipo.getNombre());

                LocalDateTime fechaBase = LocalDateTime.of(guardada.getFechaAudiencia(), guardada.getHoraAudiencia());
                colaborador.setFechaExpiracion(fechaBase.plusDays(1).withHour(23).withMinute(59));

                colaboradorRepository.save(colaborador);
            }

            redirectAttrs.addFlashAttribute("mensaje", "Audiencia guardada correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error al guardar: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/audiencias";
    }

    @GetMapping("/obtener/{id}")
    @ResponseBody
    public ResponseEntity<Audiencia> obtenerPorId(@PathVariable Integer id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        return audienciaRepository.findById(id)
                .map(audiencia -> {
                    if (usuario != null && securityService.tieneAccesoLectura(usuario, audiencia.getExpediente())) {
                        return ResponseEntity.ok(audiencia);
                    } else {
                        return ResponseEntity.status(403).<Audiencia>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/subir-acta")
    @ResponseBody
    public ResponseEntity<?> subirActa(@RequestParam("id") Integer id,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            Audiencia audiencia = audienciaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

            // SECURITY ROD-39: Validar permiso de escritura
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario actor = usuarioRepository.findByEmail(email).orElseThrow();
            if (!securityService.tieneAccesoEscritura(actor, audiencia.getExpediente())) {
                return ResponseEntity.status(403).body("Acceso denegado");
            }

            audienciaService.subirActa(id, archivo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/concluir")
    public String concluir(@RequestParam("id") Integer id,
            @RequestParam("observaciones") String observaciones,
            RedirectAttributes redirectAttrs) {
        try {
            // 1. OBTENER USUARIO ACTUAL
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

            // 2. VALIDAR PERMISOS (ROD-8: Solo Dirección puede concluir)
            Audiencia audiencia = audienciaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

            if (usuario.getRol() != RolUsuario.DIRECCION) {
                redirectAttrs.addFlashAttribute("mensaje",
                        "Acceso denegado: Solo la Dirección puede validar y concluir audiencias.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/audiencias";
            }

            // ROD-39: Validar también acceso de escritura al expediente
            if (!securityService.tieneAccesoEscritura(usuario, audiencia.getExpediente())) {
                redirectAttrs.addFlashAttribute("mensaje",
                        "Acceso denegado: No tiene permisos de escritura sobre este expediente.");
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

    @PostMapping("/eliminar")
    public String eliminar(@RequestParam("id") Integer id, RedirectAttributes redirectAttrs) {
        try {
            Audiencia audiencia = audienciaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Audiencia no encontrada"));

            // SECURITY ROD-13: Validar permiso de escritura
            Usuario actor = usuarioRepository
                    .findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow();
            if (!securityService.tieneAccesoEscritura(actor, audiencia.getExpediente())) {
                redirectAttrs.addFlashAttribute("mensaje",
                        "Acceso denegado: No tiene permisos para eliminar esta audiencia.");
                redirectAttrs.addFlashAttribute("tipo", "error");
                return "redirect:/audiencias";
            }

            audienciaRepository.deleteById(id);
            redirectAttrs.addFlashAttribute("mensaje", "Eliminado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "Error al eliminar.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
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

        // 2. LÓGICA DE FECHAS "CENTINELA" (REFACTOR ROD-52: Usar PeriodoFiltro)
        com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro periodoEnum = com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro.TODOS;

        if (periodo != null && !periodo.isEmpty()) {
            try {
                periodoEnum = com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro.valueOf(periodo.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Si no coincide, usamos TODOS
            }
        }

        LocalDate[] rango = com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro.calcularRango(periodoEnum, null,
                null);
        LocalDate fechaInicio = rango[0];
        LocalDate fechaFin = rango[1];

        // 3. LLAMAR AL REPOSITORIO
        // Ahora fechaInicio y fechaFin SIEMPRE tienen valor, nunca son null.
        List<Audiencia> listado = audienciaRepository.listarParaExcel(
                keyword, tipo, gerencia, materia, estatus,
                abogadoId, fechaInicio, fechaFin,
                filtroUsuarioId, filtroGerenciaId, filtroMateriaIds);

        AudienciaExcelExporter excelExporter = new AudienciaExcelExporter(listado);
        excelExporter.export(response);
    }
}