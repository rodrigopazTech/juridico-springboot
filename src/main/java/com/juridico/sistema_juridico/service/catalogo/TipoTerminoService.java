package com.juridico.sistema_juridico.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Catalogo.TipoTerminoRepository;
import com.juridico.sistema_juridico.repository.configuracion.PerfilAlertaTerminoRepository;
import com.juridico.sistema_juridico.util.CodigoNormalizer;

@Service
public class TipoTerminoService {

    private final TipoTerminoRepository tipoTerminoRepository;
    private final PerfilAlertaTerminoRepository perfilRepository;

    public TipoTerminoService(TipoTerminoRepository tipoTerminoRepository,
            PerfilAlertaTerminoRepository perfilRepository) {
        this.tipoTerminoRepository = tipoTerminoRepository;
        this.perfilRepository = perfilRepository;
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
    public TipoTermino crear(TipoTermino tipo) {
        tipoTerminoRepository.findByNombreIgnoreCase(tipo.getNombre().trim())
                .ifPresent(exist -> {
                    throw new BadRequestException("Ya existe un tipo de término con el nombre: " + tipo.getNombre());
                });
        validarYNormalizar(tipo);
        return tipoTerminoRepository.save(tipo);
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
        validarYNormalizar(datosActualizados);
        exist.setCodigo(datosActualizados.getCodigo());
        exist.setModoPlazo(datosActualizados.getModoPlazo());
        exist.setDiasPredeterminados(datosActualizados.getDiasPredeterminados());
        if (datosActualizados.getTipoDias() != null) {
            exist.setTipoDias(datosActualizados.getTipoDias());
        }
        exist.setPerfilAlerta(datosActualizados.getPerfilAlerta());
        exist.setMateria(datosActualizados.getMateria());
        exist.setGerencia(datosActualizados.getGerencia());
        exist.setRequiereDocumentoLiberado(datosActualizados.getRequiereDocumentoLiberado());
        exist.setRequiereAcuse(datosActualizados.getRequiereAcuse());
        exist.setPermiteExcepcionSinAcuse(datosActualizados.getPermiteExcepcionSinAcuse());
        exist.setRequiereValidacionAcuse(datosActualizados.getRequiereValidacionAcuse());
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

    private void validarYNormalizar(TipoTermino tipo) {
        if (tipo.getNombre() == null || tipo.getNombre().trim().isEmpty()) {
            throw new BadRequestException("El nombre del tipo de término es obligatorio.");
        }
        if (tipo.getMateria() == null || tipo.getMateria().getId() == null) {
            throw new BadRequestException("Selecciona la Materia a la que pertenece el Tipo de Término.");
        }
        tipo.setNombre(tipo.getNombre().trim());
        String baseCodigo = (tipo.getCodigo() == null || tipo.getCodigo().isBlank()) ? tipo.getNombre() : tipo.getCodigo();
        tipo.setCodigo(CodigoNormalizer.normalizar(baseCodigo, tipo.getId()));
        if (tipo.getDescripcion() == null || tipo.getDescripcion().isBlank()) {
            throw new BadRequestException("La descripción de uso es obligatoria.");
        }
        if (tipo.getModoPlazo() == null) {
            throw new BadRequestException("Selecciona el modo de plazo.");
        }
        if (tipo.getActivo() == null) tipo.setActivo(true);
        tipo.setRequiereDocumentoLiberado(true);
        tipo.setRequiereAcuse(true);
        tipo.setPermiteExcepcionSinAcuse(true);
        tipo.setRequiereValidacionAcuse(true);

        if (tipo.getModoPlazo() == ModoPlazoTermino.SIN_VENCIMIENTO) {
            tipo.setDiasPredeterminados(null);
            tipo.setPerfilAlerta(null);
            return;
        }
        if (tipo.getModoPlazo() == ModoPlazoTermino.CAPTURADO) {
            tipo.setDiasPredeterminados(null);
        }
        if (tipo.getModoPlazo() == ModoPlazoTermino.CALCULADO
                && (tipo.getDiasPredeterminados() == null || tipo.getDiasPredeterminados() <= 0)) {
            throw new BadRequestException("Los términos calculados requieren una cantidad de días mayor que cero.");
        }
        if (tipo.getPerfilAlerta() == null || tipo.getPerfilAlerta().getId() == null) {
            throw new BadRequestException("Selecciona una Regla de alerta compatible.");
        }
        PerfilAlertaTermino perfil = perfilRepository.findById(tipo.getPerfilAlerta().getId())
                .orElseThrow(() -> new BadRequestException("La Regla de alerta seleccionada no existe."));
        if (perfil.getEstado() != EstadoPerfil.PUBLICADO) {
            throw new BadRequestException("Sólo se pueden utilizar Reglas de alerta publicadas.");
        }
        ModoPerfilAlerta esperado = tipo.getModoPlazo() == ModoPlazoTermino.CALCULADO
                ? ModoPerfilAlerta.DIAS_RESTANTES : ModoPerfilAlerta.PROPORCIONAL_LIMITADO;
        if (perfil.getModo() != esperado) {
            throw new BadRequestException("La Regla de alerta no es compatible con el modo de plazo seleccionado.");
        }
        tipo.setPerfilAlerta(perfil);
    }
}
