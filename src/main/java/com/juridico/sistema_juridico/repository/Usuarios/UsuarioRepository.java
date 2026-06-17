package com.juridico.sistema_juridico.repository.Usuarios;

import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // 1. Para el Select del Modal (Solo mostrar ABOGADOS activos)
    List<Usuario> findByRolAndActivoTrue(RolUsuario rol);

    // 2. Para Notificaciones Dirigidas por Gerencia (Gerentes)
    List<Usuario> findByRolAndGerenciaAndActivoTrue(RolUsuario rol, Gerencia gerencia);

    // 3. Para Notificaciones a Altos Mandos (Dirección/Subdirección)
    List<Usuario> findByRolInAndActivoTrue(List<RolUsuario> roles);

    // 4. NUEVO: Buscar Jefes por Materia (Relación Many-to-Many)
    @Query("SELECT u FROM Usuario u JOIN u.materias m WHERE u.rol = :rol AND m.id = :materiaId AND u.activo = true")
    List<Usuario> findByRolAndMateriaId(@Param("rol") RolUsuario rol, @Param("materiaId") Integer materiaId);

    Page<Usuario> findByGerencia(Gerencia gerencia, Pageable pageable);

    // 5. Para jerarquía de notificaciones (Superiores de la misma gerencia)
    List<Usuario> findByGerenciaIdAndRolInAndActivoTrue(Integer gerenciaId, List<RolUsuario> roles);

    // 6. Para filtro de usuarios por gerencia
    List<Usuario> findByGerenciaIdAndActivoTrueOrderByNombreCompletoAsc(Integer gerenciaId);

    // 7. Búsqueda por keyword (nombre, email, rol) con paginación — para el buscador del módulo Usuarios
    @Query("SELECT u FROM Usuario u WHERE " +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Usuario> buscarPorKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 8. Búsqueda por keyword restringida a una gerencia (para GERENTE)
    @Query("SELECT u FROM Usuario u WHERE u.gerencia = :gerencia AND (" +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Usuario> buscarPorKeywordYGerencia(@Param("keyword") String keyword,
                                             @Param("gerencia") Gerencia gerencia,
                                             Pageable pageable);
}
