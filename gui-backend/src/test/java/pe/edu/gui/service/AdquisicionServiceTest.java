package pe.edu.gui.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import pe.edu.gui.interfaces.IAdquisicionDAO;

import java.util.List;
import java.util.Map;

class AdquisicionServiceTest {
    @Test
    void exactDocumentReturnsExistingIdWithoutWriting() {
        var dao = mock(IAdquisicionDAO.class);
        when(dao.buscarInstitucion(7, "20612118591")).thenReturn(List.of(Map.of("id", 12)));
        assertEquals(
                Map.of("found", true, "institution", Map.of("id", 12)),
                new AdquisicionService(dao).buscarInstitucion(7, " 20612118591 "));
        verify(dao).buscarInstitucion(7, "20612118591");
        verifyNoMoreInteractions(dao);
    }

    @Test
    void missingDocumentIsNotAProvider() {
        var dao = mock(IAdquisicionDAO.class);
        when(dao.buscarInstitucion(7, "SIN-REGISTRO")).thenReturn(List.of());
        assertEquals(
                Map.of("found", false),
                new AdquisicionService(dao).buscarInstitucion(7, "SIN-REGISTRO"));
    }

    @Test
    void invalidDocumentDoesNotReachSql() {
        var dao = mock(IAdquisicionDAO.class);
        var service = new AdquisicionService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.buscarInstitucion(7, " "));
        assertThrows(
                IllegalArgumentException.class, () -> service.buscarInstitucion(7, "x".repeat(31)));
        verifyNoInteractions(dao);
    }
}
