package pe.edu.gui.security;

import java.util.List;
import org.springframework.security.core.userdetails.User;

/** Identidad autenticada de Spring Security con el ID numérico del usuario de GUI. */
public class GuiPrincipal extends User {

  private final int id;
  private final int passwordVersion;

  public int passwordVersion() {
    return passwordVersion;
  }

  public GuiPrincipal(int id, String username, String hash) {
    this(id, username, hash, 0);
  }

  public GuiPrincipal(int id, String username, String hash, int version) {
    super(username, hash, List.of());
    this.id = id;
    this.passwordVersion = version;
  }

  /** ID interno utilizado como actor en los servicios y procedimientos almacenados. */
  public int id() {
    return id;
  }
}
