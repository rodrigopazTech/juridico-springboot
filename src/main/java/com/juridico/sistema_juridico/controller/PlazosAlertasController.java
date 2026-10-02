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
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.service.configuracion.PerfilAtencionService;
import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.service.catalogo.TipoTerminoService;
import com.juridico.sistema_juridico.service.catalogo.TipoAudienciaService;
import com.juridico.sistema_juridico.Entity.configuracion.DiaInhabil;
import com.juridico.sistema_juridico.service.configuracion.CalendarioInstitucionalService;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequestMapping("/administracion/plazos-alertas")
@PreAuthorize("isAuthenticated()")
public class PlazosAlertasController {

    private final PerfilAtencionService perfiles;
    private final TipoTerminoService tiposTermino;
    private final TipoAudienciaService tiposAudiencia;
    private final TipoAudienciaRepository tipoAudienciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalendarioInstitucionalService calendario;

    public PlazosAlertasController(PerfilAtencionService perfiles, TipoTerminoService tiposTermino,
            TipoAudienciaService tiposAudiencia, TipoAudienciaRepository tipoAudienciaRepository,
            UsuarioRepository usuarioRepository, CalendarioInstitucionalService calendario) {
        this.perfiles = perfiles;
        this.tiposTermino = tiposTermino;
        this.tiposAudiencia = tiposAudiencia;
        this.tipoAudienciaRepository = tipoAudienciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.calendario = calendario;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("perfilesTermino", perfiles.listarTerminos());
        model.addAttribute("perfilesAudiencia", perfiles.listarAudiencias());
        Usuario usuario = usuarioActual();
        boolean alcanceGlobal = esAlcanceGlobal(usuario);
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("listaTiposTermino", tiposTermino.listarTodos().stream()
                .filter(t -> alcanceGlobal || t.getGerencia() == null || (usuario.getGerencia() != null
                        && t.getGerencia().getId().equals(usuario.getGerencia().getId())))
                .toList());
        model.addAttribute("listaTiposAudiencia", tipoAudienciaRepository.findAll().stream()
                .filter(t -> alcanceGlobal || t.getGerencia() == null || (usuario.getGerencia() != null
                        && t.getGerencia().getId().equals(usuario.getGerencia().getId())))
                .toList());
        model.addAttribute("perfilesTerminoDias", perfiles.publicados(ModoPerfilAlerta.DIAS_RESTANTES));
        model.addAttribute("perfilesTerminoProporcionales", perfiles.publicados(ModoPerfilAlerta.PROPORCIONAL_LIMITADO));
        model.addAttribute("perfilesAudienciaPublicados", perfiles.audienciasPublicadas());
        model.addAttribute("modosPlazoTermino", ModoPlazoTermino.values());
        model.addAttribute("nuevoPerfilTermino", PerfilAlertaTermino.builder()
                .modo(ModoPerfilAlerta.DIAS_RESTANTES)
                .amarilloDias(5).rojoDias(2)
                .amarilloPorcentaje(50).amarilloMinimoDias(3).amarilloMaximoDias(10)
                .rojoPorcentaje(20).rojoMinimoDias(1).rojoMaximoDias(4)
                .version(1).estado(EstadoPerfil.BORRADOR).build());
        model.addAttribute("nuevoPerfilAudiencia", PerfilRecordatorioAudiencia.builder()
                .anticipacionAmarillaHoras(72).anticipacionRojaHoras(24)
                .recordatoriosMinutos("10080,4320,1440,120")
                .version(1).estado(EstadoPerfil.BORRADOR).build());
        model.addAttribute("modosPerfil", ModoPerfilAlerta.values());
        model.addAttribute("diasInhabiles", calendario.listar());
        model.addAttribute("pageTitle", "Plazos y alertas");
        model.addAttribute("activePage", "plazos-alertas");
        return "views/admin/plazos-alertas";
    }

    @PostMapping("/terminos")
    public String guardarTermino(@ModelAttribute PerfilAlertaTermino perfil,
            @RequestParam(defaultValue = "borrador") String accion, RedirectAttributes flash) {
        try {
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
        return ejecutar(() -> perfiles.publicarTermino(id), "Regla de alerta para Términos publicada.", flash);
    }

    @PostMapping("/audiencias/{id}/publicar")
    public String publicarAudiencia(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> perfiles.publicarAudiencia(id), "Regla de alerta para Audiencias publicada.", flash);
    }

    @PostMapping("/terminos/{id}/inactivar")
    public String inactivarTermino(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> perfiles.inactivarTermino(id), "Regla de alerta para Términos inactivada.", flash);
    }

    @PostMapping("/audiencias/{id}/inactivar")
    public String inactivarAudiencia(@PathVariable Integer id, RedirectAttributes flash) {
        return ejecutar(() -> perfiles.inactivarAudiencia(id), "Regla de alerta para Audiencias inactivada.", flash);
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
    public String guardarTipoTermino(@ModelAttribute TipoTermino tipo, RedirectAttributes flash) {
        try {
            aplicarPoliticaDocumental(tipo);
            if (tipo.getId() == null) {
                tipo.setGerencia(usuarioActual().getGerencia());
                tiposTermino.crear(tipo);
            } else {
                tiposTermino.actualizar(tipo.getId(), tipo);
            }
            flash.addFlashAttribute("mensajeExito", "Tipo de Término guardado correctamente.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#tipos-termino";
    }

    @PostMapping("/tipos-termino/{id}/estado")
    public String cambiarTipoTermino(@PathVariable Integer id, @RequestParam boolean activo,
            RedirectAttributes flash) {
        return ejecutar(() -> tiposTermino.cambiarEstado(id, activo),
                activo ? "Tipo de Término activado." : "Tipo de Término inactivado.", flash);
    }

    @PostMapping("/tipos-audiencia")
    public String guardarTipoAudiencia(@ModelAttribute TipoAudiencia tipo, RedirectAttributes flash) {
        try {
            Usuario actor = usuarioActual();
            if (tipo.getId() == null) {
                tipo.setGerencia(actor.getGerencia());
                tipo.setUsuario(actor);
            }
            tipo.setRequiereActa(true);
            tipo.setPermiteExcepcionSinActa(false);
            tipo.setRequiereValidacionActa(true);
            tiposAudiencia.guardar(tipo);
            flash.addFlashAttribute("mensajeExito", "Tipo de Audiencia guardado correctamente.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#tipos-audiencia";
    }

    @PostMapping("/tipos-audiencia/{id}/estado")
    public String cambiarTipoAudiencia(@PathVariable Integer id, @RequestParam boolean activo,
            RedirectAttributes flash) {
        return ejecutar(() -> {
            TipoAudiencia tipo = tipoAudienciaRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de Audiencia no encontrado."));
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

    private boolean esAlcanceGlobal(Usuario usuario) {
        return usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECTOR_PRINCIPAL
                || usuario.getRol() == RolUsuario.DIRECTOR || usuario.getRol() == RolUsuario.SUBDIRECTOR;
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
            calendario.guardar(dia);
            flash.addFlashAttribute("mensajeExito", "Día inhábil agregado al calendario institucional.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#calendario";
    }

    @PostMapping("/calendario/{id}/estado")
    public String cambiarDiaInhabil(@PathVariable Integer id, @RequestParam boolean activo, RedirectAttributes flash) {
        try {
            calendario.cambiarEstado(id, activo);
            flash.addFlashAttribute("mensajeExito", activo ? "Día inhábil activado." : "Día inhábil inactivado.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/administracion/plazos-alertas#calendario";
    }
}
