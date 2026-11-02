package pe.edu.gui.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.gui.config.SecurityConfig;
import pe.edu.gui.controller.PasswordResetController;
import pe.edu.gui.dao.*;
import pe.edu.gui.interfaces.*;
import pe.edu.gui.security.*;

/** Solo correos example.invalid y contraseñas de cuentas nuevas dentro de rollback. */
@EnabledIfSystemProperty(named = "gui.sql.integration", matches = "true")
@WebMvcTest(PasswordResetController.class)
@Import({
  SecurityConfig.class,
  Access.class,
  MovementBatchSqlTest.Config.class,
  UsuarioDAO.class,
  SeguridadService.class,
  PasswordRecoverySqlTest.Beans.class
})
class PasswordRecoverySqlTest {
  static class Capture {
    final List<PasswordResetService.CodeMail> events = new ArrayList<>();

    @EventListener
    public void code(PasswordResetService.CodeMail e) {
      events.add(e);
    }
  }

  @org.springframework.boot.test.context.TestConfiguration
  static class Beans {
    @Bean
    IPasswordResetDAO resetDAO(ISqlExecutor sql) {
      return new PasswordResetDAO(sql);
    }

    @Bean
    Capture capture() {
      return new Capture();
    }

    @Bean
    PasswordResetMailer mailer() {
      return new PasswordResetMailer() {
        public boolean available() {
          return true;
        }

        public void send(String to, String code) {
          throw new AssertionError("No enviar correos reales en pruebas");
        }
      };
    }

    @Bean
    PasswordResetService resetService(
        IPasswordResetDAO dao,
        PasswordResetMailer mailer,
        PasswordEncoder encoder,
        ApplicationEventPublisher events) {
      return new PasswordResetService(
          dao, mailer, encoder, events, "test-key-only-012345678901234567890123456789");
    }
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordResetService service;
  @Autowired Capture capture;
  @Autowired MockMvc mvc;
  @Autowired DataSourceTransactionManager manager;
  @Autowired PasswordEncoder encoder;
  Map<String, String> before;

