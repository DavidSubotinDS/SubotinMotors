package lithan.autostrada.auctions.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import lithan.autostrada.auctions.dto.CarSearchCriteria;
import lithan.autostrada.auctions.dto.ListingCommentForm;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.service.CarService;
import lithan.autostrada.auctions.service.UserCarService;
import lithan.autostrada.auctions.service.AuctionFollowService;
import lithan.autostrada.auctions.service.ListingCommentService;

@Controller
@RequestMapping("/cars")
public class CarController {

  @Autowired
  private CarService carService;

  @Autowired
  private UserCarService userCarService;

  @Autowired
  private AuctionFollowService auctionFollowService;

  @Autowired
  private ListingCommentService listingCommentService;

  @GetMapping("")
  public String carPage(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "8") int size,
      @RequestParam(defaultValue = "idCar") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      @Valid @ModelAttribute("search") CarSearchCriteria search,
      BindingResult bindingResult,
      Model model) {
    String sortProperty = catalogSortProperty(sort);
    Sort.Direction sortDirection = sortDirection(direction);
    PageRequest pageRequest = PageRequest.of(
        Math.max(page, 0), pageSize(size), Sort.by(sortDirection, sortProperty));
    Page<Car> carPage;
    if (bindingResult.hasErrors()) {
      carPage = Page.empty(pageRequest);
      model.addAttribute("searchError", bindingResult.getAllErrors().get(0).getDefaultMessage());
    } else {
      carPage = carService.findCatalogCars(
          search.getKeyword(), search.getLow(), search.getHigh(), pageRequest);
    }

    model.addAttribute("carPage", carPage);
    model.addAttribute("listCar", carPage.getContent());
    model.addAttribute("keyword", search.getKeyword());
    model.addAttribute("low", search.getLow());
    model.addAttribute("high", search.getHigh());
    model.addAttribute("sort", sortProperty);
    model.addAttribute("direction", sortDirection.name().toLowerCase());
    return "cars";
  }

  // Car Details
  @GetMapping("/{make}/{model}/{year}/{id_car}")
  public String carDetails(@PathVariable("id_car") int id, Model model) {
    Car car = carService.getCarById(id);
    if ("DEACTIVE".equals(car.getStatus()) || "PENDING".equals(car.getStatus())) {
      throw new ResourceNotFoundException();
    }

    int higestBidding = userCarService.highestBidding(id);

    if (higestBidding == 0) {
    }

    model.addAttribute("car", car);
    model.addAttribute("highestBidding", higestBidding);
    model.addAttribute("following", auctionFollowService.isFollowingCurrentUser(car));
    model.addAttribute("comments", listingCommentService.commentsForCar(car));
    model.addAttribute("commentForm", new ListingCommentForm());

    return "car-details";
  }

  private int pageSize(int size) {
    return Math.min(Math.max(size, 1), 24);
  }

  private Sort.Direction sortDirection(String direction) {
    return "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
  }

  private String catalogSortProperty(String sort) {
    return switch (sort) {
      case "make", "model", "year", "price", "idCar", "auctionEndTime", "status" -> sort;
      default -> "idCar";
    };
  }
}
