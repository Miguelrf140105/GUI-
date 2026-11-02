package pe.edu.gui.dao;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.edu.gui.interfaces.IActivoDAO;
import pe.edu.gui.interfaces.ISqlExecutor;

/**
 * Acceso a los procedimientos almacenados de unidades físicas, su ficha y su trazabilidad. No
 * contiene estado de pantalla.
 */
@Repository
public class ActivoDAO extends BaseDAO implements IActivoDAO {

  public void eliminar(int actor, int id) {
    sql.execute("EXEC INV.sp_ActivoEliminar ?,?", actor, id);
  }

  public ActivoDAO(ISqlExecutor sql) {
    super(sql);
  }

  /** Busca la ficha del QR con el usuario autenticado; la consulta SQL aplica su alcance. */
  @Override
  public Map<String, Object> buscarPorQR(int actor, UUID token) {
    var rows = sql.query("EXEC INV.sp_ActivoPorQR ?,?", actor, token.toString());
    if (rows.isEmpty()) {
      throw new NoSuchElementException("Activo inexistente");
    }
    return rows.get(0);
  }

  /** Consulta los eventos del Kardex sin modificar los registros históricos. */
  @Override
  public List<Map<String, Object>> historial(int actor, int id) {
    return sql.query("EXEC INV.sp_Kardex_ObtenerHistorialProducto ?,?", actor, id);
  }

  /** Obtiene los atributos EAV de la unidad autorizada. */
  @Override
  public List<Map<String, Object>> especificaciones(int actor, int id) {
    return sql.query("EXEC INV.sp_Especificaciones ?,?", actor, id);
  }

  /** La consulta SQL comprueba permisos y ubicación antes de devolver la ficha. */
  @Override
  public Map<String, Object> buscar(int actor, String consulta) {
    return sql.query("EXEC INV.sp_ActivoBuscar ?,?", actor, consulta).stream()
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Activo inexistente"));
  }

  @Override
  public Map<String, Object> cambiarNombre(int actor, int id, String nombre, String anterior) {
    return sql
        .query("EXEC INV.sp_ActivoCambiarNombre ?,?,?,?", actor, id, nombre, anterior)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Activo inexistente"));
  }
}
