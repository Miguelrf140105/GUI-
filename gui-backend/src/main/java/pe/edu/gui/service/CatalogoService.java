package pe.edu.gui.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.ICatalogoDAO;

/**
 * Casos de uso para parámetros que alimentan los catálogos del sistema. Coordina validaciones y
 * acceso a persistencia.
 */
@Service
public class CatalogoService {

    private final ICatalogoDAO dao;

    public CatalogoService(ICatalogoDAO dao) {
        this.dao = dao;
    }

    /** Registra un valor de catálogo dentro de su grupo, identificado por ID. */
    @Transactional
    public ResultadoRegistro guardar(int actor, Requests.Parameter dto) {
        return dao.guardar(actor, dto);
    }

    /** El catálogo de tipos se administra independientemente de las recepciones. */
    @Transactional
    public ResultadoRegistro guardarFamilia(int actor, Requests.Family dto) {
        return dao.guardarFamilia(actor, dto);
    }
}
