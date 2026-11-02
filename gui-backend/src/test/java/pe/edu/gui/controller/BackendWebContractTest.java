package pe.edu.gui.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.gui.config.SecurityConfig;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.SeguridadService;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, Access.class})
class BackendWebContractTest {
    @Autowired MockMvc mvc;
    @MockBean SeguridadService seguridad;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/inventario", "/index.html", "/main.js"})
    void rutasDelFrontendNoSeSirvenDesdeElBackend(String path) throws Exception {
        mvc.perform(get(path).with(user("prueba"))).andExpect(status().isNotFound());
    }

    @Test
    void entregaCsrfParaElFrontendSeparado() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    @Test
    void datosSiguenExigiendoSesion() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginSigueExigiendoCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").param("username", "prueba"))
                .andExpect(status().isForbidden());
    }
}
