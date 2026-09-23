package lithan.autostrada.auctions.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import lithan.autostrada.auctions.entity.StockHold;

public interface StockHoldRepository extends JpaRepository<StockHold, Long> {
  List<StockHold> findByAttemptId(String attemptId);
  long countByStatus(String status);
}

