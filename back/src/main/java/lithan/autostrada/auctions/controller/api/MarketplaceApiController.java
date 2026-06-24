package lithan.autostrada.auctions.controller.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lithan.autostrada.auctions.dto.api.AuctionSummaryResponse;
import lithan.autostrada.auctions.dto.api.ListingSummaryResponse;
import lithan.autostrada.auctions.dto.api.MarketplaceSummaryResponse;
import lithan.autostrada.auctions.dto.api.PageResponse;
import lithan.autostrada.auctions.dto.api.PartSummaryResponse;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.UserProfile;
import lithan.autostrada.auctions.service.CarListingService;
import lithan.autostrada.auctions.service.CarPartService;
import lithan.autostrada.auctions.service.CarService;

@RestController
@RequestMapping("/api/public")
public class MarketplaceApiController {

  private static final int MAX_PAGE_SIZE = 24;

  private final CarService carService;
  private final CarListingService listingService;
  private final CarPartService partService;

  public MarketplaceApiController(
      CarService carService,
      CarListingService listingService,
      CarPartService partService) {
    this.carService = carService;
    this.listingService = listingService;
    this.partService = partService;
  }

  @GetMapping("/summary")
  public MarketplaceSummaryResponse summary() {
    List<AuctionSummaryResponse> featuredAuctions = carService.findCatalogCars(
        null,
        null,
        null,
        PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "idCar")))
        .map(this::toAuction)
        .getContent();
    List<ListingSummaryResponse> fixedPriceListings = listingService.browse(
        null,
        PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt")))
        .map(this::toListing)
        .getContent();
    List<PartSummaryResponse> storeParts = partService.browse(
        null,
        null,
        PageRequest.of(0, 4, Sort.by(Sort.Direction.ASC, "name")))
        .map(this::toPart)
        .getContent();

    return new MarketplaceSummaryResponse(
        featuredAuctions,
        fixedPriceListings,
        storeParts,
        partService.categories());
  }

  @GetMapping("/auctions")
  public PageResponse<AuctionSummaryResponse> auctions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size,
      @RequestParam(defaultValue = "idCar") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer low,
      @RequestParam(required = false) Integer high) {
    Page<Car> cars = carService.findCatalogCars(
        keyword,
        low,
        high,
        PageRequest.of(
            safePage(page),
            safePageSize(size),
            Sort.by(sortDirection(direction), auctionSort(sort))));
    return PageResponse.from(cars.map(this::toAuction));
  }

  @GetMapping("/listings")
  public PageResponse<ListingSummaryResponse> listings(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      @RequestParam(required = false) String keyword) {
    Page<CarListing> listings = listingService.browse(
        keyword,
        PageRequest.of(
            safePage(page),
            safePageSize(size),
            Sort.by(sortDirection(direction), listingSort(sort))));
    return PageResponse.from(listings.map(this::toListing));
  }

  @GetMapping("/parts")
  public PageResponse<PartSummaryResponse> parts(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "8") int size,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "asc") String direction,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category) {
    Page<CarPart> parts = partService.browse(
        keyword,
        category,
        PageRequest.of(
            safePage(page),
            safePageSize(size),
            Sort.by(sortDirection(direction), partSort(sort))));
    return PageResponse.from(parts.map(this::toPart));
  }

  @GetMapping("/part-categories")
  public List<String> partCategories() {
    return partService.categories();
  }

  private AuctionSummaryResponse toAuction(Car car) {
    return new AuctionSummaryResponse(
        car.getIdCar(),
        car.getMake(),
        car.getModel(),
        car.getYear(),
        car.getPrice(),
        car.getAuctionStatus(),
        car.getAuctionStatusLabel(),
        car.getAuctionEndTimeDisplay(),
        car.getAuctionEndTimeEpochMillis(),
        car.getCarPicture() == null
            ? null
            : imageDataUrl(car.getCarPicture().getFileType(), car.getCarPicture().getImage()),
        displayName(car.getUser().getProfile(), car.getUser().getUsername()));
  }

  private ListingSummaryResponse toListing(CarListing listing) {
    return new ListingSummaryResponse(
        listing.getIdListing(),
        listing.getTitle(),
        listing.getMake(),
        listing.getModel(),
        listing.getYear(),
        listing.getMileage(),
        listing.getFuelType(),
        listing.getTransmission(),
        listing.getPriceMinor(),
        listing.getDepositAmountMinor(),
        listing.getStatus().name(),
        listing.getPicture() == null
            ? null
            : imageDataUrl(listing.getPicture().getFileType(), listing.getPicture().getImage()),
        displayName(listing.getSeller().getProfile(), listing.getSeller().getUsername()));
  }

  private PartSummaryResponse toPart(CarPart part) {
    return new PartSummaryResponse(
        part.getIdPart(),
        part.getSku(),
        part.getName(),
        part.getCategory(),
        part.getDescription(),
        part.getPriceMinor(),
        part.getStockQuantity(),
        part.getImageUrl());
  }

  private String imageDataUrl(String fileType, String image) {
    if (fileType == null || fileType.isBlank() || image == null || image.isBlank()) {
      return null;
    }
    return "data:" + fileType + ";base64," + image;
  }

  private String displayName(UserProfile profile, String fallback) {
    if (profile == null) {
      return fallback;
    }
    return (profile.getFirstName() + " " + profile.getLastName()).trim();
  }

  private int safePage(int page) {
    return Math.max(page, 0);
  }

  private int safePageSize(int size) {
    return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
  }

  private Sort.Direction sortDirection(String direction) {
    return "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
  }

  private String auctionSort(String sort) {
    return switch (sort) {
      case "make", "model", "year", "price", "idCar", "auctionEndTime", "status" -> sort;
      default -> "idCar";
    };
  }

  private String listingSort(String sort) {
    return switch (sort) {
      case "createdAt", "priceMinor", "make", "model", "year", "mileage" -> sort;
      default -> "createdAt";
    };
  }

  private String partSort(String sort) {
    return switch (sort) {
      case "name", "category", "priceMinor", "stockQuantity" -> sort;
      default -> "name";
    };
  }
}
