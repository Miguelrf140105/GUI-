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
import pe.edu.gui.dto.Pagina;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.security.Access;
import pe.edu.gui.security.GuiPrincipal;
import pe.edu.gui.service.ActivoService;
import pe.edu.gui.service.AdquisicionService;
import pe.edu.gui.service.ConsultaService;
import pe.edu.gui.service.MovimientoService;
import pe.edu.gui.service.SeguridadService;

import java.util.List;
import java.util.Map;

@WebMvcTest({
    ActivoController.class,
    InventarioController.class,
    MovimientoController.class,
    ConsultaController.class,
    UbicacionController.class,
    AdquisicionController.class
})
@Import({SecurityConfig.class, Access.class})
class ApiContractTest {

    @Autowired MockMvc mvc;

    @MockBean ActivoService activos;
    @MockBean pe.edu.gui.service.InventarioService inventario;

    @MockBean AdquisicionService adquisiciones;
    @MockBean pe.edu.gui.service.UbicacionService ubicaciones;

    @MockBean MovimientoService movimientos;

    @MockBean ConsultaService consultas;

    @MockBean SeguridadService seguridad;

    private UsernamePasswordAuthenticationToken actor() {
        return UsernamePasswordAuthenticationToken.authenticated(
                new GuiPrincipal(7, "prueba", "hash"), null, List.of());
    }

