package com.juridico.sistema_juridico.service.procesal;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import com.juridico.sistema_juridico.Entity.catalogo.TipoTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilAlertaTermino;
import com.juridico.sistema_juridico.Entity.configuracion.PerfilRecordatorioAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstatusAudiencia;
import com.juridico.sistema_juridico.Entity.enums.EstatusTermino;
import com.juridico.sistema_juridico.Entity.enums.ModoPerfilAlerta;
import com.juridico.sistema_juridico.Entity.enums.ModoPlazoTermino;
import com.juridico.sistema_juridico.Entity.enums.NivelAtencionTemporal;
import com.juridico.sistema_juridico.Entity.procesal.Audiencia;
import com.juridico.sistema_juridico.Entity.procesal.Termino;
import com.juridico.sistema_juridico.dto.response.procesal.AtencionTemporal;

/**
 * Única fuente de verdad para la atención temporal mostrada en SIAJ.
 * Las entidades y las plantillas no deben reconstruir estas reglas.
 */
@Service("atencionTemporal")
public class EvaluadorAtencionTemporalService {

    private final CalculadoraPlazoService calculadoraPlazo;

    public EvaluadorAtencionTemporalService(CalculadoraPlazoService calculadoraPlazo) {
        this.calculadoraPlazo = calculadoraPlazo;
    }

    public AtencionTemporal evaluarTermino(Termino termino) {
        return evaluarTermino(termino, LocalDate.now());
    }

    public AtencionTemporal evaluarTermino(Termino termino, LocalDate fechaEvaluacion) {
        if (termino == null) return sinCalculo("Sin término", "No hay información para evaluar.");

        if (termino.getEstatusTermino() == EstatusTermino.PRESENTADO) {
            return cerrado("Presentado", "El riesgo de vencimiento se detuvo al registrar la presentación.", "fa-paper-plane");
        }
        if (termino.getEstatusTermino() == EstatusTermino.CONCLUIDO) {
            return cerrado("Concluido", "El término se encuentra concluido.", "fa-circle-check");
        }

        TipoTermino tipo = termino.getTipoTerminoRef();
        if (tipo != null && tipo.getModoPlazo() == ModoPlazoTermino.SIN_VENCIMIENTO) {
            return sinCalculo("Sin vencimiento", "Este tipo no genera alertas temporales.");
        }
        if (termino.getFechaVencimiento() == null) {
            return sinCalculo("Sin vencimiento", "No existe una fecha límite aplicable.");
        }
        boolean tieneSnapshot = termino.getAlertaAmarilloDias() != null && termino.getAlertaRojoDias() != null;
        if (!tieneSnapshot && (tipo == null || tipo.getPerfilAlerta() == null)) {
            return sinCalculo("Sin regla", "El Tipo de Término debe tener una Regla de alerta publicada.");
        }

        PerfilAlertaTermino perfil = tieneSnapshot ? null : tipo.getPerfilAlerta();
        int[] umbrales = tieneSnapshot
                ? new int[] {termino.getAlertaAmarilloDias(), termino.getAlertaRojoDias()}
                : resolverUmbrales(termino, perfil);
        String tipoDias = resolverTipoDias(termino);
        long restantes = termino.getPlazoCalendarioVersion() != null
                ? calculadoraPlazo.diasEntreSnapshot(termino, fechaEvaluacion, termino.getFechaVencimiento())
                : calculadoraPlazo.diasEntre(fechaEvaluacion, termino.getFechaVencimiento(), tipoDias);
        String unidad = "HABILES".equalsIgnoreCase(tipoDias) ? "días hábiles" : "días naturales";
        String nombreRegla = tieneSnapshot ? termino.getAlertaPerfilNombre() : perfil.getNombre();
        String explicacion = "Regla “" + nombreRegla + "”: amarillo con " + umbrales[0]
                + " días y rojo con " + umbrales[1] + " días restantes.";

        if (restantes < 0) {
            long atraso = Math.abs(restantes);
            return resultado(NivelAtencionTemporal.VENCIDO, "Vencido",
                    "Venció hace " + atraso + (atraso == 1 ? " día" : " días"), explicacion,
                    "fa-triangle-exclamation", "bg-rose-100 text-rose-900 border-rose-300",
                    "bg-rose-800", restantes, unidad, umbrales);
        }
        if (restantes <= umbrales[1]) {
            String etiqueta = restantes == 0 ? "Vence hoy" : restantes == 1 ? "Vence mañana" : "Vencimiento urgente";
            return resultado(NivelAtencionTemporal.ROJO, etiqueta, detalleRestante(restantes, unidad), explicacion,
                    "fa-circle-exclamation", "bg-red-50 text-red-800 border-red-300",
                    "bg-red-600", restantes, unidad, umbrales);
        }
        if (restantes <= umbrales[0]) {
            return resultado(NivelAtencionTemporal.AMARILLO, "Atención próxima", detalleRestante(restantes, unidad), explicacion,
                    "fa-clock", "bg-amber-50 text-amber-900 border-amber-300",
                    "bg-amber-400", restantes, unidad, umbrales);
        }
        return resultado(NivelAtencionTemporal.VERDE, "A tiempo", detalleRestante(restantes, unidad), explicacion,
                "fa-circle-check", "bg-emerald-50 text-emerald-800 border-emerald-300",
                "bg-emerald-500", restantes, unidad, umbrales);
    }

