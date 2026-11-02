package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.IMantenimientoDAO;
import pe.edu.gui.interfaces.ISqlExecutor;

/**
 * Acceso a los procedimientos almacenados de programación de mantenimiento de activos. No contiene
 * estado de pantalla.
 */
@Repository
public class MantenimientoDAO extends BaseDAO implements IMantenimientoDAO {

    public MantenimientoDAO(ISqlExecutor sql) {
        super(sql);
    }

    /**
     * Programa la atención del activo; esta operación todavía no representa el cierre del
     * mantenimiento.
     */
    @Override
    public ResultadoRegistro programar(int actor, Requests.Maintenance dto) {
        return guardar(actor, 602, dto);
    }
}
