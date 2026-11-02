package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pe.edu.gui.config.SecurityConfig;
import pe.edu.gui.security.*;
import pe.edu.gui.controller.AdquisicionController;
import pe.edu.gui.dao.*;
import pe.edu.gui.interfaces.*;
import pe.edu.gui.dto.*;
import java.util.*;

@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@WebMvcTest(AdquisicionController.class)
@Import({SecurityConfig.class,Access.class,MovementBatchSqlTest.Config.class,UsuarioDAO.class,SeguridadService.class,IntakeCancelSqlTest.Beans.class})
class IntakeCancelSqlTest {
 @org.springframework.boot.test.context.TestConfiguration static class Beans {
  @Bean IAdquisicionDAO adquisicionDAO(ISqlExecutor sql){return spy(new AdquisicionDAO(sql));}
  @Bean AdquisicionService adquisicionService(IAdquisicionDAO dao){return new AdquisicionService(dao);}
  @Bean IInventarioDAO inventarioDAO(ISqlExecutor sql){return new InventarioDAO(sql);}
  @Bean InventarioService inventarioService(IInventarioDAO dao){return new InventarioService(dao);}
 }
 @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired AdquisicionService service;
 @Autowired InventarioService inventory; @Autowired MovimientoService movements;
 @Autowired IAdquisicionDAO dao; @Autowired DataSourceTransactionManager manager;
 Map<String,String> before;
 Map<String,String> snapshot(){
  var result=new LinkedHashMap<String,String>();
  for(String table:List.of("INV.OPERACION","INV.OPERACION_DETALLE","INV.ACTIVO","INV.ITEM","INV.ACTIVO_ESPECIFICACION","INV.REGISTRO_LOTE","INV.CORRELATIVO_TIPO","INV.CORRELATIVO_INGRESO","INV.MOVIMIENTO","INV.MANTENIMIENTO","INV.KARDEX_MOVIMIENTO","DOC.DOCUMENTO","DOC.OPERACION_DOCUMENTO","AUD.EVENTO","SEC.USUARIO","SEC.USUARIO_ROL","SEC.USUARIO_ALCANCE","SEC.ROL_PERMISO")){
   var text=new StringBuilder();for(var row:jdbc.queryForList("SELECT * FROM "+table+" ORDER BY 1,2")){for(var e:row.entrySet()){String v=e.getValue() instanceof byte[] b?Base64.getEncoder().encodeToString(b):String.valueOf(e.getValue());text.append(e.getKey()).append(':').append(v.length()).append(':').append(v).append(';');}text.append('\n');}
   try{result.put(table,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));}catch(Exception e){throw new IllegalStateException(e);}
  }return result;
 }
 @BeforeEach void start(){reset(dao);before=snapshot();}
 @AfterEach void unchanged(){assertEquals(before,snapshot(),"No se conservaron fixtures, cambios en registros reales ni auditorías de prueba");}
 void tx(java.util.function.Consumer<org.springframework.transaction.TransactionStatus> run){new TransactionTemplate(manager).execute(status->{run.accept(status);status.setRollbackOnly();return null;});}
 record Fixture(int id,List<Integer> assets,RegistroInventario request){}
 Fixture create(){
  int family=jdbc.queryForObject("SELECT MIN(ID_FAMILIA) FROM INV.FAMILIA_PRODUCTO WHERE NOMBRE=N'Microscopio'",Integer.class);
  int model=jdbc.queryForObject("SELECT MIN(ID_ITEM) FROM INV.ITEM WHERE ID_FAMILIA=? AND ESTADO_REGISTRO=1",Integer.class,family);
  int provider=jdbc.queryForObject("SELECT MIN(ID_TERCERO) FROM INV.TERCERO WHERE ESTADO_REGISTRO=1",Integer.class);
  int location=jdbc.queryForObject("SELECT MIN(ID_UBICACION) FROM INV.UBICACION WHERE ESTADO_REGISTRO=1",Integer.class);
  var request=new RegistroInventario(UUID.randomUUID(),1,provider,null,java.time.LocalDate.now(),"Anulación QA revertida",location,1,List.of(new RegistroInventario.Linea(family,model,null,null,null,2,java.math.BigDecimal.ZERO,List.of(),List.of())),List.of(),List.of());
  var rows=inventory.registrar(1,request);assertEquals(2,rows.size());
  return new Fixture(((Number)rows.get(0).get("intakeId")).intValue(),rows.stream().map(row->((Number)row.get("id")).intValue()).toList(),request);
 }
 int actor(int role,boolean global){String tag="anula-qa-"+UUID.randomUUID();int id=jdbc.queryForObject("INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) OUTPUT INSERTED.ID_USUARIO VALUES(?,?,?,'NO-LOGIN-QA')",Integer.class,tag,tag,tag+"@example.invalid");jdbc.update("INSERT SEC.USUARIO_ROL VALUES(?,?)",id,role);if(global)jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(?,NULL)",id);for(int perm:new int[]{1401,1403})jdbc.update("IF NOT EXISTS(SELECT 1 FROM SEC.ROL_PERMISO WHERE ID_ROL=? AND ID_PERMISO=?) INSERT SEC.ROL_PERMISO VALUES(?,?)",role,perm,role,perm);return id;}
 org.springframework.test.web.servlet.MvcResult cancel(int actor,int id,int status,boolean csrf){
  var auth=new UsernamePasswordAuthenticationToken(new GuiPrincipal(actor,"qa","not-login"),null,List.of());var request=post("/api/intakes/"+id+"/delete").with(authentication(auth));if(csrf)request=request.with(csrf());
  try{return mvc.perform(request).andExpect(status().is(status)).andReturn();}catch(Exception e){throw new IllegalStateException(e);}
 }
 @ParameterizedTest @ValueSource(ints={1,2,3}) void perfilesYCsrf(int role){tx(status->{var fixture=create();int actor=actor(role,true);
  cancel(actor,fixture.id,403,false);cancel(actor,fixture.id,role==1?200:403,true);
  assertEquals(role==1?2:1,jdbc.queryForObject("SELECT ESTADO_REGISTRO FROM INV.OPERACION WHERE ID_OPERACION=?",Integer.class,fixture.id));
  for(int id:fixture.assets)assertEquals(role==1?2:10,jdbc.queryForObject("SELECT ESTADO_REGISTRO FROM INV.ACTIVO WHERE ID_ACTIVO=?",Integer.class,id));
 });}
 @Test void anulaTodoConservandoIdentificadoresHistorialYComprobantes(){tx(status->{
  var fixture=create();var original=jdbc.queryForList("SELECT a.ID_ACTIVO,a.CODIGO_PATRIMONIAL,a.TOKEN_QR,a.ID_ITEM,a.ID_DETALLE_INGRESO,a.ID_UBICACION_ACTUAL,a.NUMERO_SERIE FROM INV.ACTIVO a JOIN INV.OPERACION_DETALLE d ON d.ID_DETALLE=a.ID_DETALLE_INGRESO WHERE d.ID_OPERACION=? ORDER BY a.ID_ACTIVO",fixture.id);
  long doc=service.guardarDocumento(1,new Requests.Document(301,fixture.request.institutionId(),"QA-"+UUID.randomUUID(),java.time.LocalDate.now(),null)).id();jdbc.update("INSERT DOC.OPERACION_DOCUMENTO VALUES(?,?)",fixture.id,doc);
  int oldAssets=((Number)jdbc.queryForMap("EXEC INV.sp_Listar 1,10,0,51").get("assets")).intValue();
  service.anularIngreso(1,fixture.id);
  assertEquals(original,jdbc.queryForList("SELECT a.ID_ACTIVO,a.CODIGO_PATRIMONIAL,a.TOKEN_QR,a.ID_ITEM,a.ID_DETALLE_INGRESO,a.ID_UBICACION_ACTUAL,a.NUMERO_SERIE FROM INV.ACTIVO a JOIN INV.OPERACION_DETALLE d ON d.ID_DETALLE=a.ID_DETALLE_INGRESO WHERE d.ID_OPERACION=? ORDER BY a.ID_ACTIVO",fixture.id));
  assertEquals(oldAssets-2,((Number)jdbc.queryForMap("EXEC INV.sp_Listar 1,10,0,51").get("assets")).intValue());
  assertTrue(jdbc.queryForList("EXEC INV.sp_Listar 1,140,0,100").stream().noneMatch(row->Integer.valueOf(fixture.id).equals(row.get("id"))));
  assertTrue(jdbc.queryForList("EXEC INV.sp_Listar 1,30,0,100").stream().noneMatch(row->fixture.assets.contains(row.get("id"))));
  assertEquals(oldAssets-2,jdbc.queryForList("EXEC INV.sp_Listar 1,80,0,100").stream().mapToInt(row->((Number)row.get("quantity")).intValue()).sum());
  assertEquals(oldAssets-2,jdbc.queryForList("EXEC INV.sp_Dashboard 1,2").stream().mapToInt(row->((Number)row.get("count")).intValue()).sum());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM DOC.OPERACION_DOCUMENTO WHERE ID_OPERACION=? AND ID_DOCUMENTO=?",Integer.class,fixture.id,doc));
  for(int id:fixture.assets){assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM INV.KARDEX_MOVIMIENTO WHERE ID_ACTIVO=? AND ID_TIPO_MOVIMIENTO=1",Integer.class,id));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM INV.KARDEX_MOVIMIENTO WHERE ID_ACTIVO=? AND ID_TIPO_MOVIMIENTO=8 AND ID_ESTADO_EVENTO=2",Integer.class,id));}
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM AUD.EVENTO WHERE ACCION=1403 AND ID_REGISTRO=?",Integer.class,fixture.id));
  assertEquals(2,service.detalleIngreso(1,fixture.id).get("units").size());
 });}
 @Test void rechazaTodaLaOperacionSiLaSegundaUnidadTuvoMovimientos(){tx(status->{var fixture=create();int id=fixture.assets.get(1);String code=jdbc.queryForObject("SELECT CODIGO_PATRIMONIAL FROM INV.ACTIVO WHERE ID_ACTIVO=?",String.class,id);int dest=jdbc.queryForObject("SELECT MIN(ID_UBICACION) FROM INV.UBICACION WHERE ESTADO_REGISTRO=1 AND ID_UBICACION<>?",Integer.class,fixture.request.locationId());movements.solicitar(1,new Requests.Movement(id,2,dest,null,"QA revertida",UUID.randomUUID()));
  var response=new String(cancel(1,fixture.id,409,true).getResponse().getContentAsByteArray(),java.nio.charset.StandardCharsets.UTF_8);assertTrue(response.contains(code));assertTrue(response.contains("tuvo movimientos"));
 });}
 @Test void rechazaMantenimientoPosterior(){tx(status->{var fixture=create();jdbc.update("INSERT INV.MANTENIMIENTO(ID_ACTIVO,FECHA_PROGRAMADA,DESCRIPCION,ID_TECNICO) VALUES(?,GETDATE(),N'QA revertida',1)",fixture.assets.get(0));var response=new String(cancel(1,fixture.id,409,true).getResponse().getContentAsByteArray(),java.nio.charset.StandardCharsets.UTF_8);assertTrue(response.contains("mantenimiento"));});}
 @Test void rechazaAlcanceParcial(){tx(status->{var fixture=create();int actor=actor(1,false);int other=jdbc.queryForObject("SELECT MIN(ID_UBICACION) FROM INV.UBICACION WHERE ESTADO_REGISTRO=1 AND ID_UBICACION<>?",Integer.class,fixture.request.locationId());jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(?,?)",actor,other);cancel(actor,fixture.id,403,true);});}
 @Test void falloTrasSqlRevierteTodaLaAnulacion(){
  var error=assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).execute(status->{var fixture=create();doAnswer(call->{call.callRealMethod();throw new IllegalStateException("Fallo simulado después de las escrituras");}).when(dao).anularIngreso(1,fixture.id);service.anularIngreso(1,fixture.id);return null;}));assertTrue(error.getMessage().contains("Fallo simulado"));
 }
 @Test void noReactivaConUnReintentoDeRegistro(){tx(status->{var fixture=create();service.anularIngreso(1,fixture.id);assertThrows(org.springframework.dao.DataAccessException.class,()->inventory.registrar(1,fixture.request));});}
 @Test void anularNoEsUnTipoSolicitable(){tx(status->{assertTrue(jdbc.queryForList("EXEC INV.sp_Lookup 1,7").stream().noneMatch(row->Integer.valueOf(8).equals(row.get("id"))));var fixture=create();assertThrows(IllegalArgumentException.class,()->movements.solicitar(1,new Requests.Movement(fixture.assets.get(0),8,null,null,"QA",UUID.randomUUID()))); var error=assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.queryForList("EXEC INV.sp_Gestionar @usuario=1,@accion=402,@datos=?","<request><assetId>"+fixture.assets.get(0)+"</assetId><typeId>8</typeId><reason>QA</reason><idempotencyKey>"+UUID.randomUUID()+"</idempotencyKey></request>")); assertEquals(51003,((java.sql.SQLException)error.getRootCause()).getErrorCode());});}
 @Test void anulacionNoLiberaNiRenombraElCodigoDelIngreso(){tx(status->{
  int before=jdbc.queryForObject("SELECT ULTIMO FROM INV.CORRELATIVO_INGRESO WHERE ID=1",Integer.class);
  var first=create();String code=jdbc.queryForObject("SELECT CODIGO_INGRESO FROM INV.OPERACION WHERE ID_OPERACION=?",String.class,first.id);
  assertEquals("ING"+String.format("%04d",before+1),code);service.anularIngreso(1,first.id);
  assertEquals(code,jdbc.queryForObject("SELECT CODIGO_INGRESO FROM INV.OPERACION WHERE ID_OPERACION=?",String.class,first.id));
  var next=create();assertEquals("ING"+String.format("%04d",before+2),jdbc.queryForObject("SELECT CODIGO_INGRESO FROM INV.OPERACION WHERE ID_OPERACION=?",String.class,next.id));
 });}
}
