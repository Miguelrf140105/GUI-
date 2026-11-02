package pe.edu.gui.interfaces;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;

/** Contrato de persistencia para parámetros que alimentan los catálogos del sistema. */
public interface ICatalogoDAO {

    /** Registra un valor de catálogo dentro de su grupo, identificado por ID. */
    ResultadoRegistro guardar(int actor, Requests.Parameter dto);

    ResultadoRegistro guardarFamilia(int actor, Requests.Family dto);
}
