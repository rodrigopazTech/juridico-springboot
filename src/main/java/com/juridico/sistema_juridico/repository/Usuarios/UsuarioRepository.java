package com.juridico.sistema_juridico.repository.Usuarios;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    // Método derivado (Spring crea la query automáticamente por el nombre)
    // SELECT * FROM usuarios WHERE email = ?
    Optional<Usuario> findByEmail(String email);

    // Para verificar si existe antes de crear uno nuevo
    boolean existsByEmail(String email);
    
    // Buscar usuarios activos de una gerencia específica
    // Útil para poblar los "Select" de abogados disponibles
    List<Usuario> findByGerenciaIdAndActivoTrue(Integer gerenciaId);
}