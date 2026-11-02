package pe.edu.gui.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.gui.interfaces.*;
import pe.edu.gui.utils.PoliticaContrasena;

@Service
public class PasswordResetService {
  /** El evento solo vive en memoria; evitar filtrar el código en registros accidentales. */
  public record CodeMail(String recipient, String code) {
    @Override
    public String toString() {
      return "CodeMail[contenido protegido]";
    }
  }

  private final IPasswordResetDAO dao;
  private final PasswordResetMailer mailer;
  private final PasswordEncoder encoder;
  private final ApplicationEventPublisher events;
  private final byte[] secret;
  private final SecureRandom random = new SecureRandom();

  public PasswordResetService(
      IPasswordResetDAO dao,
      PasswordResetMailer mailer,
      PasswordEncoder encoder,
      ApplicationEventPublisher events,
      @Value("${gui.recovery.secret:}") String secret) {
    this.dao = dao;
    this.mailer = mailer;
    this.encoder = encoder;
    this.events = events;
    if (secret.isBlank()) {
      this.secret = new byte[32];
      random.nextBytes(this.secret);
    } else {
      this.secret = secret.getBytes(StandardCharsets.UTF_8);
      if (this.secret.length < 32)
        throw new IllegalArgumentException("GUI_RESET_SECRET debe tener al menos 32 bytes.");
    }
  }

  public boolean available() {
    return mailer.available();
  }

  @Transactional
  public UUID request(String email) {
    if (!available())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "La recuperación por correo no está disponible. Contacta al administrador.");
    UUID id = UUID.randomUUID();
    String code = String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
    dao.request(email.strip().toLowerCase(Locale.ROOT), id, digest(id, code))
        .ifPresent(recipient -> events.publishEvent(new CodeMail(recipient, code)));
    return id;
  }

  /** No envolver en una transacción externa: los intentos fallidos deben quedar contados. */
  public void complete(UUID id, String code, String password) {
    PoliticaContrasena.validar(password);
    byte[] hash = digest(id, code);
    if (!dao.attempt(id, hash)) throw invalid();
    if (!dao.complete(id, hash, encoder.encode(password))) throw invalid();
  }

  private IllegalArgumentException invalid() {
    return new IllegalArgumentException(
        "El código no es válido o ha caducado. Solicita uno nuevo.");
  }

  private byte[] digest(UUID id, String code) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return mac.doFinal((id + ":" + code).getBytes(StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
