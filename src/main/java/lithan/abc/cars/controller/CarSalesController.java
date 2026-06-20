package lithan.abc.cars.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.TestDrive;
import lithan.abc.cars.service.UserCarService;

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
      @RequestParam("bidPrice") int bidPrice, Model model) {
    try {
      userCarService.placeBid(carId, bidPrice);
      Car car = userCarService.getCarById(carId);
      return "redirect:/cars/" + car.getMake() + "/" + car.getModel() + "/" + car.getYear() + "/" + car.getIdCar();
    } catch (IllegalArgumentException | IllegalStateException exception) {
      CarBidding form = new CarBidding();
      form.setBidPrice(bidPrice);
      populateBidModel(carId, form, model);
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
    model.addAttribute("testDrive", new TestDrive());
    model.addAttribute("car", userCarService.getCarById(idCar));
    return "user/test-drive";
  }

  @PostMapping("/test-drive/testDriveProcess")
  public String saveTestDrive(@RequestParam("carId") int carId,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      Model model) {
    try {
      userCarService.saveTestDrive(date, carId);
      return "redirect:/";
    } catch (IllegalArgumentException | IllegalStateException exception) {
      TestDrive form = new TestDrive();
      form.setDate(date);
      model.addAttribute("testDrive", form);
      model.addAttribute("car", userCarService.getCarById(carId));
      model.addAttribute("message", exception.getMessage());
      return "user/test-drive";
    }
  }
}
