package pe.edu.gui.interfaces;

public interface PasswordResetMailer {
  boolean available();

  void send(String recipient, String code);
}
