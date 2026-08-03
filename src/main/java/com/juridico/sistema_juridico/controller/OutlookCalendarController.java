package com.juridico.sistema_juridico.controller;

import java.util.UUID;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftTokenResponse;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.service.MicrosoftGraphClient;
import com.juridico.sistema_juridico.service.OutlookCalendarService;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OutlookCalendarController {

    private static final String OUTLOOK_OAUTH_STATE = "OUTLOOK_OAUTH_STATE";

    private final MicrosoftAuthProperties properties;
    private final MicrosoftGraphClient graphClient;
    private final OutlookCalendarService outlookCalendarService;
    private final UsuarioRepository usuarioRepository;

    public OutlookCalendarController(
            MicrosoftAuthProperties properties,
            MicrosoftGraphClient graphClient,
            OutlookCalendarService outlookCalendarService,
            UsuarioRepository usuarioRepository) {
        this.properties = properties;
        this.graphClient = graphClient;
        this.outlookCalendarService = outlookCalendarService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({ "/calendario/outlook/conectar", "/perfil/outlook/conectar" })
    public String conectar(HttpSession session, RedirectAttributes redirectAttrs) {
        if (!properties.isCalendarConfigured()) {
            redirectAttrs.addFlashAttribute("mensaje", "La vinculacion de calendario Outlook aun no esta configurada.");
            redirectAttrs.addFlashAttribute("tipo", "warning");
            return "redirect:/calendario";
        }
        String state = UUID.randomUUID().toString();
        session.setAttribute(OUTLOOK_OAUTH_STATE, state);
        return "redirect:" + graphClient.buildCalendarAuthorizationUrl(state);
    }

    @GetMapping({ "/calendario/outlook/callback", "/perfil/outlook/callback" })
    public String callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        String expectedState = (String) session.getAttribute(OUTLOOK_OAUTH_STATE);
        session.removeAttribute(OUTLOOK_OAUTH_STATE);
        if (error != null || code == null || expectedState == null || !expectedState.equals(state)) {
            redirectAttrs.addFlashAttribute("mensaje", "Microsoft no completo la vinculacion del calendario.");
            redirectAttrs.addFlashAttribute("tipo", "error");
            return "redirect:/calendario";
        }

        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
            MicrosoftTokenResponse token = graphClient.exchangeCalendarCode(code);
            outlookCalendarService.conectarCalendario(usuario, token, null);
            redirectAttrs.addFlashAttribute("mensaje", "Calendario Outlook vinculado correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("mensaje", "No fue posible vincular Outlook: " + ex.getMessage());
            redirectAttrs.addFlashAttribute("tipo", "error");
        }
        return "redirect:/calendario";
    }

    @PostMapping({ "/calendario/outlook/desconectar", "/perfil/outlook/desconectar" })
    public String desconectar(RedirectAttributes redirectAttrs) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        outlookCalendarService.desconectarCalendario(usuario);
        redirectAttrs.addFlashAttribute("mensaje", "Calendario Outlook desconectado.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        return "redirect:/calendario";
    }
}
