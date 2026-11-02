package pe.edu.gui.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.entity.Activo;
import pe.edu.gui.entity.EtapaMovimiento;
import pe.edu.gui.entity.MovimientoRegistrado;
import pe.edu.gui.entity.TipoMovimiento;
import pe.edu.gui.interfaces.IMovimientoDAO;

import java.util.Optional;
import java.util.UUID;

class MovimientoServiceTest {

    private Requests.Movement request(
            int type, Integer destination, Integer institution, UUID key) {
        return new Requests.Movement(7, type, destination, institution, "Traslado académico", key);
    }

    @Test
    void rejectsDonationWithoutRecipientBeforeWriting() {
        var dao = mock(IMovimientoDAO.class);
        var dto = request(4, null, null, UUID.randomUUID());
        when(dao.porClave(1, dto.idempotencyKey())).thenReturn(Optional.empty());
        when(dao.activo(1, 7)).thenReturn(new Activo(7, 10, 2));
        when(dao.tipo(1, 4)).thenReturn(new TipoMovimiento(4, 3, true));
        assertThrows(
                IllegalArgumentException.class, () -> new MovimientoService(dao).solicitar(1, dto));
        verify(dao, never()).solicitar(anyInt(), any());
    }

    @Test
    void rejectsTransferToTheCurrentLocation() {
        var dao = mock(IMovimientoDAO.class);
        var dto = request(2, 2, null, UUID.randomUUID());
        when(dao.porClave(1, dto.idempotencyKey())).thenReturn(Optional.empty());
        when(dao.activo(1, 7)).thenReturn(new Activo(7, 10, 2));
        when(dao.tipo(1, 2)).thenReturn(new TipoMovimiento(2, 2, false));
        assertThrows(
                IllegalArgumentException.class, () -> new MovimientoService(dao).solicitar(1, dto));
        verify(dao, never()).solicitar(anyInt(), any());
    }

    @Test
    void receivedAssetCannotSkipAuthorizationAndDispatch() {
        var dao = mock(IMovimientoDAO.class);
        when(dao.estado(1, 23)).thenReturn(201);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MovimientoService(dao)
                                .avanzar(1, EtapaMovimiento.RECIBIR, new Requests.Transition(23L)));
        verify(dao, never()).avanzar(anyInt(), any(), any());
    }

    @Test
    void retryReturnsPriorResultWithoutAnotherWrite() {
        var dao = mock(IMovimientoDAO.class);
        var dto = request(2, 9, null, UUID.randomUUID());
        when(dao.porClave(1, dto.idempotencyKey()))
                .thenReturn(Optional.of(new MovimientoRegistrado(31, 7, 2, 9, null, dto.reason())));
        assertEquals(31, new MovimientoService(dao).solicitar(1, dto).id());
        verify(dao, never()).solicitar(anyInt(), any());
        verify(dao, never()).activo(anyInt(), anyInt());
    }

    @Test
    void validRequestReachesTheDao() {
        var dao = mock(IMovimientoDAO.class);
        var dto = request(2, 9, null, UUID.randomUUID());
        when(dao.porClave(1, dto.idempotencyKey())).thenReturn(Optional.empty());
        when(dao.activo(1, 7)).thenReturn(new Activo(7, 10, 2));
        when(dao.tipo(1, 2)).thenReturn(new TipoMovimiento(2, 2, false));
        when(dao.solicitar(1, dto)).thenReturn(new ResultadoRegistro(31));
        assertEquals(31, new MovimientoService(dao).solicitar(1, dto).id());
        verify(dao).solicitar(1, dto);
    }
}
