package com.juridico.sistema_juridico.service.configuracion;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.catalogo.TipoAudiencia;
import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstadoPerfil;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.exception.BadRequestException;
import com.juridico.sistema_juridico.repository.configuracion.PerfilAlertaTerminoRepository;
import com.juridico.sistema_juridico.repository.configuracion.PerfilRecordatorioAudienciaRepository;
import com.juridico.sistema_juridico.service.catalogo.TipoAudienciaService;
import com.juridico.sistema_juridico.service.catalogo.TipoTerminoService;
import com.juridico.sistema_juridico.util.CodigoNormalizer;

/**
 * Orquesta la configuración contextual de un tipo y mantiene las reglas
 * versionadas como un detalle interno, no como una tarea adicional del usuario.
 */
@Service
public class PlazosAlertasConfiguracionService {

    private static final String PREFIJO_TERMINO = "TIPO_TERMINO_";
    private static final String PREFIJO_AUDIENCIA = "TIPO_AUDIENCIA_";

    private final TipoTerminoService tiposTermino;
    private final TipoAudienciaService tiposAudiencia;
    private final PerfilAtencionService perfiles;
    private final PerfilAlertaTerminoRepository perfilesTermino;
    private final PerfilRecordatorioAudienciaRepository perfilesAudiencia;

    public PlazosAlertasConfiguracionService(TipoTerminoService tiposTermino,
            TipoAudienciaService tiposAudiencia, PerfilAtencionService perfiles,
            PerfilAlertaTerminoRepository perfilesTermino,
            PerfilRecordatorioAudienciaRepository perfilesAudiencia) {
        this.tiposTermino = tiposTermino;
        this.tiposAudiencia = tiposAudiencia;
        this.perfiles = perfiles;
        this.perfilesTermino = perfilesTermino;
        this.perfilesAudiencia = perfilesAudiencia;
    }

    @Transactional
    public TipoTermino guardarTipoTermino(TipoTermino tipo, Integer atencionDias, Integer criticoDias) {
        normalizarCodigo(tipo);
        PerfilAlertaTermino anterior = tipo.getId() == null ? null
                : tiposTermino.obtenerPorId(tipo.getId()).getPerfilAlerta();

        if (tipo.getModoPlazo() == ModoPlazoTermino.SIN_VENCIMIENTO) {
            if (esContextual(anterior, PREFIJO_TERMINO)) {
                perfiles.inactivarTermino(anterior.getId());
            }
            tipo.setPerfilAlerta(null);
        } else {
            validarUmbrales(atencionDias, criticoDias, "término");
            String codigo = codigoContextual(anterior, PREFIJO_TERMINO, tipo.getCodigo());
            PerfilAlertaTermino perfil = ultimoPerfilTermino(codigo)
                    .orElseGet(PerfilAlertaTermino::new);
            perfil.setCodigo(codigo);
            perfil.setNombre(nombreInterno("Avisos de ", tipo.getNombre()));
            perfil.setDescripcion("Configuración interna de avisos para el tipo de término " + tipo.getNombre() + ".");
            perfil.setModo(ModoPerfilAlerta.DIAS_RESTANTES);
            perfil.setAmarilloDias(atencionDias);
            perfil.setRojoDias(criticoDias);
            prepararEdicion(perfil);
            PerfilAlertaTermino guardado = perfiles.guardarTermino(perfil);
            perfiles.publicarTermino(guardado.getId());
            tipo.setPerfilAlerta(guardado);
        }

        return tipo.getId() == null ? tiposTermino.crear(tipo) : tiposTermino.actualizar(tipo.getId(), tipo);
    }

