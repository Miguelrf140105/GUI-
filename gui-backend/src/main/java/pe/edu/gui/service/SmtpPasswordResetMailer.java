package pe.edu.gui.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import pe.edu.gui.interfaces.PasswordResetMailer;

@Component
public class SmtpPasswordResetMailer implements PasswordResetMailer {
  private final ObjectProvider<JavaMailSender> sender;
  private final boolean enabled;
  private final String from, password, host, username;

  public SmtpPasswordResetMailer(
      ObjectProvider<JavaMailSender> sender,
      @Value("${gui.mail.enabled:false}") boolean enabled,
      @Value("${gui.mail.from:}") String from,
      @Value("${spring.mail.password:}") String password,
      @Value("${spring.mail.host:}") String host,
      @Value("${spring.mail.username:}") String username) {
    this.sender = sender;
    this.enabled = enabled;
    this.from = from.strip();
    this.password = password;
    this.host = host;
    this.username = username.strip();
  }

  public boolean available() {
    return enabled
        // Una sola identidad remitente, sin listas ni cabeceras proporcionadas por el cliente.
        && mailbox(from)
        && from.equalsIgnoreCase(username)
        && !password.isBlank()
        && !host.isBlank()
        && sender.getIfAvailable() != null;
  }

  private static boolean mailbox(String value) {
    if (value == null || value.contains("\r") || value.contains("\n")) return false;
    try {
      var address = new jakarta.mail.internet.InternetAddress(value, true);
      address.validate();
      return address.getPersonal() == null && value.equals(address.getAddress());
    } catch (jakarta.mail.internet.AddressException e) {
      return false;
    }
  }

  public void send(String recipient, String code) {
    if (!available()) throw new IllegalStateException("Correo no configurado");
    if (!mailbox(recipient) || !code.matches("[0-9]{8}"))
      throw new IllegalArgumentException("Mensaje de recuperación inválido");
    var message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(recipient);
    message.setSubject("GUI · Código para restablecer tu contraseña");
    message.setText(
        "Tu código de recuperación es: "
            + code
            + "\n\n"
            + "Caduca en 10 minutos y solo puede usarse una vez.\n"
            + "Si no solicitaste este cambio, ignora este mensaje.");
    sender.getObject().send(message);
  }
}
