package lithan.abc.cars.entity;

public enum CarListingStatus {
  ACTIVE("Active"),
  RESERVED("Reserved"),
  SOLD("Sold"),
  INACTIVE("Inactive");

  private final String label;

  CarListingStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
