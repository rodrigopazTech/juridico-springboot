package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import org.springframework.data.domain.Page; 
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface AudienciaDesahogadaRepository extends JpaRepository<AudienciaDesahogada, Integer> {
    
    // Cambiamos List -> Page y agregamos Pageable
    Page<AudienciaDesahogada> findByFechaDesahogoBetweenOrderByFechaDesahogoDesc(LocalDate inicio, LocalDate fin, Pageable pageable);
}