package lithan.abc.cars.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarPart;
import lithan.abc.cars.entity.ListingComment;

public interface ListingCommentRepository extends JpaRepository<ListingComment, Integer> {

  @EntityGraph(attributePaths = {"author", "author.profile", "author.roles"})
  List<ListingComment> findByCarOrderByCreatedAtAscIdCommentAsc(Car car);

  @EntityGraph(attributePaths = {"author", "author.profile", "author.roles"})
  List<ListingComment> findByPartOrderByCreatedAtAscIdCommentAsc(CarPart part);
}
