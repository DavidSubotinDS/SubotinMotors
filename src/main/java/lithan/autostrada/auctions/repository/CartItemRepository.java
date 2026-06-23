package lithan.autostrada.auctions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.CartItem;
import lithan.autostrada.auctions.entity.UserAccount;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
  List<CartItem> findByUserOrderByCreatedAtAsc(UserAccount user);

  Optional<CartItem> findByUserAndPart(UserAccount user, CarPart part);

  Optional<CartItem> findByIdCartItemAndUser(int idCartItem, UserAccount user);

  long countByUser(UserAccount user);

  void deleteByUser(UserAccount user);
}
