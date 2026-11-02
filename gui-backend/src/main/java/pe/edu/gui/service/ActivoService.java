package pe.edu.gui.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.edu.gui.interfaces.IActivoDAO;

/**
 * Casos de uso para unidades físicas, su ficha y su trazabilidad. Coordina validaciones y acceso a
 * persistencia.
 */
@Service
public class ActivoService {
  @org.springframework.transaction.annotation.Transactional
  public void eliminar(int actor, int id) {
    if (id < 1) throw new IllegalArgumentException("Activo inválido");
    dao.eliminar(actor, id);
  }

  private final IActivoDAO dao;

  public ActivoService(IActivoDAO dao) {
    this.dao = dao;
  }

  /** Busca la ficha del QR con el usuario autenticado; la consulta SQL aplica su alcance. */
  public Map<String, Object> buscarPorQR(int actor, UUID token) {
    return dao.buscarPorQR(actor, token);
  }

  /** Consulta los eventos del Kardex sin modificar los registros históricos. */
  public List<Map<String, Object>> historial(int actor, int id) {
    return dao.historial(actor, id);
  }

  /** Obtiene los atributos EAV de la unidad autorizada. */
  public List<Map<String, Object>> especificaciones(int actor, int id) {
    return dao.especificaciones(actor, id);
  }

  /** Admite también la URL de una etiqueta QR, extrayendo su token final. */
  public Map<String, Object> buscar(int actor, String consulta) {
    String value = consulta == null ? "" : consulta.trim();
    if (value.contains("/a/")) {
      value = value.substring(value.lastIndexOf("/a/") + 3).split("[?#/]", -1)[0];
    }
    if (value.isBlank() || value.length() > 200) {
      throw new IllegalArgumentException("Indique ID, código o QR del activo");
    }
    return dao.buscar(actor, value);
  }

  public Map<String, Object> cambiarNombre(int actor, int id, String nombre, String anterior) {
    String value = nombre == null ? "" : nombre.strip();
    if (id <= 0
        || value.isBlank()
        || value.length() > 200
        || anterior == null
        || anterior.isBlank()
        || anterior.length() > 200) {
      throw new IllegalArgumentException("Indique un nombre de entre 1 y 200 caracteres.");
    }
    return dao.cambiarNombre(actor, id, value, anterior);
  }
}
