package com.juridico.sistema_juridico.controller;

import java.util.UUID;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.config.MicrosoftAuthProperties;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftTokenResponse;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftUserProfile;
import com.juridico.sistema_juridico.security.CustomUserDetailsService;
import com.juridico.sistema_juridico.security.JwtTokenProvider;
import com.juridico.sistema_juridico.service.AuditoriaService;
import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;
import com.juridico.sistema_juridico.service.MicrosoftGraphClient;
import com.juridico.sistema_juridico.service.MicrosoftIdentityService;
import com.juridico.sistema_juridico.service.PasswordResetService;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MicrosoftAuthController {

    private static final String MICROSOFT_OAUTH_STATE = "MICROSOFT_OAUTH_STATE";
    private static final String MICROSOFT_ACTIVATION_TOKEN = "MICROSOFT_ACTIVATION_TOKEN";

    private final MicrosoftAuthProperties properties;
    private final MicrosoftGraphClient graphClient;
    private final MicrosoftIdentityService identityService;
    private final PasswordResetService passwordResetService;
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
            PasswordResetService passwordResetService,
            CustomUserDetailsService userDetailsService,
            JwtTokenProvider jwtTokenProvider,
            AuditoriaService auditoriaService,
            AuditoriaTecnicaService auditoriaTecnicaService) {
        this.properties = properties;
        this.graphClient = graphClient;
        this.identityService = identityService;
        this.passwordResetService = passwordResetService;
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditoriaService = auditoriaService;
        this.auditoriaTecnicaService = auditoriaTecnicaService;
    }

    @GetMapping("/auth/microsoft/login")
    public String login(HttpSession session, RedirectAttributes redirectAttrs) {
        if (!properties.isConfigured()) {
            redirectAttrs.addFlashAttribute("microsoftError",
                    "El inicio de sesion con Microsoft aun no esta configurado.");
            return "redirect:/";
        }

        String state = UUID.randomUUID().toString();
        session.setAttribute(MICROSOFT_OAUTH_STATE, state);
        return "redirect:" + graphClient.buildAuthorizationUrl(state);
    }

    @GetMapping("/auth/activar/microsoft")
    public String activarConMicrosoft(@RequestParam("token") String activationToken,
            HttpSession session,
            RedirectAttributes redirectAttrs) {
        if (!passwordResetService.isTokenValid(activationToken)) {
            redirectAttrs.addFlashAttribute("error", "El enlace es invalido o ha expirado.");
            return "redirect:/auth/activar?token=" + activationToken;
        }
        if (!properties.isConfigured()) {
            return "redirect:/auth/activar?token=" + activationToken
                    + "&microsoftError=" + safeRedirectMessage("Microsoft no esta configurado.");
        }

        String state = UUID.randomUUID().toString();
        session.setAttribute(MICROSOFT_OAUTH_STATE, state);
        session.setAttribute(MICROSOFT_ACTIVATION_TOKEN, activationToken);
        return "redirect:" + graphClient.buildAuthorizationUrl(state);
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

        if (error != null) {
            String activationToken = (String) session.getAttribute(MICROSOFT_ACTIVATION_TOKEN);
            if (activationToken != null) {
                return "redirect:/auth/activar?token=" + activationToken
                        + "&microsoftError=" + safeRedirectMessage("Microsoft no completo la autorizacion.");
            }
            redirectAttrs.addFlashAttribute("microsoftError",
                    "Microsoft no completo el inicio de sesion: " + safeMessage(errorDescription, error));
            return "redirect:/";
        }

        String expectedState = (String) session.getAttribute(MICROSOFT_OAUTH_STATE);
        session.removeAttribute(MICROSOFT_OAUTH_STATE);
        if (code == null || code.isBlank() || expectedState == null || !expectedState.equals(state)) {
            redirectAttrs.addFlashAttribute("microsoftError",
                    "No fue posible validar la respuesta de Microsoft. Intenta nuevamente.");
            return "redirect:/";
        }

        try {
            MicrosoftTokenResponse token = graphClient.exchangeCode(code);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                redirectAttrs.addFlashAttribute("microsoftError",
                        "Microsoft no regreso un token de acceso valido.");
                return "redirect:/";
            }

            MicrosoftUserProfile profile = graphClient.fetchMe(token.accessToken());
            String activationToken = (String) session.getAttribute(MICROSOFT_ACTIVATION_TOKEN);
            if (activationToken != null) {
                return completarActivacionMicrosoft(activationToken, profile, request, session);
            }

            Usuario usuario = identityService.vincularYObtenerUsuarioLocal(profile);
            Authentication authentication = autenticarUsuarioSiaj(usuario, request, response);

            try {
                auditoriaService.registrarActividadGeneral(usuario, "Inicio de Sesion Microsoft",
                        "El usuario ha ingresado al sistema con su cuenta institucional.", "CLOCK");
                auditoriaTecnicaService.registrarLoginExitoso(usuario, request);
                emitirCookieJwt(authentication, response);
            } catch (Exception auditOrJwtError) {
                // El inicio de sesion no debe bloquearse por auditoria o JWT auxiliar.
            }

            return "redirect:/dashboard";
        } catch (Exception ex) {
            String activationToken = (String) session.getAttribute(MICROSOFT_ACTIVATION_TOKEN);
            if (activationToken != null) {
                return "redirect:/auth/activar?token=" + activationToken
                        + "&microsoftError=" + safeRedirectMessage(ex.getMessage());
            }
            auditoriaTecnicaService.registrarLoginFallido("microsoft", request, ex.getMessage());
            redirectAttrs.addFlashAttribute("microsoftError", safeMessage(ex.getMessage(),
                    "No fue posible iniciar sesion con Microsoft."));
            return "redirect:/";
        }
    }

    private String completarActivacionMicrosoft(String activationToken, MicrosoftUserProfile profile,
            HttpServletRequest request, HttpSession session) {
        session.removeAttribute(MICROSOFT_ACTIVATION_TOKEN);
        Usuario usuario = passwordResetService.findUserForValidToken(activationToken)
                .orElseThrow(() -> new IllegalArgumentException("El enlace es invalido o ha expirado."));
        Usuario vinculado = identityService.vincularPerfilAUsuario(usuario, profile);
        auditoriaTecnicaService.registrarEventoSeguridad(vinculado, vinculado.getEmail(), request,
                "CUENTA_ACTIVACION_MICROSOFT_VINCULADA", "EXITOSO", 200,
                "El usuario valido su correo institucional con Microsoft durante la activacion.",
                "USUARIO", String.valueOf(vinculado.getId()));
        return "redirect:/auth/reset-password?token=" + activationToken;
    }

    private Authentication autenticarUsuarioSiaj(Usuario usuario, HttpServletRequest request, HttpServletResponse response) {
        UserDetails userDetails = userDetailsService.loadUserByEmail(usuario.getEmail());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);
        return authentication;
    }

    private void emitirCookieJwt(Authentication authentication, HttpServletResponse response) {
        String jwt = jwtTokenProvider.generateToken(authentication);
        Cookie jwtCookie = new Cookie("JWT_TOKEN", jwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge((int) (jwtExpirationMs / 1000));
        response.addCookie(jwtCookie);
    }

    private String safeMessage(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() > 220 ? value.substring(0, 220) + "..." : value;
    }

    private String safeRedirectMessage(String value) {
        String message = safeMessage(value, "No fue posible vincular la cuenta Microsoft.");
        return java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
    }
}
