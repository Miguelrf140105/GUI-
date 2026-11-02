package pe.edu.gui.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import pe.edu.gui.dto.RegistroInventario;
import pe.edu.gui.dto.Requests;
import pe.edu.gui.interfaces.IInventarioDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class InventarioServiceTest {
    private RegistroInventario.Linea linea(
            int quantity, List<String> serials, List<Requests.Specification> specs) {
        return new RegistroInventario.Linea(
                1,
                null,
                "Laptop Lenovo",
                "Lenovo",
                "E14",
                quantity,
                BigDecimal.ZERO,
                serials,
                specs);
    }

    private RegistroInventario request(
            Integer provider, Requests.Institution newProvider, RegistroInventario.Linea line) {
        return new RegistroInventario(
                UUID.randomUUID(),
                1,
                provider,
                newProvider,
                LocalDate.of(2026, 10, 22),
                null,
                2,
                1,
                List.of(line),
                List.of(),
                List.of());
    }

    @Test
    void completeRequestReachesDaoUnchangedWithSessionActor() {
        var dao = mock(IInventarioDAO.class);
        var dto = request(4, null, linea(24, List.of(), List.of()));
        var rows = List.<Map<String, Object>>of(Map.of("id", 1, "code", "LAP-000001"));
        when(dao.registrar(7, dto)).thenReturn(rows);
        assertSame(rows, new InventarioService(dao).registrar(7, dto));
        verify(dao).registrar(7, dto);
    }

    @Test
    void rejectsMissingProviderBeforeOpeningTransactionInDao() {
        var dao = mock(IInventarioDAO.class);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InventarioService(dao)
                                .registrar(7, request(null, null, linea(1, List.of(), List.of()))));
        verifyNoInteractions(dao);
    }

    @Test
    void rejectsIncompleteSerialList() {
        var dao = mock(IInventarioDAO.class);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InventarioService(dao)
                                .registrar(
                                        7,
                                        request(4, null, linea(24, List.of("SN-1"), List.of()))));
        verifyNoInteractions(dao);
    }

    @Test
    void rejectsDuplicateSeriesIgnoringCaseAndSpaces() {
        var dao = mock(IInventarioDAO.class);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InventarioService(dao)
                                .registrar(
                                        7,
                                        request(
                                                4,
                                                null,
                                                linea(2, List.of("SN-1", " sn-1 "), List.of()))));
        verifyNoInteractions(dao);
    }

    @Test
    void rejectsDuplicateEavDefinition() {
        var dao = mock(IInventarioDAO.class);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InventarioService(dao)
                                .registrar(
                                        7,
                                        request(
                                                4,
                                                null,
                                                linea(
                                                        1,
                                                        List.of(),
                                                        List.of(
                                                                new Requests.Specification(2, "16"),
                                                                new Requests.Specification(
                                                                        2, "32"))))));
        verifyNoInteractions(dao);
    }

    @Test
    void rejectsOversizedReceipt() {
        var dao = mock(IInventarioDAO.class);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new InventarioService(dao)
                                .registrar(7, request(4, null, linea(1001, List.of(), List.of()))));
        verifyNoInteractions(dao);
    }
}
