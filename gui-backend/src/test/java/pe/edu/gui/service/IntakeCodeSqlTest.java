package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.util.*;

/** Comprobaciones de lectura; no modifica ningún registro. */
@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@SpringJUnitConfig(MovementBatchSqlTest.Config.class)
class IntakeCodeSqlTest {
 @Autowired JdbcTemplate jdbc;
 @Test void codigoLegibleSinTruncarNiCambiarElId(){
  assertEquals("ING0001",jdbc.queryForObject("SELECT INV.fnCodigoIngreso(1)",String.class));
  assertEquals("ING0002",jdbc.queryForObject("SELECT INV.fnCodigoIngreso(2)",String.class));
  assertEquals("ING9999",jdbc.queryForObject("SELECT INV.fnCodigoIngreso(9999)",String.class));
  assertEquals("ING10000",jdbc.queryForObject("SELECT INV.fnCodigoIngreso(10000)",String.class));
  for(var row:jdbc.queryForList("EXEC INV.sp_Listar 1,140,0,100")){
   int id=((Number)row.get("id")).intValue();String expected=jdbc.queryForObject("SELECT CODIGO_INGRESO FROM INV.OPERACION WHERE ID_OPERACION=?",String.class,id);
   assertEquals(expected,row.get("code"));
   assertEquals(id,jdbc.queryForObject("SELECT ID_OPERACION FROM INV.OPERACION WHERE ID_OPERACION=?",Integer.class,id));
  }
 }
 @Test void detalleMuestraCadaUnidadConSuCodigoOriginal(){
  for(int id:jdbc.queryForList("SELECT ID_OPERACION FROM INV.OPERACION",Integer.class)){
   var units=jdbc.queryForList("EXEC INV.sp_IngresoDetalle 1,?,3",id);
   int expected=jdbc.queryForObject("SELECT COUNT(*) FROM INV.ACTIVO a JOIN INV.OPERACION_DETALLE d ON d.ID_DETALLE=a.ID_DETALLE_INGRESO WHERE d.ID_OPERACION=? AND SEC.fnPuedeActivo(1,a.ID_ACTIVO)=1",Integer.class,id);
   assertEquals(expected,units.size());var codes=new HashSet<>();
   for(var unit:units){assertTrue(codes.add(unit.get("code")));assertEquals(unit.get("code"),jdbc.queryForObject("SELECT CODIGO_PATRIMONIAL FROM INV.ACTIVO WHERE ID_ACTIVO=?",String.class,unit.get("id")));}
  }
 }
 @Test void reportesInventarioEIngresosCoincidenEnLosDatosActuales(){
  int assets=jdbc.queryForObject("SELECT COUNT(*) FROM INV.ACTIVO WHERE SEC.fnPuedeActivo(1,ID_ACTIVO)=1",Integer.class);
  int reports=jdbc.queryForList("EXEC INV.sp_Listar 1,80,0,100").stream().mapToInt(row->((Number)row.get("quantity")).intValue()).sum();
  var stats=jdbc.queryForMap("EXEC INV.sp_Listar 1,10,0,51");assertEquals(assets,((Number)stats.get("assets")).intValue());assertEquals(assets,reports);
  assertEquals(assets,jdbc.queryForList("EXEC INV.sp_Dashboard 1,2").stream().mapToInt(row->((Number)row.get("count")).intValue()).sum());
  assertEquals(assets,jdbc.queryForObject("SELECT SUM(d.CANTIDAD) FROM INV.OPERACION_DETALLE d JOIN INV.OPERACION o ON o.ID_OPERACION=d.ID_OPERACION",Integer.class));
  assertEquals(jdbc.queryForObject("SELECT COUNT(*) FROM INV.ACTIVO WHERE ESTADO_REGISTRO=11 AND SEC.fnPuedeActivo(1,ID_ACTIVO)=1",Integer.class),((Number)stats.get("maintenance")).intValue());
  assertEquals(jdbc.queryForObject("SELECT COUNT(*) FROM INV.MOVIMIENTO WHERE ESTADO_REGISTRO=201 AND SEC.fnPuedeActivo(1,ID_ACTIVO)=1",Integer.class),((Number)stats.get("pending")).intValue());
 }
}
