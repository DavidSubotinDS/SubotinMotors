package lithan.autostrada.auctions.error;

public class MissingShippingAddressException extends IllegalStateException {

  public MissingShippingAddressException() {
    super("A complete physical address is required to ship car parts.");
  }
}
