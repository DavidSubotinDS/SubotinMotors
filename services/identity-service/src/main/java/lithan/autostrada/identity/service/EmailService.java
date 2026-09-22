package lithan.autostrada.identity.service;

public interface EmailService {

  void send(String to, String subject, String body);
}
