package lithan.abc.cars.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lithan.abc.cars.service.AuctionNotificationService;
import lithan.abc.cars.service.CartService;

@ControllerAdvice
public class NavigationModelAdvice {

  private final AuctionNotificationService notificationService;
  private final CartService cartService;

  public NavigationModelAdvice(
      AuctionNotificationService notificationService,
      CartService cartService) {
    this.notificationService = notificationService;
    this.cartService = cartService;
  }

  @ModelAttribute("unreadNotificationCount")
  public long unreadNotificationCount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return 0;
    }
    try {
      return notificationService.unreadCount();
    } catch (RuntimeException exception) {
      return 0;
    }
  }

  @ModelAttribute("cartItemCount")
  public long cartItemCount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return 0;
    }
    try {
      return cartService.itemCount();
    } catch (RuntimeException exception) {
      return 0;
    }
  }
}
