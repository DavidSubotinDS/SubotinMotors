package lithan.autostrada.auctions.dto.api;

import java.util.List;

public record ListingSummaryResponse(
    int id,
    String title,
    String make,
    String model,
    String year,
    int mileage,
    String fuelType,
    String transmission,
    long priceMinor,
    long depositAmountMinor,
    String status,
    String imageUrl,
    List<String> imageUrls,
    String sellerDisplayName) {
}
