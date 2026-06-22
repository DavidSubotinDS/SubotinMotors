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

import lithan.abc.cars.entity.CarPart;
import lithan.abc.cars.entity.StoreOrder;
import lithan.abc.cars.service.CarPartService;
import lithan.abc.cars.service.CartService;
import lithan.abc.cars.service.StoreOrderService;

@Controller
public class PartsStoreController {

  private final CarPartService partService;
  private final CartService cartService;
  private final StoreOrderService orderService;

  public PartsStoreController(
      CarPartService partService,
      CartService cartService,
      StoreOrderService orderService) {
    this.partService = partService;
    this.cartService = cartService;
    this.orderService = orderService;
  }

  @GetMapping("/parts")
  public String parts(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "asc") String direction,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category,
      Model model) {
    String safeSort = partSort(sort);
    Sort.Direction safeDirection = sortDirection(direction);
    Page<CarPart> parts = partService.browse(
        keyword,
        category,
        PageRequest.of(Math.max(page, 0), 8, Sort.by(safeDirection, safeSort)));
    model.addAttribute("partPage", parts);
    model.addAttribute("parts", parts.getContent());
    model.addAttribute("categories", partService.categories());
    model.addAttribute("keyword", keyword);
    model.addAttribute("category", category);
    model.addAttribute("sort", safeSort);
    model.addAttribute("direction", safeDirection.name().toLowerCase());
    return "store/parts";
  }

  @GetMapping("/parts/{idPart}")
  public String partDetails(@PathVariable int idPart, Model model) {
    model.addAttribute("part", partService.getActivePart(idPart));
    return "store/part-details";
  }

  @PostMapping("/cart/items")
  public String addToCart(
      @RequestParam int idPart,
      @RequestParam(defaultValue = "1") int quantity,
      RedirectAttributes redirectAttributes) {
    cartService.add(idPart, quantity);
    redirectAttributes.addFlashAttribute("storeMessage", "Product added to your cart.");
    return "redirect:/cart";
  }

  @GetMapping("/cart")
  public String cart(Model model) {
    model.addAttribute("cartItems", cartService.items());
    model.addAttribute("cartTotalMinor", cartService.totalMinor());
    model.addAttribute("stripeEnabled", orderService.isStripeEnabled());
    return "store/cart";
  }

  @PostMapping("/cart/items/{idCartItem}")
  public String updateCartItem(
      @PathVariable int idCartItem,
      @RequestParam int quantity,
      RedirectAttributes redirectAttributes) {
    cartService.update(idCartItem, quantity);
    redirectAttributes.addFlashAttribute("storeMessage", "Cart updated.");
    return "redirect:/cart";
  }

  @PostMapping("/cart/items/{idCartItem}/remove")
  public String removeCartItem(
      @PathVariable int idCartItem,
      RedirectAttributes redirectAttributes) {
    cartService.remove(idCartItem);
    redirectAttributes.addFlashAttribute("storeMessage", "Product removed from your cart.");
    return "redirect:/cart";
  }

  @PostMapping("/store/checkout")
  public String checkout() {
    return "redirect:" + orderService.startCheckout();
  }

  @GetMapping("/store/checkout/success")
  public String checkoutSuccess(@RequestParam("session_id") String sessionId, Model model) {
    model.addAttribute("order", orderService.currentUserOrderBySession(sessionId));
    return "store/checkout-success";
  }

  @GetMapping("/orders")
  public String orders(
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    Page<StoreOrder> orders = orderService.currentUserOrders(
        PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    model.addAttribute("orderPage", orders);
    model.addAttribute("orders", orders.getContent());
    return "store/orders";
  }

  @GetMapping("/orders/{idOrder}")
  public String orderDetails(@PathVariable int idOrder, Model model) {
    model.addAttribute("order", orderService.currentUserOrder(idOrder));
    return "store/order-details";
  }

  private Sort.Direction sortDirection(String direction) {
    return "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
  }

  private String partSort(String sort) {
    return switch (sort) {
      case "name", "category", "priceMinor", "stockQuantity" -> sort;
      default -> "name";
    };
  }
}
