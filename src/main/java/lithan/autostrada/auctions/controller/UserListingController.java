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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lithan.autostrada.auctions.dto.CarListingForm;
import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.service.CarListingService;
import lithan.autostrada.auctions.service.ListingDepositService;

@Controller
@RequestMapping("/user")
public class UserListingController {

  private final CarListingService listingService;
  private final ListingDepositService depositService;

  public UserListingController(
      CarListingService listingService,
      ListingDepositService depositService) {
    this.listingService = listingService;
    this.depositService = depositService;
  }

  @GetMapping("/listings")
  public String myListings(Model model) {
    model.addAttribute("listings", listingService.currentUserListings());
    return "user/my-listings";
  }

  @GetMapping("/listings/new")
  public String newListing(Model model) {
    model.addAttribute("listingForm", new CarListingForm());
    return "user/listing-form";
  }

  @PostMapping("/listings")
  public String create(
      @Valid @ModelAttribute("listingForm") CarListingForm form,
      BindingResult bindingResult,
      @RequestParam("imageFile") MultipartFile image,
      Model model) {
    if (bindingResult.hasErrors()) {
      return "user/listing-form";
    }
    try {
      return "redirect:/listings/" + listingService.create(form, image).getIdListing();
    } catch (IllegalArgumentException exception) {
      model.addAttribute("fileError", exception.getMessage());
      return "user/listing-form";
    }
  }

  @GetMapping("/listings/{listingId}/edit")
  public String edit(@PathVariable int listingId, Model model) {
    model.addAttribute(
        "listingForm",
        CarListingForm.from(listingService.ownedListing(listingId)));
    model.addAttribute("editing", true);
    return "user/listing-form";
  }

  @PostMapping("/listings/{listingId}")
  public String update(
      @PathVariable int listingId,
      @Valid @ModelAttribute("listingForm") CarListingForm form,
      BindingResult bindingResult,
      @RequestParam("imageFile") MultipartFile image,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("editing", true);
      return "user/listing-form";
    }
    try {
      listingService.update(listingId, form, image);
      return "redirect:/listings/" + listingId;
    } catch (IllegalArgumentException exception) {
      model.addAttribute("editing", true);
      model.addAttribute("fileError", exception.getMessage());
      return "user/listing-form";
    }
  }

  @PostMapping("/listings/{listingId}/activate")
  public String activate(
      @PathVariable int listingId,
      RedirectAttributes redirectAttributes) {
    listingService.activate(listingId);
    redirectAttributes.addFlashAttribute("listingMessage", "Listing activated.");
    return "redirect:/user/listings";
  }

  @PostMapping("/listings/{listingId}/deactivate")
  public String deactivate(
      @PathVariable int listingId,
      RedirectAttributes redirectAttributes) {
    listingService.deactivate(listingId);
    redirectAttributes.addFlashAttribute("listingMessage", "Listing deactivated.");
    return "redirect:/user/listings";
  }

  @PostMapping("/listings/{listingId}/deposit")
  public String startDeposit(
      @PathVariable int listingId,
      RedirectAttributes redirectAttributes) {
    try {
      return "redirect:" + depositService.startCheckout(listingId);
    } catch (IllegalArgumentException | IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("listingError", exception.getMessage());
      return "redirect:/listings/" + listingId;
    }
  }

  @GetMapping("/listing-deposits")
  public String deposits(
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    Page<ListingDeposit> deposits = depositService.currentUserDeposits(
        PageRequest.of(
            Math.max(page, 0),
            10,
            Sort.by(Sort.Direction.DESC, "createdAt")));
    model.addAttribute("depositPage", deposits);
    model.addAttribute("deposits", deposits.getContent());
    return "user/listing-deposits";
  }
}
