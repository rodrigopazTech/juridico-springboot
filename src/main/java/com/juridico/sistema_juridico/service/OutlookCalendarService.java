package com.juridico.sistema_juridico.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.juridico.sistema_juridico.Entity.enums.OutlookSyncStatus;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftTokenResponse;
import com.juridico.sistema_juridico.dto.microsoft.MicrosoftUserProfile;
import com.juridico.sistema_juridico.dto.microsoft.OutlookEventResponse;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutlookCalendarService {

    private static final DateTimeFormatter GRAPH_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String TIME_ZONE = "America/Mexico_City";

    private final MicrosoftGraphClient graphClient;
    private final OutlookTokenCipher tokenCipher;
    private final UsuarioRepository usuarioRepository;
    private final AudienciaRepository audienciaRepository;
    private final TerminoRepository terminoRepository;
    private final RecordatorioRepository recordatorioRepository;

    public OutlookCalendarService(
            MicrosoftGraphClient graphClient,
            OutlookTokenCipher tokenCipher,
            UsuarioRepository usuarioRepository,
            AudienciaRepository audienciaRepository,
            TerminoRepository terminoRepository,
            RecordatorioRepository recordatorioRepository) {
        this.graphClient = graphClient;
        this.tokenCipher = tokenCipher;
        this.usuarioRepository = usuarioRepository;
        this.audienciaRepository = audienciaRepository;
        this.terminoRepository = terminoRepository;
        this.recordatorioRepository = recordatorioRepository;
    }

    @Transactional
    public Usuario conectarCalendario(Usuario usuario, MicrosoftTokenResponse token, MicrosoftUserProfile profile) {
        if (token == null || token.refreshToken() == null || token.refreshToken().isBlank()) {
            throw new IllegalStateException("Microsoft no regreso refresh token. Revisa que offline_access este autorizado.");
        }

        usuario.setOutlookCalendarLinked(true);
        usuario.setOutlookAccountEmail(profile != null ? profile.preferredEmail() : usuario.getEmail());
        usuario.setOutlookRefreshTokenEncrypted(tokenCipher.encrypt(token.refreshToken()));
        usuario.setOutlookConnectedAt(LocalDateTime.now());
        usuario.setOutlookLastSyncAt(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void desconectarCalendario(Usuario usuario) {
        usuario.setOutlookCalendarLinked(false);
        usuario.setOutlookRefreshTokenEncrypted(null);
        usuario.setOutlookLastSyncAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void crearOActualizarEventoAudiencia(Usuario usuario, Audiencia audiencia) {
        validarUsuarioConCalendario(usuario);
        try {
            String accessToken = accessToken(usuario);
            OutlookEventResponse response;
            if (Boolean.TRUE.equals(audiencia.getOutlookLinked()) && hasText(audiencia.getOutlookEventId())) {
                response = graphClient.updateCalendarEvent(accessToken, audiencia.getOutlookEventId(), eventPayload(audiencia));
            } else {
                response = graphClient.createCalendarEvent(accessToken, eventPayload(audiencia));
            }
            audiencia.setOutlookLinked(true);
            audiencia.setOutlookEventId(response != null ? response.id() : audiencia.getOutlookEventId());
            audiencia.setOutlookWebLink(response != null ? response.webLink() : audiencia.getOutlookWebLink());
            audiencia.setOutlookCalendarId("primary");
            audiencia.setOutlookSyncedAt(LocalDateTime.now());
            audiencia.setOutlookSyncStatus(OutlookSyncStatus.VINCULADO);
            audiencia.setOutlookSyncError(null);
        } catch (Exception ex) {
            audiencia.setOutlookSyncStatus(OutlookSyncStatus.ERROR_SYNC);
            audiencia.setOutlookSyncError(limit(ex.getMessage()));
            throw ex;
        } finally {
            audienciaRepository.save(audiencia);
        }
    }

    @Transactional
    public void eliminarEventoAudiencia(Usuario usuario, Audiencia audiencia) {
        validarUsuarioConCalendario(usuario);
        if (!Boolean.TRUE.equals(audiencia.getOutlookLinked()) || !hasText(audiencia.getOutlookEventId())) {
            return;
        }
        try {
            graphClient.deleteCalendarEvent(accessToken(usuario), audiencia.getOutlookEventId());
            audiencia.setOutlookLinked(false);
            audiencia.setOutlookSyncStatus(OutlookSyncStatus.DESVINCULADO);
            audiencia.setOutlookSyncedAt(LocalDateTime.now());
            audiencia.setOutlookSyncError(null);
        } catch (Exception ex) {
            audiencia.setOutlookSyncStatus(OutlookSyncStatus.ERROR_SYNC);
            audiencia.setOutlookSyncError(limit(ex.getMessage()));
            throw ex;
        } finally {
            audienciaRepository.save(audiencia);
        }
    }

    @Transactional
    public void crearOActualizarEventoTermino(Usuario usuario, Termino termino) {
        validarUsuarioConCalendario(usuario);
        try {
            String accessToken = accessToken(usuario);
            OutlookEventResponse response;
            if (Boolean.TRUE.equals(termino.getOutlookLinked()) && hasText(termino.getOutlookEventId())) {
                response = graphClient.updateCalendarEvent(accessToken, termino.getOutlookEventId(), terminoPayload(termino));
            } else {
                response = graphClient.createCalendarEvent(accessToken, terminoPayload(termino));
            }
            termino.setOutlookLinked(true);
            termino.setOutlookEventId(response != null ? response.id() : termino.getOutlookEventId());
            termino.setOutlookWebLink(response != null ? response.webLink() : termino.getOutlookWebLink());
            termino.setOutlookCalendarId("primary");
            termino.setOutlookSyncedAt(LocalDateTime.now());
            termino.setOutlookSyncStatus(OutlookSyncStatus.VINCULADO);
            termino.setOutlookSyncError(null);
        } catch (Exception ex) {
            termino.setOutlookSyncStatus(OutlookSyncStatus.ERROR_SYNC);
            termino.setOutlookSyncError(limit(ex.getMessage()));
            throw ex;
        } finally {
            terminoRepository.save(termino);
        }
    }

    @Transactional
    public void eliminarEventoTermino(Usuario usuario, Termino termino) {
        validarUsuarioConCalendario(usuario);
        if (!Boolean.TRUE.equals(termino.getOutlookLinked()) || !hasText(termino.getOutlookEventId())) {
            return;
        }
        try {
            graphClient.deleteCalendarEvent(accessToken(usuario), termino.getOutlookEventId());
            termino.setOutlookLinked(false);
            termino.setOutlookSyncStatus(OutlookSyncStatus.DESVINCULADO);
            termino.setOutlookSyncedAt(LocalDateTime.now());
            termino.setOutlookSyncError(null);
        } catch (Exception ex) {
            termino.setOutlookSyncStatus(OutlookSyncStatus.ERROR_SYNC);
            termino.setOutlookSyncError(limit(ex.getMessage()));
            throw ex;
        } finally {
            terminoRepository.save(termino);
        }
    }

    @Transactional
    public void crearOActualizarEventoRecordatorio(Usuario usuario, Recordatorio recordatorio) {
        validarUsuarioConCalendario(usuario);
        try {
            String accessToken = accessToken(usuario);
            OutlookEventResponse response;
            if (Boolean.TRUE.equals(recordatorio.getOutlookLinked()) && hasText(recordatorio.getOutlookEventId())) {
                response = graphClient.updateCalendarEvent(accessToken, recordatorio.getOutlookEventId(), recordatorioPayload(recordatorio));
            } else {
                response = graphClient.createCalendarEvent(accessToken, recordatorioPayload(recordatorio));
            }
            recordatorio.setOutlookLinked(true);
            recordatorio.setOutlookEventId(response != null ? response.id() : recordatorio.getOutlookEventId());
            recordatorio.setOutlookWebLink(response != null ? response.webLink() : recordatorio.getOutlookWebLink());
            recordatorio.setOutlookCalendarId("primary");
            recordatorio.setOutlookSyncedAt(LocalDateTime.now());
            recordatorio.setOutlookSyncStatus(OutlookSyncStatus.VINCULADO);
            recordatorio.setOutlookSyncError(null);
        } catch (Exception ex) {
            recordatorio.setOutlookSyncStatus(OutlookSyncStatus.ERROR_SYNC);
            recordatorio.setOutlookSyncError(limit(ex.getMessage()));
            throw ex;
        } finally {
            recordatorioRepository.save(recordatorio);
        }
    }

    @Transactional
    public void eliminarEventoRecordatorio(Usuario usuario, Recordatorio recordatorio) {
        validarUsuarioConCalendario(usuario);
        if (!Boolean.TRUE.equals(recordatorio.getOutlookLinked()) || !hasText(recordatorio.getOutlookEventId())) {
            return;
        }
        try {
            graphClient.deleteCalendarEvent(accessToken(usuario), recordatorio.getOutlookEventId());
            recordatorio.setOutlookLinked(false);
            recordatorio.setOutlookSyncStatus(OutlookSyncStatus.DESVINCULADO);
            recordatorio.setOutlookSyncedAt(LocalDateTime.now());
            recordatorio.setOutlookSyncError(null);
        } catch (Exception ex) {
            recordatorio.setOutlookSyncStatus(OutlookSyncStatus.ERROR_SYNC);
            recordatorio.setOutlookSyncError(limit(ex.getMessage()));
            throw ex;
        } finally {
            recordatorioRepository.save(recordatorio);
        }
    }

    public boolean tieneCalendarioVinculado(Usuario usuario) {
        return usuario != null
                && Boolean.TRUE.equals(usuario.getOutlookCalendarLinked())
                && hasText(usuario.getOutlookRefreshTokenEncrypted());
    }

    private String accessToken(Usuario usuario) {
        String refreshToken = tokenCipher.decrypt(usuario.getOutlookRefreshTokenEncrypted());
        MicrosoftTokenResponse refreshed = graphClient.refreshCalendarAccessToken(refreshToken);
        if (refreshed == null || !hasText(refreshed.accessToken())) {
            throw new IllegalStateException("No fue posible renovar el acceso al calendario Outlook.");
        }
        if (hasText(refreshed.refreshToken())) {
            usuario.setOutlookRefreshTokenEncrypted(tokenCipher.encrypt(refreshed.refreshToken()));
        }
        usuario.setOutlookLastSyncAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
        return refreshed.accessToken();
    }

    private void validarUsuarioConCalendario(Usuario usuario) {
        if (!tieneCalendarioVinculado(usuario)) {
            throw new IllegalStateException("El usuario no tiene calendario Outlook vinculado.");
        }
    }

    private Map<String, Object> eventPayload(Audiencia audiencia) {
        LocalDateTime start = audiencia.getFechaAudiencia().atTime(audiencia.getHoraAudiencia());
        LocalDateTime end = start.plusHours(1);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subject", "SIAJ - " + safe(audiencia.getTipoAudiencia() != null
                ? audiencia.getTipoAudiencia().getNombre()
                : "Audiencia"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contentType", "HTML");
        body.put("content", buildBody(audiencia));
        payload.put("body", body);
        payload.put("start", dateTime(start));
        payload.put("end", dateTime(end));
        payload.put("location", Map.of("displayName", location(audiencia)));
        payload.put("showAs", "busy");
        payload.put("categories", java.util.List.of("SIAJ"));
        return payload;
    }

    private Map<String, Object> terminoPayload(Termino termino) {
        LocalDateTime start = termino.getFechaVencimiento().atStartOfDay();
        LocalDateTime end = termino.getFechaVencimiento().plusDays(1).atStartOfDay();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subject", "SIAJ - Vencimiento: " + safe(termino.getAsunto()));
        payload.put("isAllDay", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contentType", "HTML");
        body.put("content", buildBody(termino));
        payload.put("body", body);
        payload.put("start", dateTime(start));
        payload.put("end", dateTime(end));
        payload.put("location", Map.of("displayName", "SIAJ"));
        payload.put("showAs", "free");
        payload.put("categories", java.util.List.of("SIAJ", "Termino"));
        return payload;
    }

    private Map<String, Object> recordatorioPayload(Recordatorio recordatorio) {
        LocalDateTime start = recordatorio.getFechaRecordatorio().atTime(recordatorio.getHoraRecordatorio());
        LocalDateTime end = start.plusMinutes(30);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subject", "SIAJ - " + safe(recordatorio.getTitulo()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contentType", "HTML");
        body.put("content", buildBody(recordatorio));
        payload.put("body", body);
        payload.put("start", dateTime(start));
        payload.put("end", dateTime(end));
        payload.put("location", Map.of("displayName", "SIAJ"));
        payload.put("showAs", "busy");
        payload.put("categories", java.util.List.of("SIAJ", "Recordatorio"));
        return payload;
    }

    private Map<String, Object> dateTime(LocalDateTime value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dateTime", value.format(GRAPH_DATE_TIME));
        map.put("timeZone", TIME_ZONE);
        return map;
    }

    private String buildBody(Audiencia audiencia) {
        StringBuilder body = new StringBuilder();
        body.append("<b>Evento generado desde SIAJ</b><br>");
        body.append("Referencia: ").append(safe(audiencia.getNumeroReferencia())).append("<br>");
        body.append("Partes: ").append(safe(audiencia.getPartesReferencia())).append("<br>");
        if (hasText(audiencia.getDescripcionReferencia())) {
            body.append("Descripcion: ").append(safe(audiencia.getDescripcionReferencia())).append("<br>");
        }
        if (hasText(audiencia.getObservaciones())) {
            body.append("Observaciones: ").append(safe(audiencia.getObservaciones())).append("<br>");
        }
        if (Boolean.TRUE.equals(audiencia.getEsVirtual()) && hasText(audiencia.getUrlReunion())) {
            body.append("Liga: ").append(safe(audiencia.getUrlReunion())).append("<br>");
        }
        return body.toString();
    }

    private String buildBody(Termino termino) {
        StringBuilder body = new StringBuilder();
        body.append("<b>Termino generado desde SIAJ</b><br>");
        body.append("Referencia: ").append(safe(termino.getNumeroReferencia())).append("<br>");
        body.append("Partes: ").append(safe(termino.getPartesReferencia())).append("<br>");
        if (hasText(termino.getDescripcionReferencia())) {
            body.append("Descripcion: ").append(safe(termino.getDescripcionReferencia())).append("<br>");
        }
        if (hasText(termino.getObservaciones())) {
            body.append("Observaciones: ").append(safe(termino.getObservaciones())).append("<br>");
        }
        return body.toString();
    }

    private String buildBody(Recordatorio recordatorio) {
        StringBuilder body = new StringBuilder();
        body.append("<b>Recordatorio generado desde SIAJ</b><br>");
        if (hasText(recordatorio.getDetalles())) {
            body.append("Detalles: ").append(safe(recordatorio.getDetalles())).append("<br>");
        }
        if (recordatorio.getPrioridad() != null) {
            body.append("Prioridad: ").append(safe(recordatorio.getPrioridad().getNombre())).append("<br>");
        }
        return body.toString();
    }

    private String location(Audiencia audiencia) {
        if (Boolean.TRUE.equals(audiencia.getEsVirtual())) {
            return hasText(audiencia.getUrlReunion()) ? audiencia.getUrlReunion() : "Virtual";
        }
        return hasText(audiencia.getSalaLugar()) ? audiencia.getSalaLugar() : "Presencial";
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
