package lithan.autostrada.auctions.identity;

public interface CheckoutProfileClient {
  /** No caller-supplied account ID: only the authenticated account's private data. */
  CheckoutProfile current();
}
