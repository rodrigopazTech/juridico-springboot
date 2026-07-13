package com.juridico.sistema_juridico.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.NivelConfiguracionAlerta;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.enums.TipoEventoAlerta;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.dto.request.configuracion.ConfiguracionAlertaRequest;
import com.juridico.sistema_juridico.dto.request.configuracion.ConfiguracionSemaforoRequest;
import com.juridico.sistema_juridico.dto.response.configuracion.ConfiguracionAlertaResponse;
import com.juridico.sistema_juridico.dto.response.configuracion.ConfiguracionSemaforoResponse;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ForbiddenException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.service.configuracion.ConfiguracionAlertaService;
import com.juridico.sistema_juridico.service.configuracion.ConfiguracionSemaforoService;

@RestController
@RequestMapping("/api/configuracion-alertas")
public class ConfiguracionAlertasController {

    private final ConfiguracionAlertaService configuracionAlertaService;
    private final ConfiguracionSemaforoService configuracionSemaforoService;
    private final UsuarioRepository usuarioRepository;
    private final GerenciaRepository gerenciaRepository;
    private final TerminoRepository terminoRepository;
    private final AudienciaRepository audienciaRepository;

    public ConfiguracionAlertasController(
            ConfiguracionAlertaService configuracionAlertaService,
            ConfiguracionSemaforoService configuracionSemaforoService,
            UsuarioRepository usuarioRepository,
            GerenciaRepository gerenciaRepository,
            TerminoRepository terminoRepository,
            AudienciaRepository audienciaRepository) {
        this.configuracionAlertaService = configuracionAlertaService;
        this.configuracionSemaforoService = configuracionSemaforoService;
        this.usuarioRepository = usuarioRepository;
        this.gerenciaRepository = gerenciaRepository;
        this.terminoRepository = terminoRepository;
        this.audienciaRepository = audienciaRepository;
    }

    @GetMapping("/administrativas")
    public ResponseEntity<List<ConfiguracionAlertaResponse>> obtenerAdministrativas(
            @RequestParam TipoEventoAlerta tipoEvento,
            @RequestParam NivelConfiguracionAlerta nivel,
            @RequestParam(required = false) Integer gerenciaId) {
        Usuario actor = getUsuarioActual();
        Gerencia gerencia = validarAdministracion(actor, nivel, gerenciaId);
        return ResponseEntity.ok(configuracionAlertaService.obtenerConfiguracion(
                tipoEvento, nivel, gerencia, null, null, null));
    }

    @PutMapping("/administrativas")
    public ResponseEntity<ConfiguracionAlertaResponse> guardarAdministrativas(
            @RequestParam NivelConfiguracionAlerta nivel,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestBody ConfiguracionAlertaRequest request) {
        validarRequest(request);
        Usuario actor = getUsuarioActual();
        Gerencia gerencia = validarAdministracion(actor, nivel, gerenciaId);
        return ResponseEntity.ok(configuracionAlertaService.reemplazarConfiguracion(
                request.tipoEvento(), nivel, gerencia, null, null, null,
                request.diasAnticipacion(), Boolean.TRUE.equals(request.obligatoria())));
    }

    @GetMapping("/personales")
    public ResponseEntity<List<ConfiguracionAlertaResponse>> obtenerPersonales(
            @RequestParam TipoEventoAlerta tipoEvento) {
        Usuario actor = getUsuarioActual();
        return ResponseEntity.ok(configuracionAlertaService.obtenerConfiguracion(
                tipoEvento, NivelConfiguracionAlerta.USUARIO, null, actor, null, null));
    }

    @PutMapping("/personales")
    public ResponseEntity<ConfiguracionAlertaResponse> guardarPersonales(
            @RequestBody ConfiguracionAlertaRequest request) {
        validarRequest(request);
        Usuario actor = getUsuarioActual();
        return ResponseEntity.ok(configuracionAlertaService.reemplazarConfiguracion(
                request.tipoEvento(), NivelConfiguracionAlerta.USUARIO, null, actor, null, null,
                request.diasAnticipacion(), false));
    }

    @GetMapping("/asuntos/{tipoEvento}/{asuntoId}")
    public ResponseEntity<List<ConfiguracionAlertaResponse>> obtenerAsunto(
            @PathVariable TipoEventoAlerta tipoEvento,
            @PathVariable String asuntoId) {
        validarAccesoAsunto(getUsuarioActual(), tipoEvento, asuntoId);
        return ResponseEntity.ok(configuracionAlertaService.obtenerConfiguracion(
                tipoEvento, NivelConfiguracionAlerta.ASUNTO, null, null, tipoEvento.name(), asuntoId));
    }

    @PutMapping("/asuntos/{tipoEvento}/{asuntoId}")
    public ResponseEntity<ConfiguracionAlertaResponse> guardarAsunto(
            @PathVariable TipoEventoAlerta tipoEvento,
            @PathVariable String asuntoId,
            @RequestBody ConfiguracionAlertaRequest request) {
        validarRequest(request);
        if (request.tipoEvento() != tipoEvento) {
            throw new BadRequestException("El tipo de evento no coincide con el asunto.");
        }
        validarAccesoAsunto(getUsuarioActual(), tipoEvento, asuntoId);
        return ResponseEntity.ok(configuracionAlertaService.reemplazarConfiguracion(
                tipoEvento, NivelConfiguracionAlerta.ASUNTO, null, null, tipoEvento.name(), asuntoId,
                request.diasAnticipacion(), false));
    }

