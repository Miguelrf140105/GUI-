package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.dto.RegistroInventario;
import pe.edu.gui.interfaces.IInventarioDAO;
import pe.edu.gui.interfaces.ISqlExecutor;
import pe.edu.gui.utils.XmlPayload;

import java.util.List;
import java.util.Map;

/** Ejecuta el SP con parámetros; ningún dato recibido se concatena como SQL. */
@Repository
public class InventarioDAO implements IInventarioDAO {
    private final ISqlExecutor sql;

    public InventarioDAO(ISqlExecutor sql) {
        this.sql = sql;
    }

    @Override
    public List<Map<String, Object>> registrar(int actor, RegistroInventario dto) {
        return sql.query("EXEC INV.sp_InventarioRegistrar ?,?", actor, XmlPayload.of(dto));
    }
}
