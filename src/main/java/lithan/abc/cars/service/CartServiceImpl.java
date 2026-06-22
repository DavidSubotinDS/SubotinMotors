package lithan.abc.cars.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.abc.cars.entity.CarPart;
import lithan.abc.cars.entity.CartItem;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.error.ResourceNotFoundException;
import lithan.abc.cars.repository.CartItemRepository;

@Service
public class CartServiceImpl implements CartService {

  private final CartItemRepository cartItemRepository;
  private final CarPartService partService;
  private final UserService userService;

  public CartServiceImpl(
      CartItemRepository cartItemRepository,
      CarPartService partService,
      UserService userService) {
    this.cartItemRepository = cartItemRepository;
    this.partService = partService;
    this.userService = userService;
  }

  @Override
  public List<CartItem> items() {
    return cartItemRepository.findByUserOrderByCreatedAtAsc(userService.getUserLogin());
  }

  @Override
  @Transactional
  public void add(int idPart, int quantity) {
    if (quantity < 1) {
      throw new IllegalArgumentException("Quantity must be at least one");
    }
    UserAccount user = userService.getUserLogin();
    CarPart part = partService.getActivePart(idPart);
    if (part.getStockQuantity() < 1) {
      throw new IllegalStateException("This product is out of stock");
    }
    CartItem item = cartItemRepository.findByUserAndPart(user, part).orElse(null);
    int newQuantity = quantity + (item == null ? 0 : item.getQuantity());
    validateStock(part, newQuantity);

    Instant now = Instant.now();
    if (item == null) {
      item = new CartItem();
      item.setUser(user);
      item.setPart(part);
      item.setCreatedAt(now);
    }
    item.setQuantity(newQuantity);
    item.setUpdatedAt(now);
    cartItemRepository.save(item);
  }

  @Override
  @Transactional
  public void update(int idCartItem, int quantity) {
    UserAccount user = userService.getUserLogin();
    CartItem item = cartItemRepository.findByIdCartItemAndUser(idCartItem, user)
        .orElseThrow(ResourceNotFoundException::new);
    if (quantity <= 0) {
      cartItemRepository.delete(item);
      return;
    }
    validateStock(item.getPart(), quantity);
    item.setQuantity(quantity);
    item.setUpdatedAt(Instant.now());
  }

  @Override
  @Transactional
  public void remove(int idCartItem) {
    UserAccount user = userService.getUserLogin();
    CartItem item = cartItemRepository.findByIdCartItemAndUser(idCartItem, user)
        .orElseThrow(ResourceNotFoundException::new);
    cartItemRepository.delete(item);
  }

  @Override
  public long itemCount() {
    return items().stream().mapToLong(CartItem::getQuantity).sum();
  }

  @Override
  public long totalMinor() {
    return items().stream().mapToLong(CartItem::getLineTotalMinor).sum();
  }

  private void validateStock(CarPart part, int quantity) {
    if (quantity > part.getStockQuantity()) {
      throw new IllegalStateException(
          "Only " + part.getStockQuantity() + " units of " + part.getName() + " are available");
    }
  }
}
