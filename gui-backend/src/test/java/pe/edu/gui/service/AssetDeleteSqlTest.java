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
import pe.edu.gui.controller.ActivoController;
import pe.edu.gui.dao.*;
import pe.edu.gui.dto.*;
import pe.edu.gui.interfaces.*;
import pe.edu.gui.security.*;

@EnabledIfSystemProperty(named = "gui.sql.integration", matches = "true")
@WebMvcTest(ActivoController.class)
@Import({
  SecurityConfig.class,
  Access.class,
  MovementBatchSqlTest.Config.class,
  UsuarioDAO.class,
  SeguridadService.class,
  AssetDeleteSqlTest.Beans.class
})
class AssetDeleteSqlTest {
  @org.springframework.boot.test.context.TestConfiguration
  static class Beans {
    @Bean
    IActivoDAO activoDAO(ISqlExecutor sql) {
      return new ActivoDAO(sql);
    }

    @Bean
    ActivoService activoService(IActivoDAO dao) {
      return new ActivoService(dao);
    }

    @Bean
    IInventarioDAO inventoryDAO(ISqlExecutor sql) {
      return new InventarioDAO(sql);
    }

    @Bean
    InventarioService inventory(IInventarioDAO dao) {
      return new InventarioService(dao);
    }
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired ActivoService service;
  @Autowired InventarioService inventory;
  @Autowired MovimientoService movements;
  @Autowired MockMvc mvc;
  @Autowired DataSourceTransactionManager manager;
  Map<String, String> baseline;

  Map<String, String> snapshot() {
    var result = new LinkedHashMap<String, String>();
    for (String table :
        List.of(
            "INV.OPERACION",
            "INV.OPERACION_DETALLE",
            "INV.ACTIVO",
            "INV.ITEM",
            "INV.ACTIVO_ESPECIFICACION",
            "INV.REGISTRO_LOTE",
            "INV.CORRELATIVO_TIPO",
            "INV.CORRELATIVO_INGRESO",
            "INV.MOVIMIENTO",
            "INV.MANTENIMIENTO",
            "INV.KARDEX_MOVIMIENTO",
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
    assertEquals(
        baseline, snapshot(), "No se eliminó ningún activo real ni se conservaron fixtures");
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

  int create() {
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
            "Borrado QA revertido",
            location,
            1,
            List.of(
                new RegistroInventario.Linea(
                    family,
                    model,
                    null,
                    null,
                    null,
                    1,
                    java.math.BigDecimal.ZERO,
                    List.of(),
                    List.of())),
            List.of(),
            List.of());
    return ((Number) inventory.registrar(1, dto).get(0).get("id")).intValue();
  }

  @Test
  void administradorBorraLogicamenteSinCambiarCodigoNiQr() {
    tx(
        s -> {
          int id = create();
          var original =
              jdbc.queryForMap(
                  "SELECT CODIGO_PATRIMONIAL,TOKEN_QR,ID_ITEM,ID_DETALLE_INGRESO FROM INV.ACTIVO"
                      + " WHERE ID_ACTIVO=?",
                  id);
          service.eliminar(1, id);
          assertEquals(
              2,
              jdbc.queryForObject(
                  "SELECT ESTADO_REGISTRO FROM INV.ACTIVO WHERE ID_ACTIVO=?", Integer.class, id));
          assertEquals(
              original,
              jdbc.queryForMap(
                  "SELECT CODIGO_PATRIMONIAL,TOKEN_QR,ID_ITEM,ID_DETALLE_INGRESO FROM INV.ACTIVO"
                      + " WHERE ID_ACTIVO=?",
                  id));
          assertEquals(
              2,
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM INV.KARDEX_MOVIMIENTO WHERE ID_ACTIVO=?",
                  Integer.class,
                  id));
          assertTrue(
              jdbc.queryForList("EXEC INV.sp_Listar 1,30,0,100").stream()
                  .noneMatch(r -> Integer.valueOf(id).equals(r.get("id"))));
        });
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3})
  void otrosPerfilesNoPuedenBorrarAunqueSeConcedaElPermiso(int role) {
    tx(
        s -> {
          int id = create();
          String tag = "delete-qa-" + UUID.randomUUID();
          int actor =
              jdbc.queryForObject(
                  "INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) OUTPUT"
                      + " INSERTED.ID_USUARIO VALUES(?,?,?,'NO-LOGIN-QA')",
                  Integer.class,
                  tag,
                  tag,
                  tag + "@example.invalid");
          jdbc.update("INSERT SEC.USUARIO_ROL VALUES(?,?)", actor, role);
          jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(?,NULL)", actor);
          jdbc.update(
              "IF NOT EXISTS(SELECT 1 FROM SEC.ROL_PERMISO WHERE ID_ROL=? AND ID_PERMISO=304)"
                  + " INSERT SEC.ROL_PERMISO VALUES(?,304)",
              role,
              role);
          assertEquals(
              0, jdbc.queryForObject("SELECT SEC.fnTienePermiso(?,304)", Integer.class, actor));
          var auth =
              new UsernamePasswordAuthenticationToken(
                  new GuiPrincipal(actor, tag, "unused"), null, List.of());
          try {
            mvc.perform(
                    post("/api/assets/" + id + "/delete").with(authentication(auth)).with(csrf()))
                .andExpect(status().isForbidden());
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
          assertEquals(
              10,
              jdbc.queryForObject(
                  "SELECT ESTADO_REGISTRO FROM INV.ACTIVO WHERE ID_ACTIVO=?", Integer.class, id));
        });
  }

  @Test
  void movimientoPendienteBloqueaElBorrado() {
    tx(
        s -> {
          int id = create(),
              dest =
                  jdbc.queryForObject(
                      "SELECT MAX(ID_UBICACION) FROM INV.UBICACION WHERE ESTADO_REGISTRO=1",
                      Integer.class);
          movements.solicitar(
              1, new Requests.Movement(id, 2, dest, null, "Movimiento QA", UUID.randomUUID()));
          var e =
              assertThrows(
                  org.springframework.dao.DataAccessException.class, () -> service.eliminar(1, id));
          assertTrue(e.getMostSpecificCause().getMessage().contains("no se puede eliminar"));
        });
  }

  @Test
  void mantenimientoBloqueaElBorrado() {
    tx(
        s -> {
          int id = create();
          jdbc.update(
              "INSERT INV.MANTENIMIENTO(ID_ACTIVO,FECHA_PROGRAMADA,DESCRIPCION,ID_TECNICO)"
                  + " VALUES(?,GETDATE(),N'QA',1)",
              id);
          assertThrows(
              org.springframework.dao.DataAccessException.class, () -> service.eliminar(1, id));
        });
  }

  @Test
  void tipoInternoNoSePuedeSolicitar() {
    tx(
        s -> {
          int id = create();
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  movements.solicitar(
                      1, new Requests.Movement(id, 9, null, null, "QA", UUID.randomUUID())));
        });
  }
}
