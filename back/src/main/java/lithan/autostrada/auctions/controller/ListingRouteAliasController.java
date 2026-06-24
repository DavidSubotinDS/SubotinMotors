package lithan.autostrada.auctions.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.service.CarService;

@Controller
public class ListingRouteAliasController {

  private final CarService carService;

  public ListingRouteAliasController(CarService carService) {
    this.carService = carService;
  }

  @GetMapping({"/auctions", "/live-auctions", "/browse-auctions", "/auction-listings"})
  public String auctionBrowseAliases() {
    return "forward:/cars";
  }

  @GetMapping({"/auction/{carId:\\d+}", "/auctions/{carId:\\d+}", "/cars/{carId:\\d+}"})
  public String auctionDetailsAliases(@PathVariable int carId) {
    Car car = carService.getCarById(carId);
    return "redirect:/cars/" + car.getMake() + "/" + car.getModel() + "/"
        + car.getYear() + "/" + car.getIdCar();
  }

  @GetMapping({"/car-listings", "/cars-for-sale", "/vehicle-listings"})
  public String fixedPriceListingAliases() {
    return "forward:/listings";
  }

  @GetMapping({"/car-listings/{listingId:\\d+}", "/cars-for-sale/{listingId:\\d+}"})
  public String fixedPriceListingDetailsAliases(@PathVariable int listingId) {
    return "redirect:/listings/" + listingId;
  }

  @GetMapping({"/store", "/store/parts", "/car-parts", "/parts-store"})
  public String partsStoreAliases() {
    return "forward:/parts";
  }

  @GetMapping({"/store/parts/{idPart:\\d+}", "/car-parts/{idPart:\\d+}"})
  public String partDetailsAliases(@PathVariable int idPart) {
    return "redirect:/parts/" + idPart;
  }

  @GetMapping({"/user/my-posted-cars", "/user/my-auctions", "/user/auction-listings"})
  public String userAuctionAliases() {
    return "forward:/user/my-posted-car";
  }

  @GetMapping({"/user/my-listings", "/user/car-listings"})
  public String userFixedPriceListingAliases() {
    return "forward:/user/listings";
  }

  @GetMapping({"/user/favorites", "/user/watchlist"})
  public String userFollowedAuctionAliases() {
    return "forward:/user/followed-auctions";
  }

  @GetMapping({"/user/appointments", "/user/test-rides"})
  public String userAppointmentAliases() {
    return "forward:/user/test-drive";
  }

  @GetMapping("/user/deposits")
  public String userDepositAliases() {
    return "forward:/user/listing-deposits";
  }

  @GetMapping("/user/orders")
  public String userOrderAliases() {
    return "forward:/orders";
  }

  @GetMapping("/admin/users")
  public String adminUserListAlias() {
    return "forward:/admin/dashboard";
  }

  @GetMapping({"/admin/auctions", "/admin/listings"})
  public String adminAuctionListAlias() {
    return "forward:/admin/car-management";
  }

  @GetMapping({"/admin/store", "/admin/parts"})
  public String adminPartsAliases() {
    return "forward:/admin/store/parts";
  }

  @GetMapping("/admin/orders")
  public String adminOrderAliases() {
    return "forward:/admin/store/orders";
  }
}
