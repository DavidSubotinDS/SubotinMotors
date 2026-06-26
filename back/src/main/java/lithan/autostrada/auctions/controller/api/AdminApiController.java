package lithan.autostrada.auctions.controller.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lithan.autostrada.auctions.dto.UserProfileForm;
import lithan.autostrada.auctions.dto.api.ApiModels.AdminCarManagementResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.AdminDashboardResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.AdminTransactionsResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ApiMessageResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileResponse;
import lithan.autostrada.auctions.dto.api.PageResponse;
import lithan.autostrada.auctions.entity.UserProfile;
import lithan.autostrada.auctions.service.AdminService;
import lithan.autostrada.auctions.service.PaymentService;
import lithan.autostrada.auctions.service.UserCarService;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

  private final AdminService adminService;
  private final UserCarService userCarService;
  private final PaymentService paymentService;
  private final ApiModelMapper mapper;

  public AdminApiController(
      AdminService adminService,
      UserCarService userCarService,
      PaymentService paymentService,
      ApiModelMapper mapper) {
    this.adminService = adminService;
    this.userCarService = userCarService;
    this.paymentService = paymentService;
    this.mapper = mapper;
  }

  @GetMapping("/dashboard")
  public AdminDashboardResponse dashboard(
      @RequestParam(defaultValue = "0") int userPage,
      @RequestParam(defaultValue = "idUser") String userSort,
      @RequestParam(defaultValue = "asc") String userDirection,
      @RequestParam(defaultValue = "0") int adminPage,
      @RequestParam(defaultValue = "idUser") String adminSort,
      @RequestParam(defaultValue = "asc") String adminDirection) {
    var users = adminService.listUser(PageRequest.of(
        Math.max(userPage, 0),
        5,
        Sort.by(sortDirection(userDirection), userSortProperty(userSort))));
    var admins = adminService.listAdmin(PageRequest.of(
        Math.max(adminPage, 0),
        5,
        Sort.by(sortDirection(adminDirection), userSortProperty(adminSort))));
    return mapper.adminDashboard(
        PageResponse.from(users.map(mapper::user)),
        PageResponse.from(admins.map(mapper::user)));
  }

  @GetMapping("/users/{idProfile}")
  public ProfileResponse userProfile(@PathVariable int idProfile) {
    return mapper.profile(adminService.getProfileById(idProfile), null);
  }

  @PutMapping("/users/{idProfile}")
  public ProfileResponse updateUserProfile(
      @PathVariable int idProfile,
      @RequestBody ProfileRequest request) {
    UserProfileForm form = new UserProfileForm();
    form.setIdProfile(idProfile);
    form.setEmail(request.email());
    form.setFirstName(request.firstName());
    form.setLastName(request.lastName());
    form.setPhoneNumber(request.phoneNumber());
    form.setAddress(request.address());
    form.setStreetAddress(request.streetAddress());
    form.setCity(request.city());
    form.setPostalCode(request.postalCode());
    form.setCountry(request.country());
    form.setAbout(request.about());
    UserProfile profile = new UserProfile();
    profile.setIdProfile(form.getIdProfile());
    profile.setFirstName(form.getFirstName());
    profile.setLastName(form.getLastName());
    profile.setPhoneNumber(form.getPhoneNumber());
    profile.setAddress(form.getAddress());
    profile.setStreetAddress(form.getStreetAddress());
    profile.setCity(form.getCity());
    profile.setPostalCode(form.getPostalCode());
    profile.setCountry(form.getCountry());
    profile.setAbout(form.getAbout());
    adminService.editUser(profile);
    return mapper.profile(adminService.getProfileById(idProfile), null);
  }

  @PostMapping("/users/{idUser}/mark-admin")
  public ApiMessageResponse markAdmin(@PathVariable int idUser) {
    adminService.markAsAdmin(idUser);
    return new ApiMessageResponse("User promoted to admin.", null);
  }

  @GetMapping("/cars")
  public AdminCarManagementResponse cars(
      @RequestParam(defaultValue = "0") int carPage,
      @RequestParam(defaultValue = "idCar") String carSort,
      @RequestParam(defaultValue = "desc") String carDirection,
      @RequestParam(defaultValue = "0") int bidPage,
      @RequestParam(defaultValue = "idBid") String bidSort,
      @RequestParam(defaultValue = "desc") String bidDirection) {
    var cars = adminService.listCar(PageRequest.of(
        Math.max(carPage, 0),
        5,
        Sort.by(sortDirection(carDirection), carSortProperty(carSort))));
    var bids = adminService.listCarBid(PageRequest.of(
        Math.max(bidPage, 0),
        5,
        Sort.by(sortDirection(bidDirection), bidSortProperty(bidSort))));
    return mapper.adminCarManagement(
        PageResponse.from(cars.map(mapper::auction)),
        PageResponse.from(bids.map(mapper::bid)));
  }

  @PostMapping("/cars/{idCar}/activate")
  public ApiMessageResponse activateCar(@PathVariable int idCar) {
    userCarService.changeCarStatusByAdmin(idCar, "ACTIVE");
    return new ApiMessageResponse("Auction activated.", null);
  }

  @PostMapping("/cars/{idCar}/deactivate")
  public ApiMessageResponse deactivateCar(@PathVariable int idCar) {
    userCarService.changeCarStatusByAdmin(idCar, "DEACTIVE");
    return new ApiMessageResponse("Auction deactivated.", null);
  }

  @PostMapping("/bids/{idBid}/approve")
  public ApiMessageResponse approveBid(@PathVariable int idBid) {
    adminService.approveCarBid(idBid);
    return new ApiMessageResponse("Bid approved.", null);
  }

  @PostMapping("/bids/{idBid}/deny")
  public ApiMessageResponse denyBid(@PathVariable int idBid) {
    adminService.denyCarBid(idBid);
    return new ApiMessageResponse("Bid denied.", null);
  }

  @GetMapping("/transactions")
  public AdminTransactionsResponse transactions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "paidAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    var transactions = paymentService.listAllPayments(PageRequest.of(
        Math.max(page, 0),
        10,
        Sort.by(sortDirection(direction), paymentSortProperty(sort))));
    var webhookEvents = paymentService.listWebhookEvents(
        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "processedAt")));
    return mapper.adminTransactions(
        PageResponse.from(transactions.map(mapper::payment)),
        webhookEvents.getContent());
  }

  private Sort.Direction sortDirection(String direction) {
    return "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
  }

  private String userSortProperty(String sort) {
    return switch (sort) {
      case "idUser", "username", "email", "profile.firstName", "profile.lastName" -> sort;
      default -> "idUser";
    };
  }

  private String carSortProperty(String sort) {
    return switch (sort) {
      case "idCar", "make", "model", "year", "price", "status", "auctionEndTime" -> sort;
      default -> "idCar";
    };
  }

  private String bidSortProperty(String sort) {
    return switch (sort) {
      case "idBid", "bidPrice", "status", "car.make", "car.year" -> sort;
      default -> "idBid";
    };
  }

  private String paymentSortProperty(String sort) {
    return switch (sort) {
      case "paidAt", "createdAt", "amountMinor", "status", "bid.car.make",
          "buyer.username", "seller.username" -> sort;
      default -> "paidAt";
    };
  }
}
