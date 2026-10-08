package com.juridico.sistema_juridico.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.service.configuracion.PerfilAtencionService;
import com.juridico.sistema_juridico.service.configuracion.PlazosAlertasConfiguracionService;
import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.service.catalogo.TipoTerminoService;
import com.juridico.sistema_juridico.Entity.configuracion.DiaInhabil;
import com.juridico.sistema_juridico.Entity.configuracion.SuspensionPlazo;
import com.juridico.sistema_juridico.service.configuracion.CalendarioInstitucionalService;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequestMapping("/administracion/plazos-alertas")
@PreAuthorize("isAuthenticated()")
public class PlazosAlertasController {

    private final PerfilAtencionService perfiles;
    private final TipoTerminoService tiposTermino;
    private final TipoAudienciaRepository tipoAudienciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalendarioInstitucionalService calendario;
    private final MateriaRepository materiaRepository;
    private final PlazosAlertasConfiguracionService configuracion;

    public PlazosAlertasController(PerfilAtencionService perfiles, TipoTerminoService tiposTermino,
            TipoAudienciaRepository tipoAudienciaRepository,
            UsuarioRepository usuarioRepository, CalendarioInstitucionalService calendario,
            MateriaRepository materiaRepository, PlazosAlertasConfiguracionService configuracion) {
        this.perfiles = perfiles;
        this.tiposTermino = tiposTermino;
        this.tipoAudienciaRepository = tipoAudienciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.calendario = calendario;
        this.materiaRepository = materiaRepository;
        this.configuracion = configuracion;
    }

    @GetMapping
    public String index(Model model) {
        Usuario usuario = usuarioActual();
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("puedeGestionarReglas", puedeGestionarReglas(usuario));
        model.addAttribute("puedeGestionarTipos", puedeGestionarTipos(usuario));
        model.addAttribute("puedeGestionarCalendario", puedeGestionarCalendario(usuario));
        var materiasDisponibles = materiaRepository.findByActivoTrueOrderByNombreAsc();
        model.addAttribute("materiasDisponibles", materiasDisponibles);
        model.addAttribute("listaTiposTermino", tiposTermino.listarTodos().stream()
                .toList());
        model.addAttribute("listaTiposAudiencia", tipoAudienciaRepository.findAll().stream()
                .toList());
        model.addAttribute("modosPlazoTermino", ModoPlazoTermino.values());
        model.addAttribute("diasInhabiles", calendario.listar());
        model.addAttribute("suspensionesPlazo", calendario.listarSuspensiones());
        model.addAttribute("versionCalendario", calendario.snapshotActual().version());
        model.addAttribute("pageTitle", "Plazos y alertas");
        model.addAttribute("activePage", "plazos-alertas");
        return "views/admin/plazos-alertas";
    }

    @PostMapping("/terminos")
    public String guardarTermino(@ModelAttribute PerfilAlertaTermino perfil,
            @RequestParam(defaultValue = "borrador") String accion, RedirectAttributes flash) {
        try {
            exigirGestionReglas();
            PerfilAlertaTermino guardado = perfiles.guardarTermino(perfil);
            if ("publicar".equals(accion)) {
                perfiles.publicarTermino(guardado.getId());
                flash.addFlashAttribute("mensajeExito", "Regla de alerta guardada y publicada.");
            } else {
                flash.addFlashAttribute("mensajeExito", "Regla de alerta guardada como borrador.");
            }
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#terminos";
    }

    @PostMapping("/audiencias")
    public String guardarAudiencia(@ModelAttribute PerfilRecordatorioAudiencia perfil,
            @RequestParam(defaultValue = "borrador") String accion, RedirectAttributes flash) {
        try {
            exigirGestionReglas();
            PerfilRecordatorioAudiencia guardado = perfiles.guardarAudiencia(perfil);
            if ("publicar".equals(accion)) {
                perfiles.publicarAudiencia(guardado.getId());
                flash.addFlashAttribute("mensajeExito", "Regla de alerta guardada y publicada.");
            } else {
                flash.addFlashAttribute("mensajeExito", "Regla de alerta guardada como borrador.");
            }
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#audiencias";
    }

    @PostMapping("/terminos/{id}/publicar")
    public String publicarTermino(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> { exigirGestionReglas(); perfiles.publicarTermino(id); }, "Regla de alerta para Términos publicada.", flash);
    }

    @PostMapping("/audiencias/{id}/publicar")
    public String publicarAudiencia(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> { exigirGestionReglas(); perfiles.publicarAudiencia(id); }, "Regla de alerta para Audiencias publicada.", flash);
    }

