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

    // ESTE ES EL MÉTODO QUE FALTABA
    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    public Usuario guardarUsuario(Usuario usuario) {
        // 1. Nuevo Usuario
        if (usuario.getId() == null) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            return usuarioRepository.save(usuario);
        } 
        // 2. Editar Usuario
        else {
            Usuario usuarioExistente = buscarPorId(usuario.getId());

            usuarioExistente.setNombreCompleto(usuario.getNombreCompleto());
            usuarioExistente.setEmail(usuario.getEmail());
            usuarioExistente.setRol(usuario.getRol());
            usuarioExistente.setGerencia(usuario.getGerencia());
            usuarioExistente.setActivo(usuario.getActivo());

            // Solo actualizamos password si viene una nueva
            if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                usuarioExistente.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }

            return usuarioRepository.save(usuarioExistente);
        }
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

    public void eliminarUsuario(Integer id) { 
        usuarioRepository.deleteById(id); 
    }
}