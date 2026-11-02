package pe.edu.gui.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.entity.EtapaMovimiento;
import pe.edu.gui.interfaces.IMovimientoDAO;

/**
 * Casos de uso para solicitudes y etapas de traslado, donación, mantenimiento o baja. Coordina
 * validaciones y acceso a persistencia.
 */
@Service
public class MovimientoService {

    private final IMovimientoDAO dao;

    public MovimientoService(IMovimientoDAO dao) {
        this.dao = dao;
    }

    /**
     * Comprueba activo, destino y contraparte. Un reintento con la misma clave y datos recupera la
     * solicitud existente.
     */
    @Transactional
    public ResultadoRegistro solicitar(int actor, Requests.Movement dto) {
        var prior = dao.porClave(actor, dto.idempotencyKey());
        if (prior.isPresent()) {
            if (!prior.get().corresponde(dto)) {
                throw new IllegalArgumentException(
                        "Clave de idempotencia usada para otra solicitud");
            }
            return new ResultadoRegistro(prior.get().id());
        }
        var activo = dao.activo(actor, dto.assetId());
        activo.validarMovimiento();
        dao.tipo(actor, dto.typeId())
                .validarSolicitud(activo.locationId(), dto.locationId(), dto.institutionId());
        return dao.solicitar(actor, dto);
    }

    /**
     * Valida la etapa actual y solicita la transición. SQL vuelve a comprobarla bajo bloqueo ante
     * peticiones concurrentes.
     */
    @Transactional
    public ResultadoRegistro avanzar(int actor, EtapaMovimiento stage, Requests.Transition dto) {
        stage.validarEstado(dao.estado(actor, dto.movementId()));
        return dao.avanzar(actor, stage, dto);
    }

    public record UnitMovement(int assetId, long movementId) {}

    /** Todas las unidades se validan antes de escribir; un fallo revierte el lote completo. */
    @Transactional
    public java.util.List<UnitMovement> solicitarLote(int actor, pe.edu.gui.dto.MovementBatch dto) {
        if (dto.assetIds() == null || dto.assetIds().isEmpty() || dto.assetIds().size() > 100
                || dto.assetIds().stream().anyMatch(id -> id == null || id <= 0)
                || new java.util.HashSet<>(dto.assetIds()).size() != dto.assetIds().size()) {
            throw new IllegalArgumentException("Seleccione entre 1 y 100 activos sin duplicados.");
        }
        if (dto.typeId() == null || dto.typeId() <= 0 || dto.idempotencyKey() == null
                || dto.reason() == null || dto.reason().isBlank() || dto.reason().length() > 500) {
            throw new IllegalArgumentException("Complete tipo y motivo del movimiento.");
        }
        var ids = dto.assetIds().stream().sorted().toList();
        String reason = dto.reason().strip();
        String canonical = ids + "|" + dto.typeId() + "|" + dto.locationId() + "|" + dto.institutionId() + "|" + reason;
        String hash;
        try {
            hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
        dao.reservarLote(actor, dto.idempotencyKey(), hash);
        var planned = new java.util.ArrayList<Requests.Movement>();
        var existing = new java.util.HashMap<Integer, Long>();
        for (int id : ids) {
            var key = java.util.UUID.nameUUIDFromBytes(("GUI-MOVEMENT|" + actor + "|" + dto.idempotencyKey() + "|" + id)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var unit = new Requests.Movement(id,dto.typeId(),dto.locationId(),dto.institutionId(),reason,key);
            var prior = dao.porClave(actor,key);
            if (prior.isPresent()) {
                if (!prior.get().corresponde(unit)) throw new IllegalArgumentException("El reintento no corresponde al movimiento original.");
                existing.put(id,prior.get().id());
            } else {
                var asset = dao.activo(actor,id);
                asset.validarMovimiento();
                dao.tipo(actor,dto.typeId()).validarSolicitud(asset.locationId(),dto.locationId(),dto.institutionId());
            }
            planned.add(unit);
        }
        var result = new java.util.ArrayList<UnitMovement>();
        for (var unit : planned) {
            long movementId = existing.containsKey(unit.assetId()) ? existing.get(unit.assetId()) : dao.solicitar(actor,unit).id();
            // Guarda la agrupación sin sustituir el movimiento ni el Kardex individual.
            dao.vincularLote(actor, dto.idempotencyKey(), movementId);
            result.add(new UnitMovement(unit.assetId(),movementId));
        }
        return java.util.List.copyOf(result);
    }
}
