package pe.edu.gui.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.IMantenimientoDAO;

/**
 * Casos de uso para programación de mantenimiento de activos. Coordina validaciones y acceso a
 * persistencia.
 */
@Service
public class MantenimientoService {

    private final IMantenimientoDAO dao;

    public MantenimientoService(IMantenimientoDAO dao) {
        this.dao = dao;
    }

    /**
     * Programa la atención del activo; esta operación todavía no representa el cierre del
     * mantenimiento.
     */
    @Transactional
    public ResultadoRegistro programar(int actor, Requests.Maintenance dto) {
        return dao.programar(actor, dto);
    }
}
