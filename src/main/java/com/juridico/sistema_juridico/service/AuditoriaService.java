package com.juridico.sistema_juridico.service;

import com.juridico.sistema_juridico.Entity.expediente.ActividadExpediente;
import com.juridico.sistema_juridico.Entity.expediente.BitacoraDato;
import com.juridico.sistema_juridico.Entity.expediente.Expediente;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Expediente.ActividadExpedienteRepository;
import com.juridico.sistema_juridico.repository.Expediente.BitacoraDatoRepository;
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

        /**
         * Registra una actividad general en el historial del expediente.
         */
        @Transactional
        public void registrar(Expediente expediente, String titulo, String descripcion, String tipoIcono,
                        Usuario usuario) {
                ActividadExpediente actividad = ActividadExpediente.builder()
                                .expediente(expediente)
                                .titulo(titulo)
                                .descripcion(descripcion)
                                .tipoIcono(tipoIcono)
                                .usuario(usuario)
                                .fechaRegistro(LocalDateTime.now())
                                .build();
                actividadRepository.save(actividad);
        }

        /**
         * Compara dos versiones de un expediente y registra los cambios de forma
         * estructurada.
         */
        @Transactional
        public void registrarCambiosEstructurados(Expediente actual, Expediente anterior, Usuario usuario) {
                if (anterior == null)
                        return;

                List<BitacoraDato> cambios = new ArrayList<>();
                StringBuilder resumen = new StringBuilder();

                compararYAgregar(cambios, actual, anterior, "Número", anterior.getNumero(), actual.getNumero());
                compararYAgregar(cambios, actual, anterior, "Descripción", anterior.getDescripcion(),
                                actual.getDescripcion());
                compararYAgregar(cambios, actual, anterior, "Materia",
                                anterior.getMateria() != null ? anterior.getMateria().getNombre() : "N/A",
                                actual.getMateria() != null ? actual.getMateria().getNombre() : "N/A");
                compararYAgregar(cambios, actual, anterior, "Gerencia",
                                anterior.getGerencia() != null ? anterior.getGerencia().getNombre() : "N/A",
                                actual.getGerencia() != null ? actual.getGerencia().getNombre() : "N/A");
                compararYAgregar(cambios, actual, anterior, "Órgano Jurisdiccional",
                                anterior.getOrganoJurisdiccional() != null
                                                ? anterior.getOrganoJurisdiccional().getNombre()
                                                : "N/A",
                                actual.getOrganoJurisdiccional() != null ? actual.getOrganoJurisdiccional().getNombre()
                                                : "N/A");
                compararYAgregar(cambios, actual, anterior, "Órgano (texto)", anterior.getOrganoJurisdiccionalTexto(),
                                actual.getOrganoJurisdiccionalTexto());
                compararYAgregar(cambios, actual, anterior, "Sede", anterior.getSede(), actual.getSede());
                compararYAgregar(cambios, actual, anterior, "Prioridad",
                                anterior.getPrioridad() != null ? anterior.getPrioridad().getNombre() : "N/A",
                                actual.getPrioridad() != null ? actual.getPrioridad().getNombre() : "N/A");
                compararYAgregar(cambios, actual, anterior, "Etapa Procesal",
                                anterior.getEtapaProcesal() != null ? anterior.getEtapaProcesal().name() : "N/A",
                                actual.getEtapaProcesal() != null ? actual.getEtapaProcesal().name() : "N/A");
                compararYAgregar(cambios, actual, anterior, "Abogado Responsable",
                                anterior.getAbogadoResponsable() != null
                                                ? anterior.getAbogadoResponsable().getNombreCompleto()
                                                : "N/A",
                                actual.getAbogadoResponsable() != null
                                                ? actual.getAbogadoResponsable().getNombreCompleto()
                                                : "N/A");

                if (!cambios.isEmpty()) {
                        for (BitacoraDato cambio : cambios) {
                                cambio.setUsuario(usuario);
                                cambio.setFecha(LocalDateTime.now());
                                bitacoraDatoRepository.save(cambio);

                                resumen.append("- ").append(cambio.getCampo()).append(": ")
                                                .append(cambio.getValorAnterior()).append(" -> ")
                                                .append(cambio.getValorNuevo()).append("\n");
                        }

                        registrar(actual, "Actualización de datos", resumen.toString(), "EDIT", usuario);
                }
        }

        private void compararYAgregar(List<BitacoraDato> cambios, Expediente actual, Expediente anterior,
                        String nombreCampo, Object valorAnterior, Object valorNuevo) {
                if (!Objects.equals(valorAnterior, valorNuevo)) {
                        cambios.add(BitacoraDato.builder()
                                        .expediente(actual)
                                        .campo(nombreCampo)
                                        .valorAnterior(String.valueOf(valorAnterior))
                                        .valorNuevo(String.valueOf(valorNuevo))
                                        .build());
                }
        }
}
