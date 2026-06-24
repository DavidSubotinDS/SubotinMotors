package lithan.autostrada.auctions.service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.auctions.dto.ListingCommentView;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.ListingComment;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.repository.ListingCommentRepository;
import lithan.autostrada.auctions.validation.ImageUploadValidator;
import lithan.autostrada.auctions.validation.ImageUploadValidator.ValidatedImage;

@Service
public class ListingCommentServiceImpl implements ListingCommentService {

  private static final DateTimeFormatter COMMENT_TIME =
      DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

  private final ListingCommentRepository commentRepository;
  private final CarService carService;
  private final CarPartService partService;
  private final UserService userService;

  public ListingCommentServiceImpl(
      ListingCommentRepository commentRepository,
      CarService carService,
      CarPartService partService,
      UserService userService) {
    this.commentRepository = commentRepository;
    this.carService = carService;
    this.partService = partService;
    this.userService = userService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ListingCommentView> commentsForCar(Car car) {
    return commentRepository.findByCarOrderByCreatedAtAscIdCommentAsc(car).stream()
        .map(comment -> toCarView(comment, car))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ListingCommentView> commentsForPart(CarPart part) {
    return commentRepository.findByPartOrderByCreatedAtAscIdCommentAsc(part).stream()
        .map(this::toPartView)
        .toList();
  }

  @Override
  @Transactional
  public void addCarComment(int idCar, String body, MultipartFile imageFile) {
    Car car = carService.getCarById(idCar);
    if ("DEACTIVE".equals(car.getStatus()) || "PENDING".equals(car.getStatus())) {
      throw new IllegalArgumentException("Comments are only available on visible auctions");
    }

    ListingComment comment = new ListingComment();
    comment.setAuthor(userService.getUserLogin());
    comment.setCar(car);
    comment.setBody(normalizeBody(body));
    attachImage(comment, imageFile);
    comment.setCreatedAt(Instant.now());
    commentRepository.save(comment);
  }

  @Override
  @Transactional
  public void addPartComment(int idPart, String body, MultipartFile imageFile) {
    CarPart part = partService.getActivePart(idPart);

    ListingComment comment = new ListingComment();
    comment.setAuthor(userService.getUserLogin());
    comment.setPart(part);
    comment.setBody(normalizeBody(body));
    attachImage(comment, imageFile);
    comment.setCreatedAt(Instant.now());
    commentRepository.save(comment);
  }

  private ListingCommentView toCarView(ListingComment comment, Car car) {
    UserAccount author = comment.getAuthor();
    if (isAdmin(author)) {
      return view(comment, "Admin", "listing-comment--admin");
    }
    if (author.getIdUser() == car.getUser().getIdUser()) {
      return view(comment, "Seller", "listing-comment--seller");
    }
    return view(comment, null, "");
  }

  private ListingCommentView toPartView(ListingComment comment) {
    if (isAdmin(comment.getAuthor())) {
      return view(comment, "Store team", "listing-comment--admin");
    }
    return view(comment, null, "");
  }

  private ListingCommentView view(
      ListingComment comment,
      String badgeLabel,
      String highlightClass) {
    UserAccount author = comment.getAuthor();
    String authorName = author.getUsername();
    if (author.getProfile() != null) {
      authorName = author.getProfile().getFirstName() + " " + author.getProfile().getLastName();
    }
    String createdAt = COMMENT_TIME.format(
        comment.getCreatedAt().atZone(ZoneId.systemDefault()));
    return new ListingCommentView(
        comment.getIdComment(),
        authorName,
        comment.getBody(),
        createdAt,
        badgeLabel,
        highlightClass,
        comment.getImageFileName(),
        comment.getImageFileType(),
        comment.getImageData());
  }

  private boolean isAdmin(UserAccount user) {
    return user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getRole()));
  }

  private String normalizeBody(String body) {
    if (body == null || body.isBlank()) {
      throw new IllegalArgumentException("Write a comment before posting");
    }
    String normalized = body.trim();
    if (normalized.length() > 1000) {
      throw new IllegalArgumentException("Comments must not exceed 1000 characters");
    }
    return normalized;
  }

  private void attachImage(ListingComment comment, MultipartFile imageFile) {
    if (imageFile == null || imageFile.isEmpty()) {
      return;
    }
    try {
      ValidatedImage image = ImageUploadValidator.validate(imageFile);
      comment.setImageFileName(image.fileName());
      comment.setImageFileType(image.contentType());
      comment.setImageData(Base64.getEncoder().encodeToString(image.bytes()));
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read the attached image");
    }
  }
}
