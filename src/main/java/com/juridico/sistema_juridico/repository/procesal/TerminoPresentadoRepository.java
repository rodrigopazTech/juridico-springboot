package com.juridico.sistema_juridico.repository.procesal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.procesal.TerminoPresentado;

@Repository
public interface TerminoPresentadoRepository extends JpaRepository<TerminoPresentado, Integer> {
    
}