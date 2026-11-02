package pe.edu.gui.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import pe.edu.gui.interfaces.IUsuarioDAO;
import pe.edu.gui.service.SeguridadService;

import java.util.List;
import java.util.Map;

class AccessTest {

    @Test
    void permissionRevocationIsEffectiveWithoutNewLogin() {
        var repo = mock(IUsuarioDAO.class);
        var access = new Access(new SeguridadService(repo));
        var auth =
                UsernamePasswordAuthenticationToken.authenticated(
                        new GuiPrincipal(7, "tester", "hash"), null, List.of());
        when(repo.identidad(7))
                .thenReturn(List.of(Map.of("permissionId", 403)))
                .thenReturn(List.of(Map.of("permissionId", 301)));
        assertTrue(access.has(auth, 403));
        assertFalse(access.has(auth, 403));
    }

    @Test
    void anonymousRequestCannotProvideAnActorId() {
        assertThrows(
                AccessDeniedException.class,
                () -> new Access(new SeguridadService(mock(IUsuarioDAO.class))).user(null));
    }
}
