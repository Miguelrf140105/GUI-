package pe.edu.gui.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.IUbicacionDAO;

/**
 * Casos de uso para sedes y ambientes organizados en un árbol de ubicaciones. Coordina validaciones
 * y acceso a persistencia.
 */
@Service
public class UbicacionService {

    @Transactional
    public ResultadoRegistro eliminar(int actor, int id) {
        if (id <= 0) throw new IllegalArgumentException("Ubicación inválida");
        return dao.eliminar(actor, id);
    }

    private final IUbicacionDAO dao;

    public UbicacionService(IUbicacionDAO dao) {
        this.dao = dao;
    }

    /**
     * Crea o modifica una ubicación del árbol. SQL valida ciclos y serializa la reestructuración.
     */
    @Transactional
    public ResultadoRegistro guardar(int actor, Requests.Location dto) {
        if (dto.id() != null && dto.id().equals(dto.parentId())) {
            throw new IllegalArgumentException("Una ubicación no puede ser su propio padre");
        }
        return dao.guardar(actor, dto);
    }
}
