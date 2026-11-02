package pe.edu.gui.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.EgresoService;

@RestController
@RequestMapping("/api/egresses")
public class EgresoController {
  private final Access access;
  private final EgresoService service;

  public EgresoController(Access access, EgresoService service) {
    this.access = access;
    this.service = service;
  }

  public record DocumentLink(@NotNull @Positive Integer documentId) {}

  @GetMapping("/{id}/details")
  @PreAuthorize("@access.has(authentication,1501)")
  public Map<String, List<Map<String, Object>>> detalle(
      Authentication auth, @PathVariable long id) {
    return service.detalle(access.user(auth), id);
  }

  @PostMapping("/{id}/documents")
  @PreAuthorize("@access.has(authentication,1502)")
  public void documento(
      Authentication auth, @PathVariable long id, @Valid @RequestBody DocumentLink dto) {
    service.asociarDocumento(access.user(auth), id, dto.documentId());
  }
}
