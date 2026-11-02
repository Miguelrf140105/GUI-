package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.entity.ArchivoAdjunto;
import pe.edu.gui.interfaces.IDocumentoDAO;
import pe.edu.gui.interfaces.ISqlExecutor;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Acceso a los procedimientos almacenados de archivos adjuntos y sus relaciones con documentos o
 * activos. No contiene estado de pantalla.
 */
@Repository
public class DocumentoDAO implements IDocumentoDAO {

    private final ISqlExecutor sql;

    public DocumentoDAO(ISqlExecutor sql) {
        this.sql = sql;
    }

    /** Lista adjuntos de un documento o de un activo, según el padre indicado. */
    @Override
    public List<Map<String, Object>> listar(int actor, Integer document, Integer asset) {
        return sql.query("EXEC DOC.sp_Adjuntos ?,?,?", actor, document, asset);
    }

    /**
     * Guarda contenido en GUI_BF y metadatos en GUI_BP a través del SP y la transacción de la
     * conexión actual.
     */
    @Override
    public void guardar(
            int actor,
            UUID id,
            Integer document,
            Integer asset,
            String name,
            String mime,
            String hash,
            byte[] bytes) {
        sql.execute(
                "EXEC DOC.sp_AdjuntoGuardar ?,?,?,?,?,?,?,?,?",
                actor,
                id.toString(),
                document,
                asset,
                name,
                mime,
                bytes.length,
                hash,
                bytes);
    }

    /** Lee un adjunto aplicando permisos y alcance; su UUID no concede acceso por sí solo. */
    @Override
    public ArchivoAdjunto descargar(int actor, UUID id) {
        var rows = sql.query("EXEC DOC.sp_AdjuntoLeer ?,?", actor, id.toString());
        if (rows.isEmpty()) {
            throw new NoSuchElementException("Adjunto inexistente");
        }
        var row = rows.get(0);
        return new ArchivoAdjunto(
                (String) row.get("name"), (String) row.get("mime"), (byte[]) row.get("content"));
    }
}
