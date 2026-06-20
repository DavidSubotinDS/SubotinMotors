package lithan.abc.cars.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.service.PaymentService;

@Controller
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @GetMapping("/user/payments")
  public String paymentDashboard(
      @RequestParam(defaultValue = "0") int purchasePage,
      @RequestParam(defaultValue = "createdAt") String purchaseSort,
      @RequestParam(defaultValue = "desc") String purchaseDirection,
      @RequestParam(defaultValue = "0") int salePage,
      @RequestParam(defaultValue = "createdAt") String saleSort,
      @RequestParam(defaultValue = "desc") String saleDirection,
      Model model) {
    String safePurchaseSort = paymentSortProperty(purchaseSort);
    String safeSaleSort = paymentSortProperty(saleSort);
    Sort.Direction safePurchaseDirection = sortDirection(purchaseDirection);
    Sort.Direction safeSaleDirection = sortDirection(saleDirection);
    Page<PaymentOrder> purchases = paymentService.listCurrentUserPurchases(
        PageRequest.of(Math.max(purchasePage, 0), 5,
            Sort.by(safePurchaseDirection, safePurchaseSort)));
    Page<PaymentOrder> sales = paymentService.listCurrentUserSales(
        PageRequest.of(Math.max(salePage, 0), 5,
            Sort.by(safeSaleDirection, safeSaleSort)));

    model.addAttribute("purchasePage", purchases);
    model.addAttribute("purchases", purchases.getContent());
    model.addAttribute("purchaseSort", safePurchaseSort);
    model.addAttribute("purchaseDirection", safePurchaseDirection.name().toLowerCase());
    model.addAttribute("salePage", sales);
    model.addAttribute("sales", sales.getContent());
    model.addAttribute("saleSort", safeSaleSort);
    model.addAttribute("saleDirection", safeSaleDirection.name().toLowerCase());
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

  private Sort.Direction sortDirection(String direction) {
    return "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
  }

  private String paymentSortProperty(String sort) {
    return switch (sort) {
      case "createdAt", "amountMinor", "status", "bid.car.make" -> sort;
      default -> "createdAt";
    };
  }
}
