package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.gui.dto.Requests;
import pe.edu.gui.entity.EtapaMovimiento;

@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@SpringJUnitConfig(MovementBatchSqlTest.Config.class)
class DashboardMovementSqlTest {
 @Autowired JdbcTemplate jdbc; @Autowired MovimientoService service; @Autowired DataSourceTransactionManager manager;
 int movements(){return ((Number)jdbc.queryForMap("EXEC INV.sp_Listar @usuario=1,@modulo=10,@offset=0,@take=51").get("movements")).intValue();}
 @Test void contadorCoincideConSolicitudesYNoConIngresos(){
  int expected=jdbc.queryForObject("SELECT COUNT(*) FROM INV.MOVIMIENTO m WHERE SEC.fnPuedeActivo(1,m.ID_ACTIVO)=1 AND FECHA>=DATEFROMPARTS(YEAR(SYSUTCDATETIME()),MONTH(SYSUTCDATETIME()),1) AND FECHA<DATEADD(MONTH,1,DATEFROMPARTS(YEAR(SYSUTCDATETIME()),MONTH(SYSUTCDATETIME()),1))",Integer.class);
  assertEquals(expected,movements());
  var chart=jdbc.queryForList("EXEC INV.sp_Dashboard @usuario=1,@vista=1");
  assertEquals(6,chart.size());
  int events=jdbc.queryForObject("SELECT COUNT(*) FROM INV.KARDEX_MOVIMIENTO k WHERE SEC.fnPuedeActivo(1,k.ID_ACTIVO)=1 AND FECHA_MOVIMIENTO>=DATEFROMPARTS(YEAR(SYSUTCDATETIME()),MONTH(SYSUTCDATETIME()),1)",Integer.class);
  assertEquals(events,((Number)chart.get(5).get("count")).intValue());
 }
 @Test void etapasDeUnMovimientoNoDuplicanElContadorYSeRevierteLaPrueba(){
  int before=movements();long history=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.KARDEX_MOVIMIENTO",Long.class);
  long audit=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AUD.EVENTO",Long.class);
  new TransactionTemplate(manager).execute(status->{
   int asset=jdbc.queryForObject("SELECT MIN(ID_ACTIVO) FROM INV.ACTIVO WHERE ESTADO_REGISTRO=10",Integer.class);
   int dest=jdbc.queryForObject("SELECT MIN(ID_UBICACION) FROM INV.UBICACION WHERE ESTADO_REGISTRO=1 AND ID_UBICACION<>(SELECT ID_UBICACION_ACTUAL FROM INV.ACTIVO WHERE ID_ACTIVO=?)",Integer.class,asset);
   var movement=service.solicitar(1,new Requests.Movement(asset,2,dest,null,"Prueba dashboard revertida",java.util.UUID.randomUUID()));
   assertEquals(before+1,movements());
   service.avanzar(1,EtapaMovimiento.AUTORIZAR,new Requests.Transition(movement.id()));
   assertEquals(before+1,movements());
   assertTrue(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.KARDEX_MOVIMIENTO",Long.class)>history+1);
   status.setRollbackOnly();return null;
  });
  assertEquals(before,movements());assertEquals(history,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.KARDEX_MOVIMIENTO",Long.class));
  assertEquals(audit,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AUD.EVENTO",Long.class));
 }
}
