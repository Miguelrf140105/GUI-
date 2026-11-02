package pe.edu.gui.dao;

import org.springframework.stereotype.Repository;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.entity.Activo;
import pe.edu.gui.entity.EtapaMovimiento;
import pe.edu.gui.entity.MovimientoRegistrado;
import pe.edu.gui.entity.TipoMovimiento;
import pe.edu.gui.interfaces.IMovimientoDAO;
import pe.edu.gui.interfaces.ISqlExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a los procedimientos almacenados de solicitudes y etapas de traslado, donación,
 * mantenimiento o baja. No contiene estado de pantalla.
 */
@Repository
public class MovimientoDAO extends BaseDAO implements IMovimientoDAO {

    public MovimientoDAO(ISqlExecutor sql) {
        super(sql);
    }

    /**
     * Comprueba activo, destino y contraparte. Un reintento con la misma clave y datos recupera la
     * solicitud existente.
     */
    @Override
    public ResultadoRegistro solicitar(int actor, Requests.Movement dto) {
        return guardar(actor, 402, dto);
    }

    /**
     * Valida la etapa actual y solicita la transición. SQL vuelve a comprobarla bajo bloqueo ante
     * peticiones concurrentes.
     */
    @Override
    public ResultadoRegistro avanzar(int actor, EtapaMovimiento stage, Requests.Transition dto) {
        int action =
                switch (stage) {
                    case AUTORIZAR -> 403;
                    case DESPACHAR -> 404;
                    case RECIBIR -> 405;
                };
        return guardar(actor, action, dto);
    }

    /** Obtiene el estado y la ubicación del activo dentro del alcance del usuario. */
    @Override
    public Activo activo(int actor, int id) {
        var r =
                sql.query("EXEC INV.sp_ContextoServicio @usuario=?,@vista=2,@id=?", actor, id)
                        .get(0);
        return new Activo(number(r, "id"), number(r, "stateId"), optionalNumber(r, "locationId"));
    }

    /** Obtiene las reglas del tipo seleccionado usando su ID numérico. */
    @Override
    public TipoMovimiento tipo(int actor, int id) {
        var r =
                sql.query("EXEC INV.sp_Lookup ?,7", actor).stream()
                        .filter(x -> number(x, "id") == id)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Tipo de movimiento inexistente"));
        return new TipoMovimiento(
                number(r, "id"),
                number(r, "mode"),
                Boolean.TRUE.equals(r.get("institutionRequired")));
    }

    /** Consulta el estado del movimiento antes de solicitar una transición. */
    @Override
    public int estado(int actor, long id) {
        return number(
                sql.query("EXEC INV.sp_ContextoServicio @usuario=?,@vista=3,@id=?", actor, id)
                        .get(0),
                "stateId");
    }

    /** Busca una solicitud previa por su clave de idempotencia dentro del acceso autorizado. */
    @Override
    public Optional<MovimientoRegistrado> porClave(int actor, UUID key) {
        return sql
                .query(
                        "EXEC INV.sp_ContextoServicio @usuario=?,@vista=4,@clave=?",
                        actor,
                        key.toString())
                .stream()
                .findFirst()
                .map(
                        r ->
                                new MovimientoRegistrado(
                                        ((Number) r.get("id")).longValue(),
                                        number(r, "assetId"),
                                        number(r, "typeId"),
                                        optionalNumber(r, "locationId"),
                                        optionalNumber(r, "institutionId"),
                                        (String) r.get("reason")));
    }
    @Override
    public void vincularLote(int actor, UUID key, long movementId) {
        sql.execute("EXEC INV.sp_MovimientoVincularLote ?,?,?", actor, key.toString(), movementId);
    }
    @Override
    public void reservarLote(int actor, UUID key, String fingerprint) {
        sql.execute("EXEC INV.sp_LoteMovimientoReservar ?,?,?",actor,key.toString(),fingerprint);
    }
}
