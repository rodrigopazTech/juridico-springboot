package com.juridico.sistema_juridico.service.configuracion;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.exception.ResourceNotFoundException;
import com.juridico.sistema_juridico.repository.configuracion.PerfilAlertaTerminoRepository;
import com.juridico.sistema_juridico.repository.configuracion.PerfilRecordatorioAudienciaRepository;

@Service
public class PerfilAtencionService {

    private final PerfilAlertaTerminoRepository terminoRepository;
    private final PerfilRecordatorioAudienciaRepository audienciaRepository;

    public PerfilAtencionService(PerfilAlertaTerminoRepository terminoRepository,
            PerfilRecordatorioAudienciaRepository audienciaRepository) {
        this.terminoRepository = terminoRepository;
        this.audienciaRepository = audienciaRepository;
    }

    public List<PerfilAlertaTermino> listarTerminos() {
        return terminoRepository.findAllByOrderByNombreAscVersionDesc();
    }

    public List<PerfilRecordatorioAudiencia> listarAudiencias() {
        return audienciaRepository.findAllByOrderByNombreAscVersionDesc();
    }

    public List<PerfilAlertaTermino> publicados(ModoPerfilAlerta modo) {
        return terminoRepository.findByEstadoAndModoOrderByNombreAsc(EstadoPerfil.PUBLICADO, modo);
    }

    public List<PerfilRecordatorioAudiencia> audienciasPublicadas() {
        return audienciaRepository.findByEstadoOrderByNombreAsc(EstadoPerfil.PUBLICADO);
    }

