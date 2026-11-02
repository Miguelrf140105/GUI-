package pe.edu.gui.interfaces;

import java.util.List;
import java.util.Map;

public interface IEgresoDAO {
  List<Map<String, Object>> detalle(int actor, long id, int view);

  void asociarDocumento(int actor, long id, int documentId);
}
