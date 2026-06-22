package lithan.abc.cars.dto;

public class ListingCommentView {

  private final int idComment;
  private final String authorName;
  private final String body;
  private final String createdAtDisplay;
  private final String badgeLabel;
  private final String highlightClass;
  private final String imageFileName;
  private final String imageFileType;
  private final String imageData;

  public ListingCommentView(
      int idComment,
      String authorName,
      String body,
      String createdAtDisplay,
      String badgeLabel,
      String highlightClass,
      String imageFileName,
      String imageFileType,
      String imageData) {
    this.idComment = idComment;
    this.authorName = authorName;
    this.body = body;
    this.createdAtDisplay = createdAtDisplay;
    this.badgeLabel = badgeLabel;
    this.highlightClass = highlightClass;
    this.imageFileName = imageFileName;
    this.imageFileType = imageFileType;
    this.imageData = imageData;
  }

  public int getIdComment() {
    return idComment;
  }

  public String getAuthorName() {
    return authorName;
  }

  public String getBody() {
    return body;
  }

  public String getCreatedAtDisplay() {
    return createdAtDisplay;
  }

  public String getBadgeLabel() {
    return badgeLabel;
  }

  public String getHighlightClass() {
    return highlightClass;
  }

  public boolean isHighlighted() {
    return badgeLabel != null;
  }

  public String getImageFileName() {
    return imageFileName;
  }

  public String getImageFileType() {
    return imageFileType;
  }

  public String getImageData() {
    return imageData;
  }

  public boolean isHasImage() {
    return imageData != null && !imageData.isBlank();
  }
}
