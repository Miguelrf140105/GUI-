package pe.edu.gui.interfaces;

import java.util.List;
import java.util.Map;

/** Contrato de persistencia para listados paginados, catálogos de selección y dashboard. */
public interface IConsultaDAO {

    /**
     * Obtiene una página del módulo indicado, limitada a los datos que puede consultar el usuario.
     */
    List<Map<String, Object>> listar(int actor, int module, int offset, int take);

    /** Consulta uno de los catálogos de selección autorizados para el usuario. */
    List<Map<String, Object>> lookup(int actor, int type);

    /** Consulta los resúmenes visibles para el usuario. Los cálculos se realizan en SQL Server. */
    List<Map<String, Object>> dashboard(int actor, int view);
}
