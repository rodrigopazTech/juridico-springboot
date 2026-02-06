package com.juridico.sistema_juridico.repository.Usuarios;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por su correo electrónico.
     * Utilizado para la autenticación y para cargar el perfil en el Dashboard.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el email proporcionado.
     */
    boolean existsByEmail(String email);

    // 1. Para el Select del Modal (Solo mostrar ABOGADOS activos)
    List<Usuario> findByRolAndActivoTrue(RolUsuario rol);

    // 2. Para Notificaciones Dirigidas por Gerencia
    List<Usuario> findByRolAndGerenciaAndActivoTrue(RolUsuario rol, Gerencia gerencia);

    // 3. Para Notificaciones a Altos Mandos (Dirección/Subdirección)
    List<Usuario> findByRolInAndActivoTrue(List<RolUsuario> roles);
}