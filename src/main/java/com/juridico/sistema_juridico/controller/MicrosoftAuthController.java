package com.juridico.sistema_juridico.controller;

import java.util.UUID;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftTokenResponse;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftUserProfile;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.security.CustomUserDetailsService;
import com.juridico.sistema_juridico.security.JwtTokenProvider;
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;
import com.juridico.sistema_juridico.service.MicrosoftGraphClient;
import com.juridico.sistema_juridico.service.MicrosoftIdentityService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MicrosoftAuthController {

    private static final String OAUTH_STATE = "MICROSOFT_OAUTH_STATE";
    private static final String OAUTH_FLOW = "MICROSOFT_OAUTH_FLOW";
    private static final String TARGET_USER_ID = "MICROSOFT_TARGET_USER_ID";
    private static final String PENDING_PROFILE = "MICROSOFT_PENDING_PROFILE";
    private static final String PENDING_USER_ID = "MICROSOFT_PENDING_USER_ID";
    private static final String FLOW_LOGIN = "LOGIN";
    private static final String FLOW_PROFILE_LINK = "PROFILE_LINK";
    private static final String FLOW_ACTIVATION_LINK = "ACTIVATION_LINK";

    private final MicrosoftAuthProperties properties;
    private final MicrosoftGraphClient graphClient;
    private final MicrosoftIdentityService identityService;
    private final UsuarioRepository usuarioRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditoriaService auditoriaService;
    private final AuditoriaTecnicaService auditoriaTecnicaService;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    public MicrosoftAuthController(
            MicrosoftAuthProperties properties,
            MicrosoftGraphClient graphClient,
            MicrosoftIdentityService identityService,
            UsuarioRepository usuarioRepository,
            CustomUserDetailsService userDetailsService,
            JwtTokenProvider jwtTokenProvider,
            AuditoriaService auditoriaService,
            AuditoriaTecnicaService auditoriaTecnicaService) {
        this.properties = properties;
        this.graphClient = graphClient;
        this.identityService = identityService;
        this.usuarioRepository = usuarioRepository;
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditoriaService = auditoriaService;
        this.auditoriaTecnicaService = auditoriaTecnicaService;
    }

    @GetMapping("/auth/microsoft/login")
    public String login(HttpSession session, RedirectAttributes redirectAttrs) {
        return iniciarAutorizacion(FLOW_LOGIN, null, session, redirectAttrs, "/");
    }

    @GetMapping("/auth/microsoft/vincular-activacion")
    public String vincularDespuesDeActivacion(HttpSession session, RedirectAttributes redirectAttrs) {
        Object activatedUserId = session.getAttribute(PasswordResetController.ACTIVATED_USER_ID);
        if (!(activatedUserId instanceof Integer userId)) {
            redirectAttrs.addFlashAttribute("microsoftError",
                    "La sesion de activacion ya no esta disponible. Inicia sesion para vincular Microsoft desde Mi Perfil.");
            return "redirect:/";
        }
        return iniciarAutorizacion(FLOW_ACTIVATION_LINK, userId, session, redirectAttrs,
                "/auth/activacion-completada");
    }

    @GetMapping("/perfil/seguridad/microsoft/vincular")
    public String vincularDesdePerfil(HttpSession session, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioActual();
        return iniciarAutorizacion(FLOW_PROFILE_LINK, usuario.getId(), session, redirectAttrs,
                "/perfil?tab=seguridad");
    }

    @PostMapping("/perfil/seguridad/microsoft/desvincular")
    public String desvincularDesdePerfil(HttpServletRequest request, RedirectAttributes redirectAttrs) {
        Usuario usuario = usuarioActual();
        Usuario desvinculado = identityService.desvincularPerfilMicrosoft(usuario);
        registrarVinculacion(desvinculado, request, "CUENTA_MICROSOFT_DESVINCULADA",
                "El usuario desvinculo su cuenta Microsoft desde Mi Perfil.");
        redirectAttrs.addFlashAttribute("mensaje", "Cuenta Microsoft desvinculada correctamente.");
        redirectAttrs.addFlashAttribute("tipo", "success");
        redirectAttrs.addFlashAttribute("origenMensaje", "microsoft");
        return "redirect:/perfil?tab=seguridad";
    }

    @GetMapping("/auth/microsoft/vincular-confirmacion")
    public String mostrarConfirmacion(HttpSession session, Model model) {
        MicrosoftUserProfile profile = pendingProfile(session);
        Integer userId = pendingUserId(session);
        if (profile == null || userId == null) {
            return "redirect:/";
        }
        model.addAttribute("correoMicrosoft", profile.preferredEmail());
        return "views/auth/microsoft-vincular-confirmacion";
    }

    @PostMapping("/auth/microsoft/vincular-confirmacion")
    public String confirmarVinculacion(@RequestParam("vincular") boolean vincular,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            RedirectAttributes redirectAttrs) {
        MicrosoftUserProfile profile = pendingProfile(session);
        Integer userId = pendingUserId(session);
        limpiarPendiente(session);
        if (!vincular) {
            redirectAttrs.addFlashAttribute("microsoftError", "La vinculacion con Microsoft fue cancelada.");
            return "redirect:/";
        }
        if (profile == null || userId == null) {
            redirectAttrs.addFlashAttribute("microsoftError",
                    "La solicitud de vinculacion ya no esta disponible. Intenta nuevamente.");
            return "redirect:/";
        }

        try {
            Usuario usuario = usuarioRepository.findById(userId)
                    .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No existe una cuenta SIAJ activa asociada a este correo."));
            Usuario vinculado = identityService.vincularPerfilAUsuario(usuario, profile);
            registrarVinculacion(vinculado, request, "CUENTA_MICROSOFT_VINCULADA",
                    "El usuario autorizo la vinculacion durante el inicio de sesion Microsoft.");
            iniciarSesion(vinculado, request, response);
            return "redirect:/dashboard";
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("microsoftError", mensajeSeguro(ex.getMessage(),
                    "No fue posible vincular la cuenta Microsoft."));
            return "redirect:/";
        }
    }

    @GetMapping("/auth/microsoft/callback")
    public String callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        String flow = (String) session.getAttribute(OAUTH_FLOW);
        if (error != null) {
            limpiarFlujo(session);
            return redirigirError(flow,
                    "Microsoft no completo la autorizacion: " + mensajeSeguro(errorDescription, error),
                    redirectAttrs);
        }

        String expectedState = (String) session.getAttribute(OAUTH_STATE);
        session.removeAttribute(OAUTH_STATE);
        if (code == null || code.isBlank() || expectedState == null || !expectedState.equals(state)) {
            limpiarFlujo(session);
            return redirigirError(flow,
                    "No fue posible validar la respuesta de Microsoft. Intenta nuevamente.", redirectAttrs);
        }

        try {
            MicrosoftTokenResponse token = graphClient.exchangeCode(code);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                throw new IllegalArgumentException("Microsoft no regreso un token de acceso valido.");
            }
            MicrosoftUserProfile profile = graphClient.fetchMe(token.accessToken());

            if (FLOW_PROFILE_LINK.equals(flow) || FLOW_ACTIVATION_LINK.equals(flow)) {
                return completarVinculacionSolicitada(flow, profile, request, response, session, redirectAttrs);
            }

            limpiarFlujo(session);
            try {
                Usuario vinculado = identityService.vincularYObtenerUsuarioLocal(profile);
                iniciarSesion(vinculado, request, response);
                return "redirect:/dashboard";
            } catch (org.springframework.security.core.AuthenticationException ex) {
                throw ex;
            } catch (Exception noLink) {
                var candidato = identityService.buscarCuentaActivaPorCorreoMicrosoft(profile);
                if (candidato.isEmpty()) {
                    redirectAttrs.addFlashAttribute("microsoftError",
                            "Su cuenta Microsoft se autentico correctamente, pero no existe una cuenta SIAJ activa asociada a este correo.");
                    return "redirect:/";
                }
                session.setAttribute(PENDING_PROFILE, profile);
                session.setAttribute(PENDING_USER_ID, candidato.get().getId());
                return "redirect:/auth/microsoft/vincular-confirmacion";
            }
        } catch (Exception ex) {
            limpiarFlujo(session);
            auditoriaTecnicaService.registrarLoginFallido("microsoft", request, ex.getMessage());
            return redirigirError(flow,
                    mensajeSeguro(ex.getMessage(), "No fue posible iniciar sesion con Microsoft."), redirectAttrs);
        }
    }

    private String completarVinculacionSolicitada(String flow, MicrosoftUserProfile profile,
            HttpServletRequest request, HttpServletResponse response, HttpSession session,
            RedirectAttributes redirectAttrs) {
        Integer targetUserId = (Integer) session.getAttribute(TARGET_USER_ID);
        limpiarFlujo(session);
        if (targetUserId == null) {
            return redirigirError(flow, "La sesion de vinculacion ya no esta disponible.", redirectAttrs);
        }

        Usuario usuario = usuarioRepository.findById(targetUserId)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("El usuario SIAJ se encuentra inactivo."));

        if (FLOW_PROFILE_LINK.equals(flow) && !usuario.getId().equals(usuarioActual().getId())) {
            throw new IllegalArgumentException("La cuenta Microsoft no puede vincularse a otro usuario.");
        }

        Usuario vinculado = identityService.vincularPerfilAUsuario(usuario, profile);
        registrarVinculacion(vinculado, request, "CUENTA_MICROSOFT_VINCULADA",
                "El usuario vinculo su cuenta Microsoft de forma explicita.");

        if (FLOW_PROFILE_LINK.equals(flow)) {
            redirectAttrs.addFlashAttribute("mensaje", "Cuenta Microsoft vinculada correctamente.");
            redirectAttrs.addFlashAttribute("tipo", "success");
            redirectAttrs.addFlashAttribute("origenMensaje", "microsoft");
            return "redirect:/perfil?tab=seguridad";
        }

        session.removeAttribute(PasswordResetController.ACTIVATED_USER_ID);
        iniciarSesion(vinculado, request, response);
        return "redirect:/dashboard";
    }

    private String iniciarAutorizacion(String flow, Integer targetUserId, HttpSession session,
            RedirectAttributes redirectAttrs, String fallback) {
        if (!properties.isConfigured()) {
            redirectAttrs.addFlashAttribute("microsoftError",
                    "El inicio de sesion con Microsoft aun no esta configurado.");
            return "redirect:" + fallback;
        }
        String state = UUID.randomUUID().toString();
        session.setAttribute(OAUTH_STATE, state);
        session.setAttribute(OAUTH_FLOW, flow);
        if (targetUserId != null) {
            session.setAttribute(TARGET_USER_ID, targetUserId);
        }
        return "redirect:" + graphClient.buildAuthorizationUrl(state);
    }

    private Authentication iniciarSesion(Usuario usuario, HttpServletRequest request, HttpServletResponse response) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("El usuario SIAJ se encuentra inactivo.");
        }
        UserDetails userDetails = userDetailsService.loadUserByEmail(usuario.getEmail());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        try {
            auditoriaService.registrarActividadGeneral(usuario, "Inicio de Sesion Microsoft",
                    "El usuario ha ingresado al sistema con su cuenta institucional.", "CLOCK");
            auditoriaTecnicaService.registrarLoginExitoso(usuario, request);
            emitirCookieJwt(authentication, response);
        } catch (Exception ignored) {
            // La auditoria auxiliar no debe bloquear una autenticacion valida.
        }
        return authentication;
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new IllegalArgumentException("Debe iniciar sesion para administrar la cuenta Microsoft.");
        }
        return usuarioRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario autenticado."));
    }

    private void registrarVinculacion(Usuario usuario, HttpServletRequest request, String accion, String descripcion) {
        try {
            auditoriaTecnicaService.registrarEventoSeguridad(usuario, usuario.getEmail(), request,
                    accion, "EXITOSO", 200, descripcion, "USUARIO", String.valueOf(usuario.getId()));
        } catch (Exception ignored) {
            // El cambio principal ya fue persistido; la auditoria no debe revertirlo.
        }
    }

    private void emitirCookieJwt(Authentication authentication, HttpServletResponse response) {
        String jwt = jwtTokenProvider.generateToken(authentication);
        Cookie cookie = new Cookie("JWT_TOKEN", jwt);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpirationMs / 1000));
        response.addCookie(cookie);
    }

    private MicrosoftUserProfile pendingProfile(HttpSession session) {
        Object value = session.getAttribute(PENDING_PROFILE);
        return value instanceof MicrosoftUserProfile profile ? profile : null;
    }

    private Integer pendingUserId(HttpSession session) {
        Object value = session.getAttribute(PENDING_USER_ID);
        return value instanceof Integer id ? id : null;
    }

    private void limpiarPendiente(HttpSession session) {
        session.removeAttribute(PENDING_PROFILE);
        session.removeAttribute(PENDING_USER_ID);
    }

    private void limpiarFlujo(HttpSession session) {
        session.removeAttribute(OAUTH_STATE);
        session.removeAttribute(OAUTH_FLOW);
        session.removeAttribute(TARGET_USER_ID);
    }

    private String redirigirError(String flow, String message, RedirectAttributes redirectAttrs) {
        if (FLOW_PROFILE_LINK.equals(flow)) {
            redirectAttrs.addFlashAttribute("mensaje", message);
            redirectAttrs.addFlashAttribute("tipo", "error");
            redirectAttrs.addFlashAttribute("origenMensaje", "microsoft");
            return "redirect:/perfil?tab=seguridad";
        }
        redirectAttrs.addFlashAttribute("microsoftError", message);
        return FLOW_ACTIVATION_LINK.equals(flow)
                ? "redirect:/auth/activacion-completada"
                : "redirect:/";
    }

    private String mensajeSeguro(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() > 220 ? value.substring(0, 220) + "..." : value;
    }
}
