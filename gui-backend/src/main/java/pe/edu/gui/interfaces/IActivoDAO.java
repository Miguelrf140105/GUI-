package pe.edu.gui.interfaces;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Contrato de persistencia para unidades físicas, su ficha y su trazabilidad. */
public interface IActivoDAO {
    void eliminar(int actor,int id);


    /** Busca la ficha del QR con el usuario autenticado; la consulta SQL aplica su alcance. */
    Map<String, Object> buscarPorQR(int actor, UUID token);

    /** Consulta los eventos del Kardex sin modificar los registros históricos. */
    List<Map<String, Object>> historial(int actor, int id);

    /** Obtiene los atributos EAV de la unidad autorizada. */
    List<Map<String, Object>> especificaciones(int actor, int id);

    /** Búsqueda exacta por ID interno, código o token QR. */
    Map<String, Object> buscar(int actor, String consulta);
    Map<String, Object> cambiarNombre(int actor, int id, String nombre, String anterior);
}
