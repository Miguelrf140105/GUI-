package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.IRolDAO;
import pe.edu.gui.interfaces.ISqlExecutor;

import java.util.List;
import java.util.Map;

/**
 * Acceso a los procedimientos almacenados de roles y asignación de permisos. No contiene estado de
 * pantalla.
 */
@Repository
public class RolDAO extends BaseDAO implements IRolDAO {

    public RolDAO(ISqlExecutor sql) {
        super(sql);
    }

    /**
     * Guarda los permisos del rol editable. El rol de recuperación se conserva para no perder el
     * acceso administrativo.
     */
    @Override
    public ResultadoRegistro guardar(int actor, Requests.Role dto) {
        return guardar(actor, 1002, dto);
    }

    /** Consulta los permisos asignados al rol solicitado. */
    @Override
    public List<Map<String, Object>> permisos(int actor, int id) {
        return sql.query("EXEC SEC.sp_RolPermisos ?,?", actor, id);
    }
}
