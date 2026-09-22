package lithan.autostrada.auctions.identity;
public class IdentityUnavailableException extends RuntimeException {
  public IdentityUnavailableException() { super("Account information is temporarily unavailable. Try again later."); }
}
