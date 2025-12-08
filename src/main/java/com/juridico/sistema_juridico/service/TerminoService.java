package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.model.Termino;
import com.juridico.sistema_juridico.repository.TerminoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull; 
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TerminoService {

    @Autowired
    private TerminoRepository repository;

    public List<Termino> listarTerminos() {
        return repository.findByActivoTrue();
    }

    public Termino guardarTermino(Termino termino) {
        // Si es nuevo, aseguramos que esté activo
        if (termino.getId() == null) {
            termino.setActivo(true);
            if (termino.getEstatus() == null || termino.getEstatus().isEmpty()) {
                termino.setEstatus("Proyectista");
            }
        }
        return repository.save(termino);
    }

    public Termino actualizarEstatus(@NonNull Long id, String nuevoEstatus) {
        return repository.findById(id).map(t -> {
            t.setEstatus(nuevoEstatus);
            return repository.save(t);
        }).orElse(null);
    }

   public void eliminarTermino(@NonNull Long id) {
        repository.findById(id).ifPresent(t -> {
            t.setActivo(false); // Borrado lógico
            repository.save(t);
        });
    }
}