package pe.edu.gui.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.gui.service.PasswordResetService;

@RestController
@RequestMapping("/api/auth/recovery")
public class PasswordResetController {
  private final PasswordResetService service;

  public PasswordResetController(PasswordResetService service) {
    this.service = service;
  }

  public record Request(@NotBlank @Email @Size(max = 150) String email) {}

  public record Complete(
      @NotNull UUID requestId,
      @NotBlank @Pattern(regexp = "[0-9]{8}") String code,
      @NotBlank @Size(min = 14, max = 72) String password) {}

  @GetMapping("/status")
  public Map<String, Boolean> status() {
    return Map.of("available", service.available());
  }

  @PostMapping("/request")
  public Map<String, Object> request(@Valid @RequestBody Request dto) {
    return Map.of(
        "requestId",
        service.request(dto.email()),
        "message",
        "Si el correo está registrado, recibirás un código de recuperación.");
  }

  @PostMapping("/complete")
  public void complete(@Valid @RequestBody Complete dto) {
    service.complete(dto.requestId(), dto.code(), dto.password());
  }
}
