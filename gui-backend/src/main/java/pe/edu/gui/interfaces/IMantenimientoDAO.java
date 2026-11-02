package pe.edu.gui.interfaces;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;

/** Contrato de persistencia para programación de mantenimiento de activos. */
public interface IMantenimientoDAO {

    /**
     * Programa la atención del activo; esta operación todavía no representa el cierre del
     * mantenimiento.
     */
    ResultadoRegistro programar(int actor, Requests.Maintenance dto);
}
