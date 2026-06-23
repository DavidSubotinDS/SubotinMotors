package lithan.abc.cars.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lithan.abc.cars.service.ListingDepositService;

@Controller
public class ListingDepositController {

  private final ListingDepositService depositService;

  public ListingDepositController(ListingDepositService depositService) {
    this.depositService = depositService;
  }

  @GetMapping("/listing-deposits/success")
  public String success(@RequestParam("session_id") String sessionId, Model model) {
    model.addAttribute("deposit", depositService.currentUserDepositBySession(sessionId));
    return "listing-deposit-success";
  }
}
