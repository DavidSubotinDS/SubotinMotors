package lithan.autostrada.auctions.controller;

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

import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.dto.ListingCommentForm;
import lithan.autostrada.auctions.service.CarPartService;
import lithan.autostrada.auctions.service.CartService;
import lithan.autostrada.auctions.service.ListingCommentService;
import lithan.autostrada.auctions.service.StoreOrderService;
import lithan.autostrada.auctions.service.UserService;
import lithan.autostrada.auctions.error.MissingShippingAddressException;

@Controller
public class PartsStoreController {

  private final CarPartService partService;
  private final CartService cartService;
  private final StoreOrderService orderService;
  private final ListingCommentService commentService;
  private final UserService userService;

  public PartsStoreController(
      CarPartService partService,
      CartService cartService,
      StoreOrderService orderService,
      ListingCommentService commentService,
      UserService userService) {
    this.partService = partService;
    this.cartService = cartService;
    this.orderService = orderService;
    this.commentService = commentService;
    this.userService = userService;
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
    CarPart part = partService.getActivePart(idPart);
    model.addAttribute("part", part);
    model.addAttribute("comments", commentService.commentsForPart(part));
    model.addAttribute("commentForm", new ListingCommentForm());
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
    model.addAttribute(
        "hasShippingAddress",
        userService.getUserLogin().getProfile().hasCompleteShippingAddress());
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
  public String checkout(RedirectAttributes redirectAttributes) {
    try {
      return "redirect:" + orderService.startCheckout();
    } catch (MissingShippingAddressException exception) {
      redirectAttributes.addFlashAttribute("addressError", exception.getMessage());
      return "redirect:/user/edit-profile?addressRequired";
    }
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
