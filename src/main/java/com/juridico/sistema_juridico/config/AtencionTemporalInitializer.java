package com.juridico.sistema_juridico.config;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoTerminoRepository;
import com.juridico.sistema_juridico.repository.configuracion.PerfilAlertaTerminoRepository;
import com.juridico.sistema_juridico.repository.configuracion.PerfilRecordatorioAudienciaRepository;
import com.juridico.sistema_juridico.util.CodigoNormalizer;

/**
 * Inicializador de perfiles y códigos de atención temporal.
 * Deshabilitado por defecto para no modificar silenciosamente catálogos en cada arranque.
 * La siembra inicial estándar es gestionada por Flyway (V2__catalogo_datos_iniciales.sql).
 */
@Component
@ConditionalOnProperty(name = "app.seed.temporal-atencion.enabled", havingValue = "true", matchIfMissing = false)
@Order(20)
public class AtencionTemporalInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AtencionTemporalInitializer.class);

    private final PerfilAlertaTerminoRepository perfilesTermino;
    private final PerfilRecordatorioAudienciaRepository perfilesAudiencia;
    private final TipoTerminoRepository tiposTermino;
    private final TipoAudienciaRepository tiposAudiencia;

    public AtencionTemporalInitializer(PerfilAlertaTerminoRepository perfilesTermino,
            PerfilRecordatorioAudienciaRepository perfilesAudiencia,
            TipoTerminoRepository tiposTermino,
            TipoAudienciaRepository tiposAudiencia) {
        this.perfilesTermino = perfilesTermino;
        this.perfilesAudiencia = perfilesAudiencia;
        this.tiposTermino = tiposTermino;
        this.tiposAudiencia = tiposAudiencia;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Ejecutando inicialización manual de atención temporal y catálogos...");
        PerfilAlertaTermino corto = perfilDias("PLAZO_CORTO", "Plazo corto", 2, 1);
        PerfilAlertaTermino medio = perfilDias("PLAZO_MEDIO", "Plazo medio", 5, 2);
        PerfilAlertaTermino variable = perfilVariable();
        PerfilRecordatorioAudiencia audiencia = perfilAudiencia();

        for (TipoTermino tipo : tiposTermino.findAll()) {
            if (tipo.getModoPlazo() == null) tipo.setModoPlazo(ModoPlazoTermino.CALCULADO);
            if (tipo.getCodigo() == null || tipo.getCodigo().isBlank()) {
                tipo.setCodigo(CodigoNormalizer.normalizar(tipo.getNombre() != null ? tipo.getNombre() : "TERMINO", tipo.getId()));
            }
            if (tipo.getModoPlazo() == ModoPlazoTermino.SIN_VENCIMIENTO) {
                tipo.setPerfilAlerta(null);
            } else if (tipo.getPerfilAlerta() == null) {
                tipo.setPerfilAlerta(tipo.getModoPlazo() == ModoPlazoTermino.CAPTURADO ? variable : medio);
            }
            tiposTermino.save(tipo);
        }

        for (TipoAudiencia tipo : tiposAudiencia.findAll()) {
            if (tipo.getCodigo() == null || tipo.getCodigo().isBlank()) {
                tipo.setCodigo(CodigoNormalizer.normalizar(tipo.getNombre() != null ? tipo.getNombre() : "AUDIENCIA", tipo.getId()));
            }
            if (tipo.getRequiereActa() == null) tipo.setRequiereActa(false);
            if (tipo.getPermiteExcepcionSinActa() == null) tipo.setPermiteExcepcionSinActa(false);
            if (tipo.getRequiereValidacionActa() == null) tipo.setRequiereValidacionActa(false);
            if (tipo.getPerfilRecordatorio() == null) tipo.setPerfilRecordatorio(audiencia);
            tiposAudiencia.save(tipo);
        }
    }

    private PerfilAlertaTermino perfilDias(String codigo, String nombre, int amarillo, int rojo) {
        return perfilesTermino.findFirstByCodigoOrderByVersionDesc(codigo).orElseGet(() ->
                perfilesTermino.save(PerfilAlertaTermino.builder()
                        .codigo(codigo).nombre(nombre)
                        .descripcion("Perfil inicial para plazos calculados por el sistema.")
                        .modo(ModoPerfilAlerta.DIAS_RESTANTES)
                        .amarilloDias(amarillo).rojoDias(rojo)
                        .version(1).estado(EstadoPerfil.PUBLICADO).vigenteDesde(LocalDate.now())
                        .build()));
    }

    private PerfilAlertaTermino perfilVariable() {
        return perfilesTermino.findFirstByCodigoOrderByVersionDesc("PLAZO_VARIABLE_ESTANDAR").orElseGet(() ->
                perfilesTermino.save(PerfilAlertaTermino.builder()
                        .codigo("PLAZO_VARIABLE_ESTANDAR").nombre("Plazo variable estándar")
                        .descripcion("Perfil proporcional limitado para vencimientos informados al registrar.")
                        .modo(ModoPerfilAlerta.PROPORCIONAL_LIMITADO)
                        .amarilloPorcentaje(50).amarilloMinimoDias(3).amarilloMaximoDias(10)
                        .rojoPorcentaje(20).rojoMinimoDias(1).rojoMaximoDias(4)
                        .version(1).estado(EstadoPerfil.PUBLICADO).vigenteDesde(LocalDate.now())
                        .build()));
    }

    private PerfilRecordatorioAudiencia perfilAudiencia() {
        return perfilesAudiencia.findFirstByCodigoOrderByVersionDesc("AUDIENCIA_ESTANDAR").orElseGet(() ->
                perfilesAudiencia.save(PerfilRecordatorioAudiencia.builder()
                        .codigo("AUDIENCIA_ESTANDAR").nombre("Audiencia estándar")
                        .descripcion("Perfil inicial de proximidad para audiencias.")
                        .anticipacionAmarillaHoras(72).anticipacionRojaHoras(24)
                        .recordatoriosMinutos("10080,4320,1440,120")
                        .version(1).estado(EstadoPerfil.PUBLICADO).vigenteDesde(LocalDate.now())
                        .build()));
    }
}
