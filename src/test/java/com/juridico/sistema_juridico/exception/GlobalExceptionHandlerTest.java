package com.juridico.sistema_juridico.exception;

import com.juridico.sistema_juridico.service.AuditoriaTecnicaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class GlobalExceptionHandlerTest {

    private AuditoriaTecnicaService auditoriaTecnicaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        auditoriaTecnicaService = mock(AuditoriaTecnicaService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(auditoriaTecnicaService))
                .build();
    }

    @Test
    void unexpectedWebErrorRenders500WithoutRedirect() throws Exception {
        mockMvc.perform(get("/test/unexpected").accept(MediaType.TEXT_HTML))
                .andExpect(status().isInternalServerError())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(view().name("error/500"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("path", "/test/unexpected"));
    }

    @Test
    void unexpectedApiErrorReturnsJson500WithoutRedirect() throws Exception {
        mockMvc.perform(get("/api/test/unexpected").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error", containsString("error inesperado")));
    }

    @Test
    void auditFailureDoesNotHideTheOriginal500Page() throws Exception {
        doThrow(new IllegalStateException("auditoria no disponible"))
                .when(auditoriaTecnicaService).registrarError(any(), any());

        mockMvc.perform(get("/test/unexpected").accept(MediaType.TEXT_HTML))
                .andExpect(status().isInternalServerError())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(view().name("error/500"));
    }

    @Controller
    private static class ThrowingController {

        @GetMapping({"/test/unexpected", "/api/test/unexpected"})
        String fail() {
            throw new IllegalStateException("fallo de prueba");
        }
    }
}
