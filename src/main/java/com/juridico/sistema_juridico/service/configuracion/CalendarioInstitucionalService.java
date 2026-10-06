package com.juridico.sistema_juridico.service.configuracion;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.juridico.sistema_juridico.Entity.configuracion.CalendarioVersion;
import com.juridico.sistema_juridico.Entity.configuracion.DiaInhabil;
import com.juridico.sistema_juridico.Entity.configuracion.SuspensionPlazo;
import com.juridico.sistema_juridico.repository.configuracion.CalendarioVersionRepository;
import com.juridico.sistema_juridico.repository.configuracion.DiaInhabilRepository;
import com.juridico.sistema_juridico.repository.configuracion.SuspensionPlazoRepository;

@Service
public class CalendarioInstitucionalService {
    private final DiaInhabilRepository repository;
    private final SuspensionPlazoRepository suspensiones;
    private final CalendarioVersionRepository versiones;

    public CalendarioInstitucionalService(DiaInhabilRepository repository,
            SuspensionPlazoRepository suspensiones, CalendarioVersionRepository versiones) {
        this.repository = repository;
        this.suspensiones = suspensiones;
        this.versiones = versiones;
    }

    public List<DiaInhabil> listar() { return repository.findAllByOrderByFechaAsc(); }
    public List<SuspensionPlazo> listarSuspensiones() { return suspensiones.findAllByOrderByFechaInicioDesc(); }
    public List<CalendarioVersion> listarVersiones() { return versiones.findAll().stream()
            .sorted((a, b) -> b.getVersion().compareTo(a.getVersion())).toList(); }

    public boolean esInhabil(LocalDate fecha) {
        return snapshotActual().fechasInhabiles().contains(fecha);
    }

    @Transactional
    public DiaInhabil guardar(DiaInhabil dia, String actor) {
        if (dia.getFecha() == null || dia.getDescripcion() == null || dia.getDescripcion().isBlank()) throw new IllegalArgumentException("Captura fecha y motivo del día inhábil.");
        if (dia.getActivo() == null) dia.setActivo(true);
        DiaInhabil guardado = repository.save(dia);
        publicarVersion("Día inhábil agregado: " + dia.getFecha(), actor);
        return guardado;
    }

    @Transactional
    public void cambiarEstado(Integer id, boolean activo, String actor) {
        DiaInhabil dia = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Día inhábil no encontrado."));
        dia.setActivo(activo);
        repository.save(dia);
        publicarVersion((activo ? "Día inhábil activado: " : "Día inhábil inactivado: ") + dia.getFecha(), actor);
    }

    @Transactional
    public SuspensionPlazo guardarSuspension(SuspensionPlazo suspension, String actor) {
        if (suspension.getFechaInicio() == null || suspension.getFechaFin() == null) {
            throw new IllegalArgumentException("Captura la fecha inicial y final de la suspensión.");
        }
        if (suspension.getFechaFin().isBefore(suspension.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final de la suspensión no puede ser anterior a la inicial.");
        }
        if (suspension.getMotivo() == null || suspension.getMotivo().isBlank()) {
            throw new IllegalArgumentException("Captura el motivo institucional de la suspensión.");
        }
        suspension.setId(null);
        suspension.setActivo(true);
        suspension.setRegistradoPor(normalizarActor(actor));
        SuspensionPlazo guardada = suspensiones.save(suspension);
        publicarVersion("Suspensión publicada del " + suspension.getFechaInicio() + " al "
                + suspension.getFechaFin(), actor);
        return guardada;
    }

    @Transactional
    public void cambiarEstadoSuspension(Integer id, boolean activo, String actor) {
        SuspensionPlazo suspension = suspensiones.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Suspensión no encontrada."));
        suspension.setActivo(activo);
        suspensiones.save(suspension);
        publicarVersion((activo ? "Suspensión activada: " : "Suspensión inactivada: ")
                + suspension.getFechaInicio() + " a " + suspension.getFechaFin(), actor);
    }

    @Transactional
    public CalendarioSnapshot snapshotActual() {
        CalendarioVersion version = versiones.findTopByOrderByVersionDesc()
                .orElseGet(() -> crearVersion("Versión inicial del calendario", "SIAJ"));
        return new CalendarioSnapshot(version.getVersion(), deserializar(version.getFechasInhabiles()));
    }

    public CalendarioSnapshot desdeSnapshot(Integer version, String fechas) {
        return new CalendarioSnapshot(version == null ? 0 : version, deserializar(fechas));
    }

    private CalendarioVersion publicarVersion(String motivo, String actor) {
        return crearVersion(motivo, normalizarActor(actor));
    }

    private CalendarioVersion crearVersion(String motivo, String actor) {
        int siguiente = versiones.findTopByOrderByVersionDesc().map(v -> v.getVersion() + 1).orElse(1);
        CalendarioVersion nueva = CalendarioVersion.builder()
                .version(siguiente)
                .fechasInhabiles(serializar(fechasActivas()))
                .motivoCambio(motivo)
                .publicadoPor(normalizarActor(actor))
                .build();
        return versiones.save(nueva);
    }

    private Set<LocalDate> fechasActivas() {
        Set<LocalDate> fechas = repository.findAllByOrderByFechaAsc().stream()
                .filter(d -> Boolean.TRUE.equals(d.getActivo()))
                .map(DiaInhabil::getFecha)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        suspensiones.findByActivoTrueOrderByFechaInicioAsc().forEach(s -> {
            LocalDate cursor = s.getFechaInicio();
            while (!cursor.isAfter(s.getFechaFin())) {
                fechas.add(cursor);
                cursor = cursor.plusDays(1);
            }
        });
        return fechas;
    }

    public String serializar(Set<LocalDate> fechas) {
        if (fechas == null || fechas.isEmpty()) return "";
        return fechas.stream().sorted().map(LocalDate::toString).collect(Collectors.joining(","));
    }

    public Set<LocalDate> deserializar(String fechas) {
        if (fechas == null || fechas.isBlank()) return Set.of();
        return Arrays.stream(fechas.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(LocalDate::parse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizarActor(String actor) {
        return actor == null || actor.isBlank() ? "SIAJ" : actor.trim();
    }
}
