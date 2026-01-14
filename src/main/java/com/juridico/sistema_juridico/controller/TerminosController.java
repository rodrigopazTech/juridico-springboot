package com.juridico.sistema_juridico.controller;

import com.juridico.sistema_juridico.dto.request.terminos.TerminoRequest;
import com.juridico.sistema_juridico.dto.response.terminos.TerminoResponse;
import com.juridico.sistema_juridico.service.procesal.TerminoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;


/**
 * Controlador para la gestión de términos procesales.
 * Cumple con los requisitos del módulo MOD-003.
 */
@RestController
@RequestMapping("/api/terminos")
@RequiredArgsConstructor
public class TerminosController {

    private final TerminoService terminoService;

    @GetMapping
    public ResponseEntity<Page<TerminoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(terminoService.listarPaginado(pageable));
    }

    @PostMapping
    public ResponseEntity<TerminoResponse> guardar(@Valid @RequestBody TerminoRequest request) {
        return new ResponseEntity<>(terminoService.crear(request), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> actualizarEstado(@PathVariable Integer id, @RequestBody String nuevoEstado) { 
        terminoService.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/presentar")
    public ResponseEntity<Void> presentar(@PathVariable Integer id, @RequestBody String observaciones) {
    terminoService.marcarComoPresentado(id, observaciones);
    return ResponseEntity.ok().build();
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<TerminoResponse>> obtenerVencidos() {
        return ResponseEntity.ok(terminoService.listarVencidos());
    }

    @GetMapping("/proximos-vencer")
    public ResponseEntity<List<TerminoResponse>> obtenerProximos() {
        return ResponseEntity.ok(terminoService.listarProximosAVencer());
    }

    @GetMapping("/expediente/{expedienteId}")
    public ResponseEntity<List<TerminoResponse>> porExpediente(@PathVariable UUID expedienteId) {
        return ResponseEntity.ok(terminoService.listarPorExpediente(expedienteId));
}
}