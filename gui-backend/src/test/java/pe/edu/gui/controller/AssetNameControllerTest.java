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
import pe.edu.gui.config.SecurityConfig;
import pe.edu.gui.security.Access;
import pe.edu.gui.security.GuiPrincipal;
import pe.edu.gui.service.ActivoService;
import pe.edu.gui.service.SeguridadService;
import java.util.List;
import java.util.Map;

@WebMvcTest(ActivoController.class)
@Import({SecurityConfig.class, Access.class})
class AssetNameControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ActivoService activos;
    @MockBean SeguridadService seguridad;
    private UsernamePasswordAuthenticationToken actor() {
        return UsernamePasswordAuthenticationToken.authenticated(new GuiPrincipal(7,"prueba","hash"),null,List.of());
    }
    private void allow() { when(seguridad.tienePermiso(7,301)).thenReturn(true); when(seguridad.tienePermiso(7,302)).thenReturn(true); }
    @Test void cambiaSoloNombreConActorDeSesion() throws Exception {
        allow(); when(activos.cambiarNombre(7,2,"Nombre nuevo","Anterior")).thenReturn(Map.of("id",2,"name","Nombre nuevo"));
        mvc.perform(post("/api/assets/2/name").with(authentication(actor())).with(csrf()).contentType("application/json")
            .content("{\"name\":\"Nombre nuevo\",\"expectedName\":\"Anterior\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nombre nuevo"));
        verify(activos).cambiarNombre(7,2,"Nombre nuevo","Anterior");
    }
    @Test void rechazaCodigoPatrimonialYCamposExtra() throws Exception {
        allow();
        for(String field:List.of("code","token","familyId","responsibleId","locationId")) {
            mvc.perform(post("/api/assets/2/name").with(authentication(actor())).with(csrf()).contentType("application/json")
                .content("{\"name\":\"Nuevo\",\"expectedName\":\"Anterior\",\""+field+"\":\"alterado\"}"))
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(activos);
    }
    @Test void exigePermisoYCcsrf() throws Exception {
        mvc.perform(post("/api/assets/2/name").with(authentication(actor())).with(csrf()).contentType("application/json")
            .content("{\"name\":\"Nuevo\",\"expectedName\":\"Anterior\"}")) .andExpect(status().isForbidden());
        allow(); mvc.perform(post("/api/assets/2/name").with(authentication(actor())).contentType("application/json")
            .content("{\"name\":\"Nuevo\",\"expectedName\":\"Anterior\"}")) .andExpect(status().isForbidden());
        verifyNoInteractions(activos);
    }
    @Test void rechazaNombreVacioOLargo() throws Exception {
        allow(); for(String name:List.of(" ","a".repeat(201))) {
            mvc.perform(post("/api/assets/2/name").with(authentication(actor())).with(csrf()).contentType("application/json")
                .content("{\"name\":\""+name+"\",\"expectedName\":\"Anterior\"}")) .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(activos);
    }
}
