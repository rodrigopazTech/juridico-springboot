package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;

import java.io.IOException;   
import java.nio.file.*;        
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/terminos")
public class TerminosController {

    @Autowired
    private TerminoRepository terminoRepository;

    @Autowired
    private ExpedienteRepository expedienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. VISTA PRINCIPAL (LISTADO)
    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int page,
                        // Parámetros de filtro (pueden venir vacíos)
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String estatus,
                        @RequestParam(required = false) Prioridad prioridad,
                        @RequestParam(required = false) Integer abogadoId) {

        // 1. Configurar Paginación (Ordenado por Vencimiento)
        Pageable pageable = PageRequest.of(page, 10, Sort.by("fechaVencimiento").ascending());

        // 2. Llamar a la consulta maestra
        Page<Termino> paginaTerminos = terminoRepository.buscarConFiltros(
                keyword, estatus, prioridad, abogadoId, pageable
        );

        // 3. Pasar resultados a la vista
        model.addAttribute("terminos", paginaTerminos);
        model.addAttribute("pageTitle", "Gestión de Términos - Agenda Legal");
        model.addAttribute("activePage", "terminos");

        // 4. Pasar listas para los SELECTS de los filtros
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaPrioridades", Prioridad.values());
        
        // Lista manual de estatus para el filtro (o podrías crear un Enum si prefieres)
        model.addAttribute("listaEstatus", new String[]{"Proyectista", "Revisión", "Gerencia", "Dirección", "Liberado", "Presentado", "Concluido"});

        // 5. Listas para el Modal de Crear (Ya las tenías)
        model.addAttribute("expedientesList", expedienteRepository.findAll());
        model.addAttribute("abogados", usuarioRepository.findAll());

        // 6. Mantener los filtros seleccionados en la vista (UX)
        model.addAttribute("keyword", keyword);
        model.addAttribute("paramEstatus", estatus);
        model.addAttribute("paramPrioridad", prioridad);
        model.addAttribute("paramAbogadoId", abogadoId);

        return "views/terminos/index";
    }

    // 2. GUARDAR TÉRMINO
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Termino terminoForm,
                          @RequestParam(value = "expedienteId", required = false) UUID expedienteId,
                          RedirectAttributes redirectAttrs) {
        try {
            Termino terminoGuardar;

            // CASO 1: EDICIÓN (El ID viene en el formulario)
            if (terminoForm.getId() != null) {
                Termino existente = terminoRepository.findById(terminoForm.getId()).orElse(null);
                
                if (existente != null) {
                    // Actualizamos solo los campos editables
                    existente.setActuacion(terminoForm.getActuacion());
                    existente.setFechaVencimiento(terminoForm.getFechaVencimiento());
                    existente.setAbogadoResponsable(terminoForm.getAbogadoResponsable());
                    existente.setUpdatedAt(LocalDateTime.now());
                    
                    // Si cambiaron el expediente
                    if (expedienteId != null) {
                         Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
                         if (exp != null) existente.setExpediente(exp);
                    }
                    
                    terminoGuardar = existente; // Usamos el objeto original actualizado
                } else {
                    redirectAttrs.addFlashAttribute("mensaje", "El término a editar no existe.");
                    return "redirect:/terminos";
                }
            } 
            // CASO 2: CREACIÓN (Nuevo término)
            else {
                terminoGuardar = terminoForm;
                terminoGuardar.setFechaIngreso(LocalDate.now());
                terminoGuardar.setEstatusTermino("Proyectista");
                terminoGuardar.setCreatedAt(LocalDateTime.now());
                terminoGuardar.setUpdatedAt(LocalDateTime.now());

                if (expedienteId != null) {
                    Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
                    if (exp != null) {
                        terminoGuardar.setExpediente(exp);
                        // Heredar prioridad
                        if (terminoGuardar.getPrioridad() == null) {
                            terminoGuardar.setPrioridad(exp.getPrioridad());
                        }
                    }
                } else {
                    throw new RuntimeException("Debes seleccionar un expediente.");
                }
            }

            terminoRepository.save(terminoGuardar);

            redirectAttrs.addFlashAttribute("mensaje", "Término guardado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error al guardar: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }
    
    // 3. AVANZAR ESTADO
    @GetMapping("/avanzar/{id}")
    public String avanzarEstado(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Termino termino = terminoRepository.findById(id).orElse(null);
        if(termino != null) {
            String actual = termino.getEstatusTermino();
            String siguiente = calcularSiguienteEstado(actual);
            
            termino.setEstatusTermino(siguiente);
            terminoRepository.save(termino);
            
            redirectAttrs.addFlashAttribute("mensaje", "El término avanzó a: " + siguiente);
            redirectAttrs.addFlashAttribute("tipo", "success");
        }
        return "redirect:/terminos";
    }

    private String calcularSiguienteEstado(String actual) {
        if (actual == null) return "Proyectista";
        switch (actual) {
            case "Proyectista": return "Revisión";
            case "Revisión": return "Gerencia";
            case "Gerencia": return "Dirección";
            case "Dirección": return "Liberado";
            case "Liberado": return "Presentado";
            case "Presentado": return "Concluido";
            default: return actual;
        }
    }

    // 4. ELIMINAR
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            terminoRepository.deleteById(id);
            redirectAttrs.addFlashAttribute("mensaje", "Término eliminado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensaje", "No se pudo eliminar el término.");
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/terminos";
    }

    // 5. SUBIR ARCHIVO (¡ESTE ES EL QUE FALTABA!)
    @PostMapping("/subir-archivo")
    public String subirArchivo(@RequestParam("id") Integer id,
                               @RequestParam("archivo") MultipartFile archivo,
                               RedirectAttributes redirectAttrs) {
        if (archivo.isEmpty()) {
            redirectAttrs.addFlashAttribute("mensaje", "Por favor selecciona un archivo válido.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/terminos";
        }

        try {
            Termino termino = terminoRepository.findById(id).orElse(null);
            if (termino != null) {
                // Definir carpeta de destino (ej: uploads/terminos en la raíz del proyecto)
                String carpetaDestino = "uploads/terminos/";
                Path rutaCarpeta = Paths.get(carpetaDestino);
                
                // Crear carpeta si no existe
                if (!Files.exists(rutaCarpeta)) {
                    Files.createDirectories(rutaCarpeta);
                }

                // Generar nombre único: ID_NombreOriginal
                String nombreOriginal = archivo.getOriginalFilename();
                String nombreFinal = id + "_" + nombreOriginal;
                Path rutaArchivo = rutaCarpeta.resolve(nombreFinal);

                // Guardar archivo (Reemplaza si ya existe)
                Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

                // Actualizar BD
                termino.setArchivoWord(nombreFinal);
                terminoRepository.save(termino);

                redirectAttrs.addFlashAttribute("mensaje", "Archivo cargado correctamente: " + nombreOriginal);
                redirectAttrs.addFlashAttribute("tipo", "success");
            }
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("mensaje", "Error al subir el archivo: " + e.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }

        return "redirect:/terminos";
    }
}