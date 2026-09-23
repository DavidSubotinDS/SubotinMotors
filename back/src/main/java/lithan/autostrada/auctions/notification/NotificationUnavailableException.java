package lithan.autostrada.auctions.notification;

@org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
public final class NotificationUnavailableException extends RuntimeException {
  public NotificationUnavailableException(){super("Notifications are temporarily unavailable.");}
}