    @Transactional
    public TipoAudiencia guardarTipoAudiencia(TipoAudiencia tipo, Integer atencionDias, Integer criticoDias) {
        normalizarCodigo(tipo);
        validarUmbrales(atencionDias, criticoDias, "audiencia");
        PerfilRecordatorioAudiencia anterior = tipo.getId() == null ? null
                : tiposAudiencia.obtenerPorId(tipo.getId()).getPerfilRecordatorio();
        String codigo = codigoContextual(anterior, PREFIJO_AUDIENCIA, tipo.getCodigo());
        PerfilRecordatorioAudiencia perfil = ultimoPerfilAudiencia(codigo)
                .orElseGet(PerfilRecordatorioAudiencia::new);
        perfil.setCodigo(codigo);
        perfil.setNombre(nombreInterno("Avisos de ", tipo.getNombre()));
        perfil.setDescripcion("Configuración interna de avisos para el tipo de audiencia " + tipo.getNombre() + ".");
        perfil.setAnticipacionAmarillaHoras(atencionDias * 24);
        perfil.setAnticipacionRojaHoras(criticoDias * 24);
        perfil.setRecordatoriosMinutos(recordatorios(atencionDias, criticoDias));
        prepararEdicion(perfil);
        PerfilRecordatorioAudiencia guardado = perfiles.guardarAudiencia(perfil);
        perfiles.publicarAudiencia(guardado.getId());
        tipo.setPerfilRecordatorio(guardado);
        return tiposAudiencia.guardar(tipo);
    }

    private void validarUmbrales(Integer atencion, Integer critico, String contexto) {
        if (atencion == null || critico == null || critico < 0 || atencion <= critico) {
            throw new BadRequestException("En " + contexto
                    + ", Atención debe ser mayor que Crítico y ambos deben ser valores no negativos.");
        }
    }

    private void normalizarCodigo(TipoTermino tipo) {
        String base = tipo.getCodigo() == null || tipo.getCodigo().isBlank() ? tipo.getNombre() : tipo.getCodigo();
        tipo.setCodigo(CodigoNormalizer.normalizar(base));
    }

    private void normalizarCodigo(TipoAudiencia tipo) {
        String base = tipo.getCodigo() == null || tipo.getCodigo().isBlank() ? tipo.getNombre() : tipo.getCodigo();
        tipo.setCodigo(CodigoNormalizer.normalizar(base));
    }

    private String codigoContextual(Object anterior, String prefijo, String codigoTipo) {
        if (anterior instanceof PerfilAlertaTermino perfil && esContextual(perfil, prefijo)) {
            return perfil.getCodigo();
        }
        if (anterior instanceof PerfilRecordatorioAudiencia perfil && esContextual(perfil, prefijo)) {
            return perfil.getCodigo();
        }
        return CodigoNormalizer.normalizar(prefijo + codigoTipo);
    }

    private boolean esContextual(PerfilAlertaTermino perfil, String prefijo) {
        return perfil != null && perfil.getCodigo() != null && perfil.getCodigo().startsWith(prefijo);
    }

    private boolean esContextual(PerfilRecordatorioAudiencia perfil, String prefijo) {
        return perfil != null && perfil.getCodigo() != null && perfil.getCodigo().startsWith(prefijo);
    }

    private Optional<PerfilAlertaTermino> ultimoPerfilTermino(String codigo) {
        return perfilesTermino.findFirstByCodigoOrderByVersionDesc(codigo);
    }

    private Optional<PerfilRecordatorioAudiencia> ultimoPerfilAudiencia(String codigo) {
        return perfilesAudiencia.findFirstByCodigoOrderByVersionDesc(codigo);
    }

    private void prepararEdicion(PerfilAlertaTermino perfil) {
        if (perfil.getId() == null) {
            perfil.setVersion(1);
        }
        perfil.setEstado(perfil.getEstado() == null ? EstadoPerfil.BORRADOR : perfil.getEstado());
    }

    private void prepararEdicion(PerfilRecordatorioAudiencia perfil) {
        if (perfil.getId() == null) {
            perfil.setVersion(1);
        }
        perfil.setEstado(perfil.getEstado() == null ? EstadoPerfil.BORRADOR : perfil.getEstado());
    }

    private String recordatorios(int atencionDias, int criticoDias) {
        return Stream.of(atencionDias * 1440, criticoDias * 1440, 120)
                .filter(valor -> valor > 0)
                .distinct()
                .sorted((a, b) -> Integer.compare(b, a))
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("120");
    }

    private String nombreInterno(String prefijo, String nombre) {
        String resultado = prefijo + nombre.trim();
        return resultado.length() <= 120 ? resultado : resultado.substring(0, 120);
    }
}
