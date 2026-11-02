package pe.edu.gui.interfaces;

import java.util.List;
import java.util.Map;

/** Puerto técnico de SQL: los nombres de SP son internos, nunca recibidos desde HTTP. */
public interface ISqlExecutor {

    /**
     * Ejecuta SQL parametrizado que devuelve filas. Los valores nunca se concatenan al texto de la
     * consulta.
     */
    List<Map<String, Object>> query(String sql, Object... parameters);

    /** Ejecuta SQL parametrizado sin esperar un conjunto de resultados. */
    void execute(String sql, Object... parameters);
}
