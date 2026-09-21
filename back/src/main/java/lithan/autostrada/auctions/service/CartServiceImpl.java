package lithan.autostrada.auctions.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.CartItem;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.CartItemRepository;

@Service
public class CartServiceImpl implements CartService {

  private final CartItemRepository cartItemRepository;
  private final CarPartService partService;
  private final CurrentIdentity currentIdentity;

  public CartServiceImpl(
      CartItemRepository cartItemRepository,
      CarPartService partService,
      CurrentIdentity currentIdentity) {
    this.cartItemRepository = cartItemRepository;
    this.partService = partService;
    this.currentIdentity = currentIdentity;
  }

  @Override
  public List<CartItem> items() {
    return cartItemRepository.findByUserIdOrderByCreatedAtAsc(currentIdentity.requireUserId());
  }

  @Override
  @Transactional
  public void add(int idPart, int quantity) {
    if (quantity < 1) {
      throw new IllegalArgumentException("Quantity must be at least one");
    }
    int user = currentIdentity.requireUserId();
    CarPart part = partService.getActivePart(idPart);
    if (part.getStockQuantity() < 1) {
      throw new IllegalStateException("This product is out of stock");
    }
    CartItem item = cartItemRepository.findByUserIdAndPart(user, part).orElse(null);
    int newQuantity = quantity + (item == null ? 0 : item.getQuantity());
    validateStock(part, newQuantity);

    Instant now = Instant.now();
    if (item == null) {
      item = new CartItem();
      item.setUserId(user);
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
    int user = currentIdentity.requireUserId();
    CartItem item = cartItemRepository.findByIdCartItemAndUserId(idCartItem, user)
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
    int user = currentIdentity.requireUserId();
    CartItem item = cartItemRepository.findByIdCartItemAndUserId(idCartItem, user)
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
