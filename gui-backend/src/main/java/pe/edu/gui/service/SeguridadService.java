package pe.edu.gui.service;

import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.edu.gui.entity.CredencialesUsuario;
import pe.edu.gui.interfaces.IUsuarioDAO;

/** Consulta identidad y permisos actuales para aplicar las decisiones de acceso en el backend. */
@Service
public class SeguridadService {

  private final IUsuarioDAO dao;

  public int passwordVersion(int id) {
    return dao.passwordVersion(id);
  }

  public SeguridadService(IUsuarioDAO dao) {
    this.dao = dao;
  }

  /** Carga la cuenta para autenticar y responde de forma uniforme si no existe o está inactiva. */
  public CredencialesUsuario credenciales(String username) {
    return dao.credenciales(username)
        .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
  }

  /**
   * Lee los permisos actuales para que una revocación tenga efecto sin esperar otro inicio de
   * sesión.
   */
  public boolean tienePermiso(int id, int permission) {
    return dao.identidad(id).stream()
        .anyMatch(r -> r.get("permissionId") instanceof Number n && n.intValue() == permission);
  }

  /**
   * Construye el perfil público sin incluir el hash de contraseña y rechaza cuentas sin identidad
   * activa.
   */
  public Map<String, Object> perfil(int id) {
    var rows = dao.identidad(id);
    if (rows.isEmpty()) {
      throw new AccessDeniedException("Cuenta inactiva");
    }
    var first = rows.get(0);
    return Map.of(
        "id",
        first.get("id"),
        "name",
        first.get("name"),
        "username",
        first.get("username"),
        "permissions",
        rows.stream().map(r -> r.get("permissionId")).filter(Objects::nonNull).toList());
  }
}
