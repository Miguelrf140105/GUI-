package pe.edu.gui.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import pe.edu.gui.config.SecurityConfig;
import pe.edu.gui.security.Access;
import pe.edu.gui.security.GuiPrincipal;
import pe.edu.gui.service.DocumentoService;
import pe.edu.gui.service.SeguridadService;
import java.util.List;

@WebMvcTest(DocumentoController.class)
@Import({SecurityConfig.class, Access.class})
class DocumentoControllerTest {
    @Autowired MockMvc mvc;
    @MockBean DocumentoService documentos;
    @MockBean SeguridadService seguridad;
    private UsernamePasswordAuthenticationToken actor() {
        return UsernamePasswordAuthenticationToken.authenticated(
                new GuiPrincipal(7, "prueba", "hash"), null, List.of());
    }
    @Test
    void rechazaJsonEnLugarDeMultipartSinError500() throws Exception {
        when(seguridad.tienePermiso(7, 702)).thenReturn(true);
        mvc.perform(post("/api/attachments").with(authentication(actor())).with(csrf())
                .contentType("application/json").content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(documentos);
    }
    @Test
    void rechazaMultipartSinArchivo() throws Exception {
        when(seguridad.tienePermiso(7, 702)).thenReturn(true);
        mvc.perform(multipart("/api/attachments").param("documentId", "1")
                .with(authentication(actor())).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentos);
    }
    @Test
    void exigePermisoParaSubirArchivo() throws Exception {
        mvc.perform(multipart("/api/attachments")
                .file(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}))
                .param("documentId", "1").with(authentication(actor())).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(documentos);
    }
}
