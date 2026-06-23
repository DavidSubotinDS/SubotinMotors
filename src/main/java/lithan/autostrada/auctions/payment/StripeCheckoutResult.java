package lithan.autostrada.auctions.payment;

public record StripeCheckoutResult(String sessionId, String checkoutUrl) {
}
