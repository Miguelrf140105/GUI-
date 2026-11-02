package pe.edu.gui.dao;

import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.ISqlExecutor;
import pe.edu.gui.utils.XmlPayload;

import java.util.Map;

/** Serialización del contrato SQL y parámetros JDBC. No decide reglas de negocio. */
abstract class BaseDAO {

    protected final ISqlExecutor sql;

    protected BaseDAO(ISqlExecutor sql) {
        this.sql = sql;
    }

    /**
     * Envía usuario, acción y DTO serializado como XML al SP compartido. XML mantiene la
     * compatibilidad con SQL Server 2014.
     */
    protected ResultadoRegistro guardar(int actor, int action, Object dto) {
        var rows =
                sql.query(
                        "EXEC INV.sp_Gestionar @usuario=?,@accion=?,@datos=?",
                        actor,
                        action,
                        XmlPayload.of(dto));
        return new ResultadoRegistro(((Number) rows.get(0).get("id")).longValue());
    }

    /**
     * Convierte una columna numérica obligatoria de la proyección SQL al ID esperado por el
     * dominio.
     */
    protected static int number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).intValue();
    }

    /** Conserva null cuando la relación SQL es opcional. */
    protected static Integer optionalNumber(Map<String, Object> row, String key) {
        return row.get(key) == null ? null : number(row, key);
    }
}
