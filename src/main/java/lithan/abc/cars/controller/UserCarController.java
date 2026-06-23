package lithan.abc.cars.controller;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.TestDrive;
import lithan.abc.cars.service.UserCarService;
import lithan.abc.cars.service.UserService;
import lithan.abc.cars.service.CarListingService;

@Controller
@RequestMapping("/user")
public class UserCarController {

  @Autowired
  private UserCarService userCarService;

  @Autowired
  private UserService userService;

  @Autowired
  private CarListingService carListingService;

  // User Posted Car
  @GetMapping("/my-posted-car")
  public String myPostedCarPage(Model model) {
    List<Car> userCar = userCarService.listUserCar();

    model.addAttribute("userCar", userCar);

    return "user/my-posted-car";
  }

  // Posting Car
  @GetMapping("/post-car")
  public String postCar(Model model) {
    Car car = new Car();

    model.addAttribute("car", car);

    return "user/post-car";
  }

  @PostMapping("/postCarProcess")
  public String postCarProcess(@Valid @ModelAttribute("car") Car car, BindingResult bindingResult,
      @RequestParam("imageFile") MultipartFile file, Model model) {

    validateAuctionEndTime(car, bindingResult);
    if (bindingResult.hasErrors()) {
      return "user/post-car";
    }

    if (file.isEmpty()) {
      model.addAttribute("fileError", "Car Picture is required");
      return "user/post-car";
    }

    try {
      userCarService.postCar(file, car);
    } catch (Exception e) {
      model.addAttribute("fileError", e.getMessage());
      return "user/post-car";
    }

    return "redirect:/user/my-posted-car";
  }

  // Edit Posted Car
  @GetMapping("/edit-posted-car")
  public String editPostedCar(@RequestParam("id") int id, Model model) {
    model.addAttribute("car", userCarService.getOwnedCarById(id));
    return "user/edit-posted-car";
  }

  @PostMapping("/editCarProcess")
  public String saveEditCar(@Valid @ModelAttribute("car") Car car, BindingResult bindingResult) {
    validateAuctionEndTime(car, bindingResult);
    if (bindingResult.hasErrors()) {
      return "user/edit-posted-car";
    }

    Car savedCar = userCarService.editOwnedCar(car);

    return "redirect:/cars/" + savedCar.getMake() + "/" + savedCar.getModel() + "/" + savedCar.getYear() + "/" + savedCar.getIdCar();
  }

  // Activate & Deactivate Posted Car
  @PostMapping("/activate/{idCar}")
  public String activatePostedCar(@PathVariable("idCar") int id) {

    userCarService.changeOwnedCarStatus(id, "ACTIVE");

    return "redirect:/user/my-posted-car";
  }

  @PostMapping("/deactivate/{idCar}")
  public String deactivatePostedCar(@PathVariable("idCar") int id) {

    userCarService.changeOwnedCarStatus(id, "DEACTIVE");

    return "redirect:/user/my-posted-car";
  }

  // List Test Drive
  @GetMapping("/test-drive")
  public String listTestDrive(Model model) {
    List<TestDrive> receivedTestDrives = userCarService.listTestDriveForOwnedCars();
    List<TestDrive> bookedTestDrives = userCarService.listCurrentUserTestDrives();

    model.addAttribute("receivedTestDrives", receivedTestDrives);
    model.addAttribute("bookedTestDrives", bookedTestDrives);
    model.addAttribute(
        "listingTestRideRequests",
        carListingService.testRideRequestsForCurrentSeller());
    model.addAttribute(
        "listingTestRides",
        carListingService.currentUserTestRides());

    return "user/list-test-drive";
  }

  @PostMapping("/listing-test-rides/{idTestRide}/reschedule")
  public String rescheduleListingTestRide(
      @PathVariable int idTestRide,
      @RequestParam("scheduledAt")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
      RedirectAttributes redirectAttributes) {
    try {
      carListingService.rescheduleCurrentUserTestRide(idTestRide, scheduledAt);
      redirectAttributes.addFlashAttribute("appointmentMessage", "Test ride rescheduled.");
    } catch (IllegalArgumentException | IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("appointmentError", exception.getMessage());
    }
    return "redirect:/user/test-drive";
  }

  @PostMapping("/listing-test-rides/{idTestRide}/cancel")
  public String cancelListingTestRide(
      @PathVariable int idTestRide,
      RedirectAttributes redirectAttributes) {
    carListingService.cancelCurrentUserTestRide(idTestRide);
    redirectAttributes.addFlashAttribute("appointmentMessage", "Test ride cancelled.");
    return "redirect:/user/test-drive";
  }

  @PostMapping("/listing-test-rides/{idTestRide}/accept")
  public String acceptListingTestRide(
      @PathVariable int idTestRide,
      RedirectAttributes redirectAttributes) {
    carListingService.acceptTestRideForOwnedListing(idTestRide);
    redirectAttributes.addFlashAttribute("appointmentMessage", "Test ride request accepted.");
    return "redirect:/user/test-drive";
  }