    public AtencionTemporal evaluarAudiencia(Audiencia audiencia) {
        return evaluarAudiencia(audiencia, LocalDateTime.now());
    }

    public AtencionTemporal evaluarAudiencia(Audiencia audiencia, LocalDateTime fechaEvaluacion) {
        if (audiencia == null || audiencia.getFechaAudiencia() == null) {
            return sinCalculo("Sin fecha", "La audiencia no tiene fecha programada.");
        }
        if (audiencia.getEstatusAudiencia() != null && audiencia.getEstatusAudiencia() != EstatusAudiencia.PENDIENTE) {
            if (audiencia.getEstatusAudiencia() == EstatusAudiencia.SUSPENDIDA) {
                return cerrado("Suspendida", "La audiencia fue suspendida y cuenta con Nota Informativa.", "fa-pause-circle");
            }
            String etiqueta = audiencia.getEstatusAudiencia() == EstatusAudiencia.CONCLUIDA ? "Concluida" : "Resultado registrado";
            return cerrado(etiqueta, "La proximidad dejó de estar activa.", "fa-circle-check");
        }
        boolean tieneSnapshot = audiencia.getRecordatorioAmarilloHoras() != null
                && audiencia.getRecordatorioRojoHoras() != null;
        if (!tieneSnapshot && (audiencia.getTipoAudiencia() == null
                || audiencia.getTipoAudiencia().getPerfilRecordatorio() == null)) {
            return sinCalculo("Sin regla", "El Tipo de Audiencia debe tener una Regla de alerta publicada.");
        }

        PerfilRecordatorioAudiencia perfil = tieneSnapshot ? null : audiencia.getTipoAudiencia().getPerfilRecordatorio();
        LocalDateTime fechaHora = audiencia.getFechaAudiencia().atTime(
                audiencia.getHoraAudiencia() == null ? LocalTime.of(9, 0) : audiencia.getHoraAudiencia());
        long minutos = Duration.between(fechaEvaluacion, fechaHora).toMinutes();
        int amarilloHoras = tieneSnapshot ? audiencia.getRecordatorioAmarilloHoras() : perfil.getAnticipacionAmarillaHoras();
        int rojoHoras = tieneSnapshot ? audiencia.getRecordatorioRojoHoras() : perfil.getAnticipacionRojaHoras();
        int[] umbrales = {amarilloHoras, rojoHoras};
        String nombreRegla = tieneSnapshot ? audiencia.getRecordatorioPerfilNombre() : perfil.getNombre();
        String explicacion = "Regla “" + nombreRegla + "”: atención desde "
                + formatoHoras(amarilloHoras) + " e inminente desde " + formatoHoras(rojoHoras) + ".";

        if (minutos < 0) {
            return resultado(NivelAtencionTemporal.PASADA_SIN_RESULTADO, "Pasada sin resultado",
                    "La fecha y hora programadas ya pasaron.", explicacion, "fa-triangle-exclamation",
                    "bg-rose-100 text-rose-900 border-rose-300", "bg-rose-800", minutos, "minutos", umbrales);
        }

        long horas = (long) Math.ceil(minutos / 60.0);
        String detalle = detalleAudiencia(minutos);
        if (minutos <= rojoHoras * 60L) {
            String etiqueta = minutos < 24 * 60L && fechaHora.toLocalDate().equals(fechaEvaluacion.toLocalDate())
                    ? "Hoy · " + fechaHora.toLocalTime() : "Audiencia inminente";
            return resultado(NivelAtencionTemporal.ROJO, etiqueta, detalle, explicacion, "fa-gavel",
                    "bg-red-50 text-red-800 border-red-300", "bg-red-600", horas, "horas", umbrales);
        }
        if (minutos <= amarilloHoras * 60L) {
            return resultado(NivelAtencionTemporal.AMARILLO, "Audiencia próxima", detalle, explicacion, "fa-calendar-day",
                    "bg-amber-50 text-amber-900 border-amber-300", "bg-amber-400", horas, "horas", umbrales);
        }
        return resultado(NivelAtencionTemporal.VERDE, "Programada", detalle, explicacion, "fa-calendar-check",
                "bg-emerald-50 text-emerald-800 border-emerald-300", "bg-emerald-500", horas, "horas", umbrales);
    }

