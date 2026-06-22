package lithan.abc.cars.service;

import java.util.List;

import lithan.abc.cars.dto.ListingCommentView;
import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarPart;
import org.springframework.web.multipart.MultipartFile;

public interface ListingCommentService {

  List<ListingCommentView> commentsForCar(Car car);

  List<ListingCommentView> commentsForPart(CarPart part);

  void addCarComment(int idCar, String body, MultipartFile imageFile);

  void addPartComment(int idPart, String body, MultipartFile imageFile);
}
