package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.interfaces.IConsultaDAO;
import pe.edu.gui.interfaces.ISqlExecutor;

import java.util.List;
import java.util.Map;

/**
 * Acceso a los procedimientos almacenados de listados paginados, catálogos de selección y
 * dashboard. No contiene estado de pantalla.
 */
@Repository
public class ConsultaDAO implements IConsultaDAO {

    private final ISqlExecutor sql;

    public ConsultaDAO(ISqlExecutor sql) {
        this.sql = sql;
    }

    /**
     * Obtiene una página del módulo indicado, limitada a los datos que puede consultar el usuario.
     */
    @Override
    public List<Map<String, Object>> listar(int actor, int module, int offset, int take) {
        if (module == 150) return sql.query("EXEC INV.sp_Egresos @usuario=?,@vista=1,@offset=?,@take=?", actor, offset, take);
        return sql.query("EXEC INV.sp_Listar ?,?,?,?", actor, module, offset, take);
    }

    /** Consulta uno de los catálogos de selección autorizados para el usuario. */
    @Override
    public List<Map<String, Object>> lookup(int actor, int type) {
        return sql.query("EXEC INV.sp_Lookup ?,?", actor, type);
    }

    /** Consulta los resúmenes visibles para el usuario. Los cálculos se realizan en SQL Server. */
    @Override
    public List<Map<String, Object>> dashboard(int actor, int view) {
        return sql.query("EXEC INV.sp_Dashboard ?,?", actor, view);
    }
}
