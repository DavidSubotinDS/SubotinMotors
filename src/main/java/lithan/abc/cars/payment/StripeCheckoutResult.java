package lithan.abc.cars.payment;

public record StripeCheckoutResult(String sessionId, String checkoutUrl) {
}
