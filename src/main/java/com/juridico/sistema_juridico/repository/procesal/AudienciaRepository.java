package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface AudienciaRepository extends JpaRepository<Audiencia, Integer> {

    @Query("SELECT a FROM Audiencia a " +
           "LEFT JOIN a.expediente e " +
           "WHERE " +
           // BUSCADOR
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " + 
           "LOWER(a.salaLugar) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           
           // FILTROS (Usando .nombre para acceder al texto de los objetos)
           
           "AND (:tipo IS NULL OR :tipo = '' OR a.tipoAudiencia.nombre = :tipo) " + 
           
           // Corrección crítica: e.gerencia.nombre
           "AND (:gerencia IS NULL OR :gerencia = '' OR e.gerencia.nombre = :gerencia) " +
           
           // Corrección crítica: e.materia.nombre
           "AND (:materia IS NULL OR :materia = '' OR e.materia.nombre = :materia) " +
           
           "AND (:estatus IS NULL OR :estatus = '' OR a.estatusAudiencia = :estatus)")
    Page<Audiencia> buscarConFiltros(
            @Param("keyword") String keyword,
            @Param("tipo") String tipo,
            @Param("gerencia") String gerencia,
            @Param("materia") String materia,
            @Param("estatus") String estatus,
            Pageable pageable);

       @Query("SELECT a FROM Audiencia a " +
           "LEFT JOIN a.expediente e " +
           "WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(e.numero) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.partes) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " + 
           "LOWER(a.salaLugar) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:tipo IS NULL OR :tipo = '' OR a.tipoAudiencia.nombre = :tipo) " + 
           "AND (:gerencia IS NULL OR :gerencia = '' OR e.gerencia.nombre = :gerencia) " +
           "AND (:materia IS NULL OR :materia = '' OR e.materia.nombre = :materia) " +
           "AND (:estatus IS NULL OR :estatus = '' OR a.estatusAudiencia = :estatus)")
    List<Audiencia> listarParaExcel(
            @Param("keyword") String keyword,
            @Param("tipo") String tipo,
            @Param("gerencia") String gerencia,
            @Param("materia") String materia,
            @Param("estatus") String estatus);
}