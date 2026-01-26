package com.juridico.sistema_juridico.repository.procesal;

import com.juridico.sistema_juridico.Entity.procesal.AudienciaDesahogada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AudienciaDesahogadaRepository extends JpaRepository<AudienciaDesahogada, Integer> {
    
    // Buscar por rango de fechas (Para Hoy, Semana, Mes)
    List<AudienciaDesahogada> findByFechaDesahogoBetweenOrderByFechaDesahogoDesc(LocalDate inicio, LocalDate fin);
}