package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.model.Termino;
import com.juridico.sistema_juridico.service.TerminoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/terminos")
@CrossOrigin(origins = "*")
public class TerminoApiController {

    @Autowired
    private TerminoService service;

    // GET /api/v1/terminos
    @GetMapping
    public List<Termino> listar() {
        return service.listarTerminos();
    }

    // POST /api/v1/terminos
    @PostMapping
    public Termino crear(@RequestBody Termino termino) {
        return service.guardarTermino(termino);
    }

    // PUT /api/v1/terminos/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Termino> actualizar(@PathVariable Long id, @RequestBody Termino termino) {
        termino.setId(id);
        Termino actualizado = service.guardarTermino(termino);
        return ResponseEntity.ok(actualizado);
    }

    // PATCH /api/v1/terminos/{id}/estatus
    @PatchMapping("/{id}/estatus")
    public ResponseEntity<Termino> actualizarEstatus(@PathVariable @NonNull Long id, @RequestParam String estatus) {
        Termino actualizado = service.actualizarEstatus(id, estatus);
        if (actualizado != null) {
            return ResponseEntity.ok(actualizado);
        }
        return ResponseEntity.notFound().build();
    }

    // DELETE /api/v1/terminos/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @NonNull Long id) {
        service.eliminarTermino(id);
        return ResponseEntity.noContent().build();
    }
}