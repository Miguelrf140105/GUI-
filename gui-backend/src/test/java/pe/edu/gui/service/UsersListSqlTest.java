package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@SpringJUnitConfig(MovementBatchSqlTest.Config.class)
class UsersListSqlTest {
 @Autowired JdbcTemplate jdbc;
 @Test void listaUsuariosYRolesConXmlEnSqlServer2014(){
  assertEquals(1,jdbc.queryForObject("SELECT CONVERT(INT,uses_quoted_identifier) FROM sys.sql_modules WHERE object_id=OBJECT_ID('INV.sp_Listar')",Integer.class));
  var rows=jdbc.queryForList("EXEC INV.sp_Listar @usuario=1,@modulo=90,@offset=0,@take=51");
  assertFalse(rows.isEmpty());
  var admin=rows.stream().filter(row->Integer.valueOf(1).equals(row.get("id"))).findFirst().orElseThrow();
  assertEquals("Administrador",admin.get("roles"));
  assertFalse(admin.containsKey("passwordHash"));assertFalse(admin.containsKey("PASSWORD_HASH"));
 }
}