    @Test
    void paginatedJsonContractMatchesFrontend() throws Exception {
        when(consultas.pagina(7, 30, 0, 50))
                .thenReturn(
                        new Pagina(List.of(Map.of("id", 9, "code", "GUI-00009")), 0, 50, false));
        mvc.perform(get("/api/modules/30").with(authentication(actor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].code").value("GUI-00009"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void completeRegistrationUsesSessionActorAndRequiresBothPermissions() throws Exception {
        when(seguridad.tienePermiso(7, 302)).thenReturn(true);
        when(seguridad.tienePermiso(7, 1402)).thenReturn(true);
        when(inventario.registrar(eq(7), any()))
                .thenReturn(List.of(Map.of("id", 18, "code", "LAP-000001")));
        mvc.perform(
                        post("/api/inventory/registrations")
                                .with(authentication(actor()))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        """
{"idempotencyKey":"d54234fc-b1d4-4c43-91a7-320192317672","typeId":1,"institutionId":4,
"date":"2026-10-22","locationId":2,"lines":[{"familyId":1,"name":"Laptop Lenovo",
"quantity":24,"serials":[],"specifications":[]}],"documentIds":[],"documents":[]}
"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LAP-000001"));
        verify(inventario).registrar(eq(7), any());
    }

    @Test
    void assetPermissionAloneCannotCreateAReception() throws Exception {
        when(seguridad.tienePermiso(7, 302)).thenReturn(true);
        mvc.perform(
                        post("/api/inventory/registrations")
                                .with(authentication(actor()))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        """
{"idempotencyKey":"d54234fc-b1d4-4c43-91a7-320192317672","typeId":1,"institutionId":4,"date":"2026-10-22","locationId":2,"lines":[{"familyId":1,"name":"Laptop Lenovo","quantity":24,"serials":[],"specifications":[]}],"documentIds":[],"documents":[]}
"""))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inventario);
    }

    @Test
    void deniedApprovalNeverReachesTheService() throws Exception {
        when(seguridad.tienePermiso(7, 403)).thenReturn(false);
        mvc.perform(
                        post("/api/movements/authorize")
                                .with(authentication(actor()))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"movementId\":12}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(movimientos);
    }

    @Test
    void providerUsesItsOwnPermissionAndValidatedContactDto() throws Exception {
        when(seguridad.tienePermiso(7, 1302)).thenReturn(true);
        when(adquisiciones.guardarInstitucion(eq(7), any())).thenReturn(new ResultadoRegistro(23));
        mvc.perform(
                        post("/api/institutions")
                                .with(authentication(actor()))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"Proveedor de"
                                            + " prueba\",\"document\":\"PRUEBA-23\",\"email\":\"contacto@example.invalid\",\"stateId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(23));
    }

    @Test
    void documentWriteDoesNotGrantProviderWrite() throws Exception {
        when(seguridad.tienePermiso(7, 702)).thenReturn(true);
        mvc.perform(
                        post("/api/institutions")
                                .with(authentication(actor()))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"name\":\"Proveedor\",\"document\":\"PRUEBA-24\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adquisiciones);
    }

    @Test
    void intakeDetailUsesActorFromSession() throws Exception {
        when(seguridad.tienePermiso(7, 1401)).thenReturn(true);
        when(adquisiciones.detalleIngreso(7, 42))
                .thenReturn(Map.of("lines", List.of(), "documents", List.of()));
        mvc.perform(get("/api/intakes/42/details").with(authentication(actor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.documents").isArray());
    }

    @Test
    void providerRejectsInvalidEmailBeforePersistence() throws Exception {
        when(seguridad.tienePermiso(7, 1302)).thenReturn(true);
        mvc.perform(
                        post("/api/institutions")
                                .with(authentication(actor()))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"Proveedor\",\"document\":\"PRUEBA-25\",\"email\":\"correo-invalido\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adquisiciones);
    }

    @Test
    void supplierSearchUsesSessionActorAndIntakePermission() throws Exception {
        when(seguridad.tienePermiso(7, 1402)).thenReturn(true);
        when(adquisiciones.buscarInstitucion(7, "20612118591"))
                .thenReturn(
                        Map.of(
                                "found",
                                true,
                                "institution",
                                Map.of("id", 12, "name", "Proveedor registrado")));
        mvc.perform(
                        get("/api/institutions/search")
                                .param("document", "20612118591")
                                .with(authentication(actor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institution.id").value(12));
        verify(adquisiciones).buscarInstitucion(7, "20612118591");
    }

    @Test
    void supplierSearchRejectsMissingPermissions() throws Exception {
        mvc.perform(
                        get("/api/institutions/search")
                                .param("document", "20612118591")
                                .with(authentication(actor())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adquisiciones);
    }

    @Test
    void rootLocationAcceptsNullParentAndResponsible() throws Exception {
        when(seguridad.tienePermiso(7, 502)).thenReturn(true);
        when(ubicaciones.guardar(eq(7), any())).thenReturn(new ResultadoRegistro(1L));
        mvc.perform(
                        post("/api/locations")
                                .with(authentication(actor()))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"id\":1,\"name\":\"UNID\",\"code\":\"SC\",\"typeId\":201,\"parentId\":null,\"responsibleId\":null}"))
                .andExpect(status().isOk());
        verify(ubicaciones)
                .guardar(
                        eq(7),
                        argThat(dto -> dto.parentId() == null && dto.responsibleId() == null));
    }

    @Test
    void batchMovementRequiresPermissionAndCsrf() throws Exception {
        String body="{\"assetIds\":[1,2],\"typeId\":2,\"locationId\":9,\"reason\":\"Traslado\",\"idempotencyKey\":\"f1970bf1-a21a-4b1f-a1b1-410d7391c3a4\"}";
        mvc.perform(post("/api/movements/batch").with(authentication(actor())).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        when(seguridad.tienePermiso(7,402)).thenReturn(true);
        mvc.perform(post("/api/movements/batch").with(authentication(actor())).contentType("application/json").content(body)).andExpect(status().isForbidden());
        verifyNoInteractions(movimientos);
    }
    @Test
    void batchMovementUsesSessionActorAndOneResultPerUnit() throws Exception {
        when(seguridad.tienePermiso(7,402)).thenReturn(true);
        when(movimientos.solicitarLote(eq(7),any())).thenReturn(List.of(new MovimientoService.UnitMovement(1,91),new MovimientoService.UnitMovement(2,92)));
        mvc.perform(post("/api/movements/batch").with(authentication(actor())).with(csrf()).contentType("application/json")
          .content("{\"assetIds\":[1,2],\"typeId\":2,\"locationId\":9,\"reason\":\"Traslado\",\"idempotencyKey\":\"f1970bf1-a21a-4b1f-a1b1-410d7391c3a4\"}"))
          .andExpect(status().isOk()).andExpect(jsonPath("$[0].assetId").value(1)).andExpect(jsonPath("$[1].movementId").value(92));
    }
    @Test
    void batchMovementRejectsEmptySelection() throws Exception {
        when(seguridad.tienePermiso(7,402)).thenReturn(true);
        mvc.perform(post("/api/movements/batch").with(authentication(actor())).with(csrf()).contentType("application/json")
          .content("{\"assetIds\":[],\"typeId\":2,\"reason\":\"Traslado\",\"idempotencyKey\":\"f1970bf1-a21a-4b1f-a1b1-410d7391c3a4\"}"))
          .andExpect(status().isBadRequest());
        verifyNoInteractions(movimientos);
    }
}

