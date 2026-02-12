package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.service.UsuarioService;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository; 
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors; 
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays; 

@Controller
@RequestMapping("/usuarios")
public class UsuariosController {

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository; 

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int pageUsuarios,   
                        @RequestParam(defaultValue = "0") int pageGerencias) { 
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Usamos orElseThrow con la excepción personalizada para activar la alerta roja si pasa algo raro
        Usuario usuarioActual = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario actual no encontrado en la base de datos."));
        
        Page<Usuario> usuariosPage;
        Pageable pageRequest = PageRequest.of(pageUsuarios, 10, Sort.by("nombreCompleto").ascending());

        if (usuarioActual.getRol() == RolUsuario.GERENTE && usuarioActual.getGerencia() != null) {
            usuariosPage = usuarioRepository.findByGerencia(usuarioActual.getGerencia(), pageRequest);
        } else {
            usuariosPage = usuarioService.listarUsuariosPaginados(pageRequest);
        }
        
        Page<Gerencia> gerenciasPage = usuarioService.listarGerenciasPaginadas(PageRequest.of(pageGerencias, 10, Sort.by("nombre").ascending()));

        model.addAttribute("listaUsuarios", usuariosPage); 
        model.addAttribute("listaGerencias", gerenciasPage);
        
        if (usuarioActual.getRol() == RolUsuario.GERENTE) {
            model.addAttribute("allGerencias", Arrays.asList(usuarioActual.getGerencia()));
            model.addAttribute("listaRoles", Arrays.asList(RolUsuario.ABOGADO, RolUsuario.JEFE_DEPTO));
        } else {
            model.addAttribute("allGerencias", usuarioService.listarGerencias()); 
            model.addAttribute("listaRoles", RolUsuario.values());
        }

        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("nuevoUsuario", new Usuario());
        model.addAttribute("nuevaGerencia", new Gerencia());
        model.addAttribute("activePage", "usuarios");
        
        return "views/usuarios/index";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, 
                                 @RequestParam(required = false) java.util.List<Integer> materiasIds, 
                                 RedirectAttributes redirectAttrs) {
        // SIN TRY-CATCH: Si falla (ej. correo duplicado), el GlobalHandler lo atrapa.
        usuarioService.guardarUsuario(usuario, materiasIds);
        
        redirectAttrs.addFlashAttribute("mensaje", "Usuario guardado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    @PostMapping("/gerencias/guardar")
    public String guardarGerencia(@ModelAttribute Gerencia gerencia, RedirectAttributes redirectAttrs) {
        usuarioService.guardarGerencia(gerencia);
        
        redirectAttrs.addFlashAttribute("mensaje", "Gerencia creada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=gerencias";
    }

    // --- ACCIONES DE ESTADO (Ahora lanzan excepciones limpias) ---

    @GetMapping("/toggle/{id}")
    public String toggleStatusUsuario(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioService.buscarPorId(id); // Lanza 404 si no existe
        usuario.setActivo(!usuario.getActivo());

        List<Integer> materiasIds = usuario.getMaterias().stream()
                .map(Materia::getId)
                .collect(Collectors.toList());

        usuarioService.guardarUsuario(usuario, materiasIds);

        String estado = usuario.getActivo() ? "activado" : "desactivado";
        redirectAttrs.addFlashAttribute("mensaje", "Usuario " + estado + " correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    @GetMapping("/gerencias/toggle/{id}")
    public String toggleStatusGerencia(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
        gerencia.setActivo(!gerencia.getActivo());
        usuarioService.guardarGerencia(gerencia);
        
        redirectAttrs.addFlashAttribute("mensaje", "Estatus de la gerencia actualizado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios?tab=gerencias";
    }

    // --- ELIMINACIÓN (Estandarizada a Redirect para mostrar SweetAlert) ---
    // Antes eran @DeleteMapping (API), ahora son @GetMapping (Web)

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        // Si tiene expedientes asignados, lanzará DataIntegrityViolationException 
        // y el GlobalExceptionHandler mostrará la alerta roja automáticamente.
        usuarioService.eliminarUsuario(id);
        
        redirectAttrs.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/usuarios";
    }

    @GetMapping("/gerencias/materias/eliminar/{id}")
    public String eliminarMateria(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        usuarioService.eliminarMateria(id);
        
        redirectAttrs.addFlashAttribute("mensaje", "Materia eliminada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        // Asumimos que quieres volver a la pestaña de gerencias o donde estabas
        return "redirect:/usuarios?tab=gerencias";
    }

    // --- API JSON (Mantenemos estas como API porque las usa el JavaScript de los modales) ---

    @GetMapping("/api/materias-por-gerencia/{id}")
    @ResponseBody
    public ResponseEntity<java.util.List<Materia>> getMateriasJson(@PathVariable Integer id) {
        // Aquí sí usamos try-catch local o dejamos que el Handler devuelva JSON si lo configuramos
        // Por simplicidad, mantendremos la respuesta directa.
        try {
            Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
            return ResponseEntity.ok(new ArrayList<>(gerencia.getMaterias()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/gerencias/{id}/materias")
    public String obtenerMateriasGerencia(@PathVariable Integer id, Model model) {
        Gerencia gerencia = usuarioService.buscarGerenciaPorId(id);
        model.addAttribute("materias", gerencia.getMaterias()); 
        return "fragments/usuarios/modal-materias :: lista";
    }

    @PostMapping("/gerencias/materias/guardar")
    @ResponseBody 
    public ResponseEntity<?> guardarMateria(@RequestBody Materia materia) {
        try {
            usuarioService.guardarMateria(materia);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al guardar materia");
        }
    }
}