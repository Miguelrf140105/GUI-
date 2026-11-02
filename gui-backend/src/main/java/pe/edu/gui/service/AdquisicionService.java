package pe.edu.gui.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.IAdquisicionDAO;

import java.util.List;
import java.util.Map;

/**
 * Casos de uso para proveedores, instituciones, comprobantes e ingresos. Coordina validaciones y
 * acceso a persistencia.
 */
@Service
public class AdquisicionService {

    /**
     * Solicita la baja lógica del ingreso y sus unidades en una sola transacción.
     * El SP verifica el perfil administrador, el alcance y cualquier actividad posterior;
     * conserva códigos, adjuntos e historia y rechaza el lote completo si una unidad está bloqueada.
     */
    @Transactional
    public ResultadoRegistro anularIngreso(int actor, int id) {
        if (id <= 0) throw new IllegalArgumentException("Ingreso inválido");
        return dao.anularIngreso(actor, id);
    }

    /**
     * Cambia el estado del proveedor sin destruir sus vínculos históricos.
     * La autorización definitiva se aplica también en SQL, no depende del botón de la pantalla.
     */
    @Transactional
    public ResultadoRegistro eliminarInstitucion(int actor, int id) {
        if (id <= 0) throw new IllegalArgumentException("Proveedor inválido");
        return dao.eliminarInstitucion(actor, id);
    }

    private final IAdquisicionDAO dao;

    public AdquisicionService(IAdquisicionDAO dao) {
        this.dao = dao;
    }

    /**
     * Registra la contraparte que podrá actuar como proveedor, donante o institución destinataria.
     */
    @Transactional
    public ResultadoRegistro guardarInstitucion(int actor, Requests.Institution dto) {
        if (dto.stateId() != null && dto.stateId() != 0 && dto.stateId() != 1) {
            throw new IllegalArgumentException("Estado de proveedor inválido");
        }
        return dao.guardarInstitucion(actor, dto);
    }

    /**
     * Registra la cabecera del comprobante. Su vínculo con los productos se establece en el
     * ingreso.
     */
    @Transactional
    public ResultadoRegistro guardarDocumento(int actor, Requests.Document dto) {
        return dao.guardarDocumento(actor, dto);
    }

    /** Devuelve las líneas y sus documentos; la base comprueba acceso antes de leerlos. */
    public Map<String, List<Map<String, Object>>> detalleIngreso(int actor, int id) {
        if (id < 1) {
            throw new IllegalArgumentException("ID de ingreso inválido");
        }
        return dao.detalleIngreso(actor, id);
    }

    /** El RUC es un identificador externo; las asociaciones posteriores siguen usando el ID. */
    @Transactional(readOnly = true)
    public Map<String, Object> buscarInstitucion(int actor, String document) {
        if (document == null || document.isBlank() || document.strip().length() > 30) {
            throw new IllegalArgumentException("RUC / identificación inválido");
        }
        var rows = dao.buscarInstitucion(actor, document.strip());
        return rows.isEmpty()
                ? Map.of("found", false)
                : Map.of("found", true, "institution", rows.get(0));
    }
}
