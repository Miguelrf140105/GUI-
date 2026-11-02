package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.interfaces.IAdquisicionDAO;
import pe.edu.gui.interfaces.ISqlExecutor;

import java.util.List;
import java.util.Map;

/**
 * Acceso a los procedimientos almacenados de proveedores, instituciones, comprobantes e ingresos.
 * No contiene estado de pantalla.
 */
@Repository
public class AdquisicionDAO extends BaseDAO implements IAdquisicionDAO {

    public AdquisicionDAO(ISqlExecutor sql) {
        super(sql);
    }

    /**
     * Registra la contraparte que podrá actuar como proveedor, donante o institución destinataria.
     */
    @Override
    public ResultadoRegistro anularIngreso(int actor, int id) {
        return guardar(actor, 1403, new Requests.IntakeDelete(id));
    }

    @Override
    public ResultadoRegistro eliminarInstitucion(int actor, int id) {
        return guardar(actor, 1303, new Requests.InstitutionDelete(id));
    }

    @Override
    public ResultadoRegistro guardarInstitucion(int actor, Requests.Institution dto) {
        return guardar(actor, 703, dto);
    }

    /**
     * Registra la cabecera del comprobante. Su vínculo con los productos se establece en el
     * ingreso.
     */
    @Override
    public ResultadoRegistro guardarDocumento(int actor, Requests.Document dto) {
        return guardar(actor, 702, dto);
    }

    /** Las dos vistas del SP aplican la misma autorización al ingreso solicitado. */
    @Override
    public Map<String, List<Map<String, Object>>> detalleIngreso(int actor, int id) {
        return Map.of(
                "lines", sql.query("EXEC INV.sp_IngresoDetalle ?,?,1", actor, id),
                "documents", sql.query("EXEC INV.sp_IngresoDetalle ?,?,2", actor, id),
                "units", sql.query("EXEC INV.sp_IngresoDetalle ?,?,3", actor, id));
    }

    /** Parámetros JDBC separados del SQL; el SP comprueba el permiso de consulta. */
    @Override
    public List<Map<String, Object>> buscarInstitucion(int actor, String document) {
        return sql.query("EXEC INV.sp_ProveedorPorDocumento ?,?", actor, document);
    }
}
