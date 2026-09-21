package com.juridico.sistema_juridico.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Catalogo.TipoTerminoRepository;

@Service
public class TipoTerminoService {

    private final TipoTerminoRepository tipoTerminoRepository;

    public TipoTerminoService(TipoTerminoRepository tipoTerminoRepository) {
        this.tipoTerminoRepository = tipoTerminoRepository;
    }

    public List<TipoTermino> listarTodos() {
        return tipoTerminoRepository.findAll();
    }

    public List<TipoTermino> listarActivos() {
        return tipoTerminoRepository.findByActivoTrueOrderByNombreAsc();
    }

    public TipoTermino obtenerPorId(Integer id) {
        return tipoTerminoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de término no encontrado con ID: " + id));
    }

    @Transactional
    public TipoTermino crear(TipoTermino tipoTermino) {
        if (tipoTermino.getNombre() == null || tipoTermino.getNombre().trim().isEmpty()) {
            throw new BadRequestException("El nombre del tipo de término es obligatorio.");
        }
        if (tipoTerminoRepository.findByNombreIgnoreCase(tipoTermino.getNombre().trim()).isPresent()) {
            throw new BadRequestException("Ya existe un tipo de término con el nombre: " + tipoTermino.getNombre());
        }
        return tipoTerminoRepository.save(tipoTermino);
    }

    @Transactional
    public TipoTermino actualizar(Integer id, TipoTermino datosActualizados) {
        TipoTermino exist = obtenerPorId(id);
        if (datosActualizados.getNombre() != null && !datosActualizados.getNombre().trim().equalsIgnoreCase(exist.getNombre())) {
            tipoTerminoRepository.findByNombreIgnoreCase(datosActualizados.getNombre().trim())
                    .ifPresent(dup -> {
                        throw new BadRequestException("Ya existe otro tipo de término con el nombre: " + datosActualizados.getNombre());
                    });
            exist.setNombre(datosActualizados.getNombre().trim());
        }
        if (datosActualizados.getDescripcion() != null) {
            exist.setDescripcion(datosActualizados.getDescripcion());
        }
        if (datosActualizados.getDiasPredeterminados() != null) {
            exist.setDiasPredeterminados(datosActualizados.getDiasPredeterminados());
        }
        if (datosActualizados.getTipoDias() != null) {
            exist.setTipoDias(datosActualizados.getTipoDias());
        }
        if (datosActualizados.getSemaforoRojoDias() != null) {
            exist.setSemaforoRojoDias(datosActualizados.getSemaforoRojoDias());
        }
        if (datosActualizados.getSemaforoAmarilloDias() != null) {
            exist.setSemaforoAmarilloDias(datosActualizados.getSemaforoAmarilloDias());
        }
        if (datosActualizados.getActivo() != null) {
            exist.setActivo(datosActualizados.getActivo());
        }
        return tipoTerminoRepository.save(exist);
    }

    @Transactional
    public void cambiarEstado(Integer id, boolean activo) {
        TipoTermino exist = obtenerPorId(id);
        exist.setActivo(activo);
        tipoTerminoRepository.save(exist);
    }
}
