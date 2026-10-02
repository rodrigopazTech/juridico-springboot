package com.juridico.sistema_juridico.repository.configuracion;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.juridico.sistema_juridico.Entity.configuracion.DiaInhabil;

public interface DiaInhabilRepository extends JpaRepository<DiaInhabil, Integer> {
    boolean existsByFechaAndActivoTrue(LocalDate fecha);
    List<DiaInhabil> findAllByOrderByFechaAsc();
}
