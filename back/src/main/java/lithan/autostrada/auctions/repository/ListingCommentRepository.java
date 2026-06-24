package lithan.autostrada.auctions.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.ListingComment;

public interface ListingCommentRepository extends JpaRepository<ListingComment, Integer> {

  @EntityGraph(attributePaths = {"author", "author.profile", "author.roles"})
  List<ListingComment> findByCarOrderByCreatedAtAscIdCommentAsc(Car car);

  @EntityGraph(attributePaths = {"author", "author.profile", "author.roles"})
  List<ListingComment> findByPartOrderByCreatedAtAscIdCommentAsc(CarPart part);
}
