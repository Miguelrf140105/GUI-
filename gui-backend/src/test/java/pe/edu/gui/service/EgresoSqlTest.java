package pe.edu.gui.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.gui.config.SecurityConfig;
import pe.edu.gui.controller.EgresoController;
import pe.edu.gui.dao.*;
import pe.edu.gui.dto.*;
import pe.edu.gui.entity.EtapaMovimiento;
import pe.edu.gui.interfaces.*;
import pe.edu.gui.security.*;

/** Todas las altas son fixtures nuevos dentro de transacciones que se revierten. */
@EnabledIfSystemProperty(named = "gui.sql.integration", matches = "true")
@WebMvcTest(EgresoController.class)
@Import({
  SecurityConfig.class,
  Access.class,
  MovementBatchSqlTest.Config.class,
  UsuarioDAO.class,
  SeguridadService.class,
  EgresoSqlTest.Beans.class
})
class EgresoSqlTest {
  @org.springframework.boot.test.context.TestConfiguration
  static class Beans {
    @Bean
    IEgresoDAO egresoDAO(ISqlExecutor sql) {
      return new EgresoDAO(sql);
    }

    @Bean
    EgresoService egresoService(IEgresoDAO dao) {
      return new EgresoService(dao);
    }

    @Bean
    IInventarioDAO inventarioDAO(ISqlExecutor sql) {
      return new InventarioDAO(sql);
    }

    @Bean
    InventarioService inventarioService(IInventarioDAO dao) {
      return new InventarioService(dao);
    }
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired MovimientoService movements;
  @Autowired InventarioService inventory;
  @Autowired EgresoService service;
  @Autowired MockMvc mvc;
  @Autowired DataSourceTransactionManager manager;
  Map<String, String> baseline;

