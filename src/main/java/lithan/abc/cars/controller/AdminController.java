package lithan.abc.cars.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.entity.UserProfile;
import lithan.abc.cars.service.AdminService;
import lithan.abc.cars.service.PaymentService;
import lithan.abc.cars.service.UserCarService;
import lithan.abc.cars.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {

  @Autowired
  private AdminService adminService;

  @Autowired
  private UserCarService userCarService;

  @Autowired
  private UserService userService;

  @Autowired
  private PaymentService paymentService;

  @GetMapping("")
  public String admin() {

    return "redirect:/admin/dashboard";
  }

  @GetMapping("/dashboard")
  public String dashboard(
      @RequestParam(defaultValue = "0") int userPage,
      @RequestParam(defaultValue = "idUser") String userSort,
      @RequestParam(defaultValue = "asc") String userDirection,
      @RequestParam(defaultValue = "0") int adminPage,
      @RequestParam(defaultValue = "idUser") String adminSort,
      @RequestParam(defaultValue = "asc") String adminDirection,
      Model model,
      HttpSession session) {
    String safeUserSort = userSortProperty(userSort);
    String safeAdminSort = userSortProperty(adminSort);
    Sort.Direction safeUserDirection = sortDirection(userDirection);
    Sort.Direction safeAdminDirection = sortDirection(adminDirection);
    Page<UserAccount> users = adminService.listUser(
        PageRequest.of(Math.max(userPage, 0), 5, Sort.by(safeUserDirection, safeUserSort)));
    Page<UserAccount> admins = adminService.listAdmin(
        PageRequest.of(Math.max(adminPage, 0), 5, Sort.by(safeAdminDirection, safeAdminSort)));

    model.addAttribute("userPage", users);
    model.addAttribute("listUser", users.getContent());
    model.addAttribute("userSort", safeUserSort);
    model.addAttribute("userDirection", safeUserDirection.name().toLowerCase());
    model.addAttribute("adminPage", admins);
    model.addAttribute("listAdmin", admins.getContent());
    model.addAttribute("adminSort", safeAdminSort);
    model.addAttribute("adminDirection", safeAdminDirection.name().toLowerCase());

    UserAccount user = userService.getUserLogin();
    UserProfile profile = user.getProfile();
    session.setAttribute("profileLog", profile);

    return "admin/dashboard";
  }

  // Edit User
  @GetMapping("/edit-user")
  public String editUser(@RequestParam("id") int id, Model model) {
    UserProfile profile = adminService.getProfileById(id);

    model.addAttribute("profile", profile);

    return "admin/edit-user";
  }

  @PostMapping("/editProfileProcess")
  public String saveEditUser(@Valid @ModelAttribute("profile") UserProfile profile, BindingResult bindingResult) {

    if (bindingResult.hasErrors()) {
      return "admin/edit-user";
    }

    adminService.editUser(profile);

    return "redirect:/admin/dashboard";
  }

  // Mark Admin
  @PostMapping("/mark-admin/{idUser}")
  public String markAdmin(@PathVariable("idUser") int id) {
    adminService.markAsAdmin(id);

    return "redirect:/admin/dashboard";
  }

  // Car Management
  @GetMapping("/car-management")
  public String carManagement(
      @RequestParam(defaultValue = "0") int carPage,
      @RequestParam(defaultValue = "idCar") String carSort,
      @RequestParam(defaultValue = "desc") String carDirection,
      @RequestParam(defaultValue = "0") int bidPage,
      @RequestParam(defaultValue = "idBid") String bidSort,
      @RequestParam(defaultValue = "desc") String bidDirection,
      Model model) {
    String safeCarSort = carSortProperty(carSort);
    String safeBidSort = bidSortProperty(bidSort);
    Sort.Direction safeCarDirection = sortDirection(carDirection);
    Sort.Direction safeBidDirection = sortDirection(bidDirection);
    Page<Car> cars = adminService.listCar(
        PageRequest.of(Math.max(carPage, 0), 5, Sort.by(safeCarDirection, safeCarSort)));
    Page<CarBidding> bids = adminService.listCarBid(
        PageRequest.of(Math.max(bidPage, 0), 5, Sort.by(safeBidDirection, safeBidSort)));

    model.addAttribute("carPage", cars);
    model.addAttribute("listCar", cars.getContent());
    model.addAttribute("carSort", safeCarSort);
    model.addAttribute("carDirection", safeCarDirection.name().toLowerCase());
    model.addAttribute("bidPage", bids);
    model.addAttribute("listCarBid", bids.getContent());
    model.addAttribute("bidSort", safeBidSort);
    model.addAttribute("bidDirection", safeBidDirection.name().toLowerCase());

    return "admin/car-management";
  }

  // DEACTIVATE CAR POST
  @PostMapping("/deactivate/{idCar}")
  public String deactivateCarPost(@PathVariable("idCar") int id) {
    userCarService.changeCarStatusByAdmin(id, "DEACTIVE");

    return "redirect:/admin/car-management";
  }

  // ACTIVATE CAR POST
  @PostMapping("/activate/{idCar}")
  public String activateCarPost(@PathVariable("idCar") int id) {
    userCarService.changeCarStatusByAdmin(id, "ACTIVE");

    return "redirect:/admin/car-management";
  }

  // APPROVE BID CAR
  @PostMapping("/approve-bid/{idBid}")
  public String approveBidCarPost(@PathVariable("idBid") int id) {
    adminService.approveCarBid(id);

    return "redirect:/admin/car-management";
  }

  // DENY BID CAR
  @PostMapping("/deny-bid/{idBid}")
  public String denyBidCarPost(@PathVariable("idBid") int id) {
    adminService.denyCarBid(id);

    return "redirect:/admin/car-management";
  }

  @GetMapping("/transactions")
  public String transactions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "paidAt") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      Model model) {
    String safeSort = paymentSortProperty(sort);
    Sort.Direction safeDirection = sortDirection(direction);
    Page<PaymentOrder> transactions = paymentService.listAllPayments(
        PageRequest.of(Math.max(page, 0), 10, Sort.by(safeDirection, safeSort)));

    model.addAttribute("transactionPage", transactions);
    model.addAttribute("transactions", transactions.getContent());
    model.addAttribute("sort", safeSort);
    model.addAttribute("direction", safeDirection.name().toLowerCase());

    return "admin/transactions";
  }

  private Sort.Direction sortDirection(String direction) {
    return "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
  }

  private String userSortProperty(String sort) {
    return switch (sort) {
      case "idUser", "username", "profile.firstName", "profile.lastName" -> sort;
      default -> "idUser";
    };
  }

  private String carSortProperty(String sort) {
    return switch (sort) {
      case "idCar", "make", "model", "year", "price", "status" -> sort;
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
