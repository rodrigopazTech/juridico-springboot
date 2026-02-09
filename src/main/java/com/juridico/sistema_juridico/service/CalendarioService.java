package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.dto.response.calendario.EventoResponse;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalendarioService {

    @Autowired
    private AudienciaRepository audienciaRepository;

    @Autowired
    private TerminoRepository terminoRepository;

    @Autowired
    private RecordatorioRepository recordatorioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GerenciaRepository gerenciaRepository;

    public List<EventoResponse> obtenerEventosCalendario(Usuario usuarioActual, String filtroTipo, Long filtroGerenciaId, Long filtroUsuarioId) {
        List<EventoResponse> eventos = new ArrayList<>();
        
        // Para GERENTE: ver solo su gerencia
        boolean verSoloSuGerencia = usuarioActual != null && 
            usuarioActual.getRol() == RolUsuario.GERENTE;

        // 1. Mapear Audiencias
        List<Audiencia> audiencias = audienciaRepository.findAll();
        
        // Filtrar por gerencia si es gerente
        if (verSoloSuGerencia && usuarioActual.getGerencia() != null && usuarioActual.getGerencia().getId() != null) {
            final Integer gerenciaIdInteger = usuarioActual.getGerencia().getId();
            
            audiencias = audiencias.stream()
                .filter(a -> a.getExpediente() != null && 
                            a.getExpediente().getGerencia() != null &&
                            a.getExpediente().getGerencia().getId() != null &&
                            a.getExpediente().getGerencia().getId().equals(gerenciaIdInteger))
                .collect(Collectors.toList());
        }
        
        eventos.addAll(audiencias.stream()
                .<EventoResponse>map(a -> mapToAudienciaResponse(a))
                .collect(Collectors.toList()));

        // 2. Mapear Términos
        List<Termino> terminos = terminoRepository.findAll();
        
        // Filtrar por gerencia si es gerente
        if (verSoloSuGerencia && usuarioActual.getGerencia() != null && usuarioActual.getGerencia().getId() != null) {
            final Integer gerenciaIdInteger = usuarioActual.getGerencia().getId();
            
            terminos = terminos.stream()
                .filter(t -> t.getExpediente() != null && 
                             t.getExpediente().getGerencia() != null &&
                             t.getExpediente().getGerencia().getId() != null &&
                             t.getExpediente().getGerencia().getId().equals(gerenciaIdInteger))
                .collect(Collectors.toList());
        }
        
        eventos.addAll(terminos.stream()
                .<EventoResponse>map(t -> mapToTerminoResponse(t))
                .collect(Collectors.toList()));

        // 3. Mapear Recordatorios (solo del usuario actual)
        if (usuarioActual != null && usuarioActual.getId() != null) {
            List<Recordatorio> recordatorios = recordatorioRepository
                .findByUsuarioIdAndCompletadoFalse(usuarioActual.getId());
            eventos.addAll(recordatorios.stream()
                    .<EventoResponse>map(r -> mapToRecordatorioResponse(r))
                    .collect(Collectors.toList()));
        }

        // Aplicar filtros adicionales
        return aplicarFiltros(eventos, filtroTipo, filtroGerenciaId, filtroUsuarioId, usuarioActual);
    }

    private List<EventoResponse> aplicarFiltros(List<EventoResponse> eventos, String filtroTipo, 
            Long filtroGerenciaId, Long filtroUsuarioId, Usuario usuarioActual) {
        
        return eventos.stream()
            .filter(e -> {
                // Filtro por tipo
                if (filtroTipo != null && !filtroTipo.isEmpty() && !"todos".equals(filtroTipo)) {
                    if (!e.getTipo().equals(filtroTipo)) {
                        return false;
                    }
                }
                // Filtro por gerencia
                if (filtroGerenciaId != null && filtroGerenciaId > 0) {
                    if (e.getGerenciaId() == null || !e.getGerenciaId().equals(filtroGerenciaId)) {
                        return false;
                    }
                }
                // Filtro por usuario
                if (filtroUsuarioId != null && filtroUsuarioId > 0) {
                    if (e.getUsuarioId() == null || !e.getUsuarioId().equals(filtroUsuarioId)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private EventoResponse mapToAudienciaResponse(Audiencia a) {
        Long gerenciaId = null;
        String gerenciaNombre = "";
        Long usuarioId = null;
        String usuarioNombre = "";
        
        if (a.getExpediente() != null) {
            if (a.getExpediente().getGerencia() != null) {
                gerenciaId = a.getExpediente().getGerencia().getId().longValue();
                gerenciaNombre = a.getExpediente().getGerencia().getNombre();
            }
            // Obtener abogado responsable del expediente
            if (a.getExpediente().getAbogadoResponsable() != null) {
                usuarioId = a.getExpediente().getAbogadoResponsable().getId().longValue();
                usuarioNombre = a.getExpediente().getAbogadoResponsable().getNombreCompleto();
            } else if (a.getExpediente().getAbogadoResponsableNombre() != null) {
                // Usar el nombre almacenado directamente si no hay relación
                usuarioNombre = a.getExpediente().getAbogadoResponsableNombre();
            }
        }
        
        return EventoResponse.builder()
                .id(a.getId() != null ? a.getId().longValue() : null)
                .titulo("Audiencia: " + (a.getExpediente() != null ? a.getExpediente().getNumero() : "S/N"))
                .tipo("audiencia")
                .fecha(a.getFechaAudiencia() != null ? a.getFechaAudiencia().toString() : "")
                .hora(a.getHoraAudiencia() != null ? a.getHoraAudiencia().toString() : "")
                .gerenciaId(gerenciaId)
                .gerenciaNombre(gerenciaNombre)
                .usuarioId(usuarioId)
                .usuarioNombre(usuarioNombre)
                .expediente(a.getExpediente() != null ? a.getExpediente().getNumero() : "")
                .build();
    }

    private EventoResponse mapToTerminoResponse(Termino t) {
        Long gerenciaId = null;
        String gerenciaNombre = "";
        Long usuarioId = null;
        String usuarioNombre = "";
        
        if (t.getExpediente() != null) {
            if (t.getExpediente().getGerencia() != null) {
                gerenciaId = t.getExpediente().getGerencia().getId().longValue();
                gerenciaNombre = t.getExpediente().getGerencia().getNombre();
            }
            if (t.getAbogadoResponsable() != null) {
                usuarioId = t.getAbogadoResponsable().getId().longValue();
                usuarioNombre = t.getAbogadoResponsable().getNombreCompleto();
            } else if (t.getExpediente().getAbogadoResponsableNombre() != null) {
                usuarioNombre = t.getExpediente().getAbogadoResponsableNombre();
            }
        }
        
        return EventoResponse.builder()
                .id(t.getId() != null ? t.getId().longValue() : null)
                .titulo("Término: " + t.getActuacion())
                .tipo("termino")
                .fecha(t.getFechaVencimiento() != null ? t.getFechaVencimiento().toString() : "")
                .hora("23:59")
                .gerenciaId(gerenciaId)
                .gerenciaNombre(gerenciaNombre)
                .usuarioId(usuarioId)
                .usuarioNombre(usuarioNombre)
                .expediente(t.getExpediente() != null ? t.getExpediente().getNumero() : "N/A")
                .build();
    }

    private EventoResponse mapToRecordatorioResponse(Recordatorio r) {
        return EventoResponse.builder()
                .id(r.getId() != null ? r.getId().longValue() : null)
                .titulo(r.getTitulo())
                .tipo("recordatorio")
                .fecha(r.getFechaRecordatorio() != null ? r.getFechaRecordatorio().toString() : "")
                .hora(r.getHoraRecordatorio() != null ? r.getHoraRecordatorio().toString() : "")
                .gerenciaId(null)
                .gerenciaNombre("")
                .usuarioId(r.getUsuario() != null ? r.getUsuario().getId().longValue() : null)
                .usuarioNombre(r.getUsuario() != null ? r.getUsuario().getNombreCompleto() : "")
                .detalles(r.getDetalles())
                .build();
    }

    /**
     * Obtiene todas las gerencias para el filtro
     */
    public List<Gerencia> obtenerGerencias() {
        return gerenciaRepository.findAll();
    }

    /**
     * Obtiene usuarios por gerencia (para filtro de gerente)
     */
    public List<Usuario> obtenerUsuariosPorGerencia(Long gerenciaId) {
        if (gerenciaId != null && gerenciaId > 0) {
            return usuarioRepository.findByGerenciaId(gerenciaId);
        }
        return usuarioRepository.findAll();
    }
}

