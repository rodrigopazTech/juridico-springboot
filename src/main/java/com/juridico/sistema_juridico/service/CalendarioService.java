package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.dto.response.calendario.EventoResponse;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
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

    public List<EventoResponse> obtenerEventosCalendario(Usuario usuarioActual, String filtroTipo) {
        return obtenerEventosCalendario(usuarioActual, filtroTipo, null, null, null, null, null, null, null, null);
    }

    public List<EventoResponse> obtenerEventosCalendario(Usuario usuarioActual, String filtroTipo, Long filtroGerenciaId) {
        return obtenerEventosCalendario(usuarioActual, filtroTipo, filtroGerenciaId, null, null, null, null, null, null, null);
    }

    public List<EventoResponse> obtenerEventosCalendario(Usuario usuarioActual, String filtroTipo, Long filtroGerenciaId, Long filtroUsuarioId) {
        return obtenerEventosCalendario(usuarioActual, filtroTipo, filtroGerenciaId, null, filtroUsuarioId, null, null, null, null, null);
    }

    public List<EventoResponse> obtenerEventosCalendario(Usuario usuarioActual, String filtroTipo, Long filtroGerenciaId, Long filtroUsuarioId, String filtroMateria) {
        return obtenerEventosCalendario(usuarioActual, filtroTipo, filtroGerenciaId, null, filtroUsuarioId, filtroMateria, null, null, null, null);
    }

    public List<EventoResponse> obtenerEventosCalendario(Usuario usuarioActual, String filtroTipo, Long filtroGerenciaId, Long filtroUsuarioId, String filtroMateria, Integer filtroMateriaId, String filtroEstatus, Integer filtroPrioridadId) {
        return obtenerEventosCalendario(usuarioActual, filtroTipo, filtroGerenciaId, null, filtroUsuarioId, filtroMateria, filtroMateriaId, filtroEstatus, filtroPrioridadId, null);
    }

    public List<EventoResponse> obtenerEventosCalendario(
            Usuario usuarioActual, 
            String filtroTipo, 
            Long filtroGerenciaId, 
            String filtroGerenciaNombre,
            Long filtroUsuarioId, 
            String filtroMateria, 
            Integer filtroMateriaId, 
            String filtroEstatus, 
            Integer filtroPrioridadId,
            String filtroTipoAudiencia) {
        List<EventoResponse> eventos = new ArrayList<>();

        // --- FILTRO DE SEGURIDAD MANDATORIO ---
        // Determinar la restricción de gerencia según el rol del usuario.
        // GERENTE, JEFE_DEPTO y ABOGADO solo pueden ver eventos de su propia gerencia.
        // DIRECCION, SUBDIRECCION y ADMIN ven todo (gerenciaIdRestriccion queda null).
        Integer gerenciaIdRestriccion = null;
        Integer abogadoIdRestriccion = null;

        if (usuarioActual != null) {
            RolUsuario rol = usuarioActual.getRol();
            if (rol == RolUsuario.GERENTE || rol == RolUsuario.JEFE_DEPTO || rol == RolUsuario.ABOGADO) {
                if (usuarioActual.getGerencia() != null) {
                    gerenciaIdRestriccion = usuarioActual.getGerencia().getId();
                }
            }
            if (rol == RolUsuario.ABOGADO) {
                abogadoIdRestriccion = usuarioActual.getId();
            }
        }

        // Restricción adicional para JEFE_DEPTO: solo materias asignadas
        List<Integer> jefeMateriaIds = null;
        if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.JEFE_DEPTO) {
            jefeMateriaIds = usuarioActual.getMaterias().stream()
                .map(Materia::getId)
                .collect(Collectors.toList());
        }

        // 1. Mapear Audiencias
        List<Audiencia> audiencias = audienciaRepository.findAll();

        // Aplicar restricción de gerencia
        if (gerenciaIdRestriccion != null) {
            final Integer gid = gerenciaIdRestriccion;
            audiencias = audiencias.stream()
                .filter(a -> a.getExpediente() != null &&
                            a.getExpediente().getGerencia() != null &&
                            a.getExpediente().getGerencia().getId().equals(gid))
                .collect(Collectors.toList());
        }
        // Restricción adicional para ABOGADO: solo sus audiencias
        if (abogadoIdRestriccion != null) {
            final Integer uid = abogadoIdRestriccion;
            audiencias = audiencias.stream()
                .filter(a -> {
                    boolean esResponsable = a.getExpediente() != null &&
                        a.getExpediente().getAbogadoResponsable() != null &&
                        a.getExpediente().getAbogadoResponsable().getId().equals(uid);
                    boolean esComparece = a.getAbogadoComparece() != null &&
                        a.getAbogadoComparece().getId().equals(uid);
                    return esResponsable || esComparece;
                })
                .collect(Collectors.toList());
        }
        // Restricción adicional para JEFE_DEPTO: solo materias asignadas
        if (jefeMateriaIds != null) {
            final List<Integer> mIds = jefeMateriaIds;
            audiencias = audiencias.stream()
                .filter(a -> a.getExpediente() != null &&
                            a.getExpediente().getMateria() != null &&
                            mIds.contains(a.getExpediente().getMateria().getId()))
                .collect(Collectors.toList());
        }

        eventos.addAll(audiencias.stream()
                .<EventoResponse>map(a -> mapToAudienciaResponse(a))
                .collect(Collectors.toList()));

        // 2. Mapear Términos
        List<Termino> terminos = terminoRepository.findAll();

        // Aplicar restricción de gerencia
        if (gerenciaIdRestriccion != null) {
            final Integer gid = gerenciaIdRestriccion;
            terminos = terminos.stream()
                .filter(t -> t.getGerenciaReferencia() != null &&
                             t.getGerenciaReferencia().getId().equals(gid))
                .collect(Collectors.toList());
        }
        // Restricción adicional para ABOGADO: solo sus términos
        if (abogadoIdRestriccion != null) {
            final Integer uid = abogadoIdRestriccion;
            terminos = terminos.stream()
                .filter(t -> {
                    boolean esResponsableTermino = t.getAbogadoResponsable() != null &&
                        t.getAbogadoResponsable().getId().equals(uid);
                    boolean esResponsableExp = t.getExpediente() != null &&
                        t.getExpediente().getAbogadoResponsable() != null &&
                        t.getExpediente().getAbogadoResponsable().getId().equals(uid);
                    return esResponsableTermino || esResponsableExp;
                })
                .collect(Collectors.toList());
        }
        // Restricción adicional para JEFE_DEPTO: solo materias asignadas
        if (jefeMateriaIds != null) {
            final List<Integer> mIds = jefeMateriaIds;
            terminos = terminos.stream()
                .filter(t -> {
                    Materia mat = t.getMateriaReferencia();
                    return mat != null && mIds.contains(mat.getId());
                })
                .collect(Collectors.toList());
        }

        eventos.addAll(terminos.stream()
                .<EventoResponse>map(t -> mapToTerminoResponse(t))
                .collect(Collectors.toList()));

        // 3. Mapear Recordatorios (solo del usuario actual, siempre)
        if (usuarioActual != null && usuarioActual.getId() != null) {
            List<Recordatorio> recordatorios = recordatorioRepository
                .findByUsuarioIdAndCompletadoFalse(usuarioActual.getId());
            eventos.addAll(recordatorios.stream()
                    .<EventoResponse>map(r -> mapToRecordatorioResponse(r))
                    .collect(Collectors.toList()));
        }

        // Aplicar filtros adicionales de UI (tipo, gerencia elegida, usuario elegido, materia)
        return aplicarFiltros(eventos, filtroTipo, filtroGerenciaId, filtroGerenciaNombre, filtroUsuarioId, filtroMateria, filtroMateriaId, filtroEstatus, filtroPrioridadId, filtroTipoAudiencia);
    }

    private List<EventoResponse> aplicarFiltros(
            List<EventoResponse> eventos, 
            String filtroTipo, 
            Long filtroGerenciaId, 
            String filtroGerenciaNombre,
            Long filtroUsuarioId, 
            String filtroMateria, 
            Integer filtroMateriaId, 
            String filtroEstatus, 
            Integer filtroPrioridadId,
            String filtroTipoAudiencia) {
        return eventos.stream()
            .filter(e -> {
                // Filtro por tipo
                if (filtroTipo != null && !filtroTipo.isEmpty() && !"todos".equals(filtroTipo)) {
                    if (!e.getTipo().equals(filtroTipo)) {
                        return false;
                    }
                }
                // Filtro por gerencia ID
                if (filtroGerenciaId != null && filtroGerenciaId > 0) {
                    if (e.getGerenciaId() == null || !e.getGerenciaId().equals(filtroGerenciaId)) {
                        return false;
                    }
                }
                // Filtro por gerencia nombre
                if (filtroGerenciaNombre != null && !filtroGerenciaNombre.isEmpty() && !"todos".equals(filtroGerenciaNombre)) {
                    if (e.getGerenciaNombre() == null || !e.getGerenciaNombre().equalsIgnoreCase(filtroGerenciaNombre)) {
                        return false;
                    }
                }
                // Filtro por usuario (solo si se especifica y no es "todos")
                if (filtroUsuarioId != null && filtroUsuarioId > 0) {
                    if (e.getUsuarioId() == null || !e.getUsuarioId().equals(filtroUsuarioId)) {
                        return false;
                    }
                }
                // Filtro por materia (solo si se especifica y no es "todos")
                if (filtroMateria != null && !filtroMateria.isEmpty() && !"todos".equals(filtroMateria)) {
                    if (e.getMateriaNombre() == null || !e.getMateriaNombre().equalsIgnoreCase(filtroMateria)) {
                        return false;
                    }
                }
                // Filtro por materiaId (solo si se especifica)
                if (filtroMateriaId != null && filtroMateriaId > 0) {
                    if (e.getMateriaId() == null || !e.getMateriaId().equals(filtroMateriaId)) {
                        return false;
                    }
                }
                // Filtro por estatus (solo si se especifica y no es "todos")
                if (filtroEstatus != null && !filtroEstatus.isEmpty() && !"todos".equals(filtroEstatus)) {
                    if (e.getEstatus() == null || !e.getEstatus().equalsIgnoreCase(filtroEstatus)) {
                        return false;
                    }
                }
                // Filtro por prioridadId (solo si se especifica)
                if (filtroPrioridadId != null && filtroPrioridadId > 0) {
                    if (e.getPrioridadId() == null || !e.getPrioridadId().equals(filtroPrioridadId)) {
                        return false;
                    }
                }
                // Filtro por tipo de audiencia (subtipo)
                if (filtroTipoAudiencia != null && !filtroTipoAudiencia.isEmpty() && !"todos".equals(filtroTipoAudiencia)) {
                    if (e.getTipoAudiencia() == null || !e.getTipoAudiencia().equalsIgnoreCase(filtroTipoAudiencia)) {
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
        Integer materiaId = null;
        String materiaNombre = "";
        
        if (a.getExpediente() != null) {
            if (a.getExpediente().getGerencia() != null) {
                gerenciaId = a.getExpediente().getGerencia().getId().longValue();
                gerenciaNombre = a.getExpediente().getGerencia().getNombre();
            }
            if (a.getExpediente().getMateria() != null) {
                materiaId = a.getExpediente().getMateria().getId();
                materiaNombre = a.getExpediente().getMateria().getNombre();
            }
            if (a.getExpediente().getAbogadoResponsable() != null) {
                usuarioId = a.getExpediente().getAbogadoResponsable().getId().longValue();
                usuarioNombre = a.getExpediente().getAbogadoResponsable().getNombreCompleto();
            } else if (a.getExpediente().getAbogadoResponsableNombre() != null) {
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
                .materiaId(materiaId)
                .materiaNombre(materiaNombre)
                .estatus(a.getEstatusAudiencia() != null ? a.getEstatusAudiencia().name() : null)
                .tipoAudiencia(a.getTipoAudiencia() != null ? a.getTipoAudiencia().getNombre() : null)
                .build();
    }

    private EventoResponse mapToTerminoResponse(Termino t) {
        Long gerenciaId = null;
        String gerenciaNombre = "";
        Long usuarioId = null;
        String usuarioNombre = "";
        Integer materiaId = null;
        String materiaNombre = "";
        
        Gerencia ger = t.getGerenciaReferencia();
        if (ger != null) {
            gerenciaId = ger.getId().longValue();
            gerenciaNombre = ger.getNombre();
        }
        Materia mat = t.getMateriaReferencia();
        if (mat != null) {
            materiaId = mat.getId();
            materiaNombre = mat.getNombre();
        }
        if (t.getAbogadoResponsable() != null) {
            usuarioId = t.getAbogadoResponsable().getId().longValue();
            usuarioNombre = t.getAbogadoResponsable().getNombreCompleto();
        } else if (t.getExpediente() != null && t.getExpediente().getAbogadoResponsableNombre() != null) {
            usuarioNombre = t.getExpediente().getAbogadoResponsableNombre();
        }
        
        return EventoResponse.builder()
                .id(t.getId() != null ? t.getId().longValue() : null)
                .titulo("Término: " + (t.getAsunto() != null ? t.getAsunto() : "Sin Asunto"))
                .tipo("termino")
                .fecha(t.getFechaVencimiento() != null ? t.getFechaVencimiento().toString() : "")
                .hora("")
                .gerenciaId(gerenciaId)
                .gerenciaNombre(gerenciaNombre)
                .usuarioId(usuarioId)
                .usuarioNombre(usuarioNombre)
                .expediente(t.getNumeroReferencia())
                .materiaId(materiaId)
                .materiaNombre(materiaNombre)
                .estatus(t.getEstatusTermino() != null ? t.getEstatusTermino().name() : null)
                .prioridadId(t.getPrioridad() != null ? t.getPrioridad().getId() : null)
                .build();
    }

    private EventoResponse mapToRecordatorioResponse(Recordatorio r) {
        Long gerenciaId = null;
        String gerenciaNombre = "";
        if (r.getUsuario() != null && r.getUsuario().getGerencia() != null) {
            gerenciaId = r.getUsuario().getGerencia().getId().longValue();
            gerenciaNombre = r.getUsuario().getGerencia().getNombre();
        }
        return EventoResponse.builder()
                .id(r.getId() != null ? r.getId().longValue() : null)
                .titulo("Tarea: " + r.getTitulo())
                .tipo("recordatorio")
                .fecha(r.getFechaRecordatorio() != null ? r.getFechaRecordatorio().toString() : "")
                .hora(r.getHoraRecordatorio() != null ? r.getHoraRecordatorio().toString() : "")
                .gerenciaId(gerenciaId)
                .gerenciaNombre(gerenciaNombre)
                .usuarioId(r.getUsuario() != null ? r.getUsuario().getId().longValue() : null)
                .usuarioNombre(r.getUsuario() != null ? r.getUsuario().getNombreCompleto() : "")
                .detalles(r.getDetalles())
                .prioridadId(r.getPrioridad() != null ? r.getPrioridad().getId() : null)
                .build();
    }
}
