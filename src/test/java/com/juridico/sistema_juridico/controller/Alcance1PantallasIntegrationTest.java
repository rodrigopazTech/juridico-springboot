package com.juridico.sistema_juridico.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.juridico.sistema_juridico.Entity.usuario.Usuario;
import com.juridico.sistema_juridico.repository.Catalogo.GerenciaRepository;
import com.juridico.sistema_juridico.repository.Usuarios.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "ui.admin@siaj.test", authorities = "ADMIN")
class Alcance1PantallasIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private GerenciaRepository gerencias;

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
    void renderizaAdministracionDePlazosYPerfiles() throws Exception {
        mvc.perform(get("/administracion/plazos-alertas").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"modo-perfil-termino\"")))
                .andExpect(content().string(containsString("id=\"regla-proporcional\"")));
    }

    @Test
    void renderizaCatalogosSinCatalogoDeSemaforos() throws Exception {
        mvc.perform(get("/catalogos").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"form-tipo-termino\"")))
                .andExpect(content().string(containsString("id=\"form-tipo-audiencia-completo\"")))
                .andExpect(content().string(not(containsString("id=\"panel-semaforos\""))));
    }

    @Test
    void renderizaTerminosConReglaExplicita() throws Exception {
        mvc.perform(get("/terminos").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"select-tipo-termino\"")))
                .andExpect(content().string(containsString("id=\"input-fecha-origen\"")))
                .andExpect(content().string(containsString("id=\"resumen-regla-tipo\"")));
    }
}
