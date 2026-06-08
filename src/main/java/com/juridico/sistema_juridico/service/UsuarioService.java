package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.catalogo.OrganoJurisdiccional;
import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
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

    @Transactional
    public Usuario guardarUsuario(Usuario usuario, List<Integer> materiasIds) {

        Set<Materia> materiasSeleccionadas = new HashSet<>();
        if (materiasIds != null && !materiasIds.isEmpty()) {
            materiasSeleccionadas.addAll(materiaRepository.findAllById(materiasIds));
        }
        normalizarGerenciaPorRol(usuario);

        if (usuario.getId() == null) {
            // CREACIÓN
            if (usuarioRepository.existsByEmail(usuario.getEmail())) {
                // Aquí podrías lanzar ConflictException si quisieras ser estricto
                throw new RuntimeException("El correo electrónico ya está registrado.");
            }
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setMaterias(materiasSeleccionadas);
            return usuarioRepository.save(usuario);
        } else {
            // EDICIÓN
            Usuario existente = buscarPorId(usuario.getId()); // Ya lanza 404 si no existe

            existente.setNombreCompleto(usuario.getNombreCompleto());
            existente.setEmail(usuario.getEmail());
            existente.setRol(usuario.getRol());

            existente.setGerencia(usuario.getGerencia());
            normalizarGerenciaPorRol(existente);
            // Solo actualizar materias si el formulario envió una lista explícita
            // (materiasIds != null). Si es null, conservar las materias existentes.
            if (materiasIds != null) {
                existente.getMaterias().clear();
                existente.getMaterias().addAll(materiasSeleccionadas);
            }

            existente.setActivo(usuario.getActivo());

            if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
                // Si el admin o director asigna una contraseña manual (temporal), forzar cambio en el próximo login
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
        usuarioRepository.deleteById(id);
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

    private void normalizarGerenciaPorRol(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return;
        }
        if (usuario.getRol() == RolUsuario.ADMIN
                || usuario.getRol() == RolUsuario.DIRECCION
                || usuario.getRol() == RolUsuario.SUBDIRECCION) {
            usuario.setGerencia(null);
        }
    }
}
