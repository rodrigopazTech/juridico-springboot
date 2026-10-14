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

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:identifier) "
            + "OR (u.email IS NULL AND LOWER(u.username) = LOWER(:identifier))")
    Optional<Usuario> findByEmail(@Param("identifier") String identifier);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByMicrosoftObjectId(String microsoftObjectId);

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    Optional<Usuario> findByNumeroEmpleadoIgnoreCase(String numeroEmpleado);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByNumeroEmpleadoIgnoreCase(String numeroEmpleado);

    long countByCreadoPorId(Integer creadoPorId);

    @Query("SELECT u FROM Usuario u WHERE u.email IS NULL OR TRIM(u.email) = '' ORDER BY u.id")
    List<Usuario> findUsuariosSinCorreo();

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.username) = LOWER(:identifier) "
            + "OR LOWER(u.numeroEmpleado) = LOWER(:identifier) OR LOWER(u.email) = LOWER(:identifier)")
    Optional<Usuario> findByLoginIdentifier(@Param("identifier") String identifier);

    // 1. Para el Select del Modal (Solo mostrar ABOGADOS activos)
    List<Usuario> findByRolAndActivoTrue(RolUsuario rol);

    // 2. Para Notificaciones Dirigidas por Gerencia (Gerentes)
    List<Usuario> findByRolAndGerenciasIdAndActivoTrue(RolUsuario rol, Integer gerenciaId);

    // 3. Para Notificaciones a Altos Mandos (Dirección/Subdirección)
    List<Usuario> findByRolInAndActivoTrue(List<RolUsuario> roles);

    // 4. NUEVO: Buscar Jefes por Materia (Relación Many-to-Many)
    @Query("SELECT u FROM Usuario u JOIN u.materias m WHERE u.rol = :rol AND m.id = :materiaId AND u.activo = true")
    List<Usuario> findByRolAndMateriaId(@Param("rol") RolUsuario rol, @Param("materiaId") Integer materiaId);

    Page<Usuario> findByGerenciasId(Integer gerenciaId, Pageable pageable);
    Page<Usuario> findDistinctByGerenciasIdIn(List<Integer> gerenciaIds, Pageable pageable);

    // 5. Para jerarquía de notificaciones (Superiores de la misma gerencia)
    List<Usuario> findByGerenciasIdAndRolInAndActivoTrue(Integer gerenciaId, List<RolUsuario> roles);
    List<Usuario> findByGerenciasIdAndRolInAndActivoTrueOrderByNombreCompletoAsc(Integer gerenciaId, List<RolUsuario> roles);
    List<Usuario> findDistinctByGerenciasIdInAndRolInAndActivoTrue(List<Integer> gerenciaIds, List<RolUsuario> roles);

    // 6. Para filtro de usuarios por gerencia
    List<Usuario> findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc(Integer gerenciaId);
    List<Usuario> findDistinctByGerenciasIdInAndActivoTrueOrderByNombreCompletoAsc(List<Integer> gerenciaIds);

    // 7. Búsqueda por keyword (nombre, email, rol) con paginación — para el buscador del módulo Usuarios
    @Query("SELECT DISTINCT u FROM Usuario u WHERE " +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.numeroEmpleado) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Usuario> buscarPorKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 8. Búsqueda por keyword restringida a múltiples gerencias (para GERENTE)
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g WHERE g.id IN :gerenciaIds AND (" +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.numeroEmpleado) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Usuario> buscarPorKeywordYGerenciasIn(@Param("keyword") String keyword,
                                              @Param("gerenciaIds") List<Integer> gerenciaIds,
                                              Pageable pageable);
    Page<Usuario> findByRolInAndIdNot(List<RolUsuario> roles, Integer excluidoUsuarioId, Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g JOIN u.materias m WHERE g.id IN :gerenciaIds AND m.id IN :materiaIds AND u.rol IN :roles AND u.id != :excluidoUsuarioId")
    Page<Usuario> findDistinctByGerenciasIdInAndMateriasIdInAndRolInAndIdNot(@Param("gerenciaIds") List<Integer> gerenciaIds,
                                                                             @Param("materiaIds") List<Integer> materiaIds,
                                                                             @Param("roles") List<RolUsuario> roles,
                                                                             @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                                             Pageable pageable);

    @Query("""
           SELECT COUNT(DISTINCT u)
           FROM Usuario u
           JOIN u.gerencias g
           JOIN u.materias m
           WHERE g.id = :gerenciaId
             AND m.id IN :materiaIds
             AND u.rol = :rol
             AND u.activo = true
           """)
    long countAbogadosActivosPorGerenciaYMaterias(@Param("gerenciaId") Integer gerenciaId,
                                                  @Param("materiaIds") List<Integer> materiaIds,
                                                  @Param("rol") RolUsuario rol);

    @Query("SELECT DISTINCT u FROM Usuario u WHERE u.rol IN :roles AND u.id != :excluidoUsuarioId AND (" +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.numeroEmpleado) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Usuario> buscarPorKeywordAndRolInAndIdNot(@Param("keyword") String keyword,
                                                   @Param("roles") List<RolUsuario> roles,
                                                   @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                   Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g JOIN u.materias m WHERE g.id IN :gerenciaIds AND m.id IN :materiaIds AND u.rol IN :roles AND u.id != :excluidoUsuarioId AND (" +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.numeroEmpleado) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Usuario> buscarPorKeywordYGerenciasInAndMateriasInAndRolInAndIdNot(@Param("keyword") String keyword,
                                                                            @Param("gerenciaIds") List<Integer> gerenciaIds,
                                                                            @Param("materiaIds") List<Integer> materiaIds,
                                                                            @Param("roles") List<RolUsuario> roles,
                                                                            @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                                            Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g WHERE g.id IN :gerenciaIds AND u.rol IN :roles AND u.id != :excluidoUsuarioId")
    Page<Usuario> findDistinctByGerenciasIdInAndRolInAndIdNot(@Param("gerenciaIds") List<Integer> gerenciaIds,
                                                             @Param("roles") List<RolUsuario> roles,
                                                             @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                             Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g WHERE g.id IN :gerenciaIds AND u.rol IN :roles AND u.id != :excluidoUsuarioId AND (" +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.numeroEmpleado) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Usuario> buscarPorKeywordYGerenciasInAndRolInAndIdNot(@Param("keyword") String keyword,
                                                              @Param("gerenciaIds") List<Integer> gerenciaIds,
                                                              @Param("roles") List<RolUsuario> roles,
                                                              @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                              Pageable pageable);

    // ====================================================================
    // MÉTODOS CON FILTRO EXPLÍCITO POR ESTATUS ACTIVO / INACTIVO
    // ====================================================================

    Page<Usuario> findByRolInAndActivoAndIdNot(List<RolUsuario> roles, Boolean activo, Integer excluidoUsuarioId, Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u WHERE u.rol IN :roles AND u.activo = :activo AND u.id != :excluidoUsuarioId AND (" +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.numeroEmpleado) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Usuario> buscarPorKeywordAndRolInAndActivoAndIdNot(@Param("keyword") String keyword,
                                                           @Param("roles") List<RolUsuario> roles,
                                                           @Param("activo") Boolean activo,
                                                           @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                           Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g WHERE g.id IN :gerenciaIds AND u.rol IN :roles AND u.activo = :activo AND u.id != :excluidoUsuarioId")
    Page<Usuario> findDistinctByGerenciasIdInAndRolInAndActivoAndIdNot(@Param("gerenciaIds") List<Integer> gerenciaIds,
                                                                      @Param("roles") List<RolUsuario> roles,
                                                                      @Param("activo") Boolean activo,
                                                                      @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                                      Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g WHERE g.id IN :gerenciaIds AND u.rol IN :roles AND u.activo = :activo AND u.id != :excluidoUsuarioId AND (" +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.numeroEmpleado) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Usuario> buscarPorKeywordYGerenciasInAndRolInAndActivoAndIdNot(@Param("keyword") String keyword,
                                                                       @Param("gerenciaIds") List<Integer> gerenciaIds,
                                                                       @Param("roles") List<RolUsuario> roles,
                                                                       @Param("activo") Boolean activo,
                                                                       @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                                       Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g JOIN u.materias m WHERE g.id IN :gerenciaIds AND m.id IN :materiaIds AND u.rol IN :roles AND u.activo = :activo AND u.id != :excluidoUsuarioId")
    Page<Usuario> findDistinctByGerenciasIdInAndMateriasIdInAndRolInAndActivoAndIdNot(@Param("gerenciaIds") List<Integer> gerenciaIds,
                                                                                     @Param("materiaIds") List<Integer> materiaIds,
                                                                                     @Param("roles") List<RolUsuario> roles,
                                                                                     @Param("activo") Boolean activo,
                                                                                     @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                                                     Pageable pageable);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.gerencias g JOIN u.materias m WHERE g.id IN :gerenciaIds AND m.id IN :materiaIds AND u.rol IN :roles AND u.activo = :activo AND u.id != :excluidoUsuarioId AND (" +
           "LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.numeroEmpleado) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(u.rol AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Usuario> buscarPorKeywordYGerenciasInAndMateriasInAndRolInAndActivoAndIdNot(@Param("keyword") String keyword,
                                                                                    @Param("gerenciaIds") List<Integer> gerenciaIds,
                                                                                    @Param("materiaIds") List<Integer> materiaIds,
                                                                                    @Param("roles") List<RolUsuario> roles,
                                                                                    @Param("activo") Boolean activo,
                                                                                    @Param("excluidoUsuarioId") Integer excluidoUsuarioId,
                                                                                    Pageable pageable);

    long countByRolInAndActivoAndIdNot(List<RolUsuario> roles, Boolean activo, Integer excluidoUsuarioId);

    @Query("SELECT COUNT(DISTINCT u) FROM Usuario u JOIN u.gerencias g WHERE g.id IN :gerenciaIds AND u.rol IN :roles AND u.activo = :activo AND u.id != :excluidoUsuarioId")
    long countDistinctByGerenciasIdInAndRolInAndActivoAndIdNot(@Param("gerenciaIds") List<Integer> gerenciaIds,
                                                               @Param("roles") List<RolUsuario> roles,
                                                               @Param("activo") Boolean activo,
                                                               @Param("excluidoUsuarioId") Integer excluidoUsuarioId);

    @Query("SELECT COUNT(DISTINCT u) FROM Usuario u JOIN u.gerencias g JOIN u.materias m WHERE g.id IN :gerenciaIds AND m.id IN :materiaIds AND u.rol IN :roles AND u.activo = :activo AND u.id != :excluidoUsuarioId")
    long countDistinctByGerenciasIdInAndMateriasIdInAndRolInAndActivoAndIdNot(@Param("gerenciaIds") List<Integer> gerenciaIds,
                                                                              @Param("materiaIds") List<Integer> materiaIds,
                                                                              @Param("roles") List<RolUsuario> roles,
                                                                              @Param("activo") Boolean activo,
                                                                              @Param("excluidoUsuarioId") Integer excluidoUsuarioId);
}
