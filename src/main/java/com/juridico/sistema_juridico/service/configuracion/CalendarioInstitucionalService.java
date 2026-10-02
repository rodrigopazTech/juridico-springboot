package com.juridico.sistema_juridico.service.configuracion;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.juridico.sistema_juridico.Entity.configuracion.DiaInhabil;
import com.juridico.sistema_juridico.repository.configuracion.DiaInhabilRepository;

@Service
public class CalendarioInstitucionalService {
    private final DiaInhabilRepository repository;
    public CalendarioInstitucionalService(DiaInhabilRepository repository) { this.repository = repository; }
    public List<DiaInhabil> listar() { return repository.findAllByOrderByFechaAsc(); }
    public boolean esInhabil(LocalDate fecha) { return repository.existsByFechaAndActivoTrue(fecha); }
    @Transactional public DiaInhabil guardar(DiaInhabil dia) {
        if (dia.getFecha() == null || dia.getDescripcion() == null || dia.getDescripcion().isBlank()) throw new IllegalArgumentException("Captura fecha y motivo del día inhábil.");
        if (dia.getActivo() == null) dia.setActivo(true);
        return repository.save(dia);
    }
    @Transactional public void cambiarEstado(Integer id, boolean activo) {
        DiaInhabil dia = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Día inhábil no encontrado."));
        dia.setActivo(activo); repository.save(dia);
    }
}
