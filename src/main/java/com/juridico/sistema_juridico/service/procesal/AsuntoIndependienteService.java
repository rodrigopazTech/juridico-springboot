package com.juridico.sistema_juridico.service.procesal;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.EstadoAsuntoIndependiente;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.enums.TipoReferenciaAsunto;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.procesal.AsuntoIndependiente;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AsuntoIndependienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsuntoIndependienteService {
    private final AsuntoIndependienteRepository asuntoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final TerminoRepository terminoRepository;
    private final AudienciaRepository audienciaRepository;
    private final MateriaRepository materiaRepository;
    private final GerenciaRepository gerenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<AsuntoIndependiente> listarActivos(Usuario actor) {
        return asuntoRepository.findByEstadoOrderByCreatedAtDesc(EstadoAsuntoIndependiente.ACTIVO).stream()
                .filter(asunto -> estaEnAlcance(asunto, actor))
                .toList();
    }

    @Transactional(readOnly = true)
    public AsuntoIndependiente obtenerActivo(Long id) {
        AsuntoIndependiente asunto = asuntoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El Asunto Independiente no existe."));
        if (asunto.getEstado() != EstadoAsuntoIndependiente.ACTIVO) {
            throw new BadRequestException("El Asunto Independiente ya no está activo.");
        }
        return asunto;
    }

    @Transactional(readOnly = true)
    public void validarAcceso(AsuntoIndependiente asunto, Usuario actor) {
        validarAlcance(asunto, actor);
    }

    @Transactional
    public AsuntoIndependiente resolverOCrear(Long asuntoId, TipoReferenciaAsunto tipoReferencia,
            String referencia, LocalDate fechaReferencia, Integer materiaId, Integer gerenciaId,
            String descripcion, String parteInteresada, Integer responsableId, Usuario actor) {
        if (asuntoId != null) {
            AsuntoIndependiente existente = obtenerActivo(asuntoId);
            validarAlcance(existente, actor);
            return existente;
        }
        if (referencia == null || referencia.isBlank()) throw new BadRequestException("La referencia es obligatoria.");
        if (materiaId == null) throw new BadRequestException("La materia es obligatoria.");
        if (gerenciaId == null) throw new BadRequestException("La gerencia es obligatoria.");
        if (descripcion == null || descripcion.isBlank()) throw new BadRequestException("La descripción es obligatoria.");
        if (parteInteresada == null || parteInteresada.isBlank()) throw new BadRequestException("La parte interesada es obligatoria.");

        Materia materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> new ResourceNotFoundException("La materia seleccionada no existe."));
        Gerencia gerencia = gerenciaRepository.findById(gerenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("La gerencia seleccionada no existe."));
        String referenciaNormalizada = referencia.trim();
        asuntoRepository.findFirstByGerenciaIdAndReferenciaIgnoreCase(gerenciaId, referenciaNormalizada)
                .ifPresent(duplicado -> {
                    throw new BadRequestException("Ya existe el Asunto Independiente " + duplicado.getFolioSiaj()
                            + " con esa referencia. Selecciónelo para reutilizarlo.");
                });
        if (materia.getGerencia() != null && !materia.getGerencia().getId().equals(gerencia.getId())) {
            throw new BadRequestException("La materia no pertenece a la gerencia seleccionada.");
        }
        Usuario responsable = responsableId == null ? actor : usuarioRepository.findById(responsableId)
                .orElseThrow(() -> new ResourceNotFoundException("El responsable seleccionado no existe."));

        AsuntoIndependiente candidato = AsuntoIndependiente.builder()
                .materia(materia).gerencia(gerencia).responsable(responsable).build();
        validarAlcance(candidato, actor);
        if (!puedeAsignarResponsable(actor, responsable, materia, gerencia)) {
            throw new BadRequestException("El responsable seleccionado no pertenece al alcance permitido.");
        }

        AsuntoIndependiente asunto = AsuntoIndependiente.builder()
                .folioSiaj(generarFolio())
                .tipoReferencia(tipoReferencia == null ? TipoReferenciaAsunto.OFICIO : tipoReferencia)
                .referencia(referenciaNormalizada)
                .fechaReferencia(fechaReferencia == null ? LocalDate.now() : fechaReferencia)
                .materia(materia)
                .gerencia(gerencia)
                .responsable(responsable)
                .descripcion(descripcion.trim())
                .parteInteresada(parteInteresada.trim())
                .estado(EstadoAsuntoIndependiente.ACTIVO)
                .build();
        AsuntoIndependiente guardado = asuntoRepository.save(asunto);
        auditoriaService.registrarActividadGeneral(actor, "Asunto Independiente Creado",
                "Se creó " + guardado.getFolioSiaj() + " con referencia " + guardado.getReferencia() + ".", "CHECK");
        return guardado;
    }

    @Transactional
    public AsuntoIndependiente vincularAExpediente(Long asuntoId, UUID expedienteId, String motivo, Usuario actor) {
        AsuntoIndependiente asunto = obtenerActivo(asuntoId);
        validarAlcance(asunto, actor);
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new ResourceNotFoundException("El expediente destino no existe."));
        validarAlcance(expediente, actor);
        if (expediente.getGerencia() != null && asunto.getGerencia() != null
                && !expediente.getGerencia().getId().equals(asunto.getGerencia().getId())) {
            throw new BadRequestException("El expediente debe pertenecer a la misma gerencia del asunto.");
        }

        List<Termino> terminos = terminoRepository.findByAsuntoIndependienteId(asuntoId);
        terminos.forEach(t -> { t.setExpediente(expediente); t.setAsuntoIndependiente(null); });
        terminoRepository.saveAll(terminos);

        List<Audiencia> audiencias = audienciaRepository.findByAsuntoIndependienteId(asuntoId);
        audiencias.forEach(a -> { a.setExpediente(expediente); a.setAsuntoIndependiente(null); });
        audienciaRepository.saveAll(audiencias);

        asunto.setEstado(EstadoAsuntoIndependiente.VINCULADO);
        asunto.setExpedienteDestino(expediente);
        asunto.setFechaVinculacion(LocalDateTime.now());
        asunto.setVinculadoPor(actor);
        asunto.setMotivoVinculacion(motivo == null || motivo.isBlank() ? "Vinculación operativa" : motivo.trim());
        AsuntoIndependiente guardado = asuntoRepository.save(asunto);
        auditoriaService.registrar(expediente, "Asunto Independiente Vinculado",
                "Se vinculó " + asunto.getFolioSiaj() + " con " + terminos.size() + " término(s) y "
                        + audiencias.size() + " audiencia(s). Motivo: " + asunto.getMotivoVinculacion() + ".",
                "LINK", actor);
        return guardado;
    }

    private synchronized String generarFolio() {
        int anio = LocalDate.now().getYear();
        long consecutivo = asuntoRepository.contarPorAnio(anio) + 1;
        String candidato;
        do {
            candidato = String.format("AI-%d-%04d", anio, consecutivo++);
        } while (asuntoRepository.findByFolioSiaj(candidato).isPresent());
        return candidato;
    }

    private void validarAlcance(AsuntoIndependiente asunto, Usuario actor) {
        if (!estaEnAlcance(asunto, actor)) {
            throw new BadRequestException("El Asunto Independiente no pertenece a su alcance operativo.");
        }
    }

    private boolean estaEnAlcance(AsuntoIndependiente asunto, Usuario actor) {
        if (actor == null || actor.getRol() == null) return false;
        RolUsuario rol = actor.getRol();
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR) return true;
        if (rol == RolUsuario.ABOGADO) {
            return asunto.getResponsable() != null && asunto.getResponsable().getId().equals(actor.getId());
        }
        if (asunto.getGerencia() == null || actor.getGerencias() == null
                || actor.getGerencias().stream().noneMatch(g -> g.getId().equals(asunto.getGerencia().getId()))) {
            return false;
        }
        return rol != RolUsuario.JEFE_DEPTO || (asunto.getMateria() != null && actor.tieneMateria(asunto.getMateria().getId()));
    }

    private void validarAlcance(Expediente expediente, Usuario actor) {
        if (actor == null || actor.getRol() == null) throw new BadRequestException("Usuario sin alcance operativo.");
        RolUsuario rol = actor.getRol();
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR) return;
        if (rol == RolUsuario.ABOGADO) {
            if (expediente.getAbogadoResponsable() == null
                    || !expediente.getAbogadoResponsable().getId().equals(actor.getId())) {
                throw new BadRequestException("El expediente destino no está asignado al usuario actual.");
            }
            return;
        }
        if (expediente.getGerencia() == null || actor.getGerencias() == null
                || actor.getGerencias().stream().noneMatch(g -> g.getId().equals(expediente.getGerencia().getId()))) {
            throw new BadRequestException("El expediente destino no pertenece a su gerencia.");
        }
        if (rol == RolUsuario.JEFE_DEPTO && (expediente.getMateria() == null
                || !actor.tieneMateria(expediente.getMateria().getId()))) {
            throw new BadRequestException("El expediente destino no pertenece a sus materias asignadas.");
        }
    }

    private boolean puedeAsignarResponsable(Usuario actor, Usuario responsable, Materia materia, Gerencia gerencia) {
        if (responsable == null || !Boolean.TRUE.equals(responsable.getActivo())) return false;
        if (actor.getRol() == RolUsuario.ABOGADO) return responsable.getId().equals(actor.getId());
        if (responsable.getGerencias() == null
                || responsable.getGerencias().stream().noneMatch(g -> g.getId().equals(gerencia.getId()))) return false;
        return responsable.getRol() != RolUsuario.ABOGADO || responsable.tieneMateria(materia.getId());
    }
}
