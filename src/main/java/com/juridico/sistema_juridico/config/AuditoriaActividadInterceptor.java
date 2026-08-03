package com.juridico.sistema_juridico.config;

import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuditoriaActividadInterceptor implements HandlerInterceptor {

    private final AuditoriaTecnicaService auditoriaTecnicaService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (debeAuditar(request)) {
            auditoriaTecnicaService.actualizarUltimaActividad(request);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex == null && debeAuditar(request)) {
            auditoriaTecnicaService.registrarActividadHttp(request, response.getStatus());
        }
    }

    private boolean debeAuditar(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        if ("/".equals(uri) && ("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod()))) {
            return false;
        }
        return !(uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/img/")
                || uri.startsWith("/lib/")
                || uri.startsWith("/webjars/")
                || uri.startsWith("/ws-realtime/")
                || auditoriaTecnicaService.esRutaRuidoOperativo(uri)
                || uri.equals("/manifest.json")
                || uri.equals("/sw.js")
                || uri.equals("/favicon.ico")
                || uri.equals("/error"));
    }
}
