package pe.edu.gui.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import pe.edu.gui.dto.*;
import pe.edu.gui.entity.*;
import pe.edu.gui.interfaces.IMovimientoDAO;
import java.util.*;

class MovementBatchServiceTest {
    MovementBatch batch(List<Integer> ids,UUID key) { return new MovementBatch(ids,2,9,null,"Traslado",key); }
    @Test void rechazaDuplicadosAntesDeEscribir() {
        var dao=mock(IMovimientoDAO.class); var service=new MovimientoService(dao);
        assertThrows(IllegalArgumentException.class,()->service.solicitarLote(7,batch(List.of(1,1),UUID.randomUUID())));
        verifyNoInteractions(dao);
    }
    @Test void validaTodasLasUnidadesAntesDeGuardarCualquiera() {
        var dao=mock(IMovimientoDAO.class); var service=new MovimientoService(dao);
        when(dao.activo(7,1)).thenReturn(new Activo(1,10,3));
        when(dao.activo(7,2)).thenReturn(new Activo(2,99,3));
        when(dao.tipo(7,2)).thenReturn(new TipoMovimiento(2,2,false));
        assertThrows(IllegalArgumentException.class,()->service.solicitarLote(7,batch(List.of(2,1),UUID.randomUUID())));
        verify(dao,never()).solicitar(anyInt(),any());
    }
    @Test void generaIdentidadesDistintasYEstablesPorUnidad() {
        var dao=mock(IMovimientoDAO.class); var service=new MovimientoService(dao);
        when(dao.activo(eq(7),anyInt())).thenAnswer(i->new Activo(i.getArgument(1),10,3));
        when(dao.tipo(7,2)).thenReturn(new TipoMovimiento(2,2,false));
        var requests=new ArrayList<Requests.Movement>();
        when(dao.solicitar(eq(7),any())).thenAnswer(i->{Requests.Movement dto=i.getArgument(1);requests.add(dto);return new ResultadoRegistro((long)dto.assetId()+100);});
        var key=UUID.randomUUID(); var first=service.solicitarLote(7,batch(List.of(2,1),key));
        service.solicitarLote(7,batch(List.of(1,2),key));
        assertEquals(List.of(1,2),first.stream().map(MovimientoService.UnitMovement::assetId).toList());
        assertNotEquals(requests.get(0).idempotencyKey(),requests.get(1).idempotencyKey());
        assertEquals(requests.get(0).idempotencyKey(),requests.get(2).idempotencyKey());
        var order=inOrder(dao); order.verify(dao).reservarLote(eq(7),eq(key),anyString());
        order.verify(dao).activo(7,1); order.verify(dao).activo(7,2); 
    }
}

