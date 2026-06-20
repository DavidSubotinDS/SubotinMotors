package lithan.abc.cars.payment;

public record StripeAccountState(String accountId, boolean transfersEnabled, String status) {
}
