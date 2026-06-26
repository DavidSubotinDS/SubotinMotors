package lithan.autostrada.auctions.controller.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lithan.autostrada.auctions.dto.api.ApiModels.AuctionDetailResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.CommentResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ListingDetailResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.PartDetailResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileResponse;
import lithan.autostrada.auctions.dto.api.AuctionSummaryResponse;
import lithan.autostrada.auctions.dto.api.ListingSummaryResponse;
import lithan.autostrada.auctions.dto.api.MarketplaceSummaryResponse;
import lithan.autostrada.auctions.dto.api.PageResponse;
import lithan.autostrada.auctions.dto.api.PartSummaryResponse;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.service.CarListingService;
import lithan.autostrada.auctions.service.CarPartService;
import lithan.autostrada.auctions.service.CarService;
import lithan.autostrada.auctions.service.ListingCommentService;
import lithan.autostrada.auctions.service.ListingDepositService;
import lithan.autostrada.auctions.service.AuctionFollowService;
import lithan.autostrada.auctions.service.UserCarService;
import lithan.autostrada.auctions.service.UserService;

@RestController
@RequestMapping("/api/public")
public class MarketplaceApiController {

  private static final int MAX_PAGE_SIZE = 24;

  private final CarService carService;
  private final CarListingService listingService;
  private final CarPartService partService;
  private final UserCarService userCarService;
  private final UserService userService;
  private final ListingDepositService depositService;
  private final ListingCommentService commentService;
  private final AuctionFollowService followService;
  private final ApiModelMapper mapper;

  public MarketplaceApiController(
      CarService carService,
      CarListingService listingService,
      CarPartService partService,
      UserCarService userCarService,
      UserService userService,
      ListingDepositService depositService,
      ListingCommentService commentService,
      AuctionFollowService followService,
      ApiModelMapper mapper) {
    this.carService = carService;
    this.listingService = listingService;
    this.partService = partService;
    this.userCarService = userCarService;
    this.userService = userService;
    this.depositService = depositService;
    this.commentService = commentService;
    this.followService = followService;
    this.mapper = mapper;
  }

  @GetMapping("/summary")
  public MarketplaceSummaryResponse summary() {
    var featuredAuctions = carService.findCatalogCars(
        null,
        null,
        null,
        PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "idCar")))
        .map(mapper::auction)
        .getContent();
    var fixedPriceListings = listingService.browse(
        null,
        PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt")))
        .map(mapper::listing)
        .getContent();
    var storeParts = partService.browse(
        null,
        null,
        PageRequest.of(0, 4, Sort.by(Sort.Direction.ASC, "name")))
        .map(mapper::part)
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
    return PageResponse.from(cars.map(mapper::auction));
  }

  @GetMapping("/auctions/{idCar}")
  public AuctionDetailResponse auctionDetails(@PathVariable int idCar) {
    Car car = carService.getCarById(idCar);
    return new AuctionDetailResponse(
        mapper.auction(car),
        userCarService.highestBidding(idCar),
        followService.isFollowingCurrentUser(car),
        commentService.commentsForCar(car).stream().map(mapper::comment).toList());
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
    return PageResponse.from(listings.map(mapper::listing));
  }

  @GetMapping("/listings/{listingId}")
  public ListingDetailResponse listingDetails(@PathVariable int listingId) {
    CarListing listing = listingService.publicListing(listingId);
    return mapper.listingDetail(listing, depositService.isStripeEnabled(), java.util.List.of());
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
    return PageResponse.from(parts.map(mapper::part));
  }

  @GetMapping("/parts/{idPart}")
  public PartDetailResponse partDetails(@PathVariable int idPart) {
    CarPart part = partService.getActivePart(idPart);
    return mapper.partDetail(part, commentService.commentsForPart(part));
  }

  @GetMapping("/part-categories")
  public java.util.List<String> partCategories() {
    return partService.categories();
  }

  @GetMapping("/profiles/{idProfile}")
  public ProfileResponse profile(@PathVariable int idProfile) {
    return mapper.profile(userService.getProfile(idProfile), null);
  }

  @GetMapping("/profiles/{idProfile}/auctions")
  public java.util.List<AuctionSummaryResponse> profileAuctions(@PathVariable int idProfile) {
    return carService.listCar().stream()
        .filter(car -> car.getUser().getProfile().getIdProfile() == idProfile)
        .filter(car -> !"DEACTIVE".equals(car.getStatus()))
        .filter(car -> !"PENDING".equals(car.getStatus()))
        .map(mapper::auction)
        .toList();
  }

  @GetMapping("/content/{page}")
  public java.util.Map<String, String> content(@PathVariable String page) {
    return switch (page) {
      case "about-us", "about" -> java.util.Map.of(
          "title", "About Autostrada Auctions",
          "body", "Autostrada Auctions connects buyers, sellers, and store teams around vehicle auctions, fixed-price listings, and trusted parts ordering.");
      case "contact-us", "contact" -> java.util.Map.of(
          "title", "Contact Autostrada Auctions",
          "body", "Reach the marketplace team for listing help, store questions, test rides, and checkout support.");
      default -> throw new lithan.autostrada.auctions.error.ResourceNotFoundException();
    };
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
