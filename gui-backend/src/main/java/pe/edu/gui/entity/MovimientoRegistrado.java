package pe.edu.gui.entity;

import pe.edu.gui.dto.Requests;

import java.util.Objects;

/** Solicitud persistida que permite reconocer reintentos sin crear otro movimiento. */
public record MovimientoRegistrado(
        long id,
        int assetId,
        int typeId,
        Integer locationId,
        Integer institutionId,
        String reason) {

    /**
     * Compara los datos del reintento para impedir que la misma clave se use en una solicitud
     * diferente.
     */
    public boolean corresponde(Requests.Movement dto) {
        return assetId == dto.assetId()
                && typeId == dto.typeId()
                && Objects.equals(locationId, dto.locationId())
                && Objects.equals(institutionId, dto.institutionId())
                && Objects.equals(reason, dto.reason());
    }
}
