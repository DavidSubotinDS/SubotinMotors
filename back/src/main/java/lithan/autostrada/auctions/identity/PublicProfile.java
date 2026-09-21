package lithan.autostrada.auctions.identity;

/** Public display only. The badge must never authorize an operation. */
public record PublicProfile(int userId, Integer profileId, String username,
    String firstName, String lastName, String city, String country, String about,
    String pictureType, String picture, boolean adminBadge) {
  public String displayName() {
    String name = ((firstName == null ? "" : firstName) + " "
        + (lastName == null ? "" : lastName)).trim();
    return name.isBlank() ? username : name;
  }
  public String displayLocation() {
    return city == null || city.isBlank() || country == null || country.isBlank()
        ? "" : city.trim() + ", " + country.trim();
  }
  public static PublicProfile missing(int userId) {
    return new PublicProfile(userId, null, "Unavailable user", null, null,
        null, null, null, null, null, false);
  }
}