  Map<String, String> snapshot() {
    var result = new LinkedHashMap<String, String>();
    for (String table :
        List.of(
            "SEC.USUARIO",
            "SEC.USUARIO_ROL",
            "SEC.USUARIO_ALCANCE",
            "SEC.RECUPERACION_CLAVE",
            "AUD.EVENTO")) {
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
    before = snapshot();
    capture.events.clear();
  }

  @AfterEach
  void unchanged() {
    assertEquals(
        before, snapshot(), "No se cambió ninguna contraseña real ni quedaron códigos de prueba");
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

  record Fixture(int id, String username, String email) {}

  Fixture create() {
    String tag = "recover-qa-" + UUID.randomUUID(), email = tag + "@example.invalid";
    int id =
        jdbc.queryForObject(
            "INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) OUTPUT INSERTED.ID_USUARIO"
                + " VALUES(?,?,?,?)",
            Integer.class,
            tag,
            tag,
            email,
            encoder.encode("Original-Password-QA-123"));
    jdbc.update("INSERT SEC.USUARIO_ROL VALUES(?,3)", id);
    return new Fixture(id, tag, email);
  }

  String hash(int id) {
    return jdbc.queryForObject(
        "SELECT PASSWORD_HASH FROM SEC.USUARIO WHERE ID_USUARIO=?", String.class, id);
  }

  void remote(org.springframework.mock.web.MockHttpServletRequest r) {
    r.setRemoteAddr("qa-" + UUID.randomUUID());
  }

  @Test
  void codigoConfirmadoCambiaHashUnaVezYRevocaSesiones() {
    tx(
        s -> {
          var user = create();
          UUID id = service.request(user.email);
          String code = capture.events.get(0).code();
          assertTrue(code.matches("[0-9]{8}"));
          byte[] stored =
              jdbc.queryForObject(
                  "SELECT HASH_CODIGO FROM SEC.RECUPERACION_CLAVE WHERE ID_SOLICITUD=?",
                  (rs, n) -> rs.getBytes(1),
                  id.toString());
          assertEquals(32, stored.length);
          assertNotEquals(code, new String(stored, java.nio.charset.StandardCharsets.UTF_8));
          service.complete(id, code, "New-Password-QA-123456");
          assertTrue(encoder.matches("New-Password-QA-123456", hash(user.id)));
          assertFalse(encoder.matches("Original-Password-QA-123", hash(user.id)));
          assertEquals(
              1,
              jdbc.queryForObject(
                  "SELECT VERSION_CLAVE FROM SEC.USUARIO WHERE ID_USUARIO=?",
                  Integer.class,
                  user.id));
          assertThrows(
              IllegalArgumentException.class,
              () -> service.complete(id, code, "Other-Password-QA-123"));
          var old =
              new UsernamePasswordAuthenticationToken(
                  new GuiPrincipal(user.id, user.username, "unused", 0), null, List.of());
          var fresh =
              new UsernamePasswordAuthenticationToken(
                  new GuiPrincipal(user.id, user.username, "unused", 1), null, List.of());
          try {
            mvc.perform(get("/api/auth/recovery/status").with(authentication(old)))
                .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/auth/recovery/status").with(authentication(fresh)))
                .andExpect(status().isOk());
            mvc.perform(
                    post("/api/auth/login")
                        .with(csrf())
                        .with(
                            r -> {
                              remote(r);
                              return r;
                            })
                        .param("username", user.username)
                        .param("password", "New-Password-QA-123456"))
                .andExpect(status().isNoContent());
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }

  @Test
  void correoDuplicadoInclusoInactivoNoPuedeAsignarseAOtraCuenta() {
    tx(
        s -> {
          var user = create();
          jdbc.update("UPDATE SEC.USUARIO SET ESTADO_REGISTRO=0 WHERE ID_USUARIO=?", user.id);
          assertThrows(
              org.springframework.dao.DataIntegrityViolationException.class,
              () ->
                  jdbc.update(
                      "INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) VALUES(?,?,?,?)",
                      "duplicate-" + UUID.randomUUID(),
                      "QA",
                      user.email.toUpperCase(Locale.ROOT),
                      "unused"));
          service.request(user.email);
          assertTrue(capture.events.isEmpty());
        });
  }

  @Test
  void escrituraDirectaNoPuedeEvitarUnicidadConEspacios() {
    tx(
        s -> {
          var user = create();
          assertThrows(
              org.springframework.dao.DataIntegrityViolationException.class,
              () ->
                  jdbc.update(
                      "INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) VALUES(?,?,?,?)",
                      "spaces-" + UUID.randomUUID(),
                      "QA",
                      " " + user.email,
                      "unused"));
        });
  }

  @Test
  void guardadoAdministrativoNormalizaCorreoYSigueSiendoRecuperable() {
    tx(
        s -> {
          String tag = "mail-qa-" + UUID.randomUUID(), email = tag + "@example.invalid";
          jdbc.queryForList(
              "EXEC SEC.sp_UsuarioGuardar"
                  + " @actor=1,@username=?,@nombre=N'QA',@correo=?,@hash='unused',@roles='<request><item>3</item></request>',@alcances='<request/>',@global=1",
              tag,
              "  " + email.toUpperCase(Locale.ROOT) + "  ");
          assertEquals(
              email,
              jdbc.queryForObject(
                  "SELECT CORREO FROM SEC.USUARIO WHERE USERNAME=?", String.class, tag));
          service.request("  " + email.toUpperCase(Locale.ROOT) + "  ");
          assertEquals(1, capture.events.size());
          assertEquals(email, capture.events.get(0).recipient());
        });
  }

  @Test
  void administradorRecibeErrorClaroSiCorreoPerteneceAOtraCuenta() {
    tx(
        s -> {
          var user = create();
          var error =
              assertThrows(
                  org.springframework.dao.DataAccessException.class,
                  () ->
                      jdbc.queryForList(
                          "EXEC SEC.sp_UsuarioGuardar"
                              + " @actor=1,@username=?,@nombre=N'QA',@correo=?,@hash='unused',@roles='<request><item>3</item></request>',@alcances='<request/>',@global=1",
                          "duplicate-" + UUID.randomUUID(),
                          " " + user.email.toUpperCase(Locale.ROOT) + " "));
          assertTrue(
              error
                  .getMostSpecificCause()
                  .getMessage()
                  .contains("Este correo ya pertenece a otra cuenta"));
        });
  }

  @Test
  void cincoFallosBloqueanInclusoElCodigoCorrecto() {
    tx(
        s -> {
          var user = create();
          var id = service.request(user.email);
          String code = capture.events.get(0).code(),
              wrong = code.equals("00000000") ? "11111111" : "00000000";
          String original = hash(user.id);
          for (int i = 0; i < 5; i++)
            assertThrows(
                IllegalArgumentException.class,
                () -> service.complete(id, wrong, "New-Password-QA-123456"));
          assertEquals(
              5,
              jdbc.queryForObject(
                  "SELECT INTENTOS FROM SEC.RECUPERACION_CLAVE WHERE ID_SOLICITUD=?",
                  Integer.class,
                  id.toString()));
          assertThrows(
              IllegalArgumentException.class,
              () -> service.complete(id, code, "New-Password-QA-123456"));
          assertEquals(original, hash(user.id));
        });
  }

  @Test
  void caducidadYReenvioInvalidanElCodigoAnterior() {
    tx(
        s -> {
          var user = create();
          var old = service.request(user.email);
          String oldCode = capture.events.get(0).code();
          jdbc.update(
              "UPDATE SEC.RECUPERACION_CLAVE SET"
                  + " EXPIRA=DATEADD(MINUTE,-1,SYSUTCDATETIME()),FECHA=DATEADD(SECOND,-61,SYSUTCDATETIME())"
                  + " WHERE ID_SOLICITUD=?",
              old.toString());
          assertThrows(
              IllegalArgumentException.class,
              () -> service.complete(old, oldCode, "New-Password-QA-123456"));
          var next = service.request(user.email);
          assertNotEquals(old, next);
          assertEquals(2, capture.events.size());
          assertThrows(
              IllegalArgumentException.class,
              () -> service.complete(old, oldCode, "New-Password-QA-123456"));
          service.complete(next, capture.events.get(1).code(), "New-Password-QA-123456");
        });
  }

  @Test
  void cooldownYLimitePorCuentaNoRevelanLaExistencia() {
    tx(
        s -> {
          var user = create();
          var id = service.request(user.email);
          int sent = capture.events.size();
          var ignored = service.request(user.email);
          assertNotEquals(id, ignored);
          assertEquals(sent, capture.events.size());
          service.request("missing-" + UUID.randomUUID() + "@example.invalid");
          assertEquals(sent, capture.events.size());
          for (int i = 0; i < 4; i++) {
            jdbc.update(
                "UPDATE SEC.RECUPERACION_CLAVE SET FECHA=DATEADD(SECOND,-61,SYSUTCDATETIME()) WHERE"
                    + " ID_USUARIO=?",
                user.id);
            service.request(user.email);
          }
          assertEquals(5, capture.events.size());
          jdbc.update(
              "UPDATE SEC.RECUPERACION_CLAVE SET FECHA=DATEADD(SECOND,-61,SYSUTCDATETIME()) WHERE"
                  + " ID_USUARIO=?",
              user.id);
          service.request(user.email);
          assertEquals(5, capture.events.size());
        });
  }

  @Test
  void contraseñaDebilNoConsumeCodigo() {
    tx(
        s -> {
          var user = create();
          var id = service.request(user.email);
          String code = capture.events.get(0).code();
          assertThrows(IllegalArgumentException.class, () -> service.complete(id, code, "short"));
          assertEquals(
              0,
              jdbc.queryForObject(
                  "SELECT INTENTOS FROM SEC.RECUPERACION_CLAVE WHERE ID_SOLICITUD=?",
                  Integer.class,
                  id.toString()));
        });
  }

  @Test
  void httpAnonimoConCsrfYContratoUniforme() {
    tx(
        s -> {
          var user = create();
          try {
            mvc.perform(
                    post("/api/auth/recovery/request")
                        .contentType("application/json")
                        .content("{\"email\":\"" + user.email + "\"}"))
                .andExpect(status().isForbidden());
            for (String email :
                List.of(user.email, "missing-" + UUID.randomUUID() + "@example.invalid")) {
              mvc.perform(
                      post("/api/auth/recovery/request")
                          .with(csrf())
                          .with(
                              r -> {
                                remote(r);
                                return r;
                              })
                          .contentType("application/json")
                          .content("{\"email\":\"" + email + "\"}"))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.requestId").exists())
                  .andExpect(
                      jsonPath("$.message")
                          .value(
                              "Si el correo está registrado, recibirás un código de"
                                  + " recuperación."));
            }
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }
}
