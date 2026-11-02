package pe.edu.gui.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.edu.gui.dto.Requests;
import pe.edu.gui.dto.ResultadoRegistro;
import pe.edu.gui.entity.CredencialesUsuario;
import pe.edu.gui.interfaces.ISqlExecutor;
import pe.edu.gui.interfaces.IUsuarioDAO;
import pe.edu.gui.utils.XmlPayload;

/**
 * Acceso a los procedimientos almacenados de cuentas, roles y alcance de acceso. No contiene estado
 * de pantalla.
 */
@Repository
public class UsuarioDAO implements IUsuarioDAO {

  private final ISqlExecutor sql;

  public int passwordVersion(int id) {
    return ((Number) sql.query("EXEC SEC.sp_VersionClave ?", id).get(0).get("version")).intValue();
  }

  public UsuarioDAO(ISqlExecutor sql) {
    this.sql = sql;
  }

  /**
   * Busca los datos internos de autenticación de una cuenta activa. No devuelve una contraseña en
   * texto plano.
   */
  @Override
  public Optional<CredencialesUsuario> credenciales(String username) {
    return sql.query("EXEC SEC.sp_Login ?", username).stream()
        .findFirst()
        .map(
            r ->
                new CredencialesUsuario(
                    ((Number) r.get("id")).intValue(),
                    (String) r.get("username"),
                    (String) r.get("passwordHash"),
                    r.get("passwordVersion") instanceof Number n ? n.intValue() : 0));
  }

  /**
   * Consulta identidad y permisos vigentes; no utiliza una copia de permisos guardada al iniciar
   * sesión.
   */
  @Override
  public List<Map<String, Object>> identidad(int id) {
    return sql.query("EXEC SEC.sp_Identidad ?", id);
  }

  /** Guarda la cuenta y sus asignaciones; al editar, una contraseña vacía conserva la existente. */
  @Override
  public ResultadoRegistro guardar(int actor, Requests.User dto, String hash) {
    var rows =
        sql.query(
            "EXEC SEC.sp_UsuarioGuardar"
                + " @actor=?,@id=?,@username=?,@nombre=?,@correo=?,@hash=?,@estado=?,@roles=?,@alcances=?,@global=?",
            actor,
            dto.id(),
            dto.username(),
            dto.name(),
            dto.email(),
            hash,
            dto.stateId(),
            XmlPayload.of(dto.roleIds()),
            XmlPayload.of(dto.locationIds()),
            dto.globalScope());
    return new ResultadoRegistro(((Number) rows.get(0).get("id")).longValue());
  }

  /** Consulta los roles y las raíces de ubicación asignadas a la cuenta. */
  @Override
  public Map<String, Object> asignaciones(int actor, int id) {
    return Map.of(
        "roles",
        sql.query("EXEC SEC.sp_UsuarioAsignaciones ?,?,1", actor, id),
        "locations",
        sql.query("EXEC SEC.sp_UsuarioAsignaciones ?,?,2", actor, id));
  }

  /**
   * Crea el administrador inicial solo si no existen usuarios; el SP decide esa condición de forma
   * atómica.
   */
  @Override
  public void bootstrap(String username, String hash) {
    sql.execute(
        "EXEC SEC.sp_Bootstrap ?,?,?,?",
        username,
        "Administrador inicial",
        username + "@localhost.invalid",
        hash);
  }
}
