package lithan.abc.cars.payment;

public record StripeWebhookEvent(
    String eventId,
    String eventType,
    String checkoutSessionId,
    String paymentIntentId,
    String paymentStatus) {
}
