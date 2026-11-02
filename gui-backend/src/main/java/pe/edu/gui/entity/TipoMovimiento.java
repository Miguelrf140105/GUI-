package pe.edu.gui.entity;

/** Reglas de destino y contraparte definidas para un tipo de movimiento. */
public record TipoMovimiento(int id, int mode, boolean institutionRequired) {

    /**
     * Comprueba las reglas de ingreso, traslado interno y salida externa definidas por el modo
     * numérico.
     */
    public void validarSolicitud(Integer origin, Integer destination, Integer institution) {
        if (mode == 1) {
            throw new IllegalArgumentException("Los ingresos se registran desde Adquisiciones.");
        }
        if (mode == 2 && (destination == null || destination.equals(origin))) {
            throw new IllegalArgumentException("El destino debe ser diferente al origen.");
        }
        if (mode == 3 && destination != null) {
            throw new IllegalArgumentException("Una salida externa no tiene destino interno.");
        }
        if (institutionRequired && institution == null) {
            throw new IllegalArgumentException("Indique la institución destinataria.");
        }
    }
}
