package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.NotificacionRepository;
import com.juridico.sistema_juridico.repository.Usuarios.RecordatorioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Recordatorio;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.PeriodoFiltro;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.enums.NivelAtencionTemporal;
import com.juridico.sistema_juridico.service.procesal.EvaluadorAtencionTemporalService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardService {

    private final ExpedienteRepository expedienteRepository;
    private final AudienciaRepository audienciaRepository;
    private final TerminoRepository terminoRepository;
    private final RecordatorioRepository recordatorioRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvaluadorAtencionTemporalService evaluadorAtencion;

    // Constantes para grupos de gerencias
    public static final List<Long> GRUPO_CIVIL_MERCANTIL = Arrays.asList(1L, 2L, 3L, 4L); // Civil, Mercantil, Fiscal,
                                                                                          // Administrativo
    public static final List<Long> GRUPO_LABORAL_PENAL = Arrays.asList(5L, 6L); // Laboral, Penal
    public static final List<Long> GRUPO_TRANSPARENCIA = Arrays.asList(7L, 8L); // Transparencia, Amparo

    public DashboardService(ExpedienteRepository expedienteRepository,
            AudienciaRepository audienciaRepository,
            TerminoRepository terminoRepository,
            RecordatorioRepository recordatorioRepository,
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository,
            EvaluadorAtencionTemporalService evaluadorAtencion) {
        this.expedienteRepository = expedienteRepository;
        this.audienciaRepository = audienciaRepository;
        this.terminoRepository = terminoRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.evaluadorAtencion = evaluadorAtencion;
    }

    /**
     * Convierte un grupo de gerencia a lista de IDs
     */
    public List<Long> obtenerListaGerencias(String grupoGerencia) {
        if (grupoGerencia == null)
            return null;
        return switch (grupoGerencia) {
            case "CIVIL_MERCANTIL" -> GRUPO_CIVIL_MERCANTIL;
            case "LABORAL_PENAL" -> GRUPO_LABORAL_PENAL;
            case "TRANSPARENCIA" -> GRUPO_TRANSPARENCIA;
            default -> null;
        };
    }

    public Map<String, Long> obtenerKpisPorLista(Long gerenciaId, List<Long> gerenciaIds) {
        return obtenerKpisPorLista(gerenciaId, gerenciaIds, null);
    }

    public Map<String, Long> obtenerKpisPorLista(Long gerenciaId, List<Long> gerenciaIds, List<Integer> materiaIds) {
        Map<String, Long> kpis = new HashMap<>();

        // Si hay lista de gerencias, usar esa; si hay gerenciaId individual, usar esa
        boolean hayFiltro = gerenciaIds != null && !gerenciaIds.isEmpty();
        boolean hayMaterias = materiaIds != null && !materiaIds.isEmpty();

        if (gerenciaId != null) {
            if (hayMaterias) {
                // Filtro por gerencia Y materias (Jefe de Depto)
                kpis.put("totalExpedientes", expedienteRepository.countByGerenciaIdAndMateriaIdIn(gerenciaId, materiaIds));
                kpis.put("expedientesActivos",
                        expedienteRepository.countByEtapaProcesalAndGerenciaIdAndMateriaIdIn(EtapaProcesal.TRAMITE, gerenciaId, materiaIds));
                kpis.put("audienciasProgramadas",
                        audienciaRepository.countByExpedienteGerenciaIdAndMateriaIdInAndEstatusPendiente(gerenciaId, materiaIds));
                kpis.put("terminosActivos", terminoRepository.countByExpedienteGerenciaIdAndMateriaIdInAndActivo(gerenciaId, materiaIds));
            } else {
                // Filtro por gerencia individual (Gerente/Director)
                kpis.put("totalExpedientes", expedienteRepository.countByGerenciaId(gerenciaId));
                kpis.put("expedientesActivos",
                        expedienteRepository.countByEtapaProcesalAndGerenciaId(EtapaProcesal.TRAMITE, gerenciaId));
                kpis.put("audienciasProgramadas",
                        audienciaRepository.countByExpedienteGerenciaIdAndEstatusPendiente(gerenciaId));
                kpis.put("terminosActivos", terminoRepository.countByExpedienteGerenciaIdAndActivo(gerenciaId));
            }
        } else if (hayFiltro) {
            // Filtro por lista de gerencias (grupo)
            kpis.put("totalExpedientes", expedienteRepository.countByGerenciaIdIn(gerenciaIds));
            kpis.put("expedientesActivos",
                    expedienteRepository.countByEtapaProcesalAndGerenciaIdIn(EtapaProcesal.TRAMITE, gerenciaIds));
            kpis.put("audienciasProgramadas",
                    audienciaRepository.countByExpedienteGerenciaIdInAndEstatusPendiente(gerenciaIds));
            kpis.put("terminosActivos", terminoRepository.countByExpedienteGerenciaIdInAndActivo(gerenciaIds));
        } else {
            // Sin filtro - todos los datos
            kpis.put("totalExpedientes", expedienteRepository.count());
            kpis.put("expedientesActivos", expedienteRepository.countByEtapaProcesal(EtapaProcesal.TRAMITE));
            kpis.put("audienciasProgramadas", audienciaRepository.countByEstatusPendiente());
            kpis.put("terminosActivos", terminoRepository.countByActivo());
        }
        return kpis;
    }

    public Map<String, Long> obtenerKpisPersonales(Integer usuarioId, Integer materiaId) {
        Map<String, Long> kpis = new HashMap<>();
        if (materiaId == null) {
            kpis.put("totalExpedientes", expedienteRepository.countByAbogadoResponsableId(usuarioId));
            kpis.put("expedientesActivos", expedienteRepository.countByEtapaProcesalAndAbogadoResponsableId(EtapaProcesal.TRAMITE, usuarioId));
            kpis.put("audienciasProgramadas", audienciaRepository.countPendientesByAbogadoId(usuarioId));
            kpis.put("terminosActivos", terminoRepository.countActivosByAbogadoId(usuarioId));
        } else {
            kpis.put("totalExpedientes", expedienteRepository.countByAbogadoResponsableIdAndMateriaId(usuarioId, materiaId));
            kpis.put("expedientesActivos", expedienteRepository.countByEtapaProcesalAndAbogadoResponsableIdAndMateriaId(EtapaProcesal.TRAMITE, usuarioId, materiaId));
            kpis.put("audienciasProgramadas", audienciaRepository.countPendientesByAbogadoIdAndMateriaId(usuarioId, materiaId));
            kpis.put("terminosActivos", terminoRepository.countActivosByAbogadoIdAndMateriaId(usuarioId, materiaId));
        }
        return kpis;
    }

    public Map<String, Object> obtenerMetricasPersonales(Integer usuarioId, Integer materiaId) {
        Map<String, Object> data = new HashMap<>();
        
        List<Object[]> rows;
        if (materiaId == null) {
            rows = expedienteRepository.contarExpedientesPorEstatusYAbogado(usuarioId);
        } else {
            rows = expedienteRepository.contarExpedientesPorEstatusYAbogadoYMateria(usuarioId, materiaId);
        }

        Map<EtapaProcesal, Long> statusMap = new HashMap<>();
        for (Object[] row : rows) {
            EtapaProcesal etapa = (EtapaProcesal) row[0];
            Long count = ((Number) row[1]).longValue();
            statusMap.put(etapa, count);
        }
        List<String> labelsEstatus = new ArrayList<>();
        List<Long> valuesEstatus = new ArrayList<>();
        for (EtapaProcesal etapa : EtapaProcesal.values()) {
            labelsEstatus.add(etapa.name());
            valuesEstatus.add(statusMap.getOrDefault(etapa, 0L));
        }
        data.put("estatusExpedientes", Map.of("labels", labelsEstatus, "values", valuesEstatus));

        // Priority distribution
        List<Object[]> prioridadRows;
        if (materiaId == null) {
            prioridadRows = expedienteRepository.contarExpedientesPorPrioridadYAbogado(usuarioId);
        } else {
            prioridadRows = expedienteRepository.contarExpedientesPorPrioridadYAbogadoYMateria(usuarioId, materiaId);
        }
        List<String> labelsPrioridad = new ArrayList<>();
        List<Long> valuesPrioridad = new ArrayList<>();
        for (Object[] row : prioridadRows) {
            labelsPrioridad.add((String) row[0]);
            valuesPrioridad.add(((Number) row[1]).longValue());
        }
        data.put("prioridadExpedientes", Map.of("labels", labelsPrioridad, "values", valuesPrioridad));

        List<String> labelsTrabajo = List.of("Audiencias y Términos");
        List<Integer> audiencias;
        List<Integer> terminos;

        if (materiaId == null) {
            audiencias = List.of((int) audienciaRepository.countPendientesByAbogadoId(usuarioId));
            terminos = List.of((int) terminoRepository.countActivosByAbogadoId(usuarioId));
        } else {
            audiencias = List.of((int) audienciaRepository.countPendientesByAbogadoIdAndMateriaId(usuarioId, materiaId));
            terminos = List.of((int) terminoRepository.countActivosByAbogadoIdAndMateriaId(usuarioId, materiaId));
        }
        
        data.put("cargaTrabajoUsuarios", Map.of("labels", labelsTrabajo, "audiencias", audiencias, "terminos", terminos));

        // Audiencias agrupadas con la misma evaluación temporal utilizada en las pantallas.
        List<com.juridico.sistema_juridico.Entity.procesal.Audiencia> audList = materiaId == null
            ? audienciaRepository.fetchPendientesByAbogadoId(usuarioId)
            : audienciaRepository.fetchPendientesByAbogadoIdAndMateriaId(usuarioId, materiaId);

        // Orden fijo: ALTA, MEDIA, BAJA
        List<String> prioLabels = new ArrayList<>(java.util.Arrays.asList("ALTA", "MEDIA", "BAJA"));
        java.util.Map<String, Integer> audAtencion = new java.util.LinkedHashMap<>();
        audAtencion.put("ALTA", 0);
        audAtencion.put("MEDIA", 0);
        audAtencion.put("BAJA", 0);

        for (com.juridico.sistema_juridico.Entity.procesal.Audiencia a : audList) {
            NivelAtencionTemporal nivel = evaluadorAtencion.evaluarAudiencia(a).nivel();
            if (nivel == NivelAtencionTemporal.ROJO || nivel == NivelAtencionTemporal.PASADA_SIN_RESULTADO) {
                audAtencion.merge("ALTA", 1, Integer::sum);
            } else if (nivel == NivelAtencionTemporal.AMARILLO) {
                audAtencion.merge("MEDIA", 1, Integer::sum);
            } else {
                audAtencion.merge("BAJA", 1, Integer::sum);
            }
        }

        // === PRIORITY BAR: Términos por su propia prioridad ===
        List<Object[]> terPrioRows = materiaId == null
            ? terminoRepository.contarTerminosPorPrioridadYAbogado(usuarioId)
            : terminoRepository.contarTerminosPorPrioridadYAbogadoYMateria(usuarioId, materiaId);

        java.util.Map<String, Integer> terMap = new HashMap<>();
        terPrioRows.forEach(r -> terMap.put((String) r[0], ((Number) r[1]).intValue()));

        List<Integer> audByPrio = prioLabels.stream().map(l -> audAtencion.getOrDefault(l, 0)).collect(java.util.stream.Collectors.toList());
        List<Integer> terByPrio = prioLabels.stream().map(l -> terMap.getOrDefault(l, 0)).collect(java.util.stream.Collectors.toList());

        data.put("cargaTrabajoPrioridad", Map.of("labels", prioLabels, "audiencias", audByPrio, "terminos", terByPrio));
        
        return data;
    }

    public Map<String, Object> obtenerResumenMisAsuntos(Usuario usuario, Integer materiaId) {
        LocalDate hoy = LocalDate.now();
        LocalDate sieteDias = hoy.plusDays(7);
        Integer usuarioId = usuario.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("terminosVencidos", terminoRepository.countMisTerminosVencidos(usuarioId, materiaId, hoy));
        data.put("terminosHoy", terminoRepository.countMisTerminosHoy(usuarioId, materiaId, hoy));
        data.put("terminosProximos7", terminoRepository.countMisTerminosProximos(usuarioId, materiaId, hoy, sieteDias));
        data.put("audienciasHoy", audienciaRepository.countMisAudienciasHoy(usuarioId, materiaId, hoy));
        data.put("audienciasProximas7", audienciaRepository.countMisAudienciasProximas(usuarioId, materiaId, hoy, sieteDias));
        data.put("audienciasAtrasadas", audienciaRepository.countMisAudienciasAtrasadas(usuarioId, materiaId, hoy));
        data.put("tareasPendientes", recordatorioRepository.countByUsuarioAndCompletadoFalse(usuario));
        data.put("tareasVencidas", recordatorioRepository.countByUsuarioAndCompletadoFalseAndFechaRecordatorioBefore(usuario, hoy));
        data.put("tareasProximas7", recordatorioRepository.countByUsuarioAndCompletadoFalseAndFechaRecordatorioBetween(usuario, hoy, sieteDias));
        data.put("notificacionesNoLeidas", notificacionRepository.countByUsuarioAndLeidaFalse(usuario));
        data.put("terminosCriticos", obtenerTerminosCriticos(usuarioId, materiaId, hoy.plusDays(10)));
        data.put("proximasAudiencias", obtenerProximasAudiencias(usuarioId, materiaId, hoy));
        data.put("proximasTareas", obtenerProximasTareas(usuario));
        data.put("alertasPrioridad", obtenerAlertasPrioridad(usuarioId, materiaId, hoy));
        return data;
    }

    private List<Map<String, Object>> obtenerTerminosCriticos(Integer usuarioId, Integer materiaId, LocalDate hasta) {
        List<Termino> terminos = terminoRepository.findMisTerminosCriticos(usuarioId, materiaId, hasta, PageRequest.of(0, 6));
        return terminos.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("referencia", t.getNumeroReferencia());
            item.put("asunto", textoCorto(t.getAsunto(), "Sin asunto"));
            item.put("fecha", t.getFechaVencimiento());
            item.put("estatus", t.getEstatusTermino() != null ? t.getEstatusTermino().getDescripcion() : "Sin estatus");
            item.put("prioridad", t.getPrioridad() != null ? t.getPrioridad().getNombre() : "Sin prioridad");
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), t.getFechaVencimiento()));
            item.put("tono", tonoPorFecha(t.getFechaVencimiento()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerProximasAudiencias(Integer usuarioId, Integer materiaId, LocalDate hoy) {
        List<Audiencia> audiencias = audienciaRepository.findMisProximasAudiencias(usuarioId, materiaId, hoy, PageRequest.of(0, 5));
        return audiencias.stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("referencia", a.getNumeroReferencia());
            item.put("tipo", a.getTipoAudiencia() != null ? a.getTipoAudiencia().getNombre() : "Audiencia");
            item.put("fecha", a.getFechaAudiencia());
            item.put("hora", a.getHoraAudiencia());
            item.put("lugar", textoCorto(a.getSalaLugar() != null ? a.getSalaLugar() : a.getUrlReunion(), "Sin sede definida"));
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), a.getFechaAudiencia()));
            item.put("tono", tonoPorFecha(a.getFechaAudiencia()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerProximasTareas(Usuario usuario) {
        List<Recordatorio> tareas = recordatorioRepository.findByUsuarioAndCompletadoFalseOrderByFechaRecordatorioAsc(usuario)
                .stream()
                .limit(4)
                .toList();
        return tareas.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("titulo", textoCorto(r.getTitulo(), "Tarea sin titulo"));
            item.put("fecha", r.getFechaRecordatorio());
            item.put("hora", r.getHoraRecordatorio());
            item.put("prioridad", r.getPrioridad() != null ? r.getPrioridad().getNombre() : "Sin prioridad");
            item.put("tono", tonoPorFecha(r.getFechaRecordatorio()));
            return item;
        }).toList();
    }

    private Map<String, Object> obtenerAlertasPrioridad(Integer usuarioId, Integer materiaId, LocalDate hoy) {
        Map<String, Object> data = new HashMap<>();
        long vencidos = terminoRepository.countMisTerminosVencidos(usuarioId, materiaId, hoy);
        long hoyTerminos = terminoRepository.countMisTerminosHoy(usuarioId, materiaId, hoy);
        long audienciasHoy = audienciaRepository.countMisAudienciasHoy(usuarioId, materiaId, hoy);
        long audienciasAtrasadas = audienciaRepository.countMisAudienciasAtrasadas(usuarioId, materiaId, hoy);

        data.put("criticas", vencidos + hoyTerminos + audienciasAtrasadas);
        data.put("hoy", hoyTerminos + audienciasHoy);
        data.put("semana", terminoRepository.countMisTerminosProximos(usuarioId, materiaId, hoy, hoy.plusDays(7))
                + audienciaRepository.countMisAudienciasProximas(usuarioId, materiaId, hoy, hoy.plusDays(7)));
        return data;
    }

    public Map<String, Object> obtenerResumenDepartamento(Usuario usuario) {
        LocalDate hoy = LocalDate.now();
        LocalDate sieteDias = hoy.plusDays(7);
        Long gerenciaId = usuario.getGerencia() != null ? usuario.getGerencia().getId().longValue() : null;
        List<Integer> materiaIds = usuario.getMaterias() == null
                ? List.of()
                : usuario.getMaterias().stream().map(m -> m.getId()).toList();

        Map<String, Object> data = new HashMap<>();
        if (gerenciaId == null || materiaIds.isEmpty()) {
            data.put("abogadosSupervisados", 0L);
            data.put("terminosVencidos", 0L);
            data.put("terminosHoy", 0L);
            data.put("terminosProximos7", 0L);
            data.put("audienciasHoy", 0L);
            data.put("audienciasProximas7", 0L);
            data.put("audienciasAtrasadas", 0L);
            data.put("terminosCriticos", List.of());
            data.put("proximasAudiencias", List.of());
            data.put("alertasPrioridad", Map.of("criticas", 0L, "hoy", 0L, "semana", 0L));
            return data;
        }

        long terminosVencidos = terminoRepository.countDepartamentoTerminosVencidos(gerenciaId, materiaIds, hoy);
        long terminosHoy = terminoRepository.countDepartamentoTerminosHoy(gerenciaId, materiaIds, hoy);
        long terminosProximos7 = terminoRepository.countDepartamentoTerminosProximos(gerenciaId, materiaIds, hoy, sieteDias);
        long audienciasHoy = audienciaRepository.countDepartamentoAudienciasHoy(gerenciaId, materiaIds, hoy);
        long audienciasProximas7 = audienciaRepository.countDepartamentoAudienciasProximas(gerenciaId, materiaIds, hoy, sieteDias);
        long audienciasAtrasadas = audienciaRepository.countDepartamentoAudienciasAtrasadas(gerenciaId, materiaIds, hoy);

        data.put("abogadosSupervisados", usuarioRepository.countAbogadosActivosPorGerenciaYMaterias(gerenciaId.intValue(), materiaIds, RolUsuario.ABOGADO));
        data.put("terminosVencidos", terminosVencidos);
        data.put("terminosHoy", terminosHoy);
        data.put("terminosProximos7", terminosProximos7);
        data.put("audienciasHoy", audienciasHoy);
        data.put("audienciasProximas7", audienciasProximas7);
        data.put("audienciasAtrasadas", audienciasAtrasadas);
        data.put("terminosCriticos", obtenerTerminosCriticosDepartamento(gerenciaId, materiaIds, hoy.plusDays(10)));
        data.put("proximasAudiencias", obtenerProximasAudienciasDepartamento(gerenciaId, materiaIds, hoy));
        data.put("alertasPrioridad", Map.of(
                "criticas", terminosVencidos + terminosHoy + audienciasAtrasadas,
                "hoy", terminosHoy + audienciasHoy,
                "semana", terminosProximos7 + audienciasProximas7));
        return data;
    }

    public Map<String, Object> obtenerResumenGerencia(Usuario usuario) {
        LocalDate hoy = LocalDate.now();
        LocalDate sieteDias = hoy.plusDays(7);
        Long gerenciaId = usuario.getGerencia() != null ? usuario.getGerencia().getId().longValue() : null;

        Map<String, Object> data = new HashMap<>();
        if (gerenciaId == null) {
            data.put("abogadosSupervisados", 0L);
            data.put("terminosVencidos", 0L);
            data.put("terminosHoy", 0L);
            data.put("terminosProximos7", 0L);
            data.put("audienciasHoy", 0L);
            data.put("audienciasProximas7", 0L);
            data.put("audienciasAtrasadas", 0L);
            data.put("terminosCriticos", List.of());
            data.put("proximasAudiencias", List.of());
            data.put("alertasPrioridad", Map.of("criticas", 0L, "hoy", 0L, "semana", 0L));
            return data;
        }

        long terminosVencidos = terminoRepository.countGerenciaTerminosVencidos(gerenciaId, hoy);
        long terminosHoy = terminoRepository.countGerenciaTerminosHoy(gerenciaId, hoy);
        long terminosProximos7 = terminoRepository.countGerenciaTerminosProximos(gerenciaId, hoy, sieteDias);
        long audienciasHoy = audienciaRepository.countGerenciaAudienciasHoy(gerenciaId, hoy);
        long audienciasProximas7 = audienciaRepository.countGerenciaAudienciasProximas(gerenciaId, hoy, sieteDias);
        long audienciasAtrasadas = audienciaRepository.countGerenciaAudienciasAtrasadas(gerenciaId, hoy);
        long abogadosSupervisados = usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(gerenciaId.intValue())
                .stream()
                .filter(u -> u.getRol() == RolUsuario.ABOGADO)
                .count();

        data.put("abogadosSupervisados", abogadosSupervisados);
        data.put("terminosVencidos", terminosVencidos);
        data.put("terminosHoy", terminosHoy);
        data.put("terminosProximos7", terminosProximos7);
        data.put("audienciasHoy", audienciasHoy);
        data.put("audienciasProximas7", audienciasProximas7);
        data.put("audienciasAtrasadas", audienciasAtrasadas);
        data.put("terminosCriticos", obtenerTerminosCriticosGerencia(gerenciaId, hoy.plusDays(10)));
        data.put("proximasAudiencias", obtenerProximasAudienciasGerencia(gerenciaId, hoy));
        data.put("alertasPrioridad", Map.of(
                "criticas", terminosVencidos + terminosHoy + audienciasAtrasadas,
                "hoy", terminosHoy + audienciasHoy,
                "semana", terminosProximos7 + audienciasProximas7));
        return data;
    }

    public Map<String, Object> obtenerDashboardGerente(Usuario usuario, Integer materiaId, Integer abogadoId, PeriodoFiltro periodo) {
        LocalDate hoy = LocalDate.now();
        LocalDate sieteDias = hoy.plusDays(7);
        Long gerenciaId = usuario.getGerencia() != null ? usuario.getGerencia().getId().longValue() : null;
        PeriodoFiltro periodoEfectivo = periodo != null ? periodo : PeriodoFiltro.SEMANA;
        LocalDate[] rango = periodoEfectivo.calcularRango();

        Map<String, Object> data = new HashMap<>();
        if (gerenciaId == null) {
            data.put("resumen", Map.of(
                    "abogadosSupervisados", 0L,
                    "expedientesEnTramite", 0L,
                    "terminosVencidos", 0L,
                    "terminosHoy", 0L,
                    "terminosProximos7", 0L,
                    "audienciasHoy", 0L,
                    "audienciasProximas7", 0L,
                    "audienciasAtrasadas", 0L,
                    "alertasPrioridad", Map.of("criticas", 0L, "hoy", 0L, "semana", 0L)));
            data.put("terminosPeriodo", List.of());
            data.put("audienciasPeriodo", List.of());
            data.put("cargaPorAbogado", Map.of("labels", List.of(), "audiencias", List.of(), "terminos", List.of()));
            data.put("pendientesPorMateria", Map.of("labels", List.of(), "audiencias", List.of(), "terminos", List.of()));
            data.put("fechaInicio", rango[0]);
            data.put("fechaFin", rango[1]);
            return data;
        }

        long terminosVencidos = terminoRepository.countGerenciaTerminosVencidosFiltrado(gerenciaId, materiaId, abogadoId, hoy);
        long terminosHoy = terminoRepository.countGerenciaTerminosHoyFiltrado(gerenciaId, materiaId, abogadoId, hoy);
        long terminosProximos7 = terminoRepository.countGerenciaTerminosProximosFiltrado(gerenciaId, materiaId, abogadoId, hoy, sieteDias);
        long audienciasHoy = audienciaRepository.countGerenciaAudienciasHoyFiltrado(gerenciaId, materiaId, abogadoId, hoy);
        long audienciasProximas7 = audienciaRepository.countGerenciaAudienciasProximasFiltrado(gerenciaId, materiaId, abogadoId, hoy, sieteDias);
        long audienciasAtrasadas = audienciaRepository.countGerenciaAudienciasAtrasadasFiltrado(gerenciaId, materiaId, abogadoId, hoy);
        long expedientesEnTramite = expedienteRepository.buscarConSeguridad(
                null, gerenciaId.intValue(), materiaId, null, EtapaProcesal.TRAMITE, abogadoId,
                null, null, null, PageRequest.of(0, 1)).getTotalElements();
        long abogadosSupervisados = abogadoId != null ? 1L
                : usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(gerenciaId.intValue())
                        .stream()
                        .filter(u -> u.getRol() == RolUsuario.ABOGADO)
                        .count();

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("abogadosSupervisados", abogadosSupervisados);
        resumen.put("expedientesEnTramite", expedientesEnTramite);
        resumen.put("terminosVencidos", terminosVencidos);
        resumen.put("terminosHoy", terminosHoy);
        resumen.put("terminosProximos7", terminosProximos7);
        resumen.put("audienciasHoy", audienciasHoy);
        resumen.put("audienciasProximas7", audienciasProximas7);
        resumen.put("audienciasAtrasadas", audienciasAtrasadas);
        resumen.put("alertasPrioridad", Map.of(
                "criticas", terminosVencidos + terminosHoy + audienciasAtrasadas,
                "hoy", terminosHoy + audienciasHoy,
                "semana", terminosProximos7 + audienciasProximas7));

        data.put("resumen", resumen);
        data.put("terminosPeriodo", obtenerTerminosGerenciaPeriodo(gerenciaId, materiaId, abogadoId, rango[0], rango[1]));
        data.put("audienciasPeriodo", obtenerAudienciasGerenciaPeriodo(gerenciaId, materiaId, abogadoId, rango[0], rango[1]));
        data.put("cargaPorAbogado", combinarSeries(
                audienciaRepository.contarGerenciaAudienciasPorUsuarioFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                terminoRepository.contarGerenciaTerminosPorUsuarioFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                8));
        data.put("pendientesPorMateria", combinarSeries(
                audienciaRepository.contarGerenciaAudienciasPorMateriaFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                terminoRepository.contarGerenciaTerminosPorMateriaFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                8));
        data.put("fechaInicio", rango[0]);
        data.put("fechaFin", rango[1]);
        return data;
    }

    public Map<String, Object> obtenerDashboardSubdirector(Long gerenciaId, Integer materiaId, Integer abogadoId,
            PeriodoFiltro periodo) {
        LocalDate hoy = LocalDate.now();
        LocalDate sieteDias = hoy.plusDays(7);
        PeriodoFiltro periodoEfectivo = periodo != null ? periodo : PeriodoFiltro.SEMANA;
        LocalDate[] rango = periodoEfectivo.calcularRango();

        long terminosVencidos = terminoRepository.countDirectivoTerminosVencidosFiltrado(gerenciaId, materiaId, abogadoId, hoy);
        long terminosHoy = terminoRepository.countDirectivoTerminosHoyFiltrado(gerenciaId, materiaId, abogadoId, hoy);
        long terminosProximos7 = terminoRepository.countDirectivoTerminosProximosFiltrado(gerenciaId, materiaId, abogadoId, hoy, sieteDias);
        long audienciasHoy = audienciaRepository.countDirectivoAudienciasHoyFiltrado(gerenciaId, materiaId, abogadoId, hoy);
        long audienciasProximas7 = audienciaRepository.countDirectivoAudienciasProximasFiltrado(gerenciaId, materiaId, abogadoId, hoy, sieteDias);
        long audienciasAtrasadas = audienciaRepository.countDirectivoAudienciasAtrasadasFiltrado(gerenciaId, materiaId, abogadoId, hoy);
        long expedientesEnTramite = expedienteRepository.buscarConSeguridad(
                null, gerenciaId != null ? gerenciaId.intValue() : null, materiaId, null,
                EtapaProcesal.TRAMITE, abogadoId, null, null, null, PageRequest.of(0, 1)).getTotalElements();
        long abogadosSupervisados = contarAbogadosDirectivo(gerenciaId, materiaId, abogadoId);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("abogadosSupervisados", abogadosSupervisados);
        resumen.put("expedientesEnTramite", expedientesEnTramite);
        resumen.put("terminosVencidos", terminosVencidos);
        resumen.put("terminosHoy", terminosHoy);
        resumen.put("terminosProximos7", terminosProximos7);
        resumen.put("audienciasHoy", audienciasHoy);
        resumen.put("audienciasProximas7", audienciasProximas7);
        resumen.put("audienciasAtrasadas", audienciasAtrasadas);
        resumen.put("alertasPrioridad", Map.of(
                "criticas", terminosVencidos + terminosHoy + audienciasAtrasadas,
                "hoy", terminosHoy + audienciasHoy,
                "semana", terminosProximos7 + audienciasProximas7));

        Map<String, Object> data = new HashMap<>();
        data.put("resumen", resumen);
        data.put("terminosPeriodo", obtenerTerminosDirectivoPeriodo(gerenciaId, materiaId, abogadoId, rango[0], rango[1]));
        data.put("audienciasPeriodo", obtenerAudienciasDirectivoPeriodo(gerenciaId, materiaId, abogadoId, rango[0], rango[1]));
        data.put("cargaPorAbogado", combinarSeries(
                audienciaRepository.contarDirectivoAudienciasPorUsuarioFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                terminoRepository.contarDirectivoTerminosPorUsuarioFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                8));
        data.put("pendientesPorMateria", combinarSeries(
                audienciaRepository.contarDirectivoAudienciasPorMateriaFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                terminoRepository.contarDirectivoTerminosPorMateriaFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                8));
        data.put("pendientesPorGerencia", combinarSeries(
                audienciaRepository.contarDirectivoAudienciasPorGerenciaFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                terminoRepository.contarDirectivoTerminosPorGerenciaFiltrado(gerenciaId, materiaId, abogadoId, rango[0], rango[1]),
                8));
        data.put("fechaInicio", rango[0]);
        data.put("fechaFin", rango[1]);
        return data;
    }

    private long contarAbogadosDirectivo(Long gerenciaId, Integer materiaId, Integer abogadoId) {
        if (abogadoId != null) {
            return 1L;
        }

        List<Usuario> abogados = gerenciaId != null
                ? usuarioRepository.findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(gerenciaId.intValue())
                : usuarioRepository.findByRolAndActivoTrue(RolUsuario.ABOGADO);

        return abogados.stream()
                .filter(u -> u.getRol() == RolUsuario.ABOGADO)
                .filter(u -> materiaId == null || u.tieneMateria(materiaId))
                .count();
    }

    private List<Map<String, Object>> obtenerTerminosCriticosDepartamento(Long gerenciaId, List<Integer> materiaIds, LocalDate hasta) {
        List<Termino> terminos = terminoRepository.findDepartamentoTerminosCriticos(gerenciaId, materiaIds, hasta, PageRequest.of(0, 6));
        return terminos.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("referencia", t.getNumeroReferencia());
            item.put("asunto", textoCorto(t.getAsunto(), "Sin asunto"));
            item.put("fecha", t.getFechaVencimiento());
            item.put("responsable", t.getAbogadoResponsable() != null ? t.getAbogadoResponsable().getNombreCompleto() : "Sin responsable");
            item.put("prioridad", t.getPrioridad() != null ? t.getPrioridad().getNombre() : "Sin prioridad");
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), t.getFechaVencimiento()));
            item.put("tono", tonoPorFecha(t.getFechaVencimiento()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerProximasAudienciasDepartamento(Long gerenciaId, List<Integer> materiaIds, LocalDate hoy) {
        List<Audiencia> audiencias = audienciaRepository.findDepartamentoProximasAudiencias(gerenciaId, materiaIds, hoy, PageRequest.of(0, 5));
        return audiencias.stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("referencia", a.getNumeroReferencia());
            item.put("tipo", a.getTipoAudiencia() != null ? a.getTipoAudiencia().getNombre() : "Audiencia");
            item.put("fecha", a.getFechaAudiencia());
            item.put("hora", a.getHoraAudiencia());
            item.put("responsable", a.getExpediente() != null && a.getExpediente().getAbogadoResponsable() != null
                    ? a.getExpediente().getAbogadoResponsable().getNombreCompleto()
                    : "Sin responsable");
            item.put("lugar", textoCorto(a.getSalaLugar() != null ? a.getSalaLugar() : a.getUrlReunion(), "Sin sede definida"));
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), a.getFechaAudiencia()));
            item.put("tono", tonoPorFecha(a.getFechaAudiencia()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerTerminosCriticosGerencia(Long gerenciaId, LocalDate hasta) {
        List<Termino> terminos = terminoRepository.findGerenciaTerminosCriticos(gerenciaId, hasta, PageRequest.of(0, 6));
        return terminos.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("referencia", t.getNumeroReferencia());
            item.put("asunto", textoCorto(t.getAsunto(), "Sin asunto"));
            item.put("fecha", t.getFechaVencimiento());
            item.put("responsable", t.getAbogadoResponsable() != null ? t.getAbogadoResponsable().getNombreCompleto() : "Sin responsable");
            item.put("prioridad", t.getPrioridad() != null ? t.getPrioridad().getNombre() : "Sin prioridad");
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), t.getFechaVencimiento()));
            item.put("tono", tonoPorFecha(t.getFechaVencimiento()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerProximasAudienciasGerencia(Long gerenciaId, LocalDate hoy) {
        List<Audiencia> audiencias = audienciaRepository.findGerenciaProximasAudiencias(gerenciaId, hoy, PageRequest.of(0, 5));
        return audiencias.stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("referencia", a.getNumeroReferencia());
            item.put("tipo", a.getTipoAudiencia() != null ? a.getTipoAudiencia().getNombre() : "Audiencia");
            item.put("fecha", a.getFechaAudiencia());
            item.put("hora", a.getHoraAudiencia());
            item.put("responsable", a.getExpediente() != null && a.getExpediente().getAbogadoResponsable() != null
                    ? a.getExpediente().getAbogadoResponsable().getNombreCompleto()
                    : "Sin responsable");
            item.put("lugar", textoCorto(a.getSalaLugar() != null ? a.getSalaLugar() : a.getUrlReunion(), "Sin sede definida"));
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), a.getFechaAudiencia()));
            item.put("tono", tonoPorFecha(a.getFechaAudiencia()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerTerminosGerenciaPeriodo(Long gerenciaId, Integer materiaId, Integer abogadoId,
            LocalDate fechaInicio, LocalDate fechaFin) {
        List<Termino> terminos = terminoRepository.findGerenciaTerminosPeriodoFiltrado(
                gerenciaId, materiaId, abogadoId, fechaInicio, fechaFin, PageRequest.of(0, 6));
        return terminos.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("referencia", t.getNumeroReferencia());
            item.put("asunto", textoCorto(t.getAsunto(), "Sin asunto"));
            item.put("fecha", t.getFechaVencimiento());
            item.put("responsable", t.getAbogadoResponsable() != null ? t.getAbogadoResponsable().getNombreCompleto()
                    : (t.getExpediente() != null && t.getExpediente().getAbogadoResponsable() != null
                            ? t.getExpediente().getAbogadoResponsable().getNombreCompleto()
                            : "Sin responsable"));
            item.put("prioridad", t.getPrioridad() != null ? t.getPrioridad().getNombre() : "Sin prioridad");
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), t.getFechaVencimiento()));
            item.put("tono", tonoPorFecha(t.getFechaVencimiento()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerAudienciasGerenciaPeriodo(Long gerenciaId, Integer materiaId, Integer abogadoId,
            LocalDate fechaInicio, LocalDate fechaFin) {
        List<Audiencia> audiencias = audienciaRepository.findGerenciaAudienciasPeriodoFiltrado(
                gerenciaId, materiaId, abogadoId, fechaInicio, fechaFin, PageRequest.of(0, 6));
        return audiencias.stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("referencia", a.getNumeroReferencia());
            item.put("tipo", a.getTipoAudiencia() != null ? a.getTipoAudiencia().getNombre() : "Audiencia");
            item.put("fecha", a.getFechaAudiencia());
            item.put("hora", a.getHoraAudiencia());
            item.put("responsable", a.getExpediente() != null && a.getExpediente().getAbogadoResponsable() != null
                    ? a.getExpediente().getAbogadoResponsable().getNombreCompleto()
                    : "Sin responsable");
            item.put("lugar", textoCorto(a.getSalaLugar() != null ? a.getSalaLugar() : a.getUrlReunion(), "Sin sede definida"));
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), a.getFechaAudiencia()));
            item.put("tono", tonoPorFecha(a.getFechaAudiencia()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerTerminosDirectivoPeriodo(Long gerenciaId, Integer materiaId, Integer abogadoId,
            LocalDate fechaInicio, LocalDate fechaFin) {
        List<Termino> terminos = terminoRepository.findDirectivoTerminosPeriodoFiltrado(
                gerenciaId, materiaId, abogadoId, fechaInicio, fechaFin, PageRequest.of(0, 6));
        return terminos.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("referencia", t.getNumeroReferencia());
            item.put("asunto", textoCorto(t.getAsunto(), "Sin asunto"));
            item.put("fecha", t.getFechaVencimiento());
            item.put("responsable", t.getAbogadoResponsable() != null ? t.getAbogadoResponsable().getNombreCompleto()
                    : (t.getExpediente() != null && t.getExpediente().getAbogadoResponsable() != null
                            ? t.getExpediente().getAbogadoResponsable().getNombreCompleto()
                            : "Sin responsable"));
            item.put("prioridad", t.getPrioridad() != null ? t.getPrioridad().getNombre() : "Sin prioridad");
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), t.getFechaVencimiento()));
            item.put("tono", tonoPorFecha(t.getFechaVencimiento()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> obtenerAudienciasDirectivoPeriodo(Long gerenciaId, Integer materiaId, Integer abogadoId,
            LocalDate fechaInicio, LocalDate fechaFin) {
        List<Audiencia> audiencias = audienciaRepository.findDirectivoAudienciasPeriodoFiltrado(
                gerenciaId, materiaId, abogadoId, fechaInicio, fechaFin, PageRequest.of(0, 6));
        return audiencias.stream().map(a -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("referencia", a.getNumeroReferencia());
            item.put("tipo", a.getTipoAudiencia() != null ? a.getTipoAudiencia().getNombre() : "Audiencia");
            item.put("fecha", a.getFechaAudiencia());
            item.put("hora", a.getHoraAudiencia());
            item.put("responsable", a.getExpediente() != null && a.getExpediente().getAbogadoResponsable() != null
                    ? a.getExpediente().getAbogadoResponsable().getNombreCompleto()
                    : "Sin responsable");
            item.put("lugar", textoCorto(a.getSalaLugar() != null ? a.getSalaLugar() : a.getUrlReunion(), "Sin sede definida"));
            item.put("dias", ChronoUnit.DAYS.between(LocalDate.now(), a.getFechaAudiencia()));
            item.put("tono", tonoPorFecha(a.getFechaAudiencia()));
            return item;
        }).toList();
    }

    private Map<String, Object> combinarSeries(List<Object[]> audienciasRows, List<Object[]> terminosRows, int limite) {
        Map<String, Integer> audiencias = toMap(audienciasRows);
        Map<String, Integer> terminos = toMap(terminosRows);
        Set<String> etiquetas = new LinkedHashSet<>();
        etiquetas.addAll(audiencias.keySet());
        etiquetas.addAll(terminos.keySet());

        List<String> labels = new ArrayList<>(etiquetas);
        labels.sort(Comparator.comparingInt((String label) -> audiencias.getOrDefault(label, 0) + terminos.getOrDefault(label, 0)).reversed());
        if (labels.size() > limite) {
            labels = new ArrayList<>(labels.subList(0, limite));
        }

        List<Integer> audienciasValues = new ArrayList<>();
        List<Integer> terminosValues = new ArrayList<>();
        for (String label : labels) {
            audienciasValues.add(audiencias.getOrDefault(label, 0));
            terminosValues.add(terminos.getOrDefault(label, 0));
        }

        return Map.of("labels", labels, "audiencias", audienciasValues, "terminos", terminosValues);
    }

    private String tonoPorFecha(LocalDate fecha) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fecha);
        if (dias < 0) return "vencido";
        if (dias <= 1) return "critico";
        if (dias <= 7) return "proximo";
        return "normal";
    }

    private String textoCorto(String valor, String fallback) {
        if (valor == null || valor.isBlank()) {
            return fallback;
        }
        return valor.length() > 110 ? valor.substring(0, 107) + "..." : valor;
    }

    public Map<String, Object> obtenerMetricas(Long gerenciaId) {
        return obtenerMetricasPorLista(gerenciaId, null, null);
    }

    public Map<String, Object> obtenerMetricasPorLista(Long gerenciaId, List<Long> gerenciaIds) {
        return obtenerMetricasPorLista(gerenciaId, gerenciaIds, null);
    }

    public Map<String, Object> obtenerMetricasPorLista(Long gerenciaId, List<Long> gerenciaIds, List<Integer> materiaIds) {
        Map<String, Object> data = new HashMap<>();
        data.put("estatusExpedientes", obtenerEstatusExpedientes(gerenciaId, gerenciaIds, materiaIds));
        data.put("cargaTrabajoUsuarios", obtenerCargaTrabajoUsuarios(gerenciaId, gerenciaIds, materiaIds));
        data.put("distribucionGerencias", obtenerDistribucionGerencias());
        data.put("trabajoMensual", obtenerTrabajoMensual(gerenciaId, gerenciaIds, materiaIds));
        return data;
    }

    private Map<String, Object> obtenerEstatusExpedientes(Long gerenciaId, List<Long> gerenciaIds, List<Integer> materiaIds) {
        List<Object[]> rows;

        if (gerenciaId != null) {
            if (materiaIds != null && !materiaIds.isEmpty()) {
                rows = expedienteRepository.contarExpedientesPorEstatusYGerenciaYMateriaIn(gerenciaId, materiaIds);
            } else {
                rows = expedienteRepository.contarExpedientesPorEstatusYGerencia(gerenciaId);
            }
        } else if (gerenciaIds != null && !gerenciaIds.isEmpty()) {
            rows = expedienteRepository.contarExpedientesPorEstatusYGerencias(gerenciaIds);
        } else {
            rows = expedienteRepository.contarExpedientesPorEstatus();
        }

        Map<EtapaProcesal, Long> statusMap = new HashMap<>();
        for (Object[] row : rows) {
            EtapaProcesal etapa = (EtapaProcesal) row[0];
            Long count = ((Number) row[1]).longValue();
            statusMap.put(etapa, count);
        }
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (EtapaProcesal etapa : EtapaProcesal.values()) {
            labels.add(etapa.name());
            values.add(statusMap.getOrDefault(etapa, 0L));
        }
        return Map.of("labels", labels, "values", values);
    }

    private Map<String, Object> obtenerCargaTrabajoUsuarios(Long gerenciaId, List<Long> gerenciaIds, List<Integer> materiaIds) {
        Map<String, Integer> expedientesMap;
        Map<String, Integer> audienciasMap;
        Map<String, Integer> terminosMap;

        if (gerenciaId != null) {
            if (materiaIds != null && !materiaIds.isEmpty()) {
                // Filtro por gerencia Y materias (Jefe de Depto)
                expedientesMap = toMap(expedienteRepository.contarExpedientesPorUsuarioYGerenciaYMateriaIn(gerenciaId, materiaIds));
                audienciasMap = toMap(audienciaRepository.contarAudienciasPorUsuarioYGerenciaYMateriaIn(gerenciaId, materiaIds));
                terminosMap = toMap(terminoRepository.contarTerminosPorUsuarioYGerenciaYMateriaIn(gerenciaId, materiaIds));
            } else {
                // Filtro por gerencia individual (Gerente/Director)
                expedientesMap = toMap(expedienteRepository.contarExpedientesPorUsuarioYGerencia(gerenciaId));
                audienciasMap = toMap(audienciaRepository.contarAudienciasPorUsuarioYGerencia(gerenciaId));
                terminosMap = toMap(terminoRepository.contarTerminosPorUsuarioYGerencia(gerenciaId.intValue()));
            }
        } else if (gerenciaIds != null && !gerenciaIds.isEmpty()) {
            // Filtro por lista de gerencias (grupo)
            expedientesMap = toMap(expedienteRepository.contarExpedientesPorUsuarioYGerencias(gerenciaIds));
            audienciasMap = toMap(audienciaRepository.contarAudienciasPorUsuarioYGerencias(gerenciaIds));
            terminosMap = toMap(terminoRepository.contarTerminosPorUsuarioYGerencias(
                    gerenciaIds.stream().map(Long::intValue).collect(java.util.stream.Collectors.toList())));
        } else {
            // Sin filtro
            expedientesMap = toMap(expedienteRepository.contarExpedientesPorUsuario());
            audienciasMap = toMap(audienciaRepository.contarAudienciasPorUsuario());
            terminosMap = toMap(terminoRepository.contarTerminosPorUsuario());
        }

        Set<String> usuarios = new LinkedHashSet<>();
        usuarios.addAll(expedientesMap.keySet());
        usuarios.addAll(audienciasMap.keySet());
        usuarios.addAll(terminosMap.keySet());

        List<String> labels = new ArrayList<>();
        List<Integer> expedientes = new ArrayList<>();
        List<Integer> audiencias = new ArrayList<>();
        List<Integer> terminos = new ArrayList<>();

        for (String u : usuarios) {
            labels.add(u);
            expedientes.add(expedientesMap.getOrDefault(u, 0));
            audiencias.add(audienciasMap.getOrDefault(u, 0));
            terminos.add(terminosMap.getOrDefault(u, 0));
        }
        return Map.of("labels", labels, "expedientes", expedientes, "audiencias", audiencias, "terminos", terminos);
    }

    private Map<String, Object> obtenerDistribucionGerencias() {
        return processRows(expedienteRepository.contarExpedientesPorGerencia());
    }

    private Map<String, Object> obtenerTrabajoMensual(Long gerenciaId, List<Long> gerenciaIds, List<Integer> materiaIds) {
        if (gerenciaId != null) {
            if (materiaIds != null && !materiaIds.isEmpty()) {
                return processRows(expedienteRepository.contarExpedientesCompletadosPorMesYGerenciaYMateriaIn(gerenciaId, materiaIds));
            }
            return processRows(expedienteRepository.contarExpedientesCompletadosPorMes());
        } else if (gerenciaIds != null && !gerenciaIds.isEmpty()) {
            return processRows(expedienteRepository.contarExpedientesCompletadosPorMesYGerencias(gerenciaIds));
        } else {
            return processRows(expedienteRepository.contarExpedientesCompletadosPorMes());
        }
    }

    private Map<String, Object> processRows(List<Object[]> rows) {
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (Object[] row : rows) {
            labels.add(row[0] != null ? row[0].toString() : "N/A");
            values.add(row[1] != null ? ((Number) row[1]).longValue() : 0L);
        }
        return Map.of("labels", labels, "values", values);
    }

    private Map<String, Integer> toMap(List<Object[]> rows) {
        Map<String, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null)
                map.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        return map;
    }
}
