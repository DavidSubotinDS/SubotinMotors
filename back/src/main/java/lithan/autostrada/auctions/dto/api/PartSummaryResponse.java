package lithan.autostrada.auctions.dto.api;

public record PartSummaryResponse(
    int id,
    String sku,
    String name,
    String category,
    String description,
    long priceMinor,
    int stockQuantity,
    String imageUrl) {
}
