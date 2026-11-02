package pe.edu.gui.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import pe.edu.gui.interfaces.PasswordResetMailer;

@Component
public class PasswordResetMailListener {
  private final PasswordResetMailer mailer;

  public PasswordResetMailListener(PasswordResetMailer mailer) {
    this.mailer = mailer;
  }

  @Async("passwordResetMailExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void deliver(PasswordResetService.CodeMail event) {
    try {
      mailer.send(event.recipient(), event.code());
    } catch (RuntimeException e) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("No se completó el envío de recuperación ({})", e.getClass().getSimpleName());
    }
  }
}
