package lithan.autostrada.auctions.service;

public record CheckoutOutcome(
    String checkoutUrl,
    String attemptId,
    String status,
    boolean retryable) {
}
