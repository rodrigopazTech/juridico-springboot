package com.juridico.sistema_juridico.service.catalogo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.configuracion.PerfilRecordatorioAudienciaRepository;
import com.juridico.sistema_juridico.util.CodigoNormalizer;

@Service
public class TipoAudienciaService {

    private final TipoAudienciaRepository tipoRepository;
    private final PerfilRecordatorioAudienciaRepository perfilRepository;

    public TipoAudienciaService(TipoAudienciaRepository tipoRepository,
            PerfilRecordatorioAudienciaRepository perfilRepository) {
        this.tipoRepository = tipoRepository;
        this.perfilRepository = perfilRepository;
    }

    @Transactional
    public TipoAudiencia guardar(TipoAudiencia datos) {
        TipoAudiencia destino = datos.getId() == null ? new TipoAudiencia()
                : tipoRepository.findById(datos.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tipo de Audiencia no encontrado."));
        validar(datos);
        PerfilRecordatorioAudiencia perfil = perfilRepository.findById(datos.getPerfilRecordatorio().getId())
                .orElseThrow(() -> new BadRequestException("La configuración de avisos seleccionada no existe."));
        if (perfil.getEstado() != EstadoPerfil.PUBLICADO) {
            throw new BadRequestException("Sólo se pueden utilizar Reglas de alerta publicadas.");
        }
        destino.setCodigo(normalizarCodigo(datos.getCodigo(), datos.getNombre(), datos.getId()));
        destino.setNombre(datos.getNombre().trim());
        destino.setDescripcion(datos.getDescripcion().trim());
        destino.setPerfilRecordatorio(perfil);
        destino.setDuracionPredeterminadaMinutos(datos.getDuracionPredeterminadaMinutos());
        destino.setModalidadesPermitidas(datos.getModalidadesPermitidas());
        destino.setModalidadPredeterminada(datos.getModalidadPredeterminada());
        destino.setRequiereActa(Boolean.TRUE.equals(datos.getRequiereActa()));
        destino.setPermiteExcepcionSinActa(Boolean.TRUE.equals(datos.getPermiteExcepcionSinActa()));
        destino.setRequiereValidacionActa(Boolean.TRUE.equals(datos.getRequiereValidacionActa()));
        destino.setActivo(datos.getActivo() == null || datos.getActivo());
        destino.setMateria(datos.getMateria());
        if (datos.getGerencia() != null || datos.getId() == null) destino.setGerencia(datos.getGerencia());
        if (datos.getUsuario() != null || datos.getId() == null) destino.setUsuario(datos.getUsuario());
        return tipoRepository.save(destino);
    }

    public TipoAudiencia obtenerPorId(Integer id) {
        return tipoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de Audiencia no encontrado."));
    }

    private void validar(TipoAudiencia tipo) {
        if (tipo.getNombre() == null || tipo.getNombre().isBlank()) {
            throw new BadRequestException("El nombre es obligatorio.");
        }
        if (tipo.getMateria() == null || tipo.getMateria().getId() == null) {
            throw new BadRequestException("Selecciona la Materia a la que pertenece el Tipo de Audiencia.");
        }
        if (tipo.getDescripcion() == null || tipo.getDescripcion().isBlank()) {
            throw new BadRequestException("La descripción de uso es obligatoria.");
        }
        if (tipo.getPerfilRecordatorio() == null || tipo.getPerfilRecordatorio().getId() == null) {
            throw new BadRequestException("No fue posible asociar la configuración de avisos.");
        }
        if (tipo.getDuracionPredeterminadaMinutos() != null && tipo.getDuracionPredeterminadaMinutos() <= 0) {
            throw new BadRequestException("La duración predeterminada debe ser mayor que cero.");
        }
        if (tipo.getModalidadesPermitidas() == null || tipo.getModalidadesPermitidas().isBlank()) {
            throw new BadRequestException("Selecciona al menos una modalidad permitida.");
        }
        if (tipo.getModalidadPredeterminada() != null
                && !tipo.getModalidadesPermitidas().contains(tipo.getModalidadPredeterminada())) {
            throw new BadRequestException("La modalidad predeterminada debe estar entre las permitidas.");
        }
        if (Boolean.TRUE.equals(tipo.getRequiereValidacionActa()) && !Boolean.TRUE.equals(tipo.getRequiereActa())) {
            throw new BadRequestException("No se puede exigir validación cuando el acta no es requerida.");
        }
    }

    private String normalizarCodigo(String codigo, String nombre, Integer id) {
        String base = codigo == null || codigo.isBlank() ? nombre : codigo;
        return CodigoNormalizer.normalizar(base);
    }
}
