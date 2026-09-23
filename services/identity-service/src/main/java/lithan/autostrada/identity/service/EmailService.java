package lithan.autostrada.identity.service;

public interface EmailService {

  void sendReset(String recipientEmail,String resetUrl,java.time.Instant expiresAt);
}