    public int[] resolverUmbrales(Termino termino, PerfilAlertaTermino perfil) {
        if (perfil.getModo() == ModoPerfilAlerta.DIAS_RESTANTES) {
            return new int[] {perfil.getAmarilloDias(), perfil.getRojoDias()};
        }
        LocalDate origen = termino.getFechaIngreso() != null ? termino.getFechaIngreso() : termino.getFechaCreacion();
        if (origen == null) origen = LocalDate.now();
        long duracion = Math.max(1, termino.getPlazoCalendarioVersion() != null
                ? calculadoraPlazo.diasEntreSnapshot(termino, origen, termino.getFechaVencimiento())
                : calculadoraPlazo.diasEntre(origen, termino.getFechaVencimiento(), resolverTipoDias(termino)));
        int amarillo = limitar((int) Math.ceil(duracion * perfil.getAmarilloPorcentaje() / 100.0),
                perfil.getAmarilloMinimoDias(), perfil.getAmarilloMaximoDias());
        int rojo = limitar((int) Math.ceil(duracion * perfil.getRojoPorcentaje() / 100.0),
                perfil.getRojoMinimoDias(), perfil.getRojoMaximoDias());
        return new int[] {amarillo, rojo};
    }

    public void fijarSnapshot(Termino termino) {
        if (termino == null || termino.getTipoTerminoRef() == null
                || termino.getTipoTerminoRef().getPerfilAlerta() == null
                || termino.getFechaVencimiento() == null) {
            return;
        }
        PerfilAlertaTermino perfil = termino.getTipoTerminoRef().getPerfilAlerta();
        int[] umbrales = resolverUmbrales(termino, perfil);
        termino.setAlertaPerfilNombre(perfil.getNombre());
        termino.setAlertaPerfilVersion(perfil.getVersion());
        termino.setAlertaModo(perfil.getModo());
        termino.setAlertaAmarilloDias(umbrales[0]);
        termino.setAlertaRojoDias(umbrales[1]);
        termino.setAlertaAmarilloPorcentajeSnapshot(perfil.getAmarilloPorcentaje());
        termino.setAlertaAmarilloMinimoSnapshot(perfil.getAmarilloMinimoDias());
        termino.setAlertaAmarilloMaximoSnapshot(perfil.getAmarilloMaximoDias());
        termino.setAlertaRojoPorcentajeSnapshot(perfil.getRojoPorcentaje());
        termino.setAlertaRojoMinimoSnapshot(perfil.getRojoMinimoDias());
        termino.setAlertaRojoMaximoSnapshot(perfil.getRojoMaximoDias());
    }