    @GetMapping("/semaforos")
    public ResponseEntity<ConfiguracionSemaforoResponse> obtenerSemaforo(
            @RequestParam TipoEventoAlerta tipoEvento,
            @RequestParam(required = false) Integer gerenciaId) {
        Usuario actor = getUsuarioActual();
        Gerencia gerencia = gerenciaId != null ? getGerencia(gerenciaId) : actor.getGerencia();
        return ResponseEntity.ok(configuracionSemaforoService.obtener(tipoEvento, gerencia));
    }

    @PutMapping("/semaforos")
    public ResponseEntity<ConfiguracionSemaforoResponse> guardarSemaforo(
            @RequestParam NivelConfiguracionAlerta nivel,
            @RequestParam(required = false) Integer gerenciaId,
            @RequestBody ConfiguracionSemaforoRequest request) {
        if (request == null || request.tipoEvento() == null) {
            throw new BadRequestException("El tipo de evento es obligatorio.");
        }
        Usuario actor = getUsuarioActual();
        Gerencia gerencia = validarAdministracion(actor, nivel, gerenciaId);
        return ResponseEntity.ok(configuracionSemaforoService.guardar(
                request.tipoEvento(), nivel, gerencia, request.rojoHasta(), request.amarilloHasta()));
    }

    private Gerencia validarAdministracion(Usuario actor, NivelConfiguracionAlerta nivel, Integer gerenciaId) {
        if (nivel == NivelConfiguracionAlerta.SISTEMA) {
            if (actor.getRol() != RolUsuario.ADMIN) {
                throw new ForbiddenException("Solo un administrador puede modificar la configuración del sistema.");
            }
            return null;
        }
        if (nivel != NivelConfiguracionAlerta.GERENCIA) {
            throw new BadRequestException("Este endpoint solo acepta configuración de sistema o gerencia.");
        }
        Gerencia gerencia = getGerencia(gerenciaId);
        if (actor.getRol() == RolUsuario.ADMIN) {
            return gerencia;
        }
        if (actor.getRol() != RolUsuario.GERENTE || actor.getGerencia() == null
                || !actor.getGerencia().getId().equals(gerencia.getId())) {
            throw new ForbiddenException("Solo el gerente del área puede modificar esta configuración.");
        }
        return gerencia;
    }

    private void validarAccesoAsunto(Usuario actor, TipoEventoAlerta tipoEvento, String asuntoId) {
        if (actor.getRol() == RolUsuario.ADMIN || actor.getRol() == RolUsuario.DIRECTOR_PRINCIPAL
                || actor.getRol() == RolUsuario.DIRECTOR || actor.getRol() == RolUsuario.SUBDIRECTOR) {
            return;
        }
        if (tipoEvento == TipoEventoAlerta.TERMINO) {
            Termino termino = terminoRepository.findById(parseId(asuntoId))
                    .orElseThrow(() -> new ResourceNotFoundException("Término no encontrado."));
            validarResponsableOGerencia(actor, termino.getAbogadoResponsable(), termino.getGerenciaReferencia());
            return;
        }
        Audiencia audiencia = audienciaRepository.findById(parseId(asuntoId))
                .orElseThrow(() -> new ResourceNotFoundException("Audiencia no encontrada."));
        Usuario responsable = audiencia.getAbogadoComparece() != null
                ? audiencia.getAbogadoComparece()
                : audiencia.getExpediente().getAbogadoResponsable();
        validarResponsableOGerencia(actor, responsable, audiencia.getExpediente().getGerencia());
    }

    private void validarResponsableOGerencia(Usuario actor, Usuario responsable, Gerencia gerencia) {
        if (responsable != null && responsable.getId().equals(actor.getId())) {
            return;
        }
        boolean supervisorDeGerencia = (actor.getRol() == RolUsuario.GERENTE || actor.getRol() == RolUsuario.JEFE_DEPTO)
                && actor.getGerencia() != null && gerencia != null
                && actor.getGerencia().getId().equals(gerencia.getId());
        if (!supervisorDeGerencia) {
            throw new ForbiddenException("No tienes permisos para configurar alertas de este asunto.");
        }
    }

    private Integer parseId(String asuntoId) {
        try {
            return Integer.valueOf(asuntoId);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("El identificador del asunto no es válido.");
        }
    }

    private Gerencia getGerencia(Integer gerenciaId) {
        if (gerenciaId == null) {
            throw new BadRequestException("La gerencia es obligatoria.");
        }
        return gerenciaRepository.findById(gerenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("Gerencia no encontrada."));
    }

    private Usuario getUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado."));
    }

    private void validarRequest(ConfiguracionAlertaRequest request) {
        if (request == null || request.tipoEvento() == null) {
            throw new BadRequestException("El tipo de evento es obligatorio.");
        }
        Set<Integer> dias = request.diasAnticipacion();
        if (dias == null) {
            throw new BadRequestException("Los días de anticipación son obligatorios.");
        }
    }
}
