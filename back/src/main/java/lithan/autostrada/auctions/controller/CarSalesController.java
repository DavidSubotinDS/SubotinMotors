package lithan.autostrada.auctions.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import lithan.autostrada.auctions.dto.TestDriveRequest;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.service.UserCarService;

@Controller
public class CarSalesController {

  @Autowired
  private UserCarService userCarService;

  @GetMapping("/car-bid")
  public String postBidding(@RequestParam("id") int id, Model model) {
    populateBidModel(id, new CarBidding(), model);
    return "user/car-bid";
  }

  @PostMapping("/postCarBidding")
  public String saveCarBidding(@RequestParam("carId") int carId,
      @Valid @ModelAttribute("carBidding") CarBidding carBidding,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      populateBidModel(carId, carBidding, model);
      return "user/car-bid";
    }
    try {
      userCarService.placeBid(carId, carBidding.getBidPrice());
      Car car = userCarService.getCarById(carId);
      return "redirect:/cars/" + car.getMake() + "/" + car.getModel() + "/" + car.getYear() + "/" + car.getIdCar();
    } catch (IllegalArgumentException | IllegalStateException exception) {
      populateBidModel(carId, carBidding, model);
      model.addAttribute("message", exception.getMessage());
      return "user/car-bid";
    }
  }

  private void populateBidModel(int carId, CarBidding form, Model model) {
    model.addAttribute("car", userCarService.getCarById(carId));
    model.addAttribute("carBidding", form);
    model.addAttribute("highestBidding", userCarService.highestBidding(carId));
  }

  @GetMapping("/test-drive/{idCar}")
  public String testDrive(@PathVariable("idCar") int idCar, Model model) {
    model.addAttribute("testDrive", new TestDriveRequest());
    model.addAttribute("car", userCarService.getCarById(idCar));
    return "user/test-drive";
  }

  @PostMapping("/test-drive/testDriveProcess")
  public String saveTestDrive(@RequestParam("carId") int carId,
      @Valid @ModelAttribute("testDrive") TestDriveRequest testDrive,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("car", userCarService.getCarById(carId));
      return "user/test-drive";
    }
    try {
      userCarService.saveTestDrive(testDrive.getDate(), carId);
      return "redirect:/";
    } catch (IllegalArgumentException | IllegalStateException exception) {
      model.addAttribute("car", userCarService.getCarById(carId));
      model.addAttribute("message", exception.getMessage());
      return "user/test-drive";
    }
  }
}
