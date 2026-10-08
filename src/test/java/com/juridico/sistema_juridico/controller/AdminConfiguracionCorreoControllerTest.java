package com.juridico.sistema_juridico.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class AdminConfiguracionCorreoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanOpenMailConfigurationPage() throws Exception {
        mockMvc.perform(get("/admin/configuracion-correo").header("X-API-KEY", "test-migration-key"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/admin/configuracion-correo"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Configuración de correo SMTP")));
    }

    @Test
    void unauthenticatedUserCannotOpenMailConfigurationPage() throws Exception {
        mockMvc.perform(get("/admin/configuracion-correo"))
                .andExpect(status().isUnauthorized());
    }
}