    public void recalcularUmbralesDesdeSnapshot(Termino termino) {
        if (termino == null || termino.getFechaVencimiento() == null
                || termino.getAlertaModo() != ModoPerfilAlerta.PROPORCIONAL_LIMITADO) return;
        if (termino.getAlertaAmarilloPorcentajeSnapshot() == null
                || termino.getAlertaRojoPorcentajeSnapshot() == null
                || termino.getAlertaAmarilloMinimoSnapshot() == null
                || termino.getAlertaAmarilloMaximoSnapshot() == null
                || termino.getAlertaRojoMinimoSnapshot() == null
                || termino.getAlertaRojoMaximoSnapshot() == null) return;
        LocalDate origen = termino.getFechaIngreso() != null ? termino.getFechaIngreso() : termino.getFechaCreacion();
        long duracion = Math.max(1, calculadoraPlazo.diasEntreSnapshot(
                termino, origen, termino.getFechaVencimiento()));
        termino.setPlazoDuracionAdoptada(duracion);
        termino.setAlertaAmarilloDias(limitar(
                (int) Math.ceil(duracion * termino.getAlertaAmarilloPorcentajeSnapshot() / 100.0),
                termino.getAlertaAmarilloMinimoSnapshot(), termino.getAlertaAmarilloMaximoSnapshot()));
        termino.setAlertaRojoDias(limitar(
                (int) Math.ceil(duracion * termino.getAlertaRojoPorcentajeSnapshot() / 100.0),
                termino.getAlertaRojoMinimoSnapshot(), termino.getAlertaRojoMaximoSnapshot()));
    }

    public void fijarSnapshot(Audiencia audiencia) {
        if (audiencia == null || audiencia.getTipoAudiencia() == null
                || audiencia.getTipoAudiencia().getPerfilRecordatorio() == null) {
            return;
        }
        PerfilRecordatorioAudiencia perfil = audiencia.getTipoAudiencia().getPerfilRecordatorio();
        audiencia.setRecordatorioPerfilNombre(perfil.getNombre());
        audiencia.setRecordatorioPerfilVersion(perfil.getVersion());
        audiencia.setRecordatorioAmarilloHoras(perfil.getAnticipacionAmarillaHoras());
        audiencia.setRecordatorioRojoHoras(perfil.getAnticipacionRojaHoras());
    }

    private String resolverTipoDias(Termino termino) {
        if (termino.getTipoDiasCalculado() != null) return termino.getTipoDiasCalculado();
        if (termino.getTipoTerminoRef() != null && termino.getTipoTerminoRef().getTipoDias() != null) {
            return termino.getTipoTerminoRef().getTipoDias();
        }
        return "NATURALES";
    }

    private int limitar(int valor, int minimo, int maximo) {
        return Math.max(minimo, Math.min(valor, maximo));
    }

    private String detalleRestante(long restantes, String unidad) {
        if (restantes == 0) return "La fecha límite es hoy.";
        return "Quedan " + restantes + " " + unidad + ".";
    }

    private String detalleAudiencia(long minutos) {
        if (minutos < 60) return "Audiencia en " + minutos + " minutos.";
        if (minutos < 48 * 60) {
            long horas = (long) Math.ceil(minutos / 60.0);
            return "Audiencia en " + horas + (horas == 1 ? " hora." : " horas.");
        }
        long dias = (long) Math.ceil(minutos / 1440.0);
        return "Audiencia en " + dias + (dias == 1 ? " día." : " días.");
    }

    private String formatoHoras(int horas) {
        if (horas % 24 == 0) {
            int dias = horas / 24;
            return dias + (dias == 1 ? " día" : " días");
        }
        return horas + (horas == 1 ? " hora" : " horas");
    }

    private AtencionTemporal sinCalculo(String etiqueta, String detalle) {
        return new AtencionTemporal(NivelAtencionTemporal.SIN_CALCULO, etiqueta, detalle, detalle,
                "fa-minus", "bg-slate-100 text-slate-700 border-slate-300", "bg-slate-400",
                null, null, null, null);
    }

    private AtencionTemporal cerrado(String etiqueta, String detalle, String icono) {
        return new AtencionTemporal(NivelAtencionTemporal.CERRADO, etiqueta, detalle, detalle,
                icono, "bg-blue-50 text-blue-800 border-blue-200", "bg-blue-500",
                null, null, null, null);
    }

    private AtencionTemporal resultado(NivelAtencionTemporal nivel, String etiqueta, String detalle,
            String explicacion, String icono, String clasesBadge, String clasesPunto,
            long restante, String unidad, int[] umbrales) {
        return new AtencionTemporal(nivel, etiqueta, detalle, explicacion, icono, clasesBadge, clasesPunto,
                restante, unidad, umbrales[0], umbrales[1]);
    }
}
