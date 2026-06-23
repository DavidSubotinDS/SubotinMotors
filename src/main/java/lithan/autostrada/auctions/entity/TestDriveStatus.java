package lithan.autostrada.auctions.entity;

public enum TestDriveStatus {
  PENDING("Pending"),
  ACCEPTED("Accepted"),
  REJECTED("Rejected"),
  CANCELLED("Cancelled");

  private final String label;

  TestDriveStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
