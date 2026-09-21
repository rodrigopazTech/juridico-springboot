package com.juridico.sistema_juridico.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice; // <--- OJO: No @RestControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditoriaTecnicaService auditoriaTecnicaService;

    public GlobalExceptionHandler(AuditoriaTecnicaService auditoriaTecnicaService) {
        this.auditoriaTecnicaService = auditoriaTecnicaService;
    }

    // 0. IGNORAR RECURSOS ESTÁTICOS NO ENCONTRADOS (Evita popups por favicon.ico, etc.)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // 1. RECURSO NO ENCONTRADO (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, RedirectAttributes attrs, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.NOT_FOUND, ex.getMessage());
        }

        attrs.addFlashAttribute("mensaje", ex.getMessage());
        attrs.addFlashAttribute("tipo", "error"); // Alerta Roja
        return new RedirectView(getRefererOrHome(request));
    }

    // 2. PERMISOS (403)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object handleSpringAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        auditoriaTecnicaService.registrarAccesoDenegado(request, ex.getMessage());
        if (isApiRequest(request) || isJsonRequest(request)) {
            return jsonError(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para realizar esta acción.");
    }

    @ExceptionHandler({ForbiddenException.class, UnauthorizedException.class})
    public Object handleSecurityErrors(RuntimeException ex, RedirectAttributes attrs, HttpServletRequest request) {
        auditoriaTecnicaService.registrarAccesoDenegado(request, ex.getMessage());
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
        }

        attrs.addFlashAttribute("mensaje", "No tienes permiso para realizar esta acción.");
        attrs.addFlashAttribute("tipo", "warning"); // Alerta Amarilla
        return new RedirectView(getRefererOrHome(request));
    }

    // 3. ERRORES DE LÓGICA (400)
    @ExceptionHandler({BadRequestException.class, ConflictException.class})
    public Object handleLogicErrors(RuntimeException ex, RedirectAttributes attrs, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        attrs.addFlashAttribute("mensaje", ex.getMessage());
        attrs.addFlashAttribute("tipo", "info"); // Alerta Azul
        return new RedirectView(getRefererOrHome(request));
    }

    // 4. ERROR DE BASE DE DATOS (Integridad / FK)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDatabaseError(Exception ex, RedirectAttributes attrs, HttpServletRequest request) {
        auditoriaTecnicaService.registrarError(request, ex);
        LOGGER.error("No fue posible guardar cambios por una relación de datos", ex);

        if (isApiRequest(request)) {
            return jsonError(HttpStatus.CONFLICT,
                    "No pudimos guardar los cambios porque el registro está relacionado con otros datos o ya existe.");
        }

        attrs.addFlashAttribute("mensaje", "No pudimos guardar los cambios porque el registro está relacionado con otros datos o ya existe.");
        attrs.addFlashAttribute("tipo", "error");
        return new RedirectView(getRefererOrHome(request));
    }

    // 5. ERROR GENERAL (Cualquier otra cosa)
    @ExceptionHandler(Exception.class)
    public Object handleGeneralError(Exception ex, HttpServletRequest request) {
        registrarErrorDeFormaSegura(request, ex);
        LOGGER.error("Error inesperado procesando {} {}", request.getMethod(), request.getRequestURI(), ex);

        if (isApiRequest(request)) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "No pudimos completar la acción. Inténtalo de nuevo más tarde.");
        }

        ModelAndView errorPage = new ModelAndView("error/500");
        errorPage.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorPage.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorPage.addObject("error", "No pudimos completar la acción");
        errorPage.addObject("message", "Inténtalo de nuevo más tarde. Si el problema continúa, contacta a la administración.");
        errorPage.addObject("path", request.getRequestURI());
        return errorPage;
    }

    // 6. TAMAÑO DE ARCHIVO EXCEDIDO (1GB)
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public Object handleMaxUploadSizeExceeded(org.springframework.web.multipart.MaxUploadSizeExceededException ex, RedirectAttributes attrs, HttpServletRequest request) {
        String mensaje = "El archivo excede el tamaño máximo permitido de 1GB.";
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.BAD_REQUEST, mensaje);
        }

        attrs.addFlashAttribute("mensaje", mensaje);
        attrs.addFlashAttribute("tipo", "error");
        return new RedirectView(getRefererOrHome(request));
    }

    private String getRefererOrHome(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && (referer.contains("/notificaciones/leer") || referer.contains("/leer/"))) {
            return "/notificaciones";
        }
        return referer != null ? referer : "/";
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        return (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }

    private ResponseEntity<Map<String, String>> jsonError(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }

    private void registrarErrorDeFormaSegura(HttpServletRequest request, Exception ex) {
        try {
            auditoriaTecnicaService.registrarError(request, ex);
        } catch (Exception auditError) {
            LOGGER.warn("No fue posible registrar el error en la auditoria tecnica", auditError);
        }
    }
}
