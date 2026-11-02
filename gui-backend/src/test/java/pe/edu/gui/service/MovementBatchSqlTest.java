package pe.edu.gui.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.gui.conexion.Conexion;
import pe.edu.gui.dao.MovimientoDAO;
import pe.edu.gui.dto.*;
import pe.edu.gui.interfaces.*;
import java.util.*;
import javax.sql.DataSource;

/** Integración opcional sobre SQL local. Todos los movimientos de prueba se revierten. */
@EnabledIfSystemProperty(named="gui.sql.integration",matches="true")
@SpringJUnitConfig(MovementBatchSqlTest.Config.class)
class MovementBatchSqlTest {
    @Configuration @EnableTransactionManagement static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:sqlserver://localhost:1444;databaseName=GUI_BP;encrypt=true;trustServerCertificate=true;integratedSecurity=true;ipAddressPreference=IPv4First;sslProtocol=TLSv1"); }
        @Bean JdbcTemplate jdbc(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean ISqlExecutor sql(JdbcTemplate jdbc) { return new Conexion(jdbc); }
        @Bean IMovimientoDAO dao(ISqlExecutor sql) { return spy(new MovimientoDAO(sql)); }
        @Bean MovimientoService service(IMovimientoDAO dao) { return new MovimientoService(dao); }
        @Bean DataSourceTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
    }
    @Autowired JdbcTemplate jdbc; @Autowired MovimientoService service; @Autowired IMovimientoDAO dao;
    @Autowired DataSourceTransactionManager manager;
    @BeforeEach void resetDao() { reset(dao); }
    List<Integer> ids() { return jdbc.queryForList("SELECT TOP(2) ID_ACTIVO FROM INV.ACTIVO a WHERE ESTADO_REGISTRO IN(10,11) AND NOT EXISTS(SELECT 1 FROM INV.MOVIMIENTO m WHERE m.ID_ACTIVO=a.ID_ACTIVO AND m.ESTADO_REGISTRO IN(201,202,204)) ORDER BY ID_ACTIVO",Integer.class); }
    MovementBatch request(UUID key) {
        var ids=ids(); assertEquals(2,ids.size());
        int type=jdbc.queryForObject("SELECT MIN(ID_TIPO_MOVIMIENTO) FROM INV.TIPO_MOVIMIENTO WHERE MODO=2 AND REQUIERE_TERCERO=0",Integer.class);
        int dest=jdbc.queryForObject("SELECT MIN(ID_UBICACION) FROM INV.UBICACION WHERE ESTADO_REGISTRO=1 AND ID_UBICACION NOT IN (SELECT ISNULL(ID_UBICACION_ACTUAL,0) FROM INV.ACTIVO WHERE ID_ACTIVO IN (?,?))",Integer.class,ids.get(0),ids.get(1));
        return new MovementBatch(ids,type,dest,null,"Prueba transaccional revertida",key);
    }
    long count(String table) { return jdbc.queryForObject("SELECT COUNT_BIG(*) FROM "+table,Long.class); }
    @Test void loteIndividualEIdempotenteConKardexPropio() {
        var dto=request(UUID.randomUUID()); long movements=count("INV.MOVIMIENTO"), history=count("INV.KARDEX_MOVIMIENTO"), manifests=count("INV.LOTE_MOVIMIENTO");
        new TransactionTemplate(manager).execute(status->{
            var first=service.solicitarLote(1,dto); var again=service.solicitarLote(1,dto);
            assertEquals(first,again); assertEquals(2,first.size());
            assertNotEquals(first.get(0).movementId(),first.get(1).movementId());
            assertEquals(movements+2,count("INV.MOVIMIENTO")); assertEquals(history+2,count("INV.KARDEX_MOVIMIENTO"));
            for(var unit:first) assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM INV.KARDEX_MOVIMIENTO WHERE ID_ACTIVO=? AND ID_MOVIMIENTO=?",Integer.class,unit.assetId(),unit.movementId()));
            status.setRollbackOnly(); return null;
        });
        assertEquals(movements,count("INV.MOVIMIENTO")); assertEquals(history,count("INV.KARDEX_MOVIMIENTO")); assertEquals(manifests,count("INV.LOTE_MOVIMIENTO"));
    }
    @Test void falloEnSegundaEscrituraReviertePrimeraYAuditoria() {
        var dto=request(UUID.randomUUID()); int second=dto.assetIds().get(1);
        long movements=count("INV.MOVIMIENTO"),history=count("INV.KARDEX_MOVIMIENTO"),audit=count("AUD.EVENTO"),manifest=count("INV.LOTE_MOVIMIENTO");
        doThrow(new IllegalStateException("Fallo simulado después de guardar la primera unidad"))
            .when(dao).solicitar(eq(1),argThat(unit->unit.assetId()==second));
        assertThrows(IllegalStateException.class,()->service.solicitarLote(1,dto));
        assertEquals(movements,count("INV.MOVIMIENTO")); assertEquals(history,count("INV.KARDEX_MOVIMIENTO"));
        assertEquals(audit,count("AUD.EVENTO")); assertEquals(manifest,count("INV.LOTE_MOVIMIENTO"));
    }
    @Test void mismaClaveNoAdmiteCambiarLosDatosDelLote() {
        var dto=request(UUID.randomUUID()); long movements=count("INV.MOVIMIENTO"),history=count("INV.KARDEX_MOVIMIENTO");
        new TransactionTemplate(manager).execute(status->{
            service.solicitarLote(1,dto);
            var changed=new MovementBatch(dto.assetIds(),dto.typeId(),dto.locationId(),dto.institutionId(),"Motivo distinto",dto.idempotencyKey());
            assertThrows(org.springframework.dao.DataAccessException.class,()->service.solicitarLote(1,changed));
            status.setRollbackOnly(); return null;
        });
        assertEquals(movements,count("INV.MOVIMIENTO")); assertEquals(history,count("INV.KARDEX_MOVIMIENTO"));
    }}

