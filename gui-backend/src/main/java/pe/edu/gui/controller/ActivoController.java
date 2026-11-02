package pe.edu.gui.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.gui.dto.AssetName;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.ActivoService;

/**
 * Endpoints REST para unidades físicas, su ficha y su trazabilidad. La identidad del ejecutor se
 * obtiene de la sesión.
 */
@RestController
@RequestMapping("/api/assets")
public class ActivoController {
  @PostMapping("/{id}/delete")
  @PreAuthorize("@access.has(authentication,301) and @access.has(authentication,304)")
  public void eliminar(Authentication auth, @PathVariable int id) {
    service.eliminar(access.user(auth), id);
  }

  private final Access access;

  private final ActivoService service;

  public ActivoController(Access access, ActivoService service) {
    this.access = access;
    this.service = service;
  }

  /**
   * Consulta una unidad por su token QR. Conocer el token no sustituye el permiso ni el alcance de
   * acceso.
   */
  @GetMapping("/qr/{token}")
  public Map<String, Object> qr(Authentication auth, @PathVariable UUID token) {
    return service.buscarPorQR(access.user(auth), token);
  }

  /** Consulta los eventos del Kardex sin modificar los registros históricos. */
  @GetMapping("/{id}/history")
  public List<Map<String, Object>> historial(Authentication auth, @PathVariable int id) {
    return service.historial(access.user(auth), id);
  }

  /** Obtiene los atributos EAV de la unidad autorizada. */
  @GetMapping("/{id}/specifications")
  public List<Map<String, Object>> especificaciones(Authentication auth, @PathVariable int id) {
    return service.especificaciones(access.user(auth), id);
  }

  /** Búsqueda global autorizada, independiente de la página que esté viendo el usuario. */
  @GetMapping("/search")
  @PreAuthorize("@access.has(authentication,301)")
  public Map<String, Object> buscar(Authentication auth, @RequestParam String q) {
    return service.buscar(access.user(auth), q);
  }

  /** Edición restringida del nombre de una sola unidad física. */
  @PostMapping("/{id}/name")
  @PreAuthorize("@access.has(authentication,301) and @access.has(authentication,302)")
  public Map<String, Object> cambiarNombre(
      Authentication auth, @PathVariable int id, @Valid @RequestBody AssetName body) {
    return service.cambiarNombre(access.user(auth), id, body.name(), body.expectedName());
  }
}
