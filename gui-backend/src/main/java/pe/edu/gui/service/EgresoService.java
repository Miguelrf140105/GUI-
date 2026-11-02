package pe.edu.gui.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.gui.interfaces.IEgresoDAO;

/**
 * Lee salidas ejecutadas; la creación ocurre al confirmar el movimiento, nunca desde un listado.
 */
@Service
public class EgresoService {
  private final IEgresoDAO dao;

  public EgresoService(IEgresoDAO dao) {
    this.dao = dao;
  }

  @Transactional(readOnly = true)
  public Map<String, List<Map<String, Object>>> detalle(int actor, long id) {
    if (id < 1) throw new IllegalArgumentException("Egreso inválido");
    return Map.of("units", dao.detalle(actor, id, 2), "documents", dao.detalle(actor, id, 3));
  }

  @Transactional
  public void asociarDocumento(int actor, long id, int documentId) {
    if (id < 1 || documentId < 1) throw new IllegalArgumentException("Egreso o documento inválido");
    dao.asociarDocumento(actor, id, documentId);
  }
}