  @PostMapping("/listing-test-rides/{idTestRide}/reject")
  public String rejectListingTestRide(
      @PathVariable int idTestRide,
      RedirectAttributes redirectAttributes) {
    carListingService.rejectTestRideForOwnedListing(idTestRide);
    redirectAttributes.addFlashAttribute("appointmentMessage", "Test ride request rejected.");
    return "redirect:/user/test-drive";
  }

  @PostMapping("/listing-test-rides/{idTestRide}/owner-cancel")
  public String cancelOwnedListingTestRide(
      @PathVariable int idTestRide,
      RedirectAttributes redirectAttributes) {
    carListingService.cancelTestRideForOwnedListing(idTestRide);
    redirectAttributes.addFlashAttribute("appointmentMessage", "Accepted test ride cancelled.");
    return "redirect:/user/test-drive";
  }

  @PostMapping("/test-drives/{idTestDrive}/reschedule")
  public String rescheduleTestDrive(
      @PathVariable int idTestDrive,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      RedirectAttributes redirectAttributes) {
    try {
      userCarService.rescheduleCurrentUserTestDrive(idTestDrive, date);
      redirectAttributes.addFlashAttribute("appointmentMessage", "Test drive rescheduled.");
    } catch (IllegalArgumentException | IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("appointmentError", exception.getMessage());
    }
    return "redirect:/user/test-drive";
  }

  @PostMapping("/test-drives/{idTestDrive}/cancel")
  public String cancelTestDrive(
      @PathVariable int idTestDrive,
      RedirectAttributes redirectAttributes) {
    try {
      userCarService.cancelCurrentUserTestDrive(idTestDrive);
      redirectAttributes.addFlashAttribute("appointmentMessage", "Test drive cancelled.");
    } catch (IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("appointmentError", exception.getMessage());
    }
    return "redirect:/user/test-drive";
  }

  @PostMapping("/test-drives/{idTestDrive}/accept")
  public String acceptTestDrive(
      @PathVariable int idTestDrive,
      RedirectAttributes redirectAttributes) {
    try {
      userCarService.acceptTestDriveForOwnedCar(idTestDrive);
      redirectAttributes.addFlashAttribute("appointmentMessage", "Test-drive request accepted.");
    } catch (IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("appointmentError", exception.getMessage());
    }
    return "redirect:/user/test-drive";
  }

  @PostMapping("/test-drives/{idTestDrive}/reject")
  public String rejectTestDrive(
      @PathVariable int idTestDrive,
      RedirectAttributes redirectAttributes) {
    try {
      userCarService.rejectTestDriveForOwnedCar(idTestDrive);
      redirectAttributes.addFlashAttribute("appointmentMessage", "Test-drive request rejected.");
    } catch (IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("appointmentError", exception.getMessage());
    }
    return "redirect:/user/test-drive";
  }

  @PostMapping("/test-drives/{idTestDrive}/owner-cancel")
  public String cancelOwnedCarTestDrive(
      @PathVariable int idTestDrive,
      RedirectAttributes redirectAttributes) {
    try {
      userCarService.cancelTestDriveForOwnedCar(idTestDrive);
      redirectAttributes.addFlashAttribute("appointmentMessage", "Accepted test drive cancelled.");
    } catch (IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("appointmentError", exception.getMessage());
    }
    return "redirect:/user/test-drive";
  }

  @GetMapping("/bids")
  public String listBids(Model model) {
    List<CarBidding> bids = userCarService.listCurrentUserBids();
    model.addAttribute("bids", bids);
    return "user/bids";
  }

  @PostMapping("/bids/{idBid}/cancel")
  public String cancelBid(@PathVariable int idBid, RedirectAttributes redirectAttributes) {
    try {
      userCarService.cancelCurrentUserBid(idBid);
      redirectAttributes.addFlashAttribute("bidMessage", "Bid cancelled.");
    } catch (IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("bidError", exception.getMessage());
    }
    return "redirect:/user/bids";
  }

  // Upload Car Picture
  @GetMapping("/upload-car-picture")
  public String uploadPicture(@RequestParam("idCar") int idCar, Model model) {
    userCarService.getOwnedCarById(idCar);
    model.addAttribute("idCar", idCar);
    return "user/upload-car-picture";
  }

  @PostMapping("/uploadCarPicture")
  public String uploadCarImage(@RequestParam("imageFile") MultipartFile imageFile,
      @RequestParam("idCar") int idCar, Model model) {
    try {
      userCarService.saveUploadPicture(imageFile, idCar);
      return "redirect:/user/my-posted-car";
    } catch (Exception exception) {
      model.addAttribute("idCar", idCar);
      model.addAttribute("message", exception.getMessage());
      return "user/upload-car-picture";
    }
  }

  private void validateAuctionEndTime(Car car, BindingResult bindingResult) {
    if (car.getAuctionEndTime() == null
        || !car.getAuctionEndTime().isAfter(LocalDateTime.now())) {
      bindingResult.rejectValue(
          "auctionEndTime",
          "future",
          "Auction end date and time must be in the future");
    }
  }

}
