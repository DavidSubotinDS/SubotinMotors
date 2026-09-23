package lithan.autostrada.auctions.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lithan.autostrada.auctions.service.AuctionNotificationService;
import lithan.autostrada.auctions.service.CartService;

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
  public long unreadNotificationCount(jakarta.servlet.http.HttpServletRequest request) {
    // REST DTOs compose their own counts; this MVC model must not add a peer call to every API request.
    if(request.getRequestURI().startsWith("/api/"))return 0;
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return 0;
    }
    return notificationService.unreadCount();
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
