package pe.edu.gui.dao;

import java.util.*;
import org.springframework.stereotype.Repository;
import pe.edu.gui.interfaces.*;

@Repository
public class PasswordResetDAO implements IPasswordResetDAO {
  private final ISqlExecutor sql;

  public PasswordResetDAO(ISqlExecutor sql) {
    this.sql = sql;
  }

  public Optional<String> request(String email, UUID id, byte[] hash) {
    return sql.query("EXEC SEC.sp_RecuperacionSolicitar ?,?,?", email, id.toString(), hash).stream()
        .findFirst()
        .map(r -> String.valueOf(r.get("email")));
  }

  public boolean attempt(UUID id, byte[] hash) {
    return Boolean.TRUE.equals(
        sql.query("EXEC SEC.sp_RecuperacionIntentar ?,?", id.toString(), hash).get(0).get("valid"));
  }

  public boolean complete(UUID id, byte[] hash, String encoded) {
    return Boolean.TRUE.equals(
        sql.query("EXEC SEC.sp_RecuperacionCompletar ?,?,?", id.toString(), hash, encoded)
            .get(0)
            .get("valid"));
  }
}
