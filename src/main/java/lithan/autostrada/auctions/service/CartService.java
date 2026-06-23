package lithan.autostrada.auctions.service;

import java.util.List;

import lithan.autostrada.auctions.entity.CartItem;

public interface CartService {
  List<CartItem> items();

  void add(int idPart, int quantity);

  void update(int idCartItem, int quantity);

  void remove(int idCartItem);

  long itemCount();

  long totalMinor();
}
