package lithan.abc.cars.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.CarPart;
import lithan.abc.cars.entity.CartItem;
import lithan.abc.cars.entity.UserAccount;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
  List<CartItem> findByUserOrderByCreatedAtAsc(UserAccount user);

  Optional<CartItem> findByUserAndPart(UserAccount user, CarPart part);

  Optional<CartItem> findByIdCartItemAndUser(int idCartItem, UserAccount user);

  long countByUser(UserAccount user);

  void deleteByUser(UserAccount user);
}
