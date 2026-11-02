package pe.edu.gui.entity;

/** IDs del contrato de workflow, compartidos con el seed SQL versionado. */
public enum EtapaMovimiento {
    AUTORIZAR(201, 204),
    DESPACHAR(204, 202),
    RECIBIR(202, 203);
    private final int origin, target;

    EtapaMovimiento(int origin, int target) {
        this.origin = origin;
        this.target = target;
    }

    /** ID del estado requerido antes de ejecutar esta etapa. */
    public int origin() {
        return origin;
    }

    /** ID del estado esperado después de completar esta etapa. */
    public int target() {
        return target;
    }

    /** Rechaza una transición si el movimiento ya cambió desde la última lectura del usuario. */
    public void validarEstado(int state) {
        if (state != origin) {
            throw new IllegalArgumentException(
                    "El movimiento cambió de estado. Recargue la lista.");
        }
    }
}
