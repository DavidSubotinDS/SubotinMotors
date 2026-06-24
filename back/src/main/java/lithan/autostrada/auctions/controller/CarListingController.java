package lithan.autostrada.auctions.controller;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lithan.autostrada.auctions.dto.ListingTestRideRequest;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.service.CarListingService;
import lithan.autostrada.auctions.service.ListingDepositService;

@Controller
@RequestMapping("/listings")
public class CarListingController {

  private final CarListingService listingService;
  private final ListingDepositService depositService;

  public CarListingController(
      CarListingService listingService,
      ListingDepositService depositService) {
    this.listingService = listingService;
    this.depositService = depositService;
  }

  @GetMapping
  public String listings(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      @RequestParam(required = false) String keyword,
      Model model) {
    String safeSort = listingSort(sort);
    Sort.Direction safeDirection =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Page<CarListing> listingPage = listingService.browse(
        keyword,
        PageRequest.of(
            Math.max(page, 0),
            9,
            Sort.by(safeDirection, safeSort)));
    model.addAttribute("listingPage", listingPage);
    model.addAttribute("listings", listingPage.getContent());
    model.addAttribute("keyword", keyword);
    model.addAttribute("sort", safeSort);
    model.addAttribute("direction", safeDirection.name().toLowerCase());
    return "listings";
  }

  @GetMapping("/{listingId}")
  public String details(@PathVariable int listingId, Model model) {
    populateDetails(listingId, model);
    if (!model.containsAttribute("testRide")) {
      model.addAttribute("testRide", new ListingTestRideRequest());
    }
    return "listing-details";
  }

  @PostMapping("/{listingId}/test-rides")
  public String scheduleTestRide(
      @PathVariable int listingId,
      @Valid @ModelAttribute("testRide") ListingTestRideRequest request,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      populateDetails(listingId, model);
      return "listing-details";
    }
    try {
      listingService.scheduleTestRide(listingId, request.getScheduledAt());
      redirectAttributes.addFlashAttribute(
          "listingMessage", "Test ride request sent to the seller.");
    } catch (IllegalArgumentException | IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("listingError", exception.getMessage());
    }
    return "redirect:/listings/" + listingId;
  }

  private void populateDetails(int listingId, Model model) {
    model.addAttribute("listing", listingService.publicListing(listingId));
    model.addAttribute("stripeEnabled", depositService.isStripeEnabled());
  }

  private String listingSort(String sort) {
    return switch (sort) {
      case "createdAt", "priceMinor", "make", "model", "year", "mileage" -> sort;
      default -> "createdAt";
    };
  }
}
