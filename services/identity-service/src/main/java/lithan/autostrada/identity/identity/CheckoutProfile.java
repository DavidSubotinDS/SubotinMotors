package lithan.autostrada.identity.identity;

/** Private, short-lived checkout input. Never serialize this record to public APIs or logs. */
public record CheckoutProfile(int userId, String email, String name, String streetAddress,
    String city, String postalCode, String country) {
  public boolean hasCompleteShippingAddress() {
    return hasText(streetAddress) && hasText(city) && hasText(postalCode) && hasText(country);
  }
  private static boolean hasText(String value) { return value != null && !value.isBlank(); }
  public String formattedAddress() {
    return hasCompleteShippingAddress()
        ? streetAddress.trim() + ", " + postalCode.trim() + " " + city.trim() + ", " + country.trim() : "";
  }
  @Override public String toString() { return "CheckoutProfile[redacted]"; }
}
