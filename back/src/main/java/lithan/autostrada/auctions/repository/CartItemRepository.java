package lithan.autostrada.auctions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
  List<CartItem> findByUserIdOrderByCreatedAtAsc(int userId);

  Optional<CartItem> findByUserIdAndPart(int userId, CarPart part);

  Optional<CartItem> findByIdCartItemAndUserId(int idCartItem, int userId);

  long countByUserId(int userId);

  void deleteByUserId(int userId);
}