    @PostMapping("/terminos/{id}/inactivar")
    public String inactivarTermino(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> { exigirGestionReglas(); perfiles.inactivarTermino(id); }, "Regla de alerta para Términos inactivada.", flash);
    }

    @PostMapping("/audiencias/{id}/inactivar")
    public String inactivarAudiencia(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> { exigirGestionReglas(); perfiles.inactivarAudiencia(id); }, "Regla de alerta para Audiencias inactivada.", flash);
    }

    private String ejecutar(Runnable accion, String mensaje, RedirectAttributes flash) {
        try {
            accion.run();
            flash.addFlashAttribute("mensajeExito", mensaje);
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas";
    }

    @PostMapping("/tipos-termino")
    public String guardarTipoTermino(@ModelAttribute TipoTermino tipo,
            @RequestParam(defaultValue = "5") Integer atencionDias,
            @RequestParam(defaultValue = "2") Integer criticoDias,
            RedirectAttributes flash) {
        try {
            tipo.setMateria(resolverMateria(tipo.getMateria()));
            tipo.setGerencia(tipo.getMateria().getGerencia());
            validarGestionTipo(tipo.getMateria());
            aplicarPoliticaDocumental(tipo);
            configuracion.guardarTipoTermino(tipo, atencionDias, criticoDias);
            flash.addFlashAttribute("mensajeExito", "Tipo de Término guardado correctamente.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#terminos";
    }

    @PostMapping("/tipos-termino/{id}/estado")
    public String cambiarTipoTermino(@PathVariable Integer id, @RequestParam boolean activo,
            RedirectAttributes flash) {
        return ejecutar(() -> {
            TipoTermino tipo = tiposTermino.obtenerPorId(id);
            validarGestionTipo(tipo.getMateria());
            tiposTermino.cambiarEstado(id, activo);
        },
                activo ? "Tipo de Término activado." : "Tipo de Término inactivado.", flash);
    }

    @PostMapping("/tipos-audiencia")
    public String guardarTipoAudiencia(@ModelAttribute TipoAudiencia tipo,
            @RequestParam(defaultValue = "3") Integer atencionDias,
            @RequestParam(defaultValue = "1") Integer criticoDias,
            RedirectAttributes flash) {
        try {
            Usuario actor = usuarioActual();
            tipo.setMateria(resolverMateria(tipo.getMateria()));
            tipo.setGerencia(tipo.getMateria().getGerencia());
            if (tipo.getId() == null) {
                tipo.setUsuario(actor);
            }
            validarGestionTipo(tipo.getMateria());
            tipo.setRequiereActa(true);
            tipo.setPermiteExcepcionSinActa(false);
            tipo.setRequiereValidacionActa(true);
            configuracion.guardarTipoAudiencia(tipo, atencionDias, criticoDias);
            flash.addFlashAttribute("mensajeExito", "Tipo de Audiencia guardado correctamente.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#audiencias";
    }

    @PostMapping("/tipos-audiencia/{id}/estado")
    public String cambiarTipoAudiencia(@PathVariable Integer id, @RequestParam boolean activo,
            RedirectAttributes flash) {
        return ejecutar(() -> {
            TipoAudiencia tipo = tipoAudienciaRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de Audiencia no encontrado."));
            validarGestionTipo(tipo.getMateria());
            tipo.setActivo(activo);
            tipoAudienciaRepository.save(tipo);
        }, activo ? "Tipo de Audiencia activado." : "Tipo de Audiencia inactivado.", flash);
    }

    private Usuario usuarioActual() {
        String identidad = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(identidad)
                .or(() -> usuarioRepository.findByUsernameIgnoreCase(identidad))
                .orElseThrow();
    }

    private boolean puedeGestionarReglas(Usuario usuario) {
        return true;
    }

    private boolean puedeGestionarTipos(Usuario usuario) {
        return puedeGestionarReglas(usuario);
    }

    private boolean puedeGestionarCalendario(Usuario usuario) {
        return true;
    }

    private void exigirGestionReglas() {
        // La configuración es colaborativa: cualquier usuario autenticado puede gestionarla.
    }

    private void validarGestionTipo(com.juridico.sistema_juridico.Entity.catalogo.Materia materia) {
        if (materia == null) {
            throw new IllegalArgumentException("La Materia es obligatoria.");
        }
    }

    private com.juridico.sistema_juridico.Entity.catalogo.Materia resolverMateria(
            com.juridico.sistema_juridico.Entity.catalogo.Materia materia) {
        if (materia == null || materia.getId() == null) {
            throw new IllegalArgumentException("La Materia es obligatoria.");
        }
        return materiaRepository.findById(materia.getId())
                .orElseThrow(() -> new IllegalArgumentException("La Materia seleccionada no existe."));
    }

    private void exigirGestionCalendario() {
        // La configuración es colaborativa: cualquier usuario autenticado puede gestionarla.
    }

    private void aplicarPoliticaDocumental(TipoTermino tipo) {
        tipo.setRequiereDocumentoLiberado(true);
        tipo.setRequiereAcuse(true);
        tipo.setPermiteExcepcionSinAcuse(true);
        tipo.setRequiereValidacionAcuse(true);
    }

    @PostMapping("/calendario")
    public String guardarDiaInhabil(@ModelAttribute DiaInhabil dia, RedirectAttributes flash) {
        try {
            exigirGestionCalendario();
            calendario.guardar(dia, usuarioActual().getNombreCompleto());
            flash.addFlashAttribute("mensajeExito", "Día inhábil agregado al calendario institucional.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#calendario";
    }

    @PostMapping("/calendario/{id}/estado")
    public String cambiarDiaInhabil(@PathVariable Integer id, @RequestParam boolean activo, RedirectAttributes flash) {
        try {
            exigirGestionCalendario();
            calendario.cambiarEstado(id, activo, usuarioActual().getNombreCompleto());
            flash.addFlashAttribute("mensajeExito", activo ? "Día inhábil activado." : "Día inhábil inactivado.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#calendario";
    }

    @PostMapping("/calendario/suspensiones")
    public String guardarSuspension(@ModelAttribute SuspensionPlazo suspension, RedirectAttributes flash) {
        try {
            exigirGestionCalendario();
            calendario.guardarSuspension(suspension, usuarioActual().getNombreCompleto());
            flash.addFlashAttribute("mensajeExito", "Suspensión institucional registrada. Se publicó una nueva versión del calendario.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#calendario";
    }

    @PostMapping("/calendario/suspensiones/{id}/estado")
    public String cambiarSuspension(@PathVariable Integer id, @RequestParam boolean activo,
            RedirectAttributes flash) {
        try {
            exigirGestionCalendario();
            calendario.cambiarEstadoSuspension(id, activo, usuarioActual().getNombreCompleto());
            flash.addFlashAttribute("mensajeExito", activo ? "Suspensión activada." : "Suspensión inactivada.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#calendario";
    }
}
