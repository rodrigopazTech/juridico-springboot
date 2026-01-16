package com.juridico.sistema_juridico.repository.Catalogo; // Asegúrate que el paquete coincida con tu carpeta

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;

@Repository
public interface GerenciaRepository extends JpaRepository<Gerencia, Integer> {
    
    // Método que te faltaba: Busca una gerencia exacta por su nombre
    Optional<Gerencia> findByNombre(String nombre);

    // El que ya tenías
    List<Gerencia> findByActivoTrueOrderByNombreAsc();
}