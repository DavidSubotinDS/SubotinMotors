package lithan.abc.cars.service;

import java.util.List;

import lithan.abc.cars.entity.CartItem;

public interface CartService {
  List<CartItem> items();

  void add(int idPart, int quantity);

  void update(int idCartItem, int quantity);

  void remove(int idCartItem);

  long itemCount();

  long totalMinor();
}
