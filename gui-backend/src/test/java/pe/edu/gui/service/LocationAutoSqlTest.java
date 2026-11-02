package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.gui.dao.UbicacionDAO;
import pe.edu.gui.interfaces.*;
import pe.edu.gui.dto.Requests;

@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@SpringJUnitConfig(LocationAutoSqlTest.Config.class)
class LocationAutoSqlTest {
    @Configuration @Import(MovementBatchSqlTest.Config.class) static class Config {
        @Bean IUbicacionDAO ubicacionDAO(ISqlExecutor sql){return new UbicacionDAO(sql);}
        @Bean UbicacionService ubicacionService(IUbicacionDAO dao){return new UbicacionService(dao);}
    }
    @Autowired JdbcTemplate jdbc; @Autowired UbicacionService service; @Autowired DataSourceTransactionManager manager;
    @Test void codigoAutomaticoUnicoInmutableYResponsableHeredado() {
        long initial=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.UBICACION",Long.class);
        new TransactionTemplate(manager).execute(status->{
            int parent=jdbc.queryForObject("SELECT TOP(1) ID_UBICACION FROM INV.UBICACION WHERE ID_UBICACION_PADRE IS NULL AND ESTADO_REGISTRO=1 ORDER BY ID_UBICACION",Integer.class);
            jdbc.update("UPDATE INV.UBICACION SET ID_RESPONSABLE=1 WHERE ID_UBICACION=?",parent);
            int type=jdbc.queryForObject("SELECT ID_PARAMETRO_VALOR FROM SYST.PARAMETRO_VALOR WHERE ID_PARAMETRO_TIPO=2 AND VALOR_CODIGO='AULA'",Integer.class);
            var one=service.guardar(1,new Requests.Location(null,"Prueba de espacio revertida","NO-USAR-ESTE-CODIGO",type,parent,null));
            var two=service.guardar(1,new Requests.Location(null,"Segunda prueba revertida",null,type,parent,null));
            String first=jdbc.queryForObject("SELECT CODIGO FROM INV.UBICACION WHERE ID_UBICACION=?",String.class,one.id());
            String second=jdbc.queryForObject("SELECT CODIGO FROM INV.UBICACION WHERE ID_UBICACION=?",String.class,two.id());
            assertTrue(first.matches("UBI-[0-9]{6,}"));assertNotEquals(first,second);
            assertEquals(1,jdbc.queryForObject("SELECT ID_RESPONSABLE FROM INV.UBICACION WHERE ID_UBICACION=?",Integer.class,one.id()));
            service.guardar(1,new Requests.Location((int)one.id(),"Nombre corregido de prueba","CODIGO-PROHIBIDO",type,parent,1));
            assertEquals(first,jdbc.queryForObject("SELECT CODIGO FROM INV.UBICACION WHERE ID_UBICACION=?",String.class,one.id()));
            status.setRollbackOnly();return null;
        });
        assertEquals(initial,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.UBICACION",Long.class));
    }
    @Test void noCreaEspaciosSinPermiso() {
        long initial=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.UBICACION",Long.class);
        assertThrows(org.springframework.dao.DataAccessException.class,()->service.guardar(0,new Requests.Location(null,"Prueba prohibida",null,201,null,null)));
        assertEquals(initial,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.UBICACION",Long.class));
    }
    @Test void impideDuplicarElNombreDentroDelMismoPadre() {
        long initial=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.UBICACION",Long.class);
        new TransactionTemplate(manager).execute(status->{
            int parent=jdbc.queryForObject("SELECT MIN(ID_UBICACION) FROM INV.UBICACION WHERE ID_UBICACION_PADRE IS NULL",Integer.class);
            var dto=new Requests.Location(null,"Espacio duplicado de prueba",null,206,parent,null);
            service.guardar(1,dto);
            assertThrows(org.springframework.dao.DataAccessException.class,()->service.guardar(1,dto));
            status.setRollbackOnly();return null;
        });
        assertEquals(initial,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM INV.UBICACION",Long.class));
    }
    @org.junit.jupiter.params.ParameterizedTest(name="Guardar nivel {0} con jerarquía y código automático")
    @org.junit.jupiter.params.provider.ValueSource(ints={0,1,2,3})
    void verificaSedePisoAmbienteYSubambienteSinDejarDatos(int target) {
        var before=jdbc.queryForList("SELECT ID_UBICACION,CODIGO,NOMBRE,ID_TIPO_ESPACIO_PARAM,ID_UBICACION_PADRE,ID_RESPONSABLE,ESTADO_REGISTRO FROM INV.UBICACION ORDER BY ID_UBICACION");
        long audit=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AUD.EVENTO",Long.class);
        String tag="Verificacion revertida "+java.util.UUID.randomUUID();
        String[] names={tag,"Piso 1","Ambiente de prueba","Subambiente de prueba"};
        String[] types={"SEDE","PISO","AULA","ALMACEN"};
        String[] labels={"sede","piso","ambiente","subambiente"};
        new TransactionTemplate(manager).execute(status->{
            Integer parent=null; String path="";
            var codes=new java.util.HashSet<String>();
            for(int level=0;level<=target;level++) {
                int type=jdbc.queryForObject("SELECT ID_PARAMETRO_VALOR FROM SYST.PARAMETRO_VALOR WHERE ID_PARAMETRO_TIPO=2 AND VALOR_CODIGO=?",Integer.class,types[level]);
                var result=service.guardar(1,new Requests.Location(null,names[level],null,type,parent,level==0?1:null));
                int id=Math.toIntExact(result.id());
                var stored=jdbc.queryForMap("SELECT CODIGO,NOMBRE,ID_TIPO_ESPACIO_PARAM,ID_UBICACION_PADRE,ID_RESPONSABLE FROM INV.UBICACION WHERE ID_UBICACION=?",id);
                String code=(String)stored.get("CODIGO");
                assertTrue(code.matches("UBI-[0-9]{6,}"),labels[level]);assertTrue(codes.add(code),"Código único para "+labels[level]);
                assertEquals(names[level],stored.get("NOMBRE"));assertEquals(type,stored.get("ID_TIPO_ESPACIO_PARAM"));
                assertEquals(parent,stored.get("ID_UBICACION_PADRE"),"Padre correcto para "+labels[level]);
                assertEquals(1,stored.get("ID_RESPONSABLE"),"Responsable heredado para "+labels[level]);
                path=path.isEmpty()?names[level]:path+" / "+names[level];
                assertEquals(path,jdbc.queryForObject("SELECT INV.fnRutaUbicacion(?)",String.class,id));
                assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM AUD.EVENTO WHERE MODULO=50 AND ACCION=502 AND ID_REGISTRO=?",Integer.class,id));
                parent=id;
            }
            assertEquals(before.size()+target+1,jdbc.queryForObject("SELECT COUNT(*) FROM INV.UBICACION",Integer.class));
            status.setRollbackOnly();return null;
        });
        assertEquals(before,jdbc.queryForList("SELECT ID_UBICACION,CODIGO,NOMBRE,ID_TIPO_ESPACIO_PARAM,ID_UBICACION_PADRE,ID_RESPONSABLE,ESTADO_REGISTRO FROM INV.UBICACION ORDER BY ID_UBICACION"),"No se conservaron ubicaciones de prueba ni cambios en las existentes");
        assertEquals(audit,jdbc.queryForObject("SELECT COUNT_BIG(*) FROM AUD.EVENTO",Long.class),"No se conservaron auditorías de prueba");
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM INV.UBICACION WHERE NOMBRE=?",Integer.class,tag));
        System.out.println("OK: "+labels[target]+" guardado, código automático único, padre y ruta correctos; prueba revertida.");
    }
}


