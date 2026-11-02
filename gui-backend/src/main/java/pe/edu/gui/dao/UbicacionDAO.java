package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.ISqlExecutor;
import pe.edu.gui.interfaces.IUbicacionDAO;

/**
 * Acceso a los procedimientos almacenados de sedes y ambientes organizados en un árbol de
 * ubicaciones. No contiene estado de pantalla.
 */
@Repository
public class UbicacionDAO extends BaseDAO implements IUbicacionDAO {

    public UbicacionDAO(ISqlExecutor sql) {
        super(sql);
    }

    /**
     * Crea o modifica una ubicación del árbol. SQL valida ciclos y serializa la reestructuración.
     */
    @Override
    public ResultadoRegistro eliminar(int actor, int id) {
        return guardar(actor, 503, new Requests.LocationDelete(id));
    }

    @Override
    public ResultadoRegistro guardar(int actor, Requests.Location dto) {
        return guardar(actor, 502, dto);
    }
}
