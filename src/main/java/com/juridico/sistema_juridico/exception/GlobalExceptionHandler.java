package com.juridico.sistema_juridico.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice; // ¡Ojo! ControllerAdvice, no RestControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. ERROR 404: RECURSO NO ENCONTRADO
    // Ejemplo: Intentan eliminar una audiencia que ya no existe o ponen un ID falso en la URL.
    @ExceptionHandler(ResourceNotFoundException.class)
    public RedirectView handleResourceNotFound(ResourceNotFoundException ex, RedirectAttributes attrs, HttpServletRequest request) {
        attrs.addFlashAttribute("mensaje", ex.getMessage());
        attrs.addFlashAttribute("tipo", "error"); // Rojo en SweetAlert
        
        return new RedirectView(getRefererOrHome(request));
    }

    // 2. ERROR 403/401: PERMISOS Y SEGURIDAD
    // Ejemplo: Un abogado intenta borrar un expediente de otra gerencia.
    @ExceptionHandler({ForbiddenException.class, UnauthorizedException.class})
    public RedirectView handleSecurityErrors(RuntimeException ex, RedirectAttributes attrs, HttpServletRequest request) {
        attrs.addFlashAttribute("mensaje", "Acceso Denegado: " + ex.getMessage());
        attrs.addFlashAttribute("tipo", "warning"); // Amarillo en SweetAlert
        
        return new RedirectView(getRefererOrHome(request));
    }

    // 3. ERROR 400/409: LÓGICA DE NEGOCIO
    // Ejemplo: Intentar crear un usuario con un email repetido.
    @ExceptionHandler({BadRequestException.class, ConflictException.class})
    public RedirectView handleLogicErrors(RuntimeException ex, RedirectAttributes attrs, HttpServletRequest request) {
        attrs.addFlashAttribute("mensaje", ex.getMessage());
        attrs.addFlashAttribute("tipo", "info"); // Azul en SweetAlert
        
        return new RedirectView(getRefererOrHome(request));
    }

    // 4. ERROR 500: CUALQUIER COSA IMPREVISTA
    // NullPointer, Base de datos caída, errores de sintaxis Java, etc.
    @ExceptionHandler(Exception.class)
    public RedirectView handleGeneralError(Exception ex, RedirectAttributes attrs, HttpServletRequest request) {
        // Imprimimos el error en la consola del servidor para que tú (el dev) sepas qué pasó
        System.err.println("--- ERROR NO CONTROLADO ---");
        ex.printStackTrace();
        
        attrs.addFlashAttribute("mensaje", "Ocurrió un error inesperado en el sistema. Contacte a soporte técnico.");
        attrs.addFlashAttribute("tipo", "error");
        
        return new RedirectView(getRefererOrHome(request));
    }

    // Método auxiliar para devolver al usuario a la página anterior
    private String getRefererOrHome(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return referer != null ? referer : "/";
    }
}