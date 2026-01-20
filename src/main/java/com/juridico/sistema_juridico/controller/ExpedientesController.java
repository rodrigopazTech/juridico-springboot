package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.Prioridad;
import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.*;
import com.juridico.sistema_juridico.repository.Expediente.ActividadExpedienteRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/expedientes")
public class ExpedientesController {

    @Autowired private ExpedienteRepository expedienteRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private TipoExpedienteRepository tipoExpedienteRepository;
    @Autowired private OrganoJurisdiccionalRepository organoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstadoRepository estadoRepository;
    
    // Inyectamos el repositorio para guardar el historial
    @Autowired private ActividadExpedienteRepository actividadRepository;

    // ==========================================
    // 1. LISTADO PRINCIPAL (INDEX)
    // ==========================================
    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "") String keyword,
                        // Filtros opcionales
                        @RequestParam(required = false) Integer gerenciaId,
                        @RequestParam(required = false) Integer materiaId,
                        @RequestParam(required = false) Integer tipoId,
                        @RequestParam(required = false) Prioridad prioridad,
                        @RequestParam(required = false) Integer abogadoId) {
        
        model.addAttribute("pageTitle", "Gestión de Expedientes - Agenda Legal");

        // Cargar Listas para los Selects (Modal Nuevo y Filtros)
        model.addAttribute("listaGerencias", gerenciaRepository.findAll());
        model.addAttribute("listaMaterias", materiaRepository.findAll());
        model.addAttribute("listaTipos", tipoExpedienteRepository.findAll());
        model.addAttribute("listaOrganos", organoRepository.findAll());
        model.addAttribute("listaAbogados", usuarioRepository.findAll());
        model.addAttribute("listaPrioridades", Prioridad.values());
        model.addAttribute("listaEstados", estadoRepository.findAllByOrderByNombreAsc());

        // Configurar Paginación (20 por página, ordenado por fecha creación)
        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

        // Ejecutar Consulta Maestra
        Page<Expediente> paginaExpedientes = expedienteRepository.buscarExpedientes(
                keyword, gerenciaId, materiaId, tipoId, prioridad, abogadoId, pageable
        );

        model.addAttribute("expedientes", paginaExpedientes);
        
        // Mantener filtros en la vista
        model.addAttribute("keyword", keyword);
        model.addAttribute("gerenciaId", gerenciaId);
        
        return "views/expedientes/index";
    }

    // ==========================================
    // 2. CREAR NUEVO EXPEDIENTE
    // ==========================================
    @PostMapping("/guardar")
    public String guardarExpediente(@ModelAttribute Expediente expediente, RedirectAttributes flash) {
        try {
            // Si es nuevo
            if (expediente.getId() == null) {
                expediente.setCreatedAt(LocalDateTime.now());
                expediente.setEtapaProcesal(EtapaProcesal.TRAMITE);
                
                // Opcional: Registrar actividad de creación aquí también si deseas
            }
            expediente.setUpdatedAt(LocalDateTime.now());

            Expediente guardado = expedienteRepository.save(expediente);
            
            // Registrar Actividad de Creación (Opcional, pero recomendado)
            if (expediente.getCreatedAt().isEqual(expediente.getUpdatedAt())) {
                 Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                 Usuario usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);
                 
                 ActividadExpediente actividad = ActividadExpediente.builder()
                    .expediente(guardado)
                    .titulo("Creación de Expediente")
                    .descripcion("Se registró el expediente en el sistema.")
                    .tipoIcono("CREATE")
                    .usuario(usuarioActual)
                    .fechaRegistro(LocalDateTime.now())
                    .build();
                 actividadRepository.save(actividad);
            }

            flash.addFlashAttribute("success", "Expediente guardado correctamente.");
            
        } catch (Exception e) {
            e.printStackTrace();
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/expedientes";
    }

    // ==========================================
    // 3. VER DETALLE (CON HISTORIAL)
    // ==========================================
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable UUID id, Model model) {
        // 1. Buscar Expediente
        Expediente expediente = expedienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + id));

        model.addAttribute("expediente", expediente);

        // 2. Cargar Catálogos (Necesarios para el Modal de Edición que está en esta vista)
        model.addAttribute("gerencias", gerenciaRepository.findAll());
        model.addAttribute("materias", materiaRepository.findAll());
        model.addAttribute("tipos", tipoExpedienteRepository.findAll());
        model.addAttribute("organos", organoRepository.findAll());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("prioridades", Prioridad.values());

        // 3. Cargar Historial de Cambios
        List<ActividadExpediente> historial = actividadRepository.findByExpedienteIdOrderByFechaRegistroDesc(expediente.getId());
        model.addAttribute("historial", historial);

        return "views/expedientes/detalle";
    }

    // ==========================================
    // 4. EDITAR EXPEDIENTE (CORREGIDO Y ROBUSTO)
    // ==========================================
    @PostMapping("/editar")
    public String editarExpediente(@ModelAttribute Expediente expEditado, RedirectAttributes flash) {
        try {
            // A. Obtener datos originales y usuario
            Expediente expOriginal = expedienteRepository.findById(expEditado.getId())
                    .orElseThrow(() -> new RuntimeException("Expediente no encontrado"));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuarioActual = usuarioRepository.findByEmail(auth.getName()).orElse(null);

            // B. LISTA PARA ACUMULAR LOS CAMBIOS (Aquí está el truco)
            List<ActividadExpediente> cambiosParaGuardar = new java.util.ArrayList<>();

            // C. Detectar cambios y agregarlos a la lista (SIN GUARDAR AÚN)
            verificarCambio(expOriginal, "Descripción", expOriginal.getDescripcion(), expEditado.getDescripcion(), usuarioActual, cambiosParaGuardar);
            verificarCambio(expOriginal, "Prioridad", expOriginal.getPrioridad().name(), expEditado.getPrioridad().name(), usuarioActual, cambiosParaGuardar);
            
            // Verificaciones de objetos relacionados (Gerencia, Materia, etc.)
            if (expEditado.getGerencia() != null) {
                verificarCambio(expOriginal, "Gerencia", 
                    expOriginal.getGerencia() != null ? expOriginal.getGerencia().getNombre() : "Sin asignar", 
                    obtenerNombreGerencia(expEditado.getGerencia().getId()), 
                    usuarioActual, cambiosParaGuardar);
            }
            
            if (expEditado.getMateria() != null) {
                 verificarCambio(expOriginal, "Materia", 
                    expOriginal.getMateria() != null ? expOriginal.getMateria().getNombre() : "Sin asignar", 
                    obtenerNombreMateria(expEditado.getMateria().getId()), 
                    usuarioActual, cambiosParaGuardar);
            }

            if (expEditado.getTipoExpediente() != null) {
                 verificarCambio(expOriginal, "Tipo de Expediente", 
                    expOriginal.getTipoExpediente() != null ? expOriginal.getTipoExpediente().getNombre() : "Sin asignar", 
                    tipoExpedienteRepository.findById(expEditado.getTipoExpediente().getId()).map(t -> t.getNombre()).orElse("Desconocido"), 
                    usuarioActual, cambiosParaGuardar);
            }

            // D. Actualizar y Guardar el Expediente PRIMERO
            expOriginal.setNumero(expEditado.getNumero());
            expOriginal.setDescripcion(expEditado.getDescripcion());
            expOriginal.setPrioridad(expEditado.getPrioridad());
            expOriginal.setGerencia(expEditado.getGerencia());
            expOriginal.setMateria(expEditado.getMateria());
            expOriginal.setTipoExpediente(expEditado.getTipoExpediente());
            expOriginal.setOrganoJurisdiccional(expEditado.getOrganoJurisdiccional());
            expOriginal.setAbogadoResponsable(expEditado.getAbogadoResponsable());
            expOriginal.setUpdatedAt(LocalDateTime.now());

            expedienteRepository.save(expOriginal); // Guardamos el padre

            // E. AHORA SÍ, Guardamos todo el historial de una sola vez
            if (!cambiosParaGuardar.isEmpty()) {
                actividadRepository.saveAll(cambiosParaGuardar);
            }

            flash.addFlashAttribute("success", "Expediente actualizado correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            flash.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/expedientes/" + expEditado.getId();
    }

    // MÉTODO AUXILIAR ACTUALIZADO (Ya no guarda, solo agrega a la lista)
    private void verificarCambio(Expediente exp, String campo, String valorAnt, String valorNuevo, Usuario usuario, List<ActividadExpediente> lista) {
        // Normalizamos nulos a cadenas vacías para evitar errores de comparación
        String v1 = valorAnt == null ? "" : valorAnt;
        String v2 = valorNuevo == null ? "" : valorNuevo;

        if (!v1.equals(v2)) {
            ActividadExpediente actividad = ActividadExpediente.builder()
                    .expediente(exp)
                    .titulo("Actualización de " + campo)
                    .descripcion("Cambió de '" + v1 + "' a '" + v2 + "'.")
                    .tipoIcono("EDIT")
                    .usuario(usuario)
                    .fechaRegistro(LocalDateTime.now())
                    .build();
            
            lista.add(actividad); // Solo agregamos a la lista
        }
    }


    // Helpers para obtener nombres rápidos solo con ID (Evita cargar todo el objeto si no es necesario)
    private String obtenerNombreGerencia(Integer id) {
        return gerenciaRepository.findById(id).map(g -> g.getNombre()).orElse("Desconocido");
    }
    private String obtenerNombreMateria(Integer id) {
        return materiaRepository.findById(id).map(m -> m.getNombre()).orElse("Desconocido");
    }
}