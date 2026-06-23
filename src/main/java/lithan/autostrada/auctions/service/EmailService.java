package lithan.autostrada.auctions.service;

public interface EmailService {

  void send(String to, String subject, String body);
}
