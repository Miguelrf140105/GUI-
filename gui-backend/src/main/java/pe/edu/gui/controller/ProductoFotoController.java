package pe.edu.gui.controller;

import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.gui.security.Access;
import pe.edu.gui.service.ProductoFotoService;

@RestController
@RequestMapping("/api")
public class ProductoFotoController {
  private final Access access;
  private final ProductoFotoService service;

  public ProductoFotoController(Access access, ProductoFotoService service) {
    this.access = access;
    this.service = service;
  }

  @GetMapping("/products/{product}/photos")
  public List<Map<String, Object>> listar(Authentication auth, @PathVariable int product) {
    return service.listar(access.user(auth), product);
  }

  @PostMapping(value = "/products/{product}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("@access.has(authentication,303)")
  public Map<String, UUID> subir(
      Authentication auth, @PathVariable int product, @RequestParam MultipartFile file)
      throws java.io.IOException {
    return Map.of(
        "id",
        service.subir(access.user(auth), product, file.getOriginalFilename(), file.getBytes()));
  }

  @GetMapping("/product-photos/{photo}")
  public ResponseEntity<byte[]> leer(
      Authentication auth,
      @PathVariable UUID photo,
      @RequestParam(defaultValue = "true") boolean thumbnail,
      WebRequest request) {
    // Primero autoriza en SQL, incluso si el navegador dispone de una copia en caché.
    byte[] content = service.leer(access.user(auth), photo, thumbnail);
    String tag = "\"" + photo + (thumbnail ? "-thumb" : "-full") + "\"";
    if (request.checkNotModified(tag))
      return ResponseEntity.status(304)
          .eTag(tag)
          .cacheControl(CacheControl.noCache().cachePrivate())
          .build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("image/webp"))
        .eTag(tag)
        .cacheControl(CacheControl.noCache().cachePrivate())
        .body(content);
  }

  @PostMapping("/products/{product}/photos/{photo}/cover")
  @PreAuthorize("@access.has(authentication,303)")
  public void portada(Authentication auth, @PathVariable int product, @PathVariable UUID photo) {
    service.cambiar(access.user(auth), product, photo, "cover");
  }

  @PostMapping("/products/{product}/photos/{photo}/remove")
  @PreAuthorize("@access.has(authentication,303)")
  public void retirar(Authentication auth, @PathVariable int product, @PathVariable UUID photo) {
    service.cambiar(access.user(auth), product, photo, "remove");
  }
}
