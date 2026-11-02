package pe.edu.gui.interfaces;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;

import java.util.List;
import java.util.Map;

/** Contrato de persistencia para proveedores, instituciones, comprobantes e ingresos. */
public interface IAdquisicionDAO {

    /**
     * Registra la contraparte que podrá actuar como proveedor, donante o institución destinataria.
     */
    ResultadoRegistro guardarInstitucion(int actor, Requests.Institution dto);

    ResultadoRegistro eliminarInstitucion(int actor, int id);

    ResultadoRegistro anularIngreso(int actor, int id);

    /**
     * Registra la cabecera del comprobante. Su vínculo con los productos se establece en el
     * ingreso.
     */
    ResultadoRegistro guardarDocumento(int actor, Requests.Document dto);

    /** Líneas de productos y documentos de respaldo de un ingreso autorizado. */
    Map<String, List<Map<String, Object>>> detalleIngreso(int actor, int id);

    /** Consulta exacta por identificación, sin descargar el directorio completo. */
    List<Map<String, Object>> buscarInstitucion(int actor, String document);
}
