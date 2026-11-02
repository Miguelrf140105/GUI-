package pe.edu.gui.dao;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import pe.edu.gui.interfaces.ISqlExecutor;
import java.util.UUID;

class DocumentoDAOTest {
    @Test
    void enviaLosNueveParametrosDelProcedimientoDeAdjuntos() {
        var sql = mock(ISqlExecutor.class);
        var dao = new DocumentoDAO(sql);
        var id = UUID.randomUUID();
        byte[] content = {1, 2, 3};
        dao.guardar(1, id, 2, null, "a.png", "image/png", "hash", content);
        verify(sql).execute("EXEC DOC.sp_AdjuntoGuardar ?,?,?,?,?,?,?,?,?",
                1, id.toString(), 2, null, "a.png", "image/png", 3, "hash", content);
    }
}
