package com.juridico.sistema_juridico.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.juridico.sistema_juridico.Entity.enums.RolUsuario;
import com.juridico.sistema_juridico.Entity.catalogo.Gerencia;
import com.juridico.sistema_juridico.Entity.catalogo.Materia;
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.MateriaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoAudienciaRepository;
import com.juridico.sistema_juridico.repository.Catalogo.TipoTerminoRepository;
import com.juridico.sistema_juridico.repository.configuracion.DiaInhabilRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "ui.admin@siaj.test", authorities = "ADMIN")
class Alcance1PantallasIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private GerenciaRepository gerencias;
    @Autowired private MateriaRepository materias;
    @Autowired private TipoTerminoRepository tiposTermino;
    @Autowired private TipoAudienciaRepository tiposAudiencia;
    @Autowired private DiaInhabilRepository diasInhabiles;

    @BeforeEach
    void prepararUsuario() {
        if (gerencias.count() == 0) {
            gerencias.save(Gerencia.builder().nombre("Gerencia de prueba UI").activo(true).build());
        }
        if (usuarios.findByEmail("ui.admin@siaj.test").isEmpty()) {
            usuarios.save(Usuario.builder()
                    .nombreCompleto("Administración UI")
                    .email("ui.admin@siaj.test")
                    .username("ui.admin")
                    .password("test")
                    .rol(RolUsuario.ADMIN)
                    .activo(true)
                    .forcePasswordChange(false)
                    .build());
        }
    }

    @Test
    void renderizaConfiguracionContextualDePlazosYAlertas() throws Exception {
        mvc.perform(get("/administracion/plazos-alertas").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"panel-terminos\"")))
                .andExpect(content().string(containsString("id=\"panel-audiencias\"")))
                .andExpect(content().string(containsString("id=\"panel-calendario\"")))
                .andExpect(content().string(containsString("id=\"tt-preview\"")))
                .andExpect(content().string(containsString("Plantillas sugeridas")))
                .andExpect(content().string(containsString("Versión vigente")))
                .andExpect(content().string(containsString("Registrar suspensión")))
                .andExpect(content().string(not(containsString("Reglas de Término"))))
                .andExpect(content().string(not(containsString("Reglas de Audiencia"))))
                .andExpect(content().string(not(containsString("id=\"reglas-termino\""))));
    }

    @Test
    void conservaIdentidadDeNavegacionEnPlazosYConcluidos() throws Exception {
        mvc.perform(get("/administracion/plazos-alertas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("siaj-brand-name")))
                .andExpect(content().string(containsString("siaj-user-identity")))
                .andExpect(content().string(containsString("siaj-user-chevron")))
                .andExpect(content().string(not(containsString("hidden md:flex"))));

        mvc.perform(get("/agenda"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("siaj-brand-name")))
                .andExpect(content().string(containsString("siaj-user-identity")))
                .andExpect(content().string(containsString("siaj-user-chevron")))
                .andExpect(content().string(not(containsString("hidden md:flex"))));
    }

    @Test
    void guardaTipoTerminoYPublicaSuConfiguracionDeAvisos() throws Exception {
        Materia materia = materiaDePrueba();

        mvc.perform(post("/administracion/plazos-alertas/tipos-termino").with(csrf())
                        .param("materia.id", materia.getId().toString())
                        .param("nombre", "Contestación UX")
                        .param("codigo", "CONTESTACION_UX")
                        .param("descripcion", "Plazo configurado en una sola pantalla")
                        .param("modoPlazo", "CALCULADO")
                        .param("diasPredeterminados", "15")
                        .param("tipoDias", "HABILES")
                        .param("ajustaAlSiguienteHabil", "true")
                        .param("activo", "true")
                        .param("atencionDias", "5")
                        .param("criticoDias", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/administracion/plazos-alertas#terminos"));

        var tipo = tiposTermino.findByNombreIgnoreCase("Contestación UX").orElseThrow();
        assertNotNull(tipo.getPerfilAlerta());
        assertEquals(5, tipo.getPerfilAlerta().getAmarilloDias());
        assertEquals(2, tipo.getPerfilAlerta().getRojoDias());
        assertEquals("PUBLICADO", tipo.getPerfilAlerta().getEstado().name());
    }

    @Test
    void guardaTipoAudienciaYConvierteSusAvisosADias() throws Exception {
        Materia materia = materiaDePrueba();

        mvc.perform(post("/administracion/plazos-alertas/tipos-audiencia").with(csrf())
                        .param("materia.id", materia.getId().toString())
                        .param("nombre", "Audiencia UX")
                        .param("codigo", "AUDIENCIA_UX")
                        .param("descripcion", "Recordatorios configurados en contexto")
                        .param("duracionPredeterminadaMinutos", "60")
                        .param("modalidadPredeterminada", "PRESENCIAL")
                        .param("modalidadesPermitidas", "PRESENCIAL,VIRTUAL,HIBRIDA")
                        .param("activo", "true")
                        .param("atencionDias", "3")
                        .param("criticoDias", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/administracion/plazos-alertas#audiencias"));

        var tipo = tiposAudiencia.findByNombreIgnoreCase("Audiencia UX").orElseThrow();
        assertNotNull(tipo.getPerfilRecordatorio());
        assertEquals(72, tipo.getPerfilRecordatorio().getAnticipacionAmarillaHoras());
        assertEquals(24, tipo.getPerfilRecordatorio().getAnticipacionRojaHoras());
        assertEquals("PUBLICADO", tipo.getPerfilRecordatorio().getEstado().name());
    }

    @Test
    void renderizaCatalogosSinCatalogoDeSemaforos() throws Exception {
        mvc.perform(get("/catalogos").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-tab=\"tipos-termino\""))))
                .andExpect(content().string(not(containsString("data-tab=\"tipos-audiencia\""))))
                .andExpect(content().string(not(containsString("id=\"panel-semaforos\""))));
    }

    @Test
    void renderizaTerminosConReglaExplicita() throws Exception {
        mvc.perform(get("/terminos").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"select-tipo-termino\"")))
                .andExpect(content().string(containsString("id=\"input-fecha-origen\"")))
                .andExpect(content().string(containsString("id=\"resumen-regla-tipo\"")))
                .andExpect(content().string(containsString("id=\"vista-previa-plazo\"")))
                .andExpect(content().string(containsString("fa-folder-plus")))
                .andExpect(content().string(containsString("Asuntos sin expediente")));
    }

    @Test
    @WithMockUser(username = "ui.abogado@siaj.test", authorities = "ABOGADO")
    void abogadoPuedeGestionarLaConfiguracionCompartida() throws Exception {
        Gerencia gerencia = gerencias.findAll().get(0);
        if (usuarios.findByEmail("ui.abogado@siaj.test").isEmpty()) {
            Usuario abogado = Usuario.builder()
                    .nombreCompleto("Abogado UI")
                    .email("ui.abogado@siaj.test")
                    .username("ui.abogado")
                    .password("test")
                    .rol(RolUsuario.ABOGADO)
                    .activo(true)
                    .forcePasswordChange(false)
                    .build();
            abogado.getGerencias().add(gerencia);
            usuarios.save(abogado);
        }

        mvc.perform(get("/catalogos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lectura y escritura")));
        mvc.perform(get("/administracion/plazos-alertas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Configuración compartida")))
                .andExpect(content().string(containsString("id=\"form-tipo-termino\"")))
                .andExpect(content().string(containsString("data-readonly=\"false\"")))
                .andExpect(content().string(containsString("id=\"nuevo-tipo-termino\"")))
                .andExpect(content().string(containsString("data-puede-calendario=\"true\"")));

        Materia materia = materiaDePrueba();
        mvc.perform(post("/administracion/plazos-alertas/tipos-termino").with(csrf())
                        .param("materia.id", materia.getId().toString())
                        .param("nombre", "Tipo creado por abogado")
                        .param("codigo", "TIPO_ABOGADO")
                        .param("descripcion", "Configuración colaborativa")
                        .param("modoPlazo", "CALCULADO")
                        .param("diasPredeterminados", "10")
                        .param("tipoDias", "HABILES")
                        .param("activo", "true")
                        .param("atencionDias", "3")
                        .param("criticoDias", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/administracion/plazos-alertas#terminos"));
        assertTrue(tiposTermino.findByNombreIgnoreCase("Tipo creado por abogado").isPresent());

        mvc.perform(post("/administracion/plazos-alertas/calendario").with(csrf())
                        .param("fecha", "2035-08-15")
                        .param("descripcion", "Prueba colaborativa de calendario"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/administracion/plazos-alertas#calendario"));
        assertTrue(diasInhabiles.existsByFechaAndActivoTrue(java.time.LocalDate.of(2035, 8, 15)));
    }

    private Materia materiaDePrueba() {
        return materias.findByNombreIgnoreCase("Materia de prueba UI")
                .orElseGet(() -> materias.save(Materia.builder()
                        .nombre("Materia de prueba UI")
                        .gerencia(gerencias.findAll().get(0))
                        .activo(true)
                        .build()));
    }
}