    @Transactional
    public PerfilAlertaTermino guardarTermino(PerfilAlertaTermino perfil) {
        normalizar(perfil);
        validar(perfil);
        if (perfil.getId() != null) {
            PerfilAlertaTermino existente = terminoRepository.findById(perfil.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil de término no encontrado."));
            if (existente.estaPublicado()) {
                throw new BadRequestException("Un perfil publicado es inmutable. Crea una nueva versión.");
            }
            perfil.setCreatedAt(existente.getCreatedAt());
        } else if (terminoRepository.existsByCodigoAndVersion(perfil.getCodigo(), perfil.getVersion())) {
            throw new BadRequestException("Ya existe esa versión del perfil de término.");
        }
        return terminoRepository.save(perfil);
    }

    @Transactional
    public PerfilRecordatorioAudiencia guardarAudiencia(PerfilRecordatorioAudiencia perfil) {
        normalizar(perfil);
        validar(perfil);
        if (perfil.getId() != null) {
            PerfilRecordatorioAudiencia existente = audienciaRepository.findById(perfil.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil de audiencia no encontrado."));
            if (existente.estaPublicado()) {
                throw new BadRequestException("Un perfil publicado es inmutable. Crea una nueva versión.");
            }
            perfil.setCreatedAt(existente.getCreatedAt());
        } else if (audienciaRepository.existsByCodigoAndVersion(perfil.getCodigo(), perfil.getVersion())) {
            throw new BadRequestException("Ya existe esa versión del perfil de audiencia.");
        }
        return audienciaRepository.save(perfil);
    }

    @Transactional
    public void publicarTermino(Integer id) {
        PerfilAlertaTermino perfil = terminoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de término no encontrado."));
        validar(perfil);
        perfil.setEstado(EstadoPerfil.PUBLICADO);
        if (perfil.getVigenteDesde() == null) perfil.setVigenteDesde(LocalDate.now());
        terminoRepository.save(perfil);
    }

    @Transactional
    public void publicarAudiencia(Integer id) {
        PerfilRecordatorioAudiencia perfil = audienciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de audiencia no encontrado."));
        validar(perfil);
        perfil.setEstado(EstadoPerfil.PUBLICADO);
        if (perfil.getVigenteDesde() == null) perfil.setVigenteDesde(LocalDate.now());
        audienciaRepository.save(perfil);
    }

    @Transactional
    public void inactivarTermino(Integer id) {
        PerfilAlertaTermino perfil = terminoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de término no encontrado."));
        perfil.setEstado(EstadoPerfil.INACTIVO);
        perfil.setVigenteHasta(LocalDate.now());
        terminoRepository.save(perfil);
    }

    @Transactional
    public void inactivarAudiencia(Integer id) {
        PerfilRecordatorioAudiencia perfil = audienciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de audiencia no encontrado."));
        perfil.setEstado(EstadoPerfil.INACTIVO);
        perfil.setVigenteHasta(LocalDate.now());
        audienciaRepository.save(perfil);
    }

    public void validar(PerfilAlertaTermino perfil) {
        requerirTexto(perfil.getCodigo(), "El código es obligatorio.");
        requerirTexto(perfil.getNombre(), "El nombre es obligatorio.");
        requerirTexto(perfil.getDescripcion(), "La descripción es obligatoria.");
        if (perfil.getModo() == null) throw new BadRequestException("Selecciona el modo compatible.");
        if (perfil.getModo() == ModoPerfilAlerta.DIAS_RESTANTES) {
            if (perfil.getAmarilloDias() == null || perfil.getRojoDias() == null
                    || perfil.getRojoDias() < 0 || perfil.getAmarilloDias() <= perfil.getRojoDias()) {
                throw new BadRequestException("En días restantes, amarillo debe ser mayor que rojo y ambos no negativos.");
            }
        } else {
            validarPorcentaje(perfil.getAmarilloPorcentaje(), "amarillo");
            validarPorcentaje(perfil.getRojoPorcentaje(), "rojo");
            if (perfil.getAmarilloPorcentaje() <= perfil.getRojoPorcentaje()) {
                throw new BadRequestException("El porcentaje amarillo debe ser mayor que el rojo.");
            }
            validarLimites(perfil.getAmarilloMinimoDias(), perfil.getAmarilloMaximoDias(), "amarillo");
            validarLimites(perfil.getRojoMinimoDias(), perfil.getRojoMaximoDias(), "rojo");
            if (perfil.getAmarilloMinimoDias() <= perfil.getRojoMinimoDias()
                    || perfil.getAmarilloMaximoDias() <= perfil.getRojoMaximoDias()) {
                throw new BadRequestException("Los límites amarillos deben ser mayores que los rojos.");
            }
        }
        validarVigencia(perfil.getVigenteDesde(), perfil.getVigenteHasta());
    }

    public void validar(PerfilRecordatorioAudiencia perfil) {
        requerirTexto(perfil.getCodigo(), "El código es obligatorio.");
        requerirTexto(perfil.getNombre(), "El nombre es obligatorio.");
        requerirTexto(perfil.getDescripcion(), "La descripción es obligatoria.");
        if (perfil.getAnticipacionAmarillaHoras() == null || perfil.getAnticipacionRojaHoras() == null
                || perfil.getAnticipacionRojaHoras() < 0
                || perfil.getAnticipacionAmarillaHoras() <= perfil.getAnticipacionRojaHoras()) {
            throw new BadRequestException("La anticipación amarilla debe ser mayor que la roja.");
        }
        if (perfil.getRecordatoriosMinutos() == null || !perfil.getRecordatoriosMinutos().matches("\\d+(,\\d+)*")) {
            throw new BadRequestException("Los recordatorios deben expresarse en minutos separados por comas.");
        }
        validarVigencia(perfil.getVigenteDesde(), perfil.getVigenteHasta());
    }

    private void normalizar(PerfilAlertaTermino perfil) {
        perfil.setCodigo(normalizarCodigo(perfil.getCodigo()));
        if (perfil.getVersion() == null) perfil.setVersion(1);
        if (perfil.getEstado() == null) perfil.setEstado(EstadoPerfil.BORRADOR);
        if (perfil.getModo() == ModoPerfilAlerta.DIAS_RESTANTES) {
            perfil.setAmarilloPorcentaje(null);
            perfil.setAmarilloMinimoDias(null);
            perfil.setAmarilloMaximoDias(null);
            perfil.setRojoPorcentaje(null);
            perfil.setRojoMinimoDias(null);
            perfil.setRojoMaximoDias(null);
        } else {
            perfil.setAmarilloDias(null);
            perfil.setRojoDias(null);
        }
    }

    private void normalizar(PerfilRecordatorioAudiencia perfil) {
        perfil.setCodigo(normalizarCodigo(perfil.getCodigo()));
        if (perfil.getVersion() == null) perfil.setVersion(1);
        if (perfil.getEstado() == null) perfil.setEstado(EstadoPerfil.BORRADOR);
        if (perfil.getRecordatoriosMinutos() != null) {
            perfil.setRecordatoriosMinutos(perfil.getRecordatoriosMinutos().replaceAll("\\s+", ""));
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase().replaceAll("[^A-Z0-9_]+", "_");
    }

    private void validarPorcentaje(Integer valor, String nombre) {
        if (valor == null || valor < 0 || valor > 100) {
            throw new BadRequestException("El porcentaje " + nombre + " debe estar entre 0 y 100.");
        }
    }

    private void validarLimites(Integer minimo, Integer maximo, String nombre) {
        if (minimo == null || maximo == null || minimo < 0 || minimo > maximo) {
            throw new BadRequestException("El mínimo " + nombre + " debe ser menor o igual que el máximo.");
        }
    }

    private void validarVigencia(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BadRequestException("La vigencia final no puede ser anterior a la inicial.");
        }
    }

    private void requerirTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new BadRequestException(mensaje);
    }
}
