package lithan.autostrada.auctions.payment;

public record StripeAccountState(String accountId, boolean transfersEnabled, String status) {
}
