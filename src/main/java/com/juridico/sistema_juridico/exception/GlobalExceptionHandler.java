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

@ControllerAdvice
public class GlobalExceptionHandler {

    // 0. IGNORAR RECURSOS ESTÁTICOS NO ENCONTRADOS (Evita popups por favicon.ico, etc.)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // 1. RECURSO NO ENCONTRADO (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public RedirectView handleResourceNotFound(ResourceNotFoundException ex, RedirectAttributes attrs, HttpServletRequest request) {
        attrs.addFlashAttribute("mensaje", ex.getMessage());
        attrs.addFlashAttribute("tipo", "error"); // Alerta Roja
        return new RedirectView(getRefererOrHome(request));
    }

    // 2. PERMISOS (403)
    @ExceptionHandler({ForbiddenException.class, UnauthorizedException.class})
    public RedirectView handleSecurityErrors(RuntimeException ex, RedirectAttributes attrs, HttpServletRequest request) {
        attrs.addFlashAttribute("mensaje", "Acceso Denegado: " + ex.getMessage());
        attrs.addFlashAttribute("tipo", "warning"); // Alerta Amarilla
        return new RedirectView(getRefererOrHome(request));
    }

    // 3. ERRORES DE LÓGICA (400)
    @ExceptionHandler({BadRequestException.class, ConflictException.class})
    public RedirectView handleLogicErrors(RuntimeException ex, RedirectAttributes attrs, HttpServletRequest request) {
        attrs.addFlashAttribute("mensaje", ex.getMessage());
        attrs.addFlashAttribute("tipo", "info"); // Alerta Azul
        return new RedirectView(getRefererOrHome(request));
    }

    // 4. ERROR DE BASE DE DATOS (Integridad / FK)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public RedirectView handleDatabaseError(Exception ex, RedirectAttributes attrs, HttpServletRequest request) {
        attrs.addFlashAttribute("mensaje", "No se puede realizar la acción: El registro está vinculado a otros datos o ya existe.");
        attrs.addFlashAttribute("tipo", "error");
        return new RedirectView(getRefererOrHome(request));
    }

    // 5. ERROR GENERAL (Cualquier otra cosa)
    @ExceptionHandler(Exception.class)
    public RedirectView handleGeneralError(Exception ex, RedirectAttributes attrs, HttpServletRequest request) {
        // Imprimimos el error real en la consola
        System.err.println("--- ERROR CAPTURADO POR GLOBAL HANDLER ---");
        System.err.println("Mensaje: " + ex.getMessage());
        ex.printStackTrace();
        
        attrs.addFlashAttribute("mensaje", "Ocurrió un error inesperado en el sistema.");
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
}