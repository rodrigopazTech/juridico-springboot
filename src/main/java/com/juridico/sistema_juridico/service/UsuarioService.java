package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.ActividadUsuarioRepository;
import com.juridico.sistema_juridico.repository.Usuarios.PushSubscriptionRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.procesal.AudienciaRepository;
import com.juridico.sistema_juridico.repository.procesal.TerminoRepository;
import com.juridico.sistema_juridico.exception.ConflictException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ActividadUsuarioRepository actividadUsuarioRepository;
    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;
    @Autowired
    private GerenciaRepository gerenciaRepository;
    @Autowired
    private MateriaRepository materiaRepository;
    @Autowired
    private OrganoJurisdiccionalRepository organoRepository;
    @Autowired
    private TipoAudienciaRepository tipoAudienciaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioIdentityService usuarioIdentityService;
    @Autowired
    private SessionRevocationService sessionRevocationService;
    @Autowired
    private ExpedienteRepository expedienteRepository;
    @Autowired
    private AudienciaRepository audienciaRepository;
    @Autowired
    private TerminoRepository terminoRepository;

    // --- USUARIOS ---

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    public Usuario obtenerPorUsername(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con el email: " + email));
    }

    public Page<Usuario> listarUsuariosPaginados(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    public Page<Gerencia> listarGerenciasPaginadas(Pageable pageable) {
        return gerenciaRepository.findAll(pageable);
    }

    public Page<OrganoJurisdiccional> listarOrganosPaginados(Pageable pageable) {
        return organoRepository.findAll(pageable);
    }

    public Page<TipoAudiencia> listarTiposAudienciaPaginados(Pageable pageable) {
        return tipoAudienciaRepository.findAll(pageable);
    }

    public List<RolUsuario> getRolesVisiblesPara(RolUsuario rolActor) {
        List<RolUsuario> todos = java.util.Arrays.asList(RolUsuario.values());
        switch (rolActor) {
            case ADMIN:
                return todos;
            case DIRECTOR_PRINCIPAL:
                return List.of(RolUsuario.DIRECTOR, RolUsuario.SUBDIRECTOR, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
            case DIRECTOR:
                return List.of(RolUsuario.SUBDIRECTOR, RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
            case SUBDIRECTOR:
                return List.of(RolUsuario.GERENTE, RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
            case GERENTE:
                return List.of(RolUsuario.JEFE_DEPTO, RolUsuario.ABOGADO);
            case JEFE_DEPTO:
                return List.of(RolUsuario.ABOGADO);
            case ABOGADO:
            default:
                return List.of();
        }
    }

    @Transactional
    public Usuario guardarUsuario(Usuario usuario, List<Integer> gerenciasIds, List<Integer> materiasIds) {

        validarDatosPrincipales(usuario, gerenciasIds);
        usuario.setEmail(validateOptionalEmail(usuario.getEmail()));

        Set<Materia> materiasSeleccionadas = new HashSet<>();
        if (materiasIds != null && !materiasIds.isEmpty()) {
            materiasSeleccionadas.addAll(materiaRepository.findAllById(materiasIds));
        }

        Set<Gerencia> gerenciasSeleccionadas = new HashSet<>();
        if (gerenciasIds != null && !gerenciasIds.isEmpty()) {
            gerenciasSeleccionadas.addAll(gerenciaRepository.findAllById(gerenciasIds));
        }

        String actorEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario actor = obtenerPorUsername(actorEmail);
        validarJerarquia(actor, usuario);

        // Si el rol asignado al usuario es directivo o admin, se le asignan automáticamente TODAS las gerencias y materias
        if (usuario.getRol() == RolUsuario.ADMIN || usuario.getRol() == RolUsuario.DIRECTOR_PRINCIPAL 
                || usuario.getRol() == RolUsuario.DIRECTOR || usuario.getRol() == RolUsuario.SUBDIRECTOR) {
            gerenciasSeleccionadas.clear();
            gerenciasSeleccionadas.addAll(gerenciaRepository.findByActivoTrueOrderByNombreAsc());
            materiasSeleccionadas.clear();
            materiasSeleccionadas.addAll(materiaRepository.findByActivoTrueOrderByNombreAsc());
        } else if (usuario.getRol() == RolUsuario.GERENTE || usuario.getRol() == RolUsuario.JEFE_DEPTO || usuario.getRol() == RolUsuario.ABOGADO) {
            if (usuario.getRol() == RolUsuario.GERENTE) {
                materiasSeleccionadas.clear();
                for (Gerencia g : gerenciasSeleccionadas) {
                    materiasSeleccionadas.addAll(materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(g.getId()));
                }
            }
        }

        if (usuario.getId() == null) {
            // CREACIÓN
            if (usuario.getEmail() != null && usuarioRepository.existsByEmailIgnoreCase(usuario.getEmail())) {
                throw new IllegalArgumentException("El correo electrónico ya está registrado.");
            }
            prepararIdentidadUsuarioNuevo(usuario);
            usuario.setMicrosoftLoginEnabled(false);

            // Aplicar asignaciones por default basadas en el rol del creador
            if (actor.getRol() == RolUsuario.GERENTE) {
                usuario.getGerencias().clear();
                usuario.getGerencias().addAll(actor.getGerencias());
                java.util.Set<Integer> gerenciasActorIds = actor.getGerencias().stream()
                        .map(Gerencia::getId)
                        .collect(java.util.stream.Collectors.toSet());
                materiasSeleccionadas.removeIf(m -> m.getGerencia() == null
                        || !gerenciasActorIds.contains(m.getGerencia().getId()));
            } else if (actor.getRol() == RolUsuario.JEFE_DEPTO) {
                usuario.getGerencias().clear();
                usuario.getGerencias().addAll(actor.getGerencias());
                Set<Integer> materiasActorIds = actor.getMaterias().stream()
                        .map(Materia::getId)
                        .collect(java.util.stream.Collectors.toSet());
                materiasSeleccionadas.removeIf(m -> !materiasActorIds.contains(m.getId()));
            } else {
                // Para creadores directivos/admin, se toman las gerenciasSeleccionadas
                usuario.getGerencias().clear();
                usuario.getGerencias().addAll(gerenciasSeleccionadas);
            }

            validarAsignacionOperativa(usuario.getRol(), usuario.getGerencias(), materiasSeleccionadas);

            usuario.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            usuario.setForcePasswordChange(true);
            usuario.setMaterias(materiasSeleccionadas);
            usuario.setCreadoPor(actor);
            Usuario guardado = usuarioRepository.saveAndFlush(usuario);
            registrarAsignacionUsuario(guardado, "Asignación inicial de usuario");
            return guardado;
        } else {
            // EDICIÓN
            Usuario existente = buscarPorId(usuario.getId());
            RolUsuario rolAnterior = existente.getRol();
            Set<Integer> gerenciasAnteriores = existente.getGerencias().stream()
                    .map(Gerencia::getId).collect(java.util.stream.Collectors.toSet());
            Set<Integer> materiasAnteriores = existente.getMaterias().stream()
                    .map(Materia::getId).collect(java.util.stream.Collectors.toSet());

            // Volver a validar jerarquía de actor sobre destino existente
            validarJerarquia(actor, existente);

            existente.setNombreCompleto(usuario.getNombreCompleto());
            String emailActualizado = usuario.getEmail();
            if (emailActualizado != null) {
                usuarioRepository.findByEmailIgnoreCase(emailActualizado).ifPresent(otro -> {
                    if (!otro.getId().equals(existente.getId())) {
                        throw new IllegalArgumentException("El correo electrónico ya está registrado.");
                    }
                });
            }
            if (!java.util.Objects.equals(normalizeEmail(existente.getEmail()), emailActualizado)) {
                limpiarVinculoMicrosoft(existente);
            }
            existente.setEmail(emailActualizado);
            existente.setPuesto(usuario.getPuesto());
            existente.setTelefonoInstitucional(usuario.getTelefonoInstitucional());
            existente.setTelefonoMovil(usuario.getTelefonoMovil());
            existente.setUbicacionOficina(usuario.getUbicacionOficina());
            actualizarIdentidadUsuarioExistente(existente, usuario);

            existente.setRol(usuario.getRol());

            if (actor.getRol() == RolUsuario.GERENTE || actor.getRol() == RolUsuario.JEFE_DEPTO) {
                Set<Integer> gerenciasActorIds = actor.getGerencias().stream()
                        .map(Gerencia::getId)
                        .collect(java.util.stream.Collectors.toSet());
                gerenciasSeleccionadas.removeIf(g -> !gerenciasActorIds.contains(g.getId()));
                if (gerenciasSeleccionadas.isEmpty()) {
                    gerenciasSeleccionadas.addAll(actor.getGerencias());
                }

                materiasSeleccionadas.removeIf(m -> m.getGerencia() == null
                        || !gerenciasSeleccionadas.stream().map(Gerencia::getId).toList().contains(m.getGerencia().getId()));

                if (actor.getRol() == RolUsuario.JEFE_DEPTO) {
                    Set<Integer> materiasActorIds = actor.getMaterias().stream()
                            .map(Materia::getId)
                            .collect(java.util.stream.Collectors.toSet());
                    materiasSeleccionadas.removeIf(m -> !materiasActorIds.contains(m.getId()));
                }
            }

            validarAsignacionOperativa(existente.getRol(), gerenciasSeleccionadas, materiasSeleccionadas);

            existente.getGerencias().clear();
            existente.getGerencias().addAll(gerenciasSeleccionadas);

            if (materiasIds != null || !materiasSeleccionadas.isEmpty()
                    || existente.getRol() == RolUsuario.ADMIN
                    || existente.getRol() == RolUsuario.DIRECTOR_PRINCIPAL
                    || existente.getRol() == RolUsuario.DIRECTOR
                    || existente.getRol() == RolUsuario.SUBDIRECTOR
                    || existente.getRol() == RolUsuario.GERENTE) {
                existente.getMaterias().clear();
                existente.getMaterias().addAll(materiasSeleccionadas);
            }

            boolean estabaActivo = Boolean.TRUE.equals(existente.getActivo());
            existente.setActivo(usuario.getActivo());

            if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
                existente.setForcePasswordChange(true);
            }

            Usuario guardado = usuarioRepository.save(existente);
            Set<Integer> gerenciasActuales = guardado.getGerencias().stream()
                    .map(Gerencia::getId).collect(java.util.stream.Collectors.toSet());
            Set<Integer> materiasActuales = guardado.getMaterias().stream()
                    .map(Materia::getId).collect(java.util.stream.Collectors.toSet());
            if (rolAnterior != guardado.getRol()
                    || !gerenciasAnteriores.equals(gerenciasActuales)
                    || !materiasAnteriores.equals(materiasActuales)) {
                registrarAsignacionUsuario(guardado, "Asignación actualizada");
            }
            if (estabaActivo && !Boolean.TRUE.equals(guardado.getActivo())) {
                sessionRevocationService.revokeAll(guardado, "USUARIO_SUSPENDIDO");
            }
            return guardado;
        }
    }

    public void validarPuedeAdministrarUsuario(Usuario actor, Usuario destino) {
        validarJerarquia(actor, destino);
    }

    @Transactional
    public Usuario cambiarEstadoUsuario(Integer id, boolean activo) {
        String actorEmail = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Usuario actor = obtenerPorUsername(actorEmail);
        Usuario usuario = buscarPorId(id);
        validarJerarquia(actor, usuario);

        boolean estabaActivo = Boolean.TRUE.equals(usuario.getActivo());
        usuario.setActivo(activo);
        Usuario guardado = usuarioRepository.save(usuario);
        if (estabaActivo && !activo) {
            sessionRevocationService.revokeAll(guardado, "USUARIO_SUSPENDIDO");
        }
        return guardado;
    }

    private void prepararIdentidadUsuarioNuevo(Usuario usuario) {
        usuario.setUsername(usuarioIdentityService.generateUsernameFromName(usuario));
        validarUsernameDisponible(usuario.getUsername(), null);
        usuario.setNumeroEmpleado(normalizeNumeroEmpleado(usuario.getNumeroEmpleado()));
        validarNumeroEmpleadoDisponible(usuario.getNumeroEmpleado(), null);
    }

    private void actualizarIdentidadUsuarioExistente(Usuario existente, Usuario cambios) {
        String username = usuarioIdentityService.normalizeUsername(cambios.getUsername());
        existente.setUsername(username != null ? username : existente.getUsername());
        validarUsernameDisponible(existente.getUsername(), existente.getId());
        existente.setNumeroEmpleado(normalizeNumeroEmpleado(cambios.getNumeroEmpleado()));
        validarNumeroEmpleadoDisponible(existente.getNumeroEmpleado(), existente.getId());
    }

    private void validarUsernameDisponible(String username, Integer usuarioIdActual) {
        usuarioRepository.findByUsernameIgnoreCase(username).ifPresent(u -> {
            if (usuarioIdActual == null || !u.getId().equals(usuarioIdActual)) {
                throw new IllegalArgumentException("El nombre de usuario ya está registrado.");
            }
        });
    }

    private void validarNumeroEmpleadoDisponible(String numeroEmpleado, Integer usuarioIdActual) {
        if (numeroEmpleado == null || numeroEmpleado.isBlank()) {
            return;
        }
        usuarioRepository.findByNumeroEmpleadoIgnoreCase(numeroEmpleado).ifPresent(u -> {
            if (usuarioIdActual == null || !u.getId().equals(usuarioIdActual)) {
                throw new IllegalArgumentException("El número de empleado ya está registrado.");
            }
        });
    }

    private void validarAsignacionOperativa(RolUsuario rol, Set<Gerencia> gerencias, Set<Materia> materias) {
        if (rol == RolUsuario.ADMIN || rol == RolUsuario.DIRECTOR_PRINCIPAL
                || rol == RolUsuario.DIRECTOR || rol == RolUsuario.SUBDIRECTOR) {
            return;
        }
        if (gerencias == null || gerencias.size() != 1) {
            throw new IllegalArgumentException("Selecciona una gerencia para el usuario operativo.");
        }
        Set<Integer> gerenciaIds = gerencias.stream().map(Gerencia::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (materias.stream().anyMatch(m -> m.getGerencia() == null
                || !gerenciaIds.contains(m.getGerencia().getId()))) {
            throw new IllegalArgumentException("Las materias seleccionadas deben pertenecer a la gerencia asignada.");
        }
        if ((rol == RolUsuario.JEFE_DEPTO || rol == RolUsuario.ABOGADO) && materias.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos una materia para el usuario.");
        }
    }

    private void registrarAsignacionUsuario(Usuario usuario, String titulo) {
        String gerencias = usuario.getGerencias().stream().map(Gerencia::getNombre).sorted()
                .collect(java.util.stream.Collectors.joining(", "));
        String materias = usuario.getMaterias().stream().map(Materia::getNombre).sorted()
                .collect(java.util.stream.Collectors.joining(", "));
        com.juridico.sistema_juridico.Entity.usuario.ActividadUsuario actividad =
                com.juridico.sistema_juridico.Entity.usuario.ActividadUsuario.builder()
                        .usuario(usuario)
                        .titulo(titulo)
                        .descripcion("Rol: " + usuario.getRol() + ". Gerencia: "
                                + (gerencias.isBlank() ? "Sin asignar" : gerencias)
                                + ". Materias: " + (materias.isBlank() ? "Sin asignar" : materias) + ".")
                        .tipoIcono("USER_TAG")
                        .fechaRegistro(LocalDateTime.now())
                        .build();
        actividadUsuarioRepository.save(actividad);
    }

    private String normalizeNumeroEmpleado(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String validateOptionalEmail(String value) {
        String email = normalizeEmail(value);
        if (email == null) {
            return null;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Captura un correo electrónico válido.");
        }
        return email;
    }

    private void validarDatosPrincipales(Usuario usuario, List<Integer> gerenciasIds) {
        if (usuario.getNombreCompleto() == null || usuario.getNombreCompleto().isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        usuario.setNombreCompleto(usuario.getNombreCompleto().trim());
        if (usuario.getNumeroEmpleado() == null || usuario.getNumeroEmpleado().isBlank()) {
            throw new IllegalArgumentException("El número de empleado es obligatorio.");
        }
        if (usuario.getRol() == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        if (gerenciasIds == null || gerenciasIds.size() != 1) {
            throw new IllegalArgumentException("Selecciona el área del usuario.");
        }
    }

    private void limpiarVinculoMicrosoft(Usuario usuario) {
        usuario.setMicrosoftObjectId(null);
        usuario.setMicrosoftUserPrincipalName(null);
        usuario.setMicrosoftDisplayName(null);
        usuario.setMicrosoftJobTitle(null);
        usuario.setMicrosoftBusinessPhones(null);
        usuario.setMicrosoftMobilePhone(null);
        usuario.setMicrosoftOfficeLocation(null);
        usuario.setMicrosoftLinkedAt(null);
        usuario.setMicrosoftLastSyncAt(null);
        usuario.setMicrosoftLoginEnabled(false);
    }


    @Transactional
    public void eliminarUsuario(Integer id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se puede eliminar: El usuario no existe.");
        }
        Usuario destino = buscarPorId(id);
        String actorEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario actor = obtenerPorUsername(actorEmail);
        validarJerarquia(actor, destino);

        if (expedienteRepository.countByAbogadoResponsableId(id) > 0) {
            throw new ConflictException("No se puede eliminar el usuario porque tiene expedientes asociados.");
        }
        if (audienciaRepository.countByAbogadoCompareceIdOrUsuarioDelegadoId(id, id) > 0) {
            throw new ConflictException("No se puede eliminar el usuario porque tiene audiencias asociadas.");
        }
        if (terminoRepository.countByAbogadoResponsableId(id) > 0) {
            throw new ConflictException("No se puede eliminar el usuario porque tiene terminos asociados.");
        }
        if (usuarioRepository.countByCreadoPorId(id) > 0) {
            throw new ConflictException("No se puede eliminar el usuario porque existen otras cuentas creadas por este usuario.");
        }

        // Regla específica: DIRECTOR sin permiso de borrar directivos
        if (actor.getRol() == RolUsuario.DIRECTOR) {
            if (destino.getRol() == RolUsuario.ADMIN ||
                destino.getRol() == RolUsuario.DIRECTOR_PRINCIPAL ||
                destino.getRol() == RolUsuario.DIRECTOR) {
                throw new RuntimeException("El DIRECTOR no tiene permisos para eliminar directivos de alto rango (ADMIN, DIRECTOR PRINCIPAL o DIRECTOR).");
            }
        }

        // Eliminar en cascada los registros asociados (actividades y suscripciones push)
        actividadUsuarioRepository.deleteByUsuarioId(id);
        pushSubscriptionRepository.deleteByUsuarioId(id);

        try {
            usuarioRepository.deleteById(id);
            usuarioRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("No se puede eliminar el usuario porque tiene registros historicos relacionados.");
        }
    }

    private void validarJerarquia(Usuario actor, Usuario destino) {
        if (actor.getRol() == RolUsuario.ADMIN) {
            return;
        }
        if (actor.getRol() == RolUsuario.DIRECTOR_PRINCIPAL) {
            if (destino.getRol() == RolUsuario.ADMIN || destino.getRol() == RolUsuario.DIRECTOR_PRINCIPAL) {
                throw new RuntimeException("El DIRECTOR_PRINCIPAL no puede administrar a ADMIN o DIRECTOR_PRINCIPAL.");
            }
            return;
        }
        if (actor.getRol() == RolUsuario.DIRECTOR) {
            if (destino.getRol() == RolUsuario.ADMIN || destino.getRol() == RolUsuario.DIRECTOR_PRINCIPAL || destino.getRol() == RolUsuario.DIRECTOR) {
                throw new RuntimeException("El DIRECTOR no puede administrar a ADMIN, DIRECTOR_PRINCIPAL o DIRECTOR.");
            }
            return;
        }
        if (actor.getRol() == RolUsuario.SUBDIRECTOR) {
            if (destino.getRol() == RolUsuario.ADMIN || destino.getRol() == RolUsuario.DIRECTOR_PRINCIPAL || destino.getRol() == RolUsuario.DIRECTOR || destino.getRol() == RolUsuario.SUBDIRECTOR) {
                throw new RuntimeException("El SUBDIRECTOR no puede administrar a ADMIN, DIRECTOR_PRINCIPAL, DIRECTOR o SUBDIRECTOR.");
            }
            return;
        }
        if (actor.getRol() == RolUsuario.GERENTE) {
            if (destino.getRol() != RolUsuario.JEFE_DEPTO && destino.getRol() != RolUsuario.ABOGADO) {
                throw new RuntimeException("El GERENTE solo puede administrar JEFE_DEPTO y ABOGADO.");
            }
            if (actor.getGerencias() != null && destino.getGerencias() != null) {
                boolean hasCommon = actor.getGerencias().stream().anyMatch(gActor -> 
                    destino.getGerencias().stream().anyMatch(gDest -> gDest.getId().equals(gActor.getId()))
                );
                if (!hasCommon && !destino.getGerencias().isEmpty()) {
                    throw new RuntimeException("El GERENTE solo puede administrar usuarios de su gerencia.");
                }
            }
            return;
        }
        if (actor.getRol() == RolUsuario.JEFE_DEPTO) {
            if (destino.getRol() != RolUsuario.ABOGADO) {
                throw new RuntimeException("El JEFE_DEPTO solo puede administrar ABOGADO.");
            }
            if (actor.getGerencias() != null && destino.getGerencias() != null) {
                boolean hasCommon = actor.getGerencias().stream().anyMatch(gActor -> 
                    destino.getGerencias().stream().anyMatch(gDest -> gDest.getId().equals(gActor.getId()))
                );
                if (!hasCommon && !destino.getGerencias().isEmpty()) {
                    throw new RuntimeException("El JEFE_DEPTO solo puede administrar usuarios de su gerencia.");
                }
            }
            if (actor.getMaterias() != null && destino.getMaterias() != null) {
                boolean hasCommonMat = actor.getMaterias().stream().anyMatch(mActor -> 
                    destino.getMaterias().stream().anyMatch(mDest -> mDest.getId().equals(mActor.getId()))
                );
                if (!hasCommonMat && !destino.getMaterias().isEmpty()) {
                    throw new RuntimeException("El JEFE_DEPTO solo puede administrar usuarios de su materia.");
                }
            }
            return;
        }
        if (actor.getRol() == RolUsuario.ABOGADO) {
            throw new RuntimeException("El ABOGADO no puede administrar a otros usuarios.");
        }
    }

    // --- GERENCIAS ---

    public List<Gerencia> listarGerencias() {
        return gerenciaRepository.findAll();
    }

    public Gerencia buscarGerenciaPorId(Integer id) {
        return gerenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gerencia no encontrada con ID: " + id));
    }

    public Gerencia guardarGerencia(Gerencia gerencia) {
        if (gerencia.getNombre() != null) {
            String nombreNormalizado = gerencia.getNombre().toLowerCase(Locale.ROOT);
            if (nombreNormalizado.contains("dirección de asuntos jurídicos")
                    || nombreNormalizado.contains("direccion de asuntos juridicos")) {
                throw new IllegalArgumentException(
                        "La Dirección de Asuntos Jurídicos es una unidad transversal y no debe registrarse como gerencia.");
            }
        }
        return gerenciaRepository.save(gerencia);
    }

    // --- MATERIAS ---

    public Materia guardarMateria(Materia materia) {
        return materiaRepository.save(materia);
    }

    public void eliminarMateria(Integer id) {
        if (!materiaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Materia no encontrada.");
        }
        materiaRepository.deleteById(id);
    }

    // --- ÓRGANOS JURISDICCIONALES ---

    public OrganoJurisdiccional buscarOrganoPorId(Integer id) {
        return organoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Órgano Jurisdiccional no encontrado con ID: " + id));
    }

    @Transactional
    public OrganoJurisdiccional guardarOrgano(OrganoJurisdiccional organo) {
        if (organo.getId() != null) {
            OrganoJurisdiccional existente = buscarOrganoPorId(organo.getId());
            existente.setNombre(organo.getNombre());
            existente.setTipo(organo.getTipo());
            existente.setSede(organo.getSede());
            existente.setActivo(organo.getActivo());
            existente.setUpdatedAt(LocalDateTime.now());
            return organoRepository.save(existente);
        }
        organo.setCreatedAt(LocalDateTime.now());
        organo.setUpdatedAt(LocalDateTime.now());
        return organoRepository.save(organo);
    }

    @Transactional
    public void eliminarOrgano(Integer id) {
        if (!organoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Órgano Jurisdiccional no encontrado.");
        }
        organoRepository.deleteById(id);
    }

    // --- TIPOS DE AUDIENCIA ---

    public TipoAudiencia buscarTipoAudienciaPorId(Integer id) {
        return tipoAudienciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de Audiencia no encontrado con ID: " + id));
    }

    @Transactional
    public TipoAudiencia guardarTipoAudiencia(TipoAudiencia tipo) {
        if (tipo.getId() != null) {
            TipoAudiencia existente = buscarTipoAudienciaPorId(tipo.getId());
            existente.setNombre(tipo.getNombre());
            existente.setActivo(tipo.getActivo());
            return tipoAudienciaRepository.save(existente);
        }
        return tipoAudienciaRepository.save(tipo);
    }

    @Transactional
    public void eliminarTipoAudiencia(Integer id) {
        if (!tipoAudienciaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tipo de Audiencia no encontrado.");
        }
        tipoAudienciaRepository.deleteById(id);
    }

}
