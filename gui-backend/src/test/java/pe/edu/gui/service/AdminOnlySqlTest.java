package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.gui.dto.Requests;
import java.util.*;
import java.util.stream.Stream;

/** Cuentas y ubicaciones desechables: cada caso se revierte íntegramente. */
@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@SpringJUnitConfig(LocationAutoSqlTest.Config.class)
class AdminOnlySqlTest {
 @Autowired JdbcTemplate jdbc; @Autowired UbicacionService service;
 @Autowired DataSourceTransactionManager manager;
 Map<String,String> initial;
 static final String[] TABLES={"INV.UBICACION","INV.ACTIVO","INV.KARDEX_MOVIMIENTO","INV.MOVIMIENTO","AUD.EVENTO","SEC.USUARIO","SEC.USUARIO_ROL","SEC.USUARIO_ALCANCE","SEC.ROL_PERMISO","SEC.ROL"};
 Map<String,String> snapshot() {
  var result=new LinkedHashMap<String,String>();
  for(String table:TABLES) {
   var canonical=new StringBuilder();
   for(var row:jdbc.queryForList("SELECT * FROM "+table+" ORDER BY 1,2")) {
    for(var entry:row.entrySet()) {
     Object value=entry.getValue();
     String text=value instanceof byte[] bytes ? Base64.getEncoder().encodeToString(bytes) : String.valueOf(value);
     canonical.append(entry.getKey()).append(':').append(text.length()).append(':').append(text).append(';');
    }
    canonical.append('\n');
   }
   try {
    result.put(table,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
   } catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
  }
  return result;
 }
 @BeforeEach void before(){initial=snapshot();}
 @AfterEach void unchanged(){assertEquals(initial,snapshot(),"Sin cambios persistidos, cuentas o auditorías de prueba");}
 void transaction(java.util.function.IntConsumer test,int role){
  new TransactionTemplate(manager).execute(status->{
   String tag="verificacion-"+UUID.randomUUID();
   int actor=jdbc.queryForObject("INSERT SEC.USUARIO(USERNAME,NOMBRE,CORREO,PASSWORD_HASH) OUTPUT INSERTED.ID_USUARIO VALUES(?,?,?,'HASH-NO-UTILIZABLE-PARA-LOGIN')",Integer.class,tag,tag,tag+"@example.invalid");
   jdbc.update("INSERT SEC.USUARIO_ROL VALUES(?,?)",actor,role);
   jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(?,NULL)",actor);
   // Incluso permisos asignados por error no deben habilitar edición/baja a otros perfiles.
   for(int permission:new int[]{301,302,502,503,504,902,1002,1302})
    jdbc.update("IF NOT EXISTS(SELECT 1 FROM SEC.ROL_PERMISO WHERE ID_ROL=? AND ID_PERMISO=?) INSERT SEC.ROL_PERMISO VALUES(?,?)",role,permission,role,permission);
   test.accept(actor);status.setRollbackOnly();return null;
  });
 }
 static Stream<Arguments> cases(){
  var out=new ArrayList<Arguments>();
  for(int role:new int[]{1,2,3}) for(int level=0;level<4;level++) for(String action:new String[]{"editar","eliminar"}) out.add(Arguments.of(role,level,action));
  return out.stream();
 }
 int create(Integer parent,int type){return Math.toIntExact(service.guardar(1,new Requests.Location(null,"Prueba administrativa revertida "+UUID.randomUUID(),null,type,parent,null)).id());}
 void forbidden(org.junit.jupiter.api.function.Executable operation) {
  var error=assertThrows(org.springframework.dao.DataAccessException.class,operation);
  assertInstanceOf(java.sql.SQLException.class,error.getRootCause());
  assertEquals(51003,((java.sql.SQLException)error.getRootCause()).getErrorCode(),"Rechazo por autorización, no por datos incompletos");
 }
 @ParameterizedTest(name="Perfil {0}, nivel {1}, acción {2}") @MethodSource("cases")
 void cadaNivelYPerfil(int role,int level,String action) {
  transaction(actor->{
   int[] types={201,203,206,205};Integer parent=null,previous=null;
   for(int i=0;i<=level;i++){previous=parent;parent=create(parent,types[i]);}
   int id=parent;Integer expectedParent=previous;
   String code=jdbc.queryForObject("SELECT CODIGO FROM INV.UBICACION WHERE ID_UBICACION=?",String.class,id);
   assertEquals(role==1?1:0,jdbc.queryForObject("SELECT SEC.fnTienePermiso(?,503)",Integer.class,actor));
   assertEquals(role==1?1:0,jdbc.queryForObject("SELECT SEC.fnTienePermiso(?,504)",Integer.class,actor));
   var identity=jdbc.queryForList("EXEC SEC.sp_Identidad ?",actor);
   if(role!=1) assertTrue(identity.stream().noneMatch(row->row.get("permissionId") != null && Set.of(503,504,902,1002).contains(row.get("permissionId"))));
   org.junit.jupiter.api.function.Executable operation=action.equals("eliminar")
    ? ()->service.eliminar(actor,id)
    : ()->service.guardar(actor,new Requests.Location(id,"Nombre corregido en prueba",null,types[level],expectedParent,null));
   if(role!=1){forbidden(operation);return;}
   assertDoesNotThrow(operation);
   assertEquals(code,jdbc.queryForObject("SELECT CODIGO FROM INV.UBICACION WHERE ID_UBICACION=?",String.class,id));
   assertEquals(expectedParent,jdbc.queryForObject("SELECT ID_UBICACION_PADRE FROM INV.UBICACION WHERE ID_UBICACION=?",Integer.class,id));
   assertEquals(action.equals("eliminar")?2:1,jdbc.queryForObject("SELECT ESTADO_REGISTRO FROM INV.UBICACION WHERE ID_UBICACION=?",Integer.class,id));
   if(action.equals("editar")) assertEquals("Nombre corregido en prueba",jdbc.queryForObject("SELECT NOMBRE FROM INV.UBICACION WHERE ID_UBICACION=?",String.class,id));
  },role);
 }
 @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(ints={2,3})
 void otrosModulosRechazanEdicion(int role){
  // Cada error SQL revierte la transacción completa; se prueba cada ruta en su propia transacción.
  transaction(actor->{
   int asset=jdbc.queryForObject("SELECT MIN(ID_ACTIVO) FROM INV.ACTIVO",Integer.class);
   forbidden(()->jdbc.queryForList("EXEC INV.sp_ActivoCambiarNombre ?,?,?,?",actor,asset,"Cambio prohibido","Nombre anterior"));
  },role);
  transaction(actor->forbidden(()->jdbc.queryForList("EXEC INV.sp_Gestionar @usuario=?,@accion=202,@datos=?",actor,"<request><id>1</id><name>Cambio prohibido</name></request>")),role);
  transaction(actor->forbidden(()->jdbc.queryForList("EXEC INV.sp_Gestionar @usuario=?,@accion=703,@datos=?",actor,"<request><id>1</id><name>Cambio prohibido</name></request>")),role);
  transaction(actor->{
   for(int permission:new int[]{902,1002}) assertEquals(0,jdbc.queryForObject("SELECT SEC.fnTienePermiso(?,?)",Integer.class,actor,permission));
  },role);
 }
}
