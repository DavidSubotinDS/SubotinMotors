package lithan.autostrada.auctions.dto.api;

public record AuctionSummaryResponse(
    int id,
    String make,
    String model,
    String year,
    int price,
    String status,
    String statusLabel,
    String auctionEndTime,
    long auctionEndTimeEpochMillis,
    String imageUrl,
    String sellerDisplayName) {
}
