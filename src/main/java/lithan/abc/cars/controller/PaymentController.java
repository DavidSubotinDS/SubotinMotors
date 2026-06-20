package lithan.abc.cars.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lithan.abc.cars.service.PaymentService;

@Controller
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @GetMapping("/user/payments")
  public String paymentDashboard(Model model) {
    model.addAttribute("purchases", paymentService.listCurrentUserPurchases());
    model.addAttribute("sales", paymentService.listCurrentUserSales());
    model.addAttribute("stripeEnabled", paymentService.isStripeEnabled());
    model.addAttribute("paymentAccount", paymentService.getCurrentSellerAccount().orElse(null));
    return "user/payments";
  }

  @GetMapping("/payments/seller/onboarding")
  public String sellerOnboarding() {
    return "redirect:" + paymentService.startSellerOnboarding();
  }

  @GetMapping("/payments/seller/return")
  public String sellerOnboardingReturn(RedirectAttributes redirectAttributes) {
    boolean ready = paymentService.refreshCurrentSellerAccount().isTransfersEnabled();
    redirectAttributes.addFlashAttribute(
        ready ? "paymentMessage" : "paymentWarning",
        ready ? "Your Stripe payout account is ready." : "Stripe still needs additional payout information.");
    return "redirect:/user/payments";
  }

  @PostMapping("/payments/{paymentId}/checkout")
  public String checkout(@PathVariable int paymentId) {
    return "redirect:" + paymentService.createBuyerCheckout(paymentId);
  }

  @GetMapping("/payments/success")
  public String paymentSuccess(@RequestParam("session_id") String sessionId, Model model) {
    model.addAttribute("payment", paymentService.findCurrentBuyerPaymentBySession(sessionId).orElse(null));
    return "user/payment-success";
  }
}
