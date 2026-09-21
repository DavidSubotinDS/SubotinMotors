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
import lithan.autostrada.auctions.identity.CurrentIdentity;
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
  private final CurrentIdentity currentIdentity;
  @org.springframework.beans.factory.annotation.Autowired
  private lithan.autostrada.auctions.identity.ProfileClient profiles;

  public ListingCommentServiceImpl(
      ListingCommentRepository commentRepository,
      CarService carService,
      CarPartService partService,
      CurrentIdentity currentIdentity) {
    this.commentRepository = commentRepository;
    this.carService = carService;
    this.partService = partService;
    this.currentIdentity = currentIdentity;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ListingCommentView> commentsForCar(Car car) {
    var comments = commentRepository.findByCarOrderByCreatedAtAscIdCommentAsc(car);
    var authors = profiles.findAll(comments.stream().map(ListingComment::getAuthorId).toList());
    return comments.stream()
        .map(comment -> toCarView(comment, car, authors.getOrDefault(comment.getAuthorId(),
            lithan.autostrada.auctions.identity.PublicProfile.missing(comment.getAuthorId()))))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ListingCommentView> commentsForPart(CarPart part) {
    var comments = commentRepository.findByPartOrderByCreatedAtAscIdCommentAsc(part);
    var authors = profiles.findAll(comments.stream().map(ListingComment::getAuthorId).toList());
    return comments.stream()
        .map(comment -> toPartView(comment, authors.getOrDefault(comment.getAuthorId(),
            lithan.autostrada.auctions.identity.PublicProfile.missing(comment.getAuthorId()))))
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
    comment.setAuthorId(currentIdentity.requireUserId());
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
    comment.setAuthorId(currentIdentity.requireUserId());
    comment.setPart(part);
    comment.setBody(normalizeBody(body));
    attachImage(comment, imageFile);
    comment.setCreatedAt(Instant.now());
    commentRepository.save(comment);
  }

  private ListingCommentView toCarView(ListingComment comment, Car car, lithan.autostrada.auctions.identity.PublicProfile author) {
    if (author.adminBadge()) {
      return view(comment, author, "Admin", "listing-comment--admin");
    }
    if (comment.getAuthorId() == car.getUserId()) {
      return view(comment, author, "Seller", "listing-comment--seller");
    }
    return view(comment, author, null, "");
  }

  private ListingCommentView toPartView(ListingComment comment, lithan.autostrada.auctions.identity.PublicProfile author) {
    if (author.adminBadge()) {
      return view(comment, author, "Store team", "listing-comment--admin");
    }
    return view(comment, author, null, "");
  }

  private ListingCommentView view(
      ListingComment comment,
      lithan.autostrada.auctions.identity.PublicProfile author,
      String badgeLabel,
      String highlightClass) {
    String authorName = author.displayName();
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
