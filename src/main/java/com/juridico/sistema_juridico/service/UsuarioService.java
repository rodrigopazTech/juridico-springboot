package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.Set;
import java.util.HashSet;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private GerenciaRepository gerenciaRepository;
    @Autowired private MateriaRepository materiaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // --- USUARIOS ---

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    /**
     * Busca al usuario por su identificador de inicio de sesión (email).
     * Requerido por el DashboardController.
     */
    public Usuario obtenerPorUsername(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
    }

    public Page<Usuario> listarUsuariosPaginados(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    public Page<Gerencia> listarGerenciasPaginadas(Pageable pageable) {
        return gerenciaRepository.findAll(pageable);
    }

    public Usuario guardarUsuario(Usuario usuario, List<Integer> materiasIds) {
        
        Set<Materia> materiasSeleccionadas = new HashSet<>();
        if (materiasIds != null && !materiasIds.isEmpty()) {
            materiasSeleccionadas.addAll(materiaRepository.findAllById(materiasIds));
        }

        if (usuario.getId() == null) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setMaterias(materiasSeleccionadas); 
            return usuarioRepository.save(usuario);
        } 
        else {
            Usuario existente = buscarPorId(usuario.getId());

            existente.setNombreCompleto(usuario.getNombreCompleto());
            existente.setEmail(usuario.getEmail());
            existente.setRol(usuario.getRol());
            
            if (usuario.getRol().name().equals("DIRECCION") || usuario.getRol().name().equals("SUBDIRECCION")) {
                existente.setGerencia(null);
                existente.getMaterias().clear(); 
            } else {
                existente.setGerencia(usuario.getGerencia());
                
                existente.getMaterias().clear();
                existente.getMaterias().addAll(materiasSeleccionadas);
            }
            
            existente.setActivo(usuario.getActivo());

            if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                existente.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }

            return usuarioRepository.save(existente);
        }
    }

    public void eliminarUsuario(Integer id) { 
        usuarioRepository.deleteById(id); 
    }

    // --- GERENCIAS ---

    public List<Gerencia> listarGerencias() {
        return gerenciaRepository.findAll();
    }

    public Gerencia buscarGerenciaPorId(Integer id) {
        return gerenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gerencia no encontrada"));
    }

    public Gerencia guardarGerencia(Gerencia gerencia) {
        return gerenciaRepository.save(gerencia);
    }

    // --- MATERIAS ---

    public Materia guardarMateria(Materia materia) {
        return materiaRepository.save(materia);
    }
    
    public void eliminarMateria(Integer id) {
        materiaRepository.deleteById(id);
    }
}