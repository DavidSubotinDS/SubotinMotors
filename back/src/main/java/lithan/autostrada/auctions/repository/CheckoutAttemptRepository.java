package lithan.autostrada.auctions.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import lithan.autostrada.auctions.entity.CheckoutAttempt;

public interface CheckoutAttemptRepository extends JpaRepository<CheckoutAttempt, String> {
  Optional<CheckoutAttempt> findByUserIdAndPurposeAndClientRequestId(
      int userId, String purpose, String clientRequestId);
  List<CheckoutAttempt> findByStatusInAndNextReconcileAtLessThanEqualOrderByCreatedAtAsc(
      List<String> statuses, Instant due, Pageable pageable);
  long countByStatusIn(List<String> statuses);
  Optional<CheckoutAttempt> findFirstByStatusInOrderByCreatedAtAsc(List<String> statuses);
  List<CheckoutAttempt> findByStatusInAndExpiresAtLessThanEqualOrderByCreatedAtAsc(
      List<String> statuses, Instant due, Pageable pageable);
}
