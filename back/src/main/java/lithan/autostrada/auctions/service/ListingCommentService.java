package lithan.autostrada.auctions.service;

import java.util.List;

import lithan.autostrada.auctions.dto.ListingCommentView;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarPart;
import org.springframework.web.multipart.MultipartFile;

public interface ListingCommentService {

  List<ListingCommentView> commentsForCar(Car car);

  List<ListingCommentView> commentsForPart(CarPart part);

  void addCarComment(int idCar, String body, MultipartFile imageFile);

  void addPartComment(int idPart, String body, MultipartFile imageFile);
}
