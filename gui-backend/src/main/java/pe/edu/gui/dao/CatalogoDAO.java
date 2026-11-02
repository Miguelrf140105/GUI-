package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.ICatalogoDAO;
import pe.edu.gui.interfaces.ISqlExecutor;

/**
 * Acceso a los procedimientos almacenados de parámetros que alimentan los catálogos del sistema. No
 * contiene estado de pantalla.
 */
@Repository
public class CatalogoDAO extends BaseDAO implements ICatalogoDAO {

    public CatalogoDAO(ISqlExecutor sql) {
        super(sql);
    }

    /** Registra un valor de catálogo dentro de su grupo, identificado por ID. */
    @Override
    public ResultadoRegistro guardar(int actor, Requests.Parameter dto) {
        return guardar(actor, 1102, dto);
    }

    @Override
    public ResultadoRegistro guardarFamilia(int actor, Requests.Family dto) {
        return guardar(actor, 1103, dto);
    }
}
