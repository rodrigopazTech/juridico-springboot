package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;
import com.juridico.sistema_juridico.Entity.expediente.BitacoraDato;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.expediente.ExpedienteEtapaPeriodo;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.Entity.usuario.ActividadUsuario;
import com.juridico.sistema_juridico.repository.Expediente.ActividadExpedienteRepository;
import com.juridico.sistema_juridico.repository.Expediente.BitacoraDatoRepository;
import com.juridico.sistema_juridico.repository.Usuarios.ActividadUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

        private final ActividadExpedienteRepository actividadRepository;
        private final BitacoraDatoRepository bitacoraDatoRepository;
        private final ActividadUsuarioRepository actividadUsuarioRepository;
        private final ExpedienteEtapaPeriodoService etapaPeriodoService;

        /**
         * Registra una actividad general en el historial del expediente.
         */
        @Transactional
        public void registrar(Expediente expediente, String titulo, String descripcion, String tipoIcono,
                        Usuario usuario) {
                ExpedienteEtapaPeriodo periodo = etapaPeriodoService.asegurarPeriodoActivo(expediente, usuario);
                ActividadExpediente actividad = ActividadExpediente.builder()
                                .expediente(expediente)
                                .titulo(titulo)
                                .descripcion(descripcion)
                                .tipoIcono(tipoIcono)
                                .usuario(usuario)
                                .etapaProcesal(periodo != null ? periodo.getEtapaProcesal()
                                                : expediente != null ? expediente.getEtapaProcesal() : null)
                                .etapaCatalogo(periodo != null ? periodo.getEtapaCatalogo()
                                                : expediente != null ? expediente.getEtapaCatalogo() : null)
                                .etapaPeriodo(periodo)
                                .fechaRegistro(LocalDateTime.now())
                                .build();
                actividadRepository.save(actividad);
        }

        /**
         * Registra una actividad no vinculada a un expediente (ej. Login/Logout).
         */
        @Transactional
        public void registrarActividadGeneral(Usuario usuario, String titulo, String descripcion, String tipoIcono) {
                ActividadUsuario actividad = ActividadUsuario.builder()
                                .titulo(titulo)
                                .descripcion(descripcion)
                                .tipoIcono(tipoIcono)
                                .usuario(usuario)
                                .fechaRegistro(LocalDateTime.now())
                                .build();
                actividadUsuarioRepository.save(actividad);
        }

        /**
         * Compara dos versiones de un expediente y registra los cambios de forma
         * estructurada.
         */
        @Transactional
        public void registrarCambiosEstructurados(Expediente actual, Expediente anterior, Usuario usuario) {
                if (anterior == null || actual == null) {
                        System.out.println("AUDITORIA: Uno de los expedientes es null, saliendo.");
                        return;
                }

                System.out.println("AUDITORIA: Iniciando comparación para Expediente " + actual.getNumero());
                List<BitacoraDato> cambios = new ArrayList<>();
                StringBuilder resumen = new StringBuilder();

                // Comparación de campos básicos
                compararYAgregar(cambios, actual, anterior, "Número", anterior.getNumero(), actual.getNumero());
                compararYAgregar(cambios, actual, anterior, "Descripción", anterior.getDescripcion(),
                                actual.getDescripcion());
                compararYAgregar(cambios, actual, anterior, "Sede", anterior.getSede(), actual.getSede());
                compararYAgregar(cambios, actual, anterior, "Partes", anterior.getPartes(), actual.getPartes());

                // Comparación de relaciones (Null-Safe)
                String materiaAnt = (anterior.getMateria() != null) ? anterior.getMateria().getNombre() : "N/A";
                String materiaAct = (actual.getMateria() != null) ? actual.getMateria().getNombre() : "N/A";
                compararYAgregar(cambios, actual, anterior, "Materia", materiaAnt, materiaAct);

                String gerenciaAnt = (anterior.getGerencia() != null) ? anterior.getGerencia().getNombre() : "N/A";
                String gerenciaAct = (actual.getGerencia() != null) ? actual.getGerencia().getNombre() : "N/A";
                compararYAgregar(cambios, actual, anterior, "Gerencia", gerenciaAnt, gerenciaAct);

                String organoAnt = (anterior.getOrganoJurisdiccional() != null) ? anterior.getOrganoJurisdiccional().getNombre() : "N/A";
                String organoAct = (actual.getOrganoJurisdiccional() != null) ? actual.getOrganoJurisdiccional().getNombre() : "N/A";
                compararYAgregar(cambios, actual, anterior, "Órgano Jurisdiccional", organoAnt, organoAct);

                String prioridadAnt = (anterior.getPrioridad() != null) ? anterior.getPrioridad().getNombre() : "N/A";
                String prioridadAct = (actual.getPrioridad() != null) ? actual.getPrioridad().getNombre() : "N/A";
                compararYAgregar(cambios, actual, anterior, "Prioridad", prioridadAnt, prioridadAct);

                String tipoAnt = (anterior.getTipoExpediente() != null) ? anterior.getTipoExpediente().getNombre() : "N/A";
                String tipoAct = (actual.getTipoExpediente() != null) ? actual.getTipoExpediente().getNombre() : "N/A";
                compararYAgregar(cambios, actual, anterior, "Tipo de Expediente", tipoAnt, tipoAct);

                compararYAgregar(cambios, actual, anterior, "Órgano Jurisdiccional (Texto)", anterior.getOrganoJurisdiccionalTexto(), actual.getOrganoJurisdiccionalTexto());

                String etapaAnt = (anterior.getEtapaProcesal() != null) ? anterior.getEtapaProcesal().name() : "N/A";
                String etapaAct = (actual.getEtapaProcesal() != null) ? actual.getEtapaProcesal().name() : "N/A";
                compararYAgregar(cambios, actual, anterior, "Etapa Procesal", etapaAnt, etapaAct);

                String abogadoAnt = (anterior.getAbogadoResponsable() != null) ? anterior.getAbogadoResponsable().getNombreCompleto() : "N/A";
                String abogadoAct = (actual.getAbogadoResponsable() != null) ? actual.getAbogadoResponsable().getNombreCompleto() : "N/A";
                compararYAgregar(cambios, actual, anterior, "Abogado Responsable", abogadoAnt, abogadoAct);

                if (!cambios.isEmpty()) {
                        System.out.println("AUDITORIA: Detectados " + cambios.size() + " cambios.");
                        ExpedienteEtapaPeriodo periodo = etapaPeriodoService.asegurarPeriodoActivo(actual, usuario);
                        for (BitacoraDato cambio : cambios) {
                                cambio.setUsuario(usuario);
                                cambio.setFecha(LocalDateTime.now());
                                cambio.setEtapaProcesal(periodo != null ? periodo.getEtapaProcesal() : actual.getEtapaProcesal());
                                cambio.setEtapaCatalogo(periodo != null ? periodo.getEtapaCatalogo() : actual.getEtapaCatalogo());
                                cambio.setEtapaPeriodo(periodo);
                                bitacoraDatoRepository.save(cambio);

                                resumen.append(cambio.getCampo()).append(": ")
                                                .append(cambio.getValorAnterior()).append(" -> ")
                                                .append(cambio.getValorNuevo()).append("\n");
                        }

                        registrar(actual, "Actualización de datos", resumen.toString(), "EDIT", usuario);
                } else {
                        System.out.println("AUDITORIA: No se detectaron cambios.");
                }
        }

        private void compararYAgregar(List<BitacoraDato> cambios, Expediente actual, Expediente anterior,
                        String nombreCampo, Object vAnterior, Object vNuevo) {
                String sAnterior = (vAnterior == null) ? "N/A" : String.valueOf(vAnterior).trim();
                String sNuevo = (vNuevo == null) ? "N/A" : String.valueOf(vNuevo).trim();

                if (!sAnterior.equals(sNuevo)) {
                        System.out.println("AUDITORIA: Cambio en [" + nombreCampo + "]: '" + sAnterior + "' -> '" + sNuevo + "'");
                        cambios.add(BitacoraDato.builder()
                                        .expediente(actual)
                                        .campo(nombreCampo)
                                        .valorAnterior(sAnterior)
                                        .valorNuevo(sNuevo)
                                        .build());
                }
        }
}
