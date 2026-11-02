package pe.edu.gui.entity;

/** Datos internos; hash y versión no se devuelven al frontend. */
public record CredencialesUsuario(
    int id, String username, String passwordHash, int passwordVersion) {
  public CredencialesUsuario(int id, String username, String hash) {
    this(id, username, hash, 0);
  }
}
