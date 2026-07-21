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
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            if (gerenciasSeleccionadas.size() > 1) {
                Gerencia primera = gerenciasSeleccionadas.iterator().next();
                gerenciasSeleccionadas.clear();
                gerenciasSeleccionadas.add(primera);
            }
            if (usuario.getRol() == RolUsuario.GERENTE) {
                materiasSeleccionadas.clear();
                for (Gerencia g : gerenciasSeleccionadas) {
                    materiasSeleccionadas.addAll(materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(g.getId()));
                }
            }
        }

        if (usuario.getId() == null) {
            // CREACIÓN
            if (usuarioRepository.existsByEmail(usuario.getEmail())) {
                throw new RuntimeException("El correo electrónico ya está registrado.");
            }

            // Aplicar asignaciones por default basadas en el rol del creador
            if (actor.getRol() == RolUsuario.GERENTE) {
                usuario.getGerencias().clear();
                usuario.getGerencias().addAll(actor.getGerencias());
                java.util.Set<Integer> gerenciasActorIds = actor.getGerencias().stream()
                        .map(Gerencia::getId)
                        .collect(java.util.stream.Collectors.toSet());
                materiasSeleccionadas.removeIf(m -> m.getGerencia() == null
                        || !gerenciasActorIds.contains(m.getGerencia().getId()));
                if (materiasSeleccionadas.isEmpty()) {
                    for (Gerencia g : actor.getGerencias()) {
                        materiasSeleccionadas.addAll(materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(g.getId()));
                    }
                }
            } else if (actor.getRol() == RolUsuario.JEFE_DEPTO) {
                usuario.getGerencias().clear();
                usuario.getGerencias().addAll(actor.getGerencias());
                materiasSeleccionadas.clear();
                materiasSeleccionadas.addAll(actor.getMaterias());
            } else {
                // Para creadores directivos/admin, se toman las gerenciasSeleccionadas
                usuario.getGerencias().clear();
                usuario.getGerencias().addAll(gerenciasSeleccionadas);
                
                // Si no se especificaron materias y no es directivo, auto-asignar todas las materias de las gerencias asignadas
                if ((materiasIds == null || materiasIds.isEmpty()) && !gerenciasSeleccionadas.isEmpty()
                    && usuario.getRol() != RolUsuario.ADMIN && usuario.getRol() != RolUsuario.DIRECTOR_PRINCIPAL 
                    && usuario.getRol() != RolUsuario.DIRECTOR && usuario.getRol() != RolUsuario.SUBDIRECTOR) {
                    for (Gerencia g : gerenciasSeleccionadas) {
                        List<Materia> materiasGerencia = materiaRepository.findByGerenciaIdAndActivoTrueOrderByNombreAsc(g.getId());
                        materiasSeleccionadas.addAll(materiasGerencia);
                    }
                }
            }

            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setMaterias(materiasSeleccionadas);
            usuario.setCreadoPor(actor);
            return usuarioRepository.save(usuario);
        } else {
            // EDICIÓN
            Usuario existente = buscarPorId(usuario.getId());

            // Volver a validar jerarquía de actor sobre destino existente
            validarJerarquia(actor, existente);

            existente.setNombreCompleto(usuario.getNombreCompleto());
            existente.setEmail(usuario.getEmail());

            if (actor.getRol() == RolUsuario.GERENTE) {
                // Gerente puede editar rol (entre JEFE_DEPTO y ABOGADO) y materias, pero no gerencia
                existente.setRol(usuario.getRol());
                if (materiasIds != null) {
                    existente.getMaterias().clear();
                    existente.getMaterias().addAll(materiasSeleccionadas);
                }
            } else if (actor.getRol() == RolUsuario.JEFE_DEPTO) {
                // Jefe de Departamento no puede cambiar rol, gerencia ni materias del usuario
            } else {
                // Roles directivos/admin editan todo
                existente.setRol(usuario.getRol());
                existente.getGerencias().clear();
                existente.getGerencias().addAll(gerenciasSeleccionadas);
                if (materiasIds != null || existente.getRol() == RolUsuario.ADMIN 
                        || existente.getRol() == RolUsuario.DIRECTOR_PRINCIPAL 
                        || existente.getRol() == RolUsuario.DIRECTOR 
                        || existente.getRol() == RolUsuario.SUBDIRECTOR
                        || existente.getRol() == RolUsuario.GERENTE) {
                    existente.getMaterias().clear();
                    existente.getMaterias().addAll(materiasSeleccionadas);
                }
            }

            existente.setActivo(usuario.getActivo());

            if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
                existente.setForcePasswordChange(true);
            }

            return usuarioRepository.save(existente);
        }
    }

    @Transactional
    public void actualizarPasswordTemporal(Integer usuarioId, String password) {
        Usuario existente = buscarPorId(usuarioId);
        existente.setPassword(passwordEncoder.encode(password));
        existente.setForcePasswordChange(true);
        usuarioRepository.save(existente);
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

        usuarioRepository.deleteById(id);
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
