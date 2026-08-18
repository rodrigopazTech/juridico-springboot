package com.juridico.sistema_juridico.repository.Catalogo;

import com.juridico.sistema_juridico.Entity.catalogo.EtapaCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EtapaCatalogoRepository extends JpaRepository<EtapaCatalogo, Integer> {

    List<EtapaCatalogo> findByMateriaIdAndActivoTrueOrderByOrdenAscNombreAsc(Integer materiaId);

    List<EtapaCatalogo> findByMateriaIdOrderByOrdenAscNombreAsc(Integer materiaId);

    List<EtapaCatalogo> findByMateriaIdInOrderByMateriaNombreAscOrdenAscNombreAsc(Collection<Integer> materiaIds);

    List<EtapaCatalogo> findByMateriaGerenciaIdAndActivoTrueOrderByMateriaNombreAscOrdenAscNombreAsc(Integer gerenciaId);

    boolean existsByMateriaIdAndNombreIgnoreCaseAndParentId(Integer materiaId, String nombre, Integer parentId);

    boolean existsByParentId(Integer parentId);
}
