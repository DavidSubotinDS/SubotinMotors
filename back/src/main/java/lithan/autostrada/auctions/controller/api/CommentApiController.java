package lithan.autostrada.auctions.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.auctions.dto.api.ApiModels.CommentResponse;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.service.CarPartService;
import lithan.autostrada.auctions.service.CarService;
import lithan.autostrada.auctions.service.ListingCommentService;

@RestController
@RequestMapping("/api/comments")
public class CommentApiController {

  private final CarService carService;
  private final CarPartService partService;
  private final ListingCommentService commentService;
  private final ApiModelMapper mapper;

  public CommentApiController(
      CarService carService,
      CarPartService partService,
      ListingCommentService commentService,
      ApiModelMapper mapper) {
    this.carService = carService;
    this.partService = partService;
    this.commentService = commentService;
    this.mapper = mapper;
  }

  @GetMapping("/cars/{idCar}")
  public List<CommentResponse> carComments(@PathVariable int idCar) {
    Car car = carService.getCarById(idCar);
    return commentService.commentsForCar(car).stream().map(mapper::comment).toList();
  }

  @PostMapping("/cars/{idCar}")
  public List<CommentResponse> addCarComment(
      @PathVariable int idCar,
      @RequestParam String body,
      @RequestParam(name = "imageFile", required = false) MultipartFile imageFile) {
    commentService.addCarComment(idCar, body, imageFile);
    return carComments(idCar);
  }

  @GetMapping("/parts/{idPart}")
  public List<CommentResponse> partComments(@PathVariable int idPart) {
    CarPart part = partService.getActivePart(idPart);
    return commentService.commentsForPart(part).stream().map(mapper::comment).toList();
  }

  @PostMapping("/parts/{idPart}")
  public List<CommentResponse> addPartComment(
      @PathVariable int idPart,
      @RequestParam String body,
      @RequestParam(name = "imageFile", required = false) MultipartFile imageFile) {
    commentService.addPartComment(idPart, body, imageFile);
    return partComments(idPart);
  }
}
