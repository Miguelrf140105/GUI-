package pe.edu.gui.interfaces;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;

/** Contrato de persistencia para sedes y ambientes organizados en un árbol de ubicaciones. */
public interface IUbicacionDAO {

    /**
     * Crea o modifica una ubicación del árbol. SQL valida ciclos y serializa la reestructuración.
     */
    ResultadoRegistro guardar(int actor, Requests.Location dto);

    ResultadoRegistro eliminar(int actor, int id);
}
