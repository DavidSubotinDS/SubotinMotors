package lithan.autostrada.auctions.dto.api;

import java.util.List;

public record MarketplaceSummaryResponse(
    List<AuctionSummaryResponse> featuredAuctions,
    List<ListingSummaryResponse> fixedPriceListings,
    List<PartSummaryResponse> storeParts,
    List<String> partCategories) {
}
