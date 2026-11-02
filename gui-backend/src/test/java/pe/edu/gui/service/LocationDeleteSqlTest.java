package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.gui.dto.Requests;

@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@SpringJUnitConfig(LocationAutoSqlTest.Config.class)
class LocationDeleteSqlTest {
 @Autowired JdbcTemplate jdbc;
 @Autowired UbicacionService service;
 @Autowired DataSourceTransactionManager manager;
 long locations,audit,kardex;
 @BeforeEach void before() {
  locations=count("INV.UBICACION");audit=count("AUD.EVENTO");kardex=count("INV.KARDEX_MOVIMIENTO");
 }
 @AfterEach void clean() {
  assertEquals(locations,count("INV.UBICACION"));assertEquals(audit,count("AUD.EVENTO"));assertEquals(kardex,count("INV.KARDEX_MOVIMIENTO"));
 }
 long count(String table){return jdbc.queryForObject("SELECT COUNT_BIG(*) FROM "+table,Long.class);}
 int create(Integer parent,int type) {
  return Math.toIntExact(service.guardar(1,new Requests.Location(null,"Baja revertida "+java.util.UUID.randomUUID(),null,type,parent,null)).id());
 }
 void transaction(java.util.function.Consumer<org.springframework.transaction.TransactionStatus> work) {
  new TransactionTemplate(manager).execute(status->{work.accept(status);status.setRollbackOnly();return null;});
 }
 @ParameterizedTest @ValueSource(ints={0,1,2,3}) void bajaLogicaDeCadaNivelConAuditoria(int level) {
  transaction(status->{
   int[] types={201,203,206,205};Integer parent=null;
   for(int i=0;i<=level;i++) parent=create(parent,types[i]);
   int id=parent;
   String code=jdbc.queryForObject("SELECT CODIGO FROM INV.UBICACION WHERE ID_UBICACION=?",String.class,id);
   service.eliminar(1,id);
   assertEquals(2,jdbc.queryForObject("SELECT ESTADO_REGISTRO FROM INV.UBICACION WHERE ID_UBICACION=?",Integer.class,id));
   assertEquals(code,jdbc.queryForObject("SELECT CODIGO FROM INV.UBICACION WHERE ID_UBICACION=?",String.class,id));
   assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM SEC.fnUbicaciones(1) WHERE ID_UBICACION=?",Integer.class,id));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM AUD.EVENTO WHERE ACCION=503 AND MODULO=50 AND ID_REGISTRO=?",Integer.class,id));
  });
 }
 @Test void bloqueaHijosSinBorradoEnCascada() {
  transaction(status->{int root=create(null,201);create(root,206);
   assertThrows(org.springframework.dao.DataAccessException.class,()->service.eliminar(1,root));});
 }
 @Test void gestionarNoConcedeEliminar() {
  transaction(status->{int id=create(null,201);
   jdbc.update("DELETE FROM SEC.ROL_PERMISO WHERE ID_PERMISO=503");
   assertThrows(org.springframework.dao.DataAccessException.class,()->service.eliminar(1,id));});
 }
 @Test void permisoEliminarNoIgnoraAlcance() {
  transaction(status->{int outside=create(null,201),inside=create(null,201);
   jdbc.update("DELETE FROM SEC.USUARIO_ALCANCE WHERE ID_USUARIO=1");
   jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(1,?)",inside);
   assertThrows(org.springframework.dao.DataAccessException.class,()->service.eliminar(1,outside));});
 }
 @Test void bloqueaActivos() {
  transaction(status->{int id=create(null,201);
   jdbc.update("UPDATE INV.ACTIVO SET ID_UBICACION_ACTUAL=? WHERE ID_ACTIVO=(SELECT MIN(ID_ACTIVO) FROM INV.ACTIVO)",id);
   assertThrows(org.springframework.dao.DataAccessException.class,()->service.eliminar(1,id));});
 }
 @Test void bloqueaAlcancesAsignados() {
  transaction(status->{int id=create(null,201);
   jdbc.update("INSERT SEC.USUARIO_ALCANCE(ID_USUARIO,ID_UBICACION) VALUES(1,?)",id);
   assertThrows(org.springframework.dao.DataAccessException.class,()->service.eliminar(1,id));});
 }
 @Test void bloqueaDestinoDeMovimientoPendiente() {
  transaction(status->{int id=create(null,201);
   jdbc.update("INSERT INV.MOVIMIENTO(ID_ACTIVO,ID_TIPO_MOVIMIENTO,ID_UBICACION_ORIGEN,ID_UBICACION_DESTINO,ID_USUARIO_SOLICITA,MOTIVO,CLAVE_IDEMPOTENCIA) SELECT TOP(1) ID_ACTIVO,2,ID_UBICACION_ACTUAL,?,1,N'Prueba revertida',NEWID() FROM INV.ACTIVO ORDER BY ID_ACTIVO",id);
   assertThrows(org.springframework.dao.DataAccessException.class,()->service.eliminar(1,id));});
 }
}
