package pe.edu.gui.interfaces;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.entity.Activo;
import pe.edu.gui.entity.EtapaMovimiento;
import pe.edu.gui.entity.MovimientoRegistrado;
import pe.edu.gui.entity.TipoMovimiento;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistencia para solicitudes y etapas de traslado, donación, mantenimiento o baja.
 */
public interface IMovimientoDAO {

    /**
     * Comprueba activo, destino y contraparte. Un reintento con la misma clave y datos recupera la
     * solicitud existente.
     */
    ResultadoRegistro solicitar(int actor, Requests.Movement dto);

    /**
     * Valida la etapa actual y solicita la transición. SQL vuelve a comprobarla bajo bloqueo ante
     * peticiones concurrentes.
     */
    ResultadoRegistro avanzar(int actor, EtapaMovimiento stage, Requests.Transition dto);

    /** Obtiene el estado y la ubicación del activo dentro del alcance del usuario. */
    Activo activo(int actor, int id);

    /** Obtiene las reglas del tipo seleccionado usando su ID numérico. */
    TipoMovimiento tipo(int actor, int id);

    /** Consulta el estado del movimiento antes de solicitar una transición. */
    int estado(int actor, long id);

    /** Busca una solicitud previa por su clave de idempotencia dentro del acceso autorizado. */
    Optional<MovimientoRegistrado> porClave(int actor, UUID key);
    void vincularLote(int actor, UUID key, long movementId);
    void reservarLote(int actor, UUID key, String fingerprint);
}
