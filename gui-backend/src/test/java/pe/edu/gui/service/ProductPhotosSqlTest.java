package pe.edu.gui.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.gui.config.SecurityConfig;
import pe.edu.gui.controller.ProductoFotoController;
import pe.edu.gui.dao.*;
import pe.edu.gui.dto.*;
import pe.edu.gui.interfaces.*;
import pe.edu.gui.security.*;
import pe.edu.gui.utils.ProductImageCodec;

/** Productos, unidades, cuentas y fotos nuevos; todas las escrituras se revierten. */
@EnabledIfSystemProperty(named = "gui.sql.integration", matches = "true")
@WebMvcTest(ProductoFotoController.class)
@Import({
  SecurityConfig.class,
  Access.class,
  MovementBatchSqlTest.Config.class,
  UsuarioDAO.class,
  SeguridadService.class,
  ProductImageCodec.class,
  ProductPhotosSqlTest.Beans.class
})
class ProductPhotosSqlTest {
  @org.springframework.boot.test.context.TestConfiguration
  static class Beans {
    @Bean
    IProductoFotoDAO fotoDAO(ISqlExecutor sql) {
      return new ProductoFotoDAO(sql);
    }

    @Bean
    ProductoFotoService fotos(IProductoFotoDAO dao, ProductImageCodec codec) {
      return new ProductoFotoService(dao, codec);
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
  @Autowired ProductoFotoService service;
  @Autowired InventarioService inventory;
  @Autowired MockMvc mvc;
  @Autowired DataSourceTransactionManager manager;
  Map<String, String> baseline;

  Map<String, String> snapshot() {
    var result = new LinkedHashMap<String, String>();
    for (String table :
        List.of(
            "DOC.FOTO_PRODUCTO",
            "GUI_BF.dbo.FOTO_PRODUCTO_BINARIO",
            "INV.OPERACION",
            "INV.OPERACION_DETALLE",
            "INV.ITEM",
            "INV.ACTIVO",
            "INV.ACTIVO_ESPECIFICACION",
            "INV.REGISTRO_LOTE",
            "INV.CORRELATIVO_TIPO",
            "INV.CORRELATIVO_INGRESO",
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
    assertEquals(baseline, snapshot(), "No se conservaron fotos, cuentas o cambios de prueba");
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
            "Fotos QA revertidas",
            location,
            1,
            List.of(
                new RegistroInventario.Linea(
                    family,
                    null,
                    "Foto QA " + UUID.randomUUID(),
                    "QA",
                    "test",
                    2,
                    java.math.BigDecimal.ZERO,
                    List.of(),
                    List.of())),
            List.of(),
            List.of());
    var rows = inventory.registrar(1, dto);
    return jdbc.queryForObject(
        "SELECT ID_ITEM FROM INV.ACTIVO WHERE ID_ACTIVO=?", Integer.class, rows.get(0).get("id"));
  }

  byte[] image(int color) {
    try {
      var image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
      var g = image.createGraphics();
      g.setColor(new java.awt.Color(color));
      g.fillRect(0, 0, 80, 60);
      g.dispose();
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(image, "png", bytes);
      return bytes.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  int actor(int role, boolean global) {
    String tag = "foto-qa-" + UUID.randomUUID();
    int actor =
        jdbc.queryForObject(
            "INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) OUTPUT INSERTED.ID_USUARIO"
                + " VALUES(?,?,?,'NO-LOGIN-QA')",
            Integer.class,
            tag,
            tag,
            tag + "@example.invalid");
    jdbc.update("INSERT SEC.USUARIO_ROL VALUES(?,?)", actor, role);
    if (global)
      jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(?,NULL)", actor);
    for (int perm : new int[] {301, 303})
      jdbc.update(
          "IF NOT EXISTS(SELECT 1 FROM SEC.ROL_PERMISO WHERE ID_ROL=? AND ID_PERMISO=?) INSERT"
              + " SEC.ROL_PERMISO VALUES(?,?)",
          role,
          perm,
          role,
          perm);
    return actor;
  }

  UsernamePasswordAuthenticationToken auth(int actor) {
    return new UsernamePasswordAuthenticationToken(
        new GuiPrincipal(actor, "qa", "no-login"), null, List.of());
  }

  @Test
  void unaFotoCompartidaPorDosUnidadesDeduplicadaYComprimida() {
    tx(
        s -> {
          int product = create();
          var image = image(0xabcdef);
          UUID id = service.subir(1, product, "foto.png", image),
              again = service.subir(1, product, "foto.png", image);
          assertEquals(id, again);
          assertEquals(1, service.listar(1, product).size());
          assertEquals(
              2,
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM INV.ACTIVO WHERE ID_ITEM=?", Integer.class, product));
          assertEquals(
              1,
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM GUI_BF.dbo.FOTO_PRODUCTO_BINARIO WHERE ID_FOTO=?",
                  Integer.class,
                  id.toString()));
          assertTrue(service.leer(1, id, false).length <= 153600);
          assertTrue(service.leer(1, id, true).length <= 30720);
          var assets =
              jdbc.queryForList("EXEC INV.sp_Listar 1,30,0,100").stream()
                  .filter(r -> Integer.valueOf(product).equals(r.get("productId")))
                  .toList();
          assertEquals(2, assets.size());
          for (var asset : assets)
            assertEquals(id, UUID.fromString(String.valueOf(asset.get("photoId"))));
        });
  }

  @Test
  void portadaYRetiroConservanBinarioYRegistro() {
    tx(
        s -> {
          int product = create();
          UUID first = service.subir(1, product, "uno.png", image(0xabcdef)),
              second = service.subir(1, product, "dos.png", image(0x123456));
          service.cambiar(1, product, second, "cover");
          assertEquals(
              second, UUID.fromString(String.valueOf(service.listar(1, product).get(0).get("id"))));
          service.cambiar(1, product, second, "remove");
          assertEquals(
              first, UUID.fromString(String.valueOf(service.listar(1, product).get(0).get("id"))));
          assertEquals(
              2,
              jdbc.queryForObject(
                  "SELECT ESTADO_REGISTRO FROM DOC.FOTO_PRODUCTO WHERE ID_FOTO=?",
                  Integer.class,
                  second.toString()));
          assertEquals(
              1,
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM GUI_BF.dbo.FOTO_PRODUCTO_BINARIO WHERE ID_FOTO=?",
                  Integer.class,
                  second.toString()));
          assertTrue(service.leer(1, first, true).length > 0);
        });
  }

  @Test
  void limitaCincoFotos() {
    tx(
        s -> {
          int product = create();
          for (int i = 0; i < 5; i++)
            service.subir(1, product, "foto.png", image(0x223344 + i * 0x112200));
          assertEquals(5, service.listar(1, product).size());
          assertThrows(
              org.springframework.dao.DataAccessException.class,
              () -> service.subir(1, product, "sexta.png", image(0xff22ee)));
        });
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3})
  void otrosPerfilesSoloPuedenLeerAunqueSeLesConceda303(int role) {
    tx(
        s -> {
          int product = create();
          UUID photo = service.subir(1, product, "foto.png", image(0xabcdef));
          int actor = actor(role, true);
          assertEquals(
              0, jdbc.queryForObject("SELECT SEC.fnTienePermiso(?,303)", Integer.class, actor));
          try {
            mvc.perform(
                    get("/api/products/" + product + "/photos").with(authentication(auth(actor))))
                .andExpect(status().isOk());
            mvc.perform(
                    multipart("/api/products/" + product + "/photos")
                        .file(
                            new MockMultipartFile("file", "foto.png", "image/png", image(0xaaaaaa)))
                        .with(authentication(auth(actor)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
            for (String action : List.of("cover", "remove"))
              mvc.perform(
                      post("/api/products/" + product + "/photos/" + photo + "/" + action)
                          .with(authentication(auth(actor)))
                          .with(csrf()))
                  .andExpect(status().isForbidden());
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }

  @Test
  void getNoPermiteSaltarSesionNiAlcanceYRevalidaCache() {
    tx(
        s -> {
          int product = create();
          UUID photo = service.subir(1, product, "foto.png", image(0xabcdef));
          int limited = actor(3, false);
          try {
            mvc.perform(get("/api/product-photos/" + photo)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/product-photos/" + photo).with(authentication(auth(limited))))
                .andExpect(status().isForbidden());
            var result =
                mvc.perform(get("/api/product-photos/" + photo).with(authentication(auth(1))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/webp"))
                    .andReturn();
            String tag = result.getResponse().getHeader("ETag");
            mvc.perform(
                    get("/api/product-photos/" + photo)
                        .with(authentication(auth(1)))
                        .header("If-None-Match", tag))
                .andExpect(status().isNotModified());
            mvc.perform(
                    get("/api/product-photos/" + photo)
                        .with(authentication(auth(limited)))
                        .header("If-None-Match", tag))
                .andExpect(status().isForbidden());
            service.cambiar(1, product, photo, "remove");
            mvc.perform(
                    get("/api/product-photos/" + photo)
                        .with(authentication(auth(1)))
                        .header("If-None-Match", tag))
                .andExpect(status().isNotFound());
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }

  @Test
  void administradorSubePorHttpConCsrfYRechazaArchivosFalsos() {
    tx(
        s -> {
          int product = create();
          try {
            mvc.perform(
                    multipart("/api/products/" + product + "/photos")
                        .file(
                            new MockMultipartFile("file", "foto.png", "image/png", image(0xaaaaaa)))
                        .with(authentication(auth(1))))
                .andExpect(status().isForbidden());
            mvc.perform(
                    multipart("/api/products/" + product + "/photos")
                        .file(
                            new MockMultipartFile("file", "foto.png", "image/png", image(0xaaaaaa)))
                        .with(authentication(auth(1)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
            mvc.perform(
                    multipart("/api/products/" + product + "/photos")
                        .file(
                            new MockMultipartFile(
                                "file", "falsa.png", "image/png", "malformed".getBytes()))
                        .with(authentication(auth(1)))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }
}
