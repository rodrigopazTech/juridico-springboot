package com.juridico.sistema_juridico.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice; // <--- OJO: No @RestControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;

@ControllerAdvice
public class GlobalExceptionHandler {

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
            return jsonError(HttpStatus.FORBIDDEN, "Acceso Denegado: " + ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso Denegado: " + ex.getMessage());
    }

    @ExceptionHandler({ForbiddenException.class, UnauthorizedException.class})
    public Object handleSecurityErrors(RuntimeException ex, RedirectAttributes attrs, HttpServletRequest request) {
        auditoriaTecnicaService.registrarAccesoDenegado(request, ex.getMessage());
        if (isApiRequest(request)) {
            return jsonError(HttpStatus.FORBIDDEN, "Acceso Denegado: " + ex.getMessage());
        }

        attrs.addFlashAttribute("mensaje", "Acceso Denegado: " + ex.getMessage());
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
        System.err.println("--- ERROR DE INTEGRIDAD DE DATOS CAPTURADO POR GLOBAL HANDLER ---");
        System.err.println("Mensaje: " + ex.getMessage());
        ex.printStackTrace();

        if (isApiRequest(request)) {
            return jsonError(HttpStatus.CONFLICT,
                    "No se puede realizar la accion: El registro esta vinculado a otros datos o ya existe.");
        }

        attrs.addFlashAttribute("mensaje", "No se puede realizar la acción: El registro está vinculado a otros datos o ya existe.");
        attrs.addFlashAttribute("tipo", "error");
        return new RedirectView(getRefererOrHome(request));
    }

    // 5. ERROR GENERAL (Cualquier otra cosa)
    @ExceptionHandler(Exception.class)
    public Object handleGeneralError(Exception ex, RedirectAttributes attrs, HttpServletRequest request) {
        auditoriaTecnicaService.registrarError(request, ex);
        // Imprimimos el error real en la consola
        System.err.println("--- ERROR CAPTURADO POR GLOBAL HANDLER ---");
        System.err.println("Mensaje: " + ex.getMessage());
        ex.printStackTrace();

        if (isApiRequest(request)) {
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado en el sistema.");
        }
        
        attrs.addFlashAttribute("mensaje", "Ocurrió un error inesperado en el sistema.");
        attrs.addFlashAttribute("tipo", "error");
        
        return new RedirectView(getRefererOrHome(request));
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
}
