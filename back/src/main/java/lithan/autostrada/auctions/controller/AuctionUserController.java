package lithan.autostrada.auctions.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.service.AuctionFollowService;
import lithan.autostrada.auctions.service.AuctionNotificationService;
import lithan.autostrada.auctions.service.CarService;

@Controller
@RequestMapping("/user")
public class AuctionUserController {

  private final AuctionFollowService followService;
  private final AuctionNotificationService notificationService;
  private final CarService carService;

  public AuctionUserController(
      AuctionFollowService followService,
      AuctionNotificationService notificationService,
      CarService carService) {
    this.followService = followService;
    this.notificationService = notificationService;
    this.carService = carService;
  }

  @GetMapping("/followed-auctions")
  public String followedAuctions(Model model) {
    model.addAttribute("follows", followService.listCurrentUserFollows());
    return "user/followed-auctions";
  }

  @PostMapping("/auctions/{carId}/follow")
  public String follow(
      @PathVariable int carId,
      @RequestParam(defaultValue = "details") String returnTo,
      RedirectAttributes redirectAttributes) {
    boolean created = followService.follow(carId);
    redirectAttributes.addFlashAttribute(
        "followMessage", created ? "Auction added to your watchlist." : "You already follow this auction.");
    return redirectAfterFollowAction(carId, returnTo);
  }

  @PostMapping("/auctions/{carId}/unfollow")
  public String unfollow(
      @PathVariable int carId,
      @RequestParam(defaultValue = "details") String returnTo,
      RedirectAttributes redirectAttributes) {
    followService.unfollow(carId);
    redirectAttributes.addFlashAttribute("followMessage", "Auction removed from your watchlist.");
    return redirectAfterFollowAction(carId, returnTo);
  }

  @GetMapping("/notifications")
  public String notifications(Model model) {
    model.addAttribute(
        "notifications", notificationService.listCurrentUserNotifications());
    return "user/notifications";
  }

  @PostMapping("/notifications/{notificationId}/read")
  public String markRead(@PathVariable int notificationId) {
    notificationService.markRead(notificationId);
    return "redirect:/user/notifications";
  }

  @PostMapping("/notifications/read-all")
  public String markAllRead() {
    notificationService.markAllRead();
    return "redirect:/user/notifications";
  }

  private String redirectAfterFollowAction(int carId, String returnTo) {
    if ("followed".equals(returnTo)) {
      return "redirect:/user/followed-auctions";
    }
    Car car = carService.getCarById(carId);
    return "redirect:/cars/" + car.getMake() + "/" + car.getModel() + "/"
        + car.getYear() + "/" + car.getIdCar();
  }
}
