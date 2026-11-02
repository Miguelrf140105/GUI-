package pe.edu.gui.interfaces;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;

import java.util.List;
import java.util.Map;

/** Contrato de persistencia para roles y asignación de permisos. */
public interface IRolDAO {

    /**
     * Guarda los permisos del rol editable. El rol de recuperación se conserva para no perder el
     * acceso administrativo.
     */
    ResultadoRegistro guardar(int actor, Requests.Role dto);

    /** Consulta los permisos asignados al rol solicitado. */
    List<Map<String, Object>> permisos(int actor, int id);
}
