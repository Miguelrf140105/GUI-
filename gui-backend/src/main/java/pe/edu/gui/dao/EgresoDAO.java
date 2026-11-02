package pe.edu.gui.dao;

import java.util.*;
import org.springframework.stereotype.Repository;
import pe.edu.gui.interfaces.*;

/** Consultas parametrizadas; las reglas y el alcance se comprueban en SQL. */
@Repository
public class EgresoDAO implements IEgresoDAO {
  private final ISqlExecutor sql;

  public EgresoDAO(ISqlExecutor sql) {
    this.sql = sql;
  }

  public List<Map<String, Object>> detalle(int actor, long id, int view) {
    return sql.query("EXEC INV.sp_Egresos @usuario=?,@vista=?,@id=?", actor, view, id);
  }

  public void asociarDocumento(int actor, long id, int documentId) {
    sql.execute("EXEC INV.sp_EgresoDocumento ?,?,?", actor, id, documentId);
  }
}
