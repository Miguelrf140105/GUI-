package pe.edu.gui.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.IRolDAO;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso para roles y asignación de permisos. Coordina validaciones y acceso a persistencia.
 */
@Service
public class RolService {

    private final IRolDAO dao;

    public RolService(IRolDAO dao) {
        this.dao = dao;
    }

    /**
     * Guarda los permisos del rol editable. El rol de recuperación se conserva para no perder el
     * acceso administrativo.
     */
    @Transactional
    public ResultadoRegistro guardar(int actor, Requests.Role dto) {
        if (Integer.valueOf(1).equals(dto.id())) {
            throw new IllegalArgumentException("El rol de recuperación no se modifica");
        }
        if (new HashSet<>(dto.permissionIds()).size() != dto.permissionIds().size()) {
            throw new IllegalArgumentException("No repita permisos");
        }
        return dao.guardar(actor, dto);
    }

    /** Consulta los permisos asignados al rol solicitado. */
    public List<Map<String, Object>> permisos(int actor, int id) {
        return dao.permisos(actor, id);
    }
}