  Map<String, String> snapshot() {
    var result = new LinkedHashMap<String, String>();
    for (String table :
        List.of(
            "INV.EGRESO",
            "INV.CORRELATIVO_EGRESO",
            "DOC.EGRESO_DOCUMENTO",
            "INV.OPERACION",
            "INV.OPERACION_DETALLE",
            "INV.ACTIVO",
            "INV.ITEM",
            "INV.ACTIVO_ESPECIFICACION",
            "INV.REGISTRO_LOTE",
            "INV.CORRELATIVO_TIPO",
            "INV.CORRELATIVO_INGRESO",
            "INV.MOVIMIENTO",
            "INV.LOTE_MOVIMIENTO",
            "INV.KARDEX_MOVIMIENTO",
            "DOC.DOCUMENTO",
            "AUD.EVENTO",
            "SEC.USUARIO",
            "SEC.USUARIO_ROL",
            "SEC.USUARIO_ALCANCE",
            "SEC.ROL_PERMISO")) {
      var text = new StringBuilder();
      for (var row : jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1,2")) {
        for (var e : row.entrySet()) {
          String v =
              e.getValue() instanceof byte[] b
                  ? Base64.getEncoder().encodeToString(b)
                  : String.valueOf(e.getValue());
          text.append(e.getKey()).append(':').append(v.length()).append(':').append(v).append(';');
        }
        text.append('\n');
      }
      try {
        result.put(
            table,
            HexFormat.of()
                .formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                        .digest(
                            text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    return result;
  }

  @BeforeEach
  void before() {
    baseline = snapshot();
  }

  @AfterEach
  void after() {
    assertEquals(baseline, snapshot(), "Todos los fixtures e historia de prueba deben revertirse");
  }

  void tx(java.util.function.Consumer<org.springframework.transaction.TransactionStatus> test) {
    new TransactionTemplate(manager)
        .execute(
            s -> {
              test.accept(s);
              s.setRollbackOnly();
              return null;
            });
  }

  List<Integer> create() {
    int family =
        jdbc.queryForObject(
            "SELECT MIN(ID_FAMILIA) FROM INV.FAMILIA_PRODUCTO WHERE NOMBRE=N'Microscopio'",
            Integer.class);
    int model =
        jdbc.queryForObject(
            "SELECT MIN(ID_ITEM) FROM INV.ITEM WHERE ID_FAMILIA=? AND ESTADO_REGISTRO=1",
            Integer.class,
            family);
    int provider =
        jdbc.queryForObject(
            "SELECT MIN(ID_TERCERO) FROM INV.TERCERO WHERE ESTADO_REGISTRO=1", Integer.class);
    int location =
        jdbc.queryForObject(
            "SELECT MIN(ID_UBICACION) FROM INV.UBICACION WHERE ESTADO_REGISTRO=1", Integer.class);
    var dto =
        new RegistroInventario(
            UUID.randomUUID(),
            1,
            provider,
            null,
            java.time.LocalDate.now(),
            "QA egreso revertido",
            location,
            1,
            List.of(
                new RegistroInventario.Linea(
                    family,
                    model,
                    null,
                    null,
                    null,
                    2,
                    java.math.BigDecimal.ZERO,
                    List.of(),
                    List.of())),
            List.of(),
            List.of());
    return inventory.registrar(1, dto).stream()
        .map(r -> ((Number) r.get("id")).intValue())
        .toList();
  }

  int provider() {
    return jdbc.queryForObject(
        "SELECT MIN(ID_TERCERO) FROM INV.TERCERO WHERE ESTADO_REGISTRO=1", Integer.class);
  }

  List<MovimientoService.UnitMovement> request(int type, List<Integer> ids) {
    return movements.solicitarLote(
        1,
        new MovementBatch(
            ids,
            type,
            null,
            type == 4 ? provider() : null,
            "Salida QA revertida",
            UUID.randomUUID()));
  }

  void stage(long id, EtapaMovimiento stage) {
    movements.avanzar(1, stage, new Requests.Transition(id));
  }

  void finish(long id) {
    stage(id, EtapaMovimiento.AUTORIZAR);
    stage(id, EtapaMovimiento.DESPACHAR);
    stage(id, EtapaMovimiento.RECIBIR);
  }

  long count(String table) {
    return jdbc.queryForObject("SELECT COUNT_BIG(*) FROM " + table, Long.class);
  }

  long egress(long movement) {
    return jdbc.queryForObject(
        "SELECT ID_EGRESO FROM INV.MOVIMIENTO WHERE ID_MOVIMIENTO=?", Long.class, movement);
  }

  int actor(int role, boolean scope) {
    String tag = "egreso-qa-" + UUID.randomUUID();
    int actor =
        jdbc.queryForObject(
            "INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) OUTPUT INSERTED.ID_USUARIO"
                + " VALUES(?,?,?,'NO-LOGIN-QA')",
            Integer.class,
            tag,
            tag,
            tag + "@example.invalid");
    jdbc.update("INSERT SEC.USUARIO_ROL VALUES(?,?)", actor, role);
    if (scope)
      jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(?,NULL)", actor);
    for (int perm : new int[] {401, 402, 403, 404, 405, 1501, 1502})
      jdbc.update(
          "IF NOT EXISTS(SELECT 1 FROM SEC.ROL_PERMISO WHERE ID_ROL=? AND ID_PERMISO=?) INSERT"
              + " SEC.ROL_PERMISO VALUES(?,?)",
          role,
          perm,
          role,
          perm);
    return actor;
  }

  @ParameterizedTest
  @ValueSource(ints = {4, 7})
  void loteDefinitivoSoloAlConfirmarConHistorialPorUnidad(int type) {
    tx(
        s -> {
          var ids = create();
          var original =
              jdbc.queryForList(
                  "SELECT ID_ACTIVO,CODIGO_PATRIMONIAL,TOKEN_QR FROM INV.ACTIVO WHERE ID_ACTIVO"
                      + " IN(?,?) ORDER BY ID_ACTIVO",
                  ids.get(0),
                  ids.get(1));
          var units = request(type, ids);
          long before = count("INV.EGRESO");
          assertEquals(before, count("INV.EGRESO"));
          stage(units.get(0).movementId(), EtapaMovimiento.AUTORIZAR);
          stage(units.get(0).movementId(), EtapaMovimiento.DESPACHAR);
          assertEquals(before, count("INV.EGRESO"));
          stage(units.get(0).movementId(), EtapaMovimiento.RECIBIR);
          long id = egress(units.get(0).movementId());
          var partial =
              jdbc.queryForList("EXEC INV.sp_Egresos 1,1").stream()
                  .filter(r -> ((Number) r.get("id")).longValue() == id)
                  .findFirst()
                  .orElseThrow();
          assertEquals("Confirmación parcial", partial.get("status"));
          assertTrue(String.valueOf(partial.get("date")).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"));
          if(type == 7) assertEquals("Salida QA revertida", partial.get("institution"));

          assertEquals(1, ((Number) partial.get("quantity")).intValue());
          finish(units.get(1).movementId());
          assertEquals(id, egress(units.get(1).movementId()));
          assertEquals(before + 1, count("INV.EGRESO"));
          assertTrue(
              jdbc.queryForObject(
                      "SELECT CODIGO FROM INV.EGRESO WHERE ID_EGRESO=?", String.class, id)
                  .matches("EGR[0-9]{4,}"));
          assertEquals(2, service.detalle(1, id).get("units").size());
          for (var unit : units) {
            assertEquals(
                type == 4 ? 12 : 28,
                jdbc.queryForObject(
                    "SELECT ESTADO_REGISTRO FROM INV.ACTIVO WHERE ID_ACTIVO=?",
                    Integer.class,
                    unit.assetId()));
            assertEquals(
                4,
                jdbc.queryForObject(
                    "SELECT COUNT(*) FROM INV.KARDEX_MOVIMIENTO WHERE ID_MOVIMIENTO=?",
                    Integer.class,
                    unit.movementId()));
          }
          assertEquals(
              original,
              jdbc.queryForList(
                  "SELECT ID_ACTIVO,CODIGO_PATRIMONIAL,TOKEN_QR FROM INV.ACTIVO WHERE ID_ACTIVO"
                      + " IN(?,?) ORDER BY ID_ACTIVO",
                  ids.get(0),
                  ids.get(1)));
          jdbc.execute("EXEC INV.sp_EgresoRegistrar 1," + units.get(0).movementId());
          assertEquals(before + 1, count("INV.EGRESO"));
        });
  }

  @Test
  void mantenimientoNoGeneraEgreso() {
    tx(
        s -> {
          var ids = create();
          int dest =
              jdbc.queryForObject(
                  "SELECT MAX(ID_UBICACION) FROM INV.UBICACION WHERE ESTADO_REGISTRO=1",
                  Integer.class);
          var dto =
              new Requests.Movement(
                  ids.get(0), 5, dest, null, "Mantenimiento QA", UUID.randomUUID());
          long before = count("INV.EGRESO");
          long id = movements.solicitar(1, dto).id();
          finish(id);
          assertEquals(before, count("INV.EGRESO"));
          assertEquals(
              11,
              jdbc.queryForObject(
                  "SELECT ESTADO_REGISTRO FROM INV.ACTIVO WHERE ID_ACTIVO=?",
                  Integer.class,
                  ids.get(0)));
        });
  }

  @Test
  void movimientoIndividualGeneraSuPropioEgreso() {
    tx(
        s -> {
          var ids = create();
          long id =
              movements
                  .solicitar(
                      1,
                      new Requests.Movement(
                          ids.get(0), 7, null, null, "Baja QA", UUID.randomUUID()))
                  .id();
          finish(id);
          assertEquals(1, service.detalle(1, egress(id)).get("units").size());
        });
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3})
  void noAdministradorNoAutorizaAunqueLeAsignenPermiso(int role) {
    tx(
        s -> {
          var units = request(7, create());
          int actor = actor(role, true);
          assertThrows(
              org.springframework.dao.DataAccessException.class,
              () ->
                  movements.avanzar(
                      actor,
                      EtapaMovimiento.AUTORIZAR,
                      new Requests.Transition(units.get(0).movementId())));
        });
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3})
  void noAdministradorNoDespachaAunqueLeAsignenPermiso(int role) {
    tx(
        s -> {
          var unit = request(4, create()).get(0);
          stage(unit.movementId(), EtapaMovimiento.AUTORIZAR);
          int actor = actor(role, true);
          assertThrows(
              org.springframework.dao.DataAccessException.class,
              () ->
                  movements.avanzar(
                      actor,
                      EtapaMovimiento.DESPACHAR,
                      new Requests.Transition(unit.movementId())));
        });
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3})
  void noAdministradorNoConfirmaAunqueLeAsignenPermiso(int role) {
    tx(
        s -> {
          var unit = request(7, create()).get(0);
          stage(unit.movementId(), EtapaMovimiento.AUTORIZAR);
          stage(unit.movementId(), EtapaMovimiento.DESPACHAR);
          int actor = actor(role, true);
          assertThrows(
              org.springframework.dao.DataAccessException.class,
              () ->
                  movements.avanzar(
                      actor, EtapaMovimiento.RECIBIR, new Requests.Transition(unit.movementId())));
        });
  }

  @Test
  void soloAdministradorAsociaDocumentosYNoDuplica() {
    tx(
        s -> {
          var unit = request(4, create()).get(0);
          finish(unit.movementId());
          long id = egress(unit.movementId());
          int doc =
              jdbc.queryForObject(
                  "INSERT DOC.DOCUMENTO(ID_TIPO_DOCUMENTO,ID_TERCERO,NUMERO,FECHA,ID_USUARIO)"
                      + " OUTPUT INSERTED.ID_DOCUMENTO VALUES(301,?,?,'2026-10-27',1)",
                  Integer.class,
                  provider(),
                  "QA-" + UUID.randomUUID());
          service.asociarDocumento(1, id, doc);
          service.asociarDocumento(1, id, doc);
          assertEquals(1, service.detalle(1, id).get("documents").size());
          assertEquals(
              1,
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM AUD.EVENTO WHERE MODULO=150 AND ACCION=1502 AND"
                      + " ID_REGISTRO=?",
                  Integer.class,
                  id));
          int actor = actor(2, true);
          assertEquals(
              0, jdbc.queryForObject("SELECT SEC.fnTienePermiso(?,1502)", Integer.class, actor));
          var auth =
              new UsernamePasswordAuthenticationToken(
                  new GuiPrincipal(actor, "qa", "no-login"), null, List.of());
          try {
            mvc.perform(
                    post("/api/egresses/" + id + "/documents")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"documentId\":" + doc + "}"))
                .andExpect(status().isForbidden());
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }

  @Test
  void adjuntosAsociadosRespetanPermisoYAlcanceDelEgreso() {
    tx(
        s -> {
          var unit = request(4, create()).get(0);
          finish(unit.movementId());
          long id = egress(unit.movementId());
          int doc =
              jdbc.queryForObject(
                  "INSERT DOC.DOCUMENTO(ID_TIPO_DOCUMENTO,ID_TERCERO,NUMERO,FECHA,ID_USUARIO)"
                      + " OUTPUT INSERTED.ID_DOCUMENTO VALUES(301,?,?,'2026-10-27',1)",
                  Integer.class,
                  provider(),
                  "QA-" + UUID.randomUUID());
          service.asociarDocumento(1, id, doc);
          int actor = actor(2, false);
          assertEquals(
              0,
              jdbc.queryForObject("SELECT DOC.fnPuedeDocumento(?,?)", Integer.class, actor, doc));
          int origin =
              jdbc.queryForObject(
                  "SELECT ID_UBICACION_ORIGEN FROM INV.MOVIMIENTO WHERE ID_MOVIMIENTO=?",
                  Integer.class,
                  unit.movementId());
          jdbc.update(
              "INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(?,?)", actor, origin);
          assertEquals(
              1,
              jdbc.queryForObject("SELECT DOC.fnPuedeDocumento(?,?)", Integer.class, actor, doc));
        });
  }

  @Test
  void detalleHttpRespetaSesionPermisoYAlcance() {
    tx(
        s -> {
          var unit = request(7, create()).get(0);
          finish(unit.movementId());
          long id = egress(unit.movementId());
          int actor = actor(2, false);
          var limited =
              new UsernamePasswordAuthenticationToken(
                  new GuiPrincipal(actor, "qa", "no-login"), null, List.of());
          var admin =
              new UsernamePasswordAuthenticationToken(
                  new GuiPrincipal(1, "admin", "no-login"), null, List.of());
          try {
            mvc.perform(get("/api/egresses/" + id + "/details"))
                .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/egresses/" + id + "/details").with(authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units[0].code").exists());
            mvc.perform(get("/api/egresses/" + id + "/details").with(authentication(limited)))
                .andExpect(status().isForbidden());
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }
}
