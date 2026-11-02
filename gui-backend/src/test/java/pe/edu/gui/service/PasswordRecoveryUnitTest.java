package pe.edu.gui.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.gui.interfaces.*;

class PasswordRecoveryUnitTest {
  @Test
  void sinRemitenteNoCreaCodigosNiPrometeEnvio() {
    var dao = mock(IPasswordResetDAO.class);
    var mailer = mock(PasswordResetMailer.class);
    var service =
        new PasswordResetService(
            dao, mailer, mock(PasswordEncoder.class), mock(ApplicationEventPublisher.class), "");
    assertFalse(service.available());
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.request("qa@example.invalid"));
    verifyNoInteractions(dao);
  }

  @Test
  void correoConstruidoConDestinatarioYCodigoSinRed() {
    var sender = mock(JavaMailSender.class);
    var factory = new DefaultListableBeanFactory();
    factory.registerSingleton("sender", sender);
    var mailer =
        new SmtpPasswordResetMailer(
            factory.getBeanProvider(JavaMailSender.class),
            true,
            "sender@example.invalid",
            "not-a-real-password",
            "smtp.invalid",
            "sender@example.invalid");
    mailer.send("recipient@example.invalid", "00123456");
    var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(captor.capture());
    assertArrayEquals(new String[] {"recipient@example.invalid"}, captor.getValue().getTo());
    assertTrue(captor.getValue().getText().contains("00123456"));
    assertTrue(captor.getValue().getText().contains("10 minutos"));
  }

  @Test
  void remitenteAjenoYDestinatariosMultiplesSeBloqueanSinEnviar() {
    var sender = mock(JavaMailSender.class);
    var factory = new DefaultListableBeanFactory();
    factory.registerSingleton("sender", sender);
    var foreign =
        new SmtpPasswordResetMailer(
            factory.getBeanProvider(JavaMailSender.class),
            true,
            "other@example.invalid",
            "fake",
            "smtp.invalid",
            "sender@example.invalid");
    assertFalse(foreign.available());
    assertThrows(IllegalStateException.class, () -> foreign.send("qa@example.invalid", "12345678"));
    var valid =
        new SmtpPasswordResetMailer(
            factory.getBeanProvider(JavaMailSender.class),
            true,
            "sender@example.invalid",
            "fake",
            "smtp.invalid",
            "sender@example.invalid");
    assertThrows(
        IllegalArgumentException.class,
        () -> valid.send("a@example.invalid,b@example.invalid", "12345678"));
    assertThrows(
        IllegalArgumentException.class,
        () -> valid.send("a@example.invalid\r\nBcc:b@example.invalid", "12345678"));
    verifyNoInteractions(sender);
  }

  @Test
  void eventoNoExponeCodigoNiCorreoEnLogs() {
    var event = new PasswordResetService.CodeMail("private@example.invalid", "12345678");
    assertFalse(event.toString().contains(event.code()));
    assertFalse(event.toString().contains(event.recipient()));
  }

  @Test
  void codigoIncorrectoNoCalculaHashCostoso() {
    var dao = mock(IPasswordResetDAO.class);
    var encoder = mock(PasswordEncoder.class);
    var service =
        new PasswordResetService(
            dao,
            mock(PasswordResetMailer.class),
            encoder,
            mock(ApplicationEventPublisher.class),
            "");
    assertThrows(
        IllegalArgumentException.class,
        () -> service.complete(UUID.randomUUID(), "00000000", "Password-QA-123456"));
    verifyNoInteractions(encoder);
    verify(dao, never()).complete(any(), any(), any());
  }
}
