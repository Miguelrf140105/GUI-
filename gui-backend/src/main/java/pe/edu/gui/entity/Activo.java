package pe.edu.gui.entity;

/** Estado y ubicación necesarios para decidir si una unidad admite movimientos. */
public record Activo(int id, int stateId, Integer locationId) {

    /**
     * Rechaza estados que no admiten otra solicitud de movimiento según el contrato de estados
     * versionado.
     */
    public void validarMovimiento() {
        if (stateId != 10 && stateId != 11) {
            throw new IllegalArgumentException("El estado del activo no admite movimientos.");
        }
    }
}
