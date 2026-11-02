package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pe.edu.gui.config.SecurityConfig;
import pe.edu.gui.security.*;
import pe.edu.gui.controller.UbicacionController;
import pe.edu.gui.dao.UsuarioDAO;
import pe.edu.gui.dto.Requests;
import java.util.*;
import java.util.stream.Stream;

/** Controlador, seguridad, servicio, DAO y SQL reales; ninguna baja de prueba se confirma. */
@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@WebMvcTest(UbicacionController.class)
@Import({SecurityConfig.class,Access.class,LocationAutoSqlTest.Config.class,UsuarioDAO.class,SeguridadService.class})
class LocationDeleteHttpSqlTest {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;
 @Autowired UbicacionService service; @Autowired DataSourceTransactionManager manager;
 static Stream<Arguments> cases(){
  var out=new ArrayList<Arguments>();
  for(int role:new int[]{1,2,3}) for(int level=0;level<4;level++) out.add(Arguments.of(role,level));
  return out.stream();
 }
 @ParameterizedTest(name="HTTP perfil {0}, nivel {1}") @MethodSource("cases")
 void endpointProtegidoConSqlReal(int role,int level) {
  long users=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM SEC.USUARIO",Long.class);
  long locations=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.UBICACION",Long.class);
  long audit=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AUD.EVENTO",Long.class);
  new TransactionTemplate(manager).execute(status->{
   String tag="http-revertido-"+UUID.randomUUID();
   int actor=jdbc.queryForObject("INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) OUTPUT INSERTED.ID_USUARIO VALUES(?,?,?,'HASH-NO-UTILIZABLE-PARA-LOGIN')",Integer.class,tag,tag,tag+"@example.invalid");
   jdbc.update("INSERT SEC.USUARIO_ROL VALUES(?,?)",actor,role);
   jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(?,NULL)",actor);
   jdbc.update("IF NOT EXISTS(SELECT 1 FROM SEC.ROL_PERMISO WHERE ID_ROL=? AND ID_PERMISO=503) INSERT SEC.ROL_PERMISO VALUES(?,503)",role,role);
   int[] types={201,203,206,205};Integer parent=null;
   for(int i=0;i<=level;i++) parent=Math.toIntExact(service.guardar(1,new Requests.Location(null,tag+"-"+i,null,types[i],parent,null)).id());
   int id=parent;var auth=new UsernamePasswordAuthenticationToken(new GuiPrincipal(actor,tag,"no-login"),null,List.of());
   try {
    mvc.perform(post("/api/locations/"+id+"/delete").with(authentication(auth)))
     .andExpect(status().isForbidden());
    assertEquals(1,jdbc.queryForObject("SELECT ESTADO_REGISTRO FROM INV.UBICACION WHERE ID_UBICACION=?",Integer.class,id));
    mvc.perform(post("/api/locations/"+id+"/delete").with(authentication(auth)).with(csrf()))
     .andExpect(role==1?status().isOk():status().isForbidden());
    assertEquals(role==1?2:1,jdbc.queryForObject("SELECT ESTADO_REGISTRO FROM INV.UBICACION WHERE ID_UBICACION=?",Integer.class,id));
   }catch(Exception error){throw new IllegalStateException(error);}
   status.setRollbackOnly();return null;
  });
  assertEquals(users,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM SEC.USUARIO",Long.class));
  assertEquals(locations,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.UBICACION",Long.class));
  assertEquals(audit,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AUD.EVENTO",Long.class));
 }
}
