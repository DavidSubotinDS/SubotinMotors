package lithan.autostrada.auctions.controller.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lithan.autostrada.auctions.dto.api.ApiModels.ApiMessageResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.CartItemRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.CartQuantityRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.CartResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.CheckoutResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.StoreOrderResponse;
import lithan.autostrada.auctions.dto.api.PageResponse;
import lithan.autostrada.auctions.service.CartService;
import lithan.autostrada.auctions.service.StoreOrderService;
import lithan.autostrada.auctions.service.UserService;

@RestController
@RequestMapping("/api/store")
public class StoreApiController {

  private final CartService cartService;
  private final StoreOrderService orderService;
  private final UserService userService;
  private final ApiModelMapper mapper;

  public StoreApiController(
      CartService cartService,
      StoreOrderService orderService,
      UserService userService,
      ApiModelMapper mapper) {
    this.cartService = cartService;
    this.orderService = orderService;
    this.userService = userService;
    this.mapper = mapper;
  }

  @GetMapping("/cart")
  public CartResponse cart() {
    return mapper.cart(
        cartService.items(),
        cartService.totalMinor(),
        cartService.itemCount(),
        orderService.isStripeEnabled(),
        userService.getUserLogin().getProfile().hasCompleteShippingAddress());
  }

  @PostMapping("/cart/items")
  public CartResponse addToCart(@RequestBody CartItemRequest request) {
    if (request.idPart() == null) {
      throw new IllegalArgumentException("Part is required.");
    }
    cartService.add(request.idPart(), request.quantity() == null ? 1 : request.quantity());
    return cart();
  }

  @PutMapping("/cart/items/{idCartItem}")
  public CartResponse updateCartItem(
      @PathVariable int idCartItem,
      @RequestBody CartQuantityRequest request) {
    if (request.quantity() == null) {
      throw new IllegalArgumentException("Quantity is required.");
    }
    cartService.update(idCartItem, request.quantity());
    return cart();
  }

  @PostMapping("/cart/items/{idCartItem}/remove")
  public CartResponse removeCartItem(@PathVariable int idCartItem) {
    cartService.remove(idCartItem);
    return cart();
  }

  @PostMapping("/checkout")
  public CheckoutResponse checkout() {
    return new CheckoutResponse(orderService.startCheckout());
  }

  @GetMapping("/checkout/success")
  public StoreOrderResponse checkoutSuccess(@RequestParam("session_id") String sessionId) {
    return mapper.storeOrder(orderService.currentUserOrderBySession(sessionId));
  }

  @GetMapping("/orders")
  public PageResponse<StoreOrderResponse> orders(@RequestParam(defaultValue = "0") int page) {
    var orders = orderService.currentUserOrders(
        PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    return PageResponse.from(orders.map(mapper::storeOrder));
  }

  @GetMapping("/orders/{idOrder}")
  public StoreOrderResponse order(@PathVariable int idOrder) {
    return mapper.storeOrder(orderService.currentUserOrder(idOrder));
  }

  @GetMapping("/payments")
  public ApiMessageResponse retiredAuctionPayments() {
    return new ApiMessageResponse(
        "Auction checkout has been retired. Online payments are demonstrated through the parts store.",
        "/orders");
  }
}
