package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import pe.edu.gui.controller.AdquisicionController;
import pe.edu.gui.dao.*;
import pe.edu.gui.dto.Requests;
import java.util.*;

/** Solo fixtures de prueba; todas las cuentas, proveedores y relaciones se revierten. */
@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@WebMvcTest(AdquisicionController.class)
@Import({SecurityConfig.class,Access.class,MovementBatchSqlTest.Config.class,UsuarioDAO.class,SeguridadService.class,AdquisicionDAO.class,AdquisicionService.class})
class ProviderDeleteSqlTest {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired AdquisicionService service;
 @Autowired MovimientoService movements; @Autowired DataSourceTransactionManager manager;
 Map<String,Long> initial;
 Map<String,Long> counts(){var out=new LinkedHashMap<String,Long>();for(String table:List.of("INV.TERCERO","SEC.USUARIO","SEC.USUARIO_ROL","SEC.USUARIO_ALCANCE","SEC.ROL_PERMISO","DOC.DOCUMENTO","DOC.OPERACION_DOCUMENTO","INV.OPERACION","INV.MOVIMIENTO","INV.KARDEX_MOVIMIENTO","AUD.EVENTO"))out.put(table,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM "+table,Long.class));return out;}
 @BeforeEach void before(){initial=counts();}
 @AfterEach void clean(){assertEquals(initial,counts(),"Ningún fixture o auditoría de prueba permanece");}
 void transaction(java.util.function.Consumer<org.springframework.transaction.TransactionStatus> test){new TransactionTemplate(manager).execute(status->{test.accept(status);status.setRollbackOnly();return null;});}
 int create(){return Math.toIntExact(service.guardarInstitucion(1,new Requests.Institution(null,"Proveedor QA revertido "+UUID.randomUUID(),"QA"+System.nanoTime(),"QA",null,null,null,1)).id());}
 @ParameterizedTest @ValueSource(ints={1,2,3}) void endpointExclusivoAdministradorInclusoConPermisoAsignado(int role){
  transaction(status->{
   int id=create();String tag="proveedor-qa-"+UUID.randomUUID();
   int actor=jdbc.queryForObject("INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) OUTPUT INSERTED.ID_USUARIO VALUES(?,?,?,'NO-LOGIN-TEST')",Integer.class,tag,tag,tag+"@example.invalid");
   jdbc.update("INSERT SEC.USUARIO_ROL VALUES(?,?)",actor,role);
   jdbc.update("IF NOT EXISTS(SELECT 1 FROM SEC.ROL_PERMISO WHERE ID_ROL=? AND ID_PERMISO=1303) INSERT SEC.ROL_PERMISO VALUES(?,1303)",role,role);
   var auth=new UsernamePasswordAuthenticationToken(new GuiPrincipal(actor,tag,"not-login"),null,List.of());
   try{
    mvc.perform(post("/api/institutions/"+id+"/delete").with(authentication(auth))).andExpect(status().isForbidden());
    mvc.perform(post("/api/institutions/"+id+"/delete").with(authentication(auth)).with(csrf())).andExpect(role==1?status().isOk():status().isForbidden());
   }catch(Exception error){throw new IllegalStateException(error);}
   assertEquals(role==1?2:1,jdbc.queryForObject("SELECT ESTADO_REGISTRO FROM INV.TERCERO WHERE ID_TERCERO=?",Integer.class,id));
   assertEquals(role==1?1:0,jdbc.queryForObject("SELECT COUNT(*) FROM AUD.EVENTO WHERE MODULO=130 AND ACCION=1303 AND ID_REGISTRO=?",Integer.class,id));
  });
 }
 @Test void conservaProveedorIngresosYDocumentosPeroLoRetiraDeNuevasSelecciones(){transaction(status->{
  int id=create();var before=jdbc.queryForMap("SELECT * FROM INV.TERCERO WHERE ID_TERCERO=?",id);
  int operation=jdbc.queryForObject("INSERT INV.OPERACION(CODIGO_INGRESO,ID_TIPO_MOVIMIENTO,ID_TERCERO,FECHA,ID_USUARIO) OUTPUT INSERTED.ID_OPERACION VALUES('QA'+LEFT(CONVERT(VARCHAR(36),NEWID()),20),1,?,CAST(GETDATE() AS DATE),1)",Integer.class,id);
  long doc=service.guardarDocumento(1,new Requests.Document(301,id,"QA-"+UUID.randomUUID(),java.time.LocalDate.now(),null)).id();
  jdbc.update("INSERT DOC.OPERACION_DOCUMENTO VALUES(?,?)",operation,doc);
  service.eliminarInstitucion(1,id);
  var after=jdbc.queryForMap("SELECT * FROM INV.TERCERO WHERE ID_TERCERO=?",id);before.put("ESTADO_REGISTRO",2);assertEquals(before,after);
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM INV.OPERACION WHERE ID_OPERACION=? AND ID_TERCERO=?",Integer.class,operation,id));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM DOC.DOCUMENTO WHERE ID_DOCUMENTO=? AND ID_TERCERO=?",Integer.class,doc,id));
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM DOC.OPERACION_DOCUMENTO WHERE ID_OPERACION=? AND ID_DOCUMENTO=?",Integer.class,operation,doc));
  assertTrue(jdbc.queryForList("EXEC INV.sp_Listar 1,130,0,100").stream().noneMatch(row->Integer.valueOf(id).equals(row.get("id"))));
  assertTrue(jdbc.queryForList("EXEC INV.sp_Lookup 1,5").stream().noneMatch(row->Integer.valueOf(id).equals(row.get("id"))));
  var found=service.buscarInstitucion(1,(String)after.get("DOCUMENTO"));assertEquals(true,found.get("found"));
  assertEquals(2,((Map<?,?>)found.get("institution")).get("stateId"));
  assertTrue(jdbc.queryForList("EXEC INV.sp_Listar 1,140,0,100").stream().anyMatch(row->Integer.valueOf(operation).equals(row.get("id"))));
 });}
 @Test void noReactivaEliminadoDesdeEdicionVieja(){transaction(status->{int id=create();service.eliminarInstitucion(1,id);
  assertThrows(org.springframework.dao.DataAccessException.class,()->service.guardarInstitucion(1,new Requests.Institution(id,"Reactivar prohibido","QA-OTRO",null,null,null,null,1)));});}
 @Test void noAceptaComprobanteNuevoConProveedorEliminado(){transaction(status->{int id=create();service.eliminarInstitucion(1,id);
  assertThrows(org.springframework.dao.DataAccessException.class,()->service.guardarDocumento(1,new Requests.Document(301,id,"QA",java.time.LocalDate.now(),null)));});}
 @Test void noAceptaSolicitudNuevaConInstitucionEliminada(){transaction(status->{int id=create();service.eliminarInstitucion(1,id);
  int asset=jdbc.queryForObject("SELECT MIN(ID_ACTIVO) FROM INV.ACTIVO WHERE ESTADO_REGISTRO=10",Integer.class);
  assertThrows(org.springframework.dao.DataAccessException.class,()->movements.solicitar(1,new Requests.Movement(asset,4,null,id,"Prueba revertida",UUID.randomUUID())));});}
 @Test void noDuplicaLaBaja(){transaction(status->{int id=create();service.eliminarInstitucion(1,id);
  assertThrows(org.springframework.dao.DataAccessException.class,()->service.eliminarInstitucion(1,id));});}
 @Test void rechazaIdInvalidoSinEscritura(){assertThrows(IllegalArgumentException.class,()->service.eliminarInstitucion(1,0));}
}
