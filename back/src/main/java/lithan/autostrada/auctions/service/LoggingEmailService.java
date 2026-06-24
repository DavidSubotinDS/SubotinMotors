package lithan.autostrada.auctions.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

  private static final Logger logger = LoggerFactory.getLogger(LoggingEmailService.class);

  @Override
  public void send(String to, String subject, String body) {
    logger.info("Local email to={} subject={} body={}", to, subject, body);
  }
}
