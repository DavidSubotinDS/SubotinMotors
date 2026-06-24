package lithan.autostrada.auctions.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentController {

  @GetMapping("/user/payments")
  public String paymentDashboard() {
    return "redirect:/orders";
  }

  @GetMapping("/payments/seller/onboarding")
  public String sellerOnboarding(RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute(
        "storeMessage",
        "Seller payout onboarding has been retired. Stripe Checkout is now used for the parts store.");
    return "redirect:/parts";
  }

  @GetMapping("/payments/seller/return")
  public String sellerOnboardingReturn(RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute(
        "storeMessage",
        "Seller payout onboarding has been retired. Stripe Checkout is now used for the parts store.");
    return "redirect:/parts";
  }

  @PostMapping("/payments/{paymentId}/checkout")
  public String checkout(
      @PathVariable int paymentId,
      RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute(
        "storeMessage",
        "Auction checkout has been retired. Online payments are demonstrated through the parts store.");
    return "redirect:/orders";
  }

  @GetMapping("/payments/success")
  public String paymentSuccess() {
    return "redirect:/orders";
  }

}
