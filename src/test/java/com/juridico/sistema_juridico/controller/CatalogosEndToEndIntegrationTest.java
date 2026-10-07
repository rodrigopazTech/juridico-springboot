package com.juridico.sistema_juridico.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juridico.sistema_juridico.Entity.catalogo.CatPrioridad;
import com.juridico.sistema_juridico.Entity.catalogo.EtapaCatalogo;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.enums.EtapaProcesal;
import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.CatPrioridadRepository;
import com.juridico.sistema_juridico.repository.Catalogo.EstadoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.EtapaCatalogoRepository;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.OrganoJurisdiccionalRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoExpedienteRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteEtapaPeriodoRepository;
import com.juridico.sistema_juridico.repository.Expediente.ExpedienteRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "catalogos.e2e@siaj.test", authorities = "ADMIN")
class CatalogosEndToEndIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private GerenciaRepository gerencias;
    @Autowired private MateriaRepository materias;
    @Autowired private OrganoJurisdiccionalRepository organos;
    @Autowired private TipoExpedienteRepository tiposExpediente;
    @Autowired private EstadoRepository sedes;
    @Autowired private EtapaCatalogoRepository etapas;
    @Autowired private CatPrioridadRepository prioridades;
    @Autowired private ExpedienteRepository expedientes;
    @Autowired private ExpedienteEtapaPeriodoRepository periodos;

    private Gerencia gerencia;

    @BeforeEach
    void prepararActor() {
        gerencia = gerencias.save(Gerencia.builder()
                .nombre("Gerencia Catálogos E2E")
                .activo(true)
                .build());
        Usuario admin = Usuario.builder()
                .nombreCompleto("Administrador Catálogos E2E")
                .email("catalogos.e2e@siaj.test")
                .username("catalogos.e2e")
                .password("test")
                .rol(RolUsuario.ADMIN)
                .activo(true)
                .forcePasswordChange(false)
                .build();
        admin.setGerencia(gerencia);
        usuarios.save(admin);
    }

    @Test
    void permiteAltaEdicionConsultaEstadoYEliminacionDeCadaCatalogoVisible() throws Exception {
        Materia materia = crearMateria("Civil E2E");
        crearOrgano("Juzgado Civil E2E");
        crearTipoExpediente("Juicio ordinario E2E");
        crearSede("Puebla E2E");
        Usuario abogado = crearAbogado("Abogada Catálogos E2E", materia);

        mvc.perform(get("/catalogos").param("gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Materias")))
                .andExpect(content().string(containsString("Etapas Procesales")))
                .andExpect(content().string(containsString("Órganos")))
                .andExpect(content().string(containsString("Tipos de Expediente")))
                .andExpect(content().string(containsString("Sedes / Estados")))
                .andExpect(content().string(containsString("Abogados")))
                .andExpect(content().string(containsString(abogado.getNombreCompleto())));

        postJson("/catalogos/materias/guardar", Map.of("id", materia.getId().toString(), "nombre", "Civil Actualizada E2E", "gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        var organo = organos.findByNombreIgnoreCase("Juzgado Civil E2E").orElseThrow();
        postJson("/catalogos/organos/guardar", Map.of("id", organo.getId().toString(), "nombre", "Juzgado Actualizado E2E", "gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        var tipo = tiposExpediente.findByNombreIgnoreCase("Juicio ordinario E2E").orElseThrow();
        postJson("/catalogos/tipos-expediente/guardar", Map.of("id", tipo.getId().toString(), "nombre", "Juicio Actualizado E2E", "gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        var sede = sedes.findByNombreIgnoreCase("Puebla E2E").orElseThrow();
        postJson("/catalogos/sedes/guardar", Map.of("id", sede.getId().toString(), "nombre", "Puebla Actualizada E2E", "gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));

        assertEquals("Civil Actualizada E2E", materias.findById(materia.getId()).orElseThrow().getNombre());
        assertTrue(organos.findByNombreIgnoreCase("Juzgado Actualizado E2E").isPresent());
        assertTrue(tiposExpediente.findByNombreIgnoreCase("Juicio Actualizado E2E").isPresent());
        assertTrue(sedes.findByNombreIgnoreCase("Puebla Actualizada E2E").isPresent());

        mvc.perform(get("/catalogos/organos/toggle/{id}", organo.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertFalse(organos.findById(organo.getId()).orElseThrow().getActivo());
        mvc.perform(get("/catalogos/tipos-expediente/toggle/{id}", tipo.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertFalse(tiposExpediente.findById(tipo.getId()).orElseThrow().getActivo());

        mvc.perform(delete("/catalogos/organos/eliminar/{id}", organo.getId()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(delete("/catalogos/tipos-expediente/eliminar/{id}", tipo.getId()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(delete("/catalogos/sedes/eliminar/{id}", sede.getId()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(delete("/catalogos/materias/eliminar/{id}", materia.getId()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));

        assertFalse(organos.existsById(organo.getId()));
        assertFalse(tiposExpediente.existsById(tipo.getId()));
        assertFalse(sedes.existsById(sede.getId()));
        assertFalse(materias.findById(materia.getId()).orElseThrow().getActivo());
    }

    @Test
    void mantieneEtapasJerarquicasAisladasPorMateriaYBloqueaRelacionesInvalidas() throws Exception {
        Materia civil = crearMateria("Civil Etapas E2E");
        Materia fiscal = crearMateria("Fiscal Etapas E2E");
        EtapaCatalogo tramiteCivil = crearEtapa(civil, "Trámite civil", null, 1, false);
        EtapaCatalogo pruebasCivil = crearEtapa(civil, "Periodo probatorio", tramiteCivil.getId(), 2, false);
        crearEtapa(civil, "Concluido civil", null, 99, true);
        crearEtapa(fiscal, "Investigación fiscal", null, 1, false);

        mvc.perform(get("/catalogos/etapas/materia/{id}", civil.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trámite civil")))
                .andExpect(content().string(containsString("Trámite civil / Periodo probatorio")))
                .andExpect(content().string(not(containsString("Investigación fiscal"))));
        mvc.perform(get("/catalogos/etapas/materia/{id}", fiscal.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Investigación fiscal")))
                .andExpect(content().string(not(containsString("Trámite civil"))));

        postJson("/catalogos/etapas/guardar", Map.of(
                "id", pruebasCivil.getId().toString(), "nombre", "Audiencia probatoria",
                "materiaId", civil.getId().toString(), "parentId", tramiteCivil.getId().toString(),
                "orden", "4", "cierraExpediente", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
        EtapaCatalogo etapaEditada = etapas.findById(pruebasCivil.getId()).orElseThrow();
        assertEquals("Audiencia probatoria", etapaEditada.getNombre());
        assertEquals(4, etapaEditada.getOrden());
        assertEquals(tramiteCivil.getId(), etapaEditada.getParent().getId());

        postJson("/catalogos/etapas/guardar", Map.of(
                "nombre", "Subetapa inválida", "materiaId", fiscal.getId().toString(),
                "parentId", tramiteCivil.getId().toString(), "orden", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("misma materia")));

        postJson("/catalogos/etapas/guardar", Map.of(
                "nombre", "Trámite civil", "materiaId", civil.getId().toString(), "orden", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Ya existe")));

        postJson("/catalogos/etapas/guardar", Map.of(
                "id", tramiteCivil.getId().toString(), "nombre", "Trámite civil",
                "materiaId", fiscal.getId().toString(), "orden", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("cambiar la materia")));

        mvc.perform(delete("/catalogos/etapas/eliminar/{id}", pruebasCivil.getId()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        assertFalse(etapas.findById(pruebasCivil.getId()).orElseThrow().getActivo());
        mvc.perform(get("/expedientes/api/etapas-por-materia/{id}", civil.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Audiencia probatoria"))))
                .andExpect(content().string(containsString("Concluido civil")));
    }

    @Test
    void usaTodosLosCatalogosAlCrearExpedienteYCambiarSuEtapa() throws Exception {
        Materia materia = crearMateria("Civil Flujo E2E");
        crearOrgano("Juzgado Flujo E2E");
        crearTipoExpediente("Amparo Flujo E2E");
        crearSede("Querétaro Flujo E2E");
        EtapaCatalogo inicial = crearEtapa(materia, "Admisión E2E", null, 1, false);
        EtapaCatalogo cierre = crearEtapa(materia, "Archivo E2E", null, 99, true);
        Usuario abogado = crearAbogado("Abogado Flujo E2E", materia);
        CatPrioridad prioridad = prioridades.save(CatPrioridad.builder().nombre("MEDIA E2E").activo(true).build());
        var organo = organos.findByNombreIgnoreCase("Juzgado Flujo E2E").orElseThrow();
        var tipo = tiposExpediente.findByNombreIgnoreCase("Amparo Flujo E2E").orElseThrow();

        mvc.perform(get("/expedientes/api/materias-por-gerencia/{id}", gerencia.getId()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(materia.getNombre())));
        mvc.perform(get("/expedientes/api/organos-por-gerencia/{id}", gerencia.getId()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(organo.getNombre())));
        mvc.perform(get("/expedientes/api/abogados")
                        .param("gerenciaId", gerencia.getId().toString())
                        .param("materiaId", materia.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(abogado.getNombreCompleto())));
        mvc.perform(get("/expedientes/api/etapas-por-materia/{id}", materia.getId()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(inicial.getNombre())));

        mvc.perform(post("/expedientes/guardar").with(csrf())
                        .param("numero", "EXP-CATALOGOS-E2E")
                        .param("descripcion", "Expediente creado usando catálogos configurables")
                        .param("partes", "Persona actora vs autoridad responsable")
                        .param("gerencia.id", gerencia.getId().toString())
                        .param("materia.id", materia.getId().toString())
                        .param("organoJurisdiccional.id", organo.getId().toString())
                        .param("sede", "Querétaro Flujo E2E")
                        .param("abogadoResponsable.id", abogado.getId().toString())
                        .param("tipoExpediente.id", tipo.getId().toString())
                        .param("prioridad.id", prioridad.getId().toString())
                        .param("etapaCatalogo.id", inicial.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertTrue(result.getResponse().getRedirectedUrl()
                        .startsWith("/expedientes/")));

        var expediente = expedientes.findByNumeroIgnoreCase("EXP-CATALOGOS-E2E").orElseThrow();
        assertEquals(materia.getId(), expediente.getMateria().getId());
        assertEquals(organo.getId(), expediente.getOrganoJurisdiccional().getId());
        assertEquals(tipo.getId(), expediente.getTipoExpediente().getId());
        assertEquals(abogado.getId(), expediente.getAbogadoResponsable().getId());
        assertEquals(inicial.getId(), expediente.getEtapaCatalogo().getId());
        assertEquals("Querétaro Flujo E2E", expediente.getSede());
        assertNotNull(periodos.findByExpedienteIdAndFechaFinIsNull(expediente.getId()).orElse(null));

        mvc.perform(post("/expedientes/{id}/cambiar-etapa", expediente.getId()).with(csrf())
                        .param("etapaCatalogoId", cierre.getId().toString()))
                .andExpect(status().isOk());

        var concluido = expedientes.findById(expediente.getId()).orElseThrow();
        assertEquals(cierre.getId(), concluido.getEtapaCatalogo().getId());
        assertEquals(EtapaProcesal.CONCLUIDO, concluido.getEtapaProcesal());
        assertEquals(2, periodos.findByExpedienteIdOrderByFechaInicioAsc(expediente.getId()).size());

        mvc.perform(delete("/catalogos/etapas/eliminar/{id}", inicial.getId()).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(post("/expedientes/{id}/cambiar-etapa", expediente.getId()).with(csrf())
                        .param("etapaCatalogoId", inicial.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    private Materia crearMateria(String nombre) throws Exception {
        postJson("/catalogos/materias/guardar", Map.of("nombre", nombre, "gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        return materias.findByNombreIgnoreCaseAndGerenciaId(nombre, gerencia.getId()).orElseThrow();
    }

    private void crearOrgano(String nombre) throws Exception {
        postJson("/catalogos/organos/guardar", Map.of("nombre", nombre, "gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    private void crearTipoExpediente(String nombre) throws Exception {
        postJson("/catalogos/tipos-expediente/guardar", Map.of("nombre", nombre, "gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    private void crearSede(String nombre) throws Exception {
        postJson("/catalogos/sedes/guardar", Map.of("nombre", nombre, "gerenciaId", gerencia.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    private EtapaCatalogo crearEtapa(Materia materia, String nombre, Integer parentId, int orden, boolean cierra) throws Exception {
        Map<String, String> payload = new java.util.LinkedHashMap<>();
        payload.put("nombre", nombre);
        payload.put("materiaId", materia.getId().toString());
        payload.put("orden", Integer.toString(orden));
        payload.put("cierraExpediente", Boolean.toString(cierra));
        if (parentId != null) payload.put("parentId", parentId.toString());
        postJson("/catalogos/etapas/guardar", payload)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        return etapas.findByMateriaIdOrderByOrdenAscNombreAsc(materia.getId()).stream()
                .filter(e -> e.getNombre().equals(nombre)).findFirst().orElseThrow();
    }

    private Usuario crearAbogado(String nombre, Materia materia) {
        Usuario abogado = Usuario.builder()
                .nombreCompleto(nombre)
                .email("abogado." + materia.getId() + "@siaj.test")
                .username("abogado." + materia.getId())
                .password("test")
                .rol(RolUsuario.ABOGADO)
                .activo(true)
                .forcePasswordChange(false)
                .build();
        abogado.setGerencia(gerencia);
        abogado.getMaterias().add(materia);
        return usuarios.save(abogado);
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String ruta, Map<String, String> payload) throws Exception {
        return mvc.perform(post(ruta).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)));
    }
}
