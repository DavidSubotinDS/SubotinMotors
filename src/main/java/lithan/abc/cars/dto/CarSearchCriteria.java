package lithan.abc.cars.dto;

import jakarta.validation.constraints.AssertTrue;

public class CarSearchCriteria {

  private String keyword;
  private Integer low;
  private Integer high;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public Integer getLow() {
    return low;
  }

  public void setLow(Integer low) {
    this.low = low;
  }

  public Integer getHigh() {
    return high;
  }

  public void setHigh(Integer high) {
    this.high = high;
  }

  @AssertTrue(message = "Minimum price cannot be greater than maximum price")
  public boolean isPriceRangeValid() {
    return low == null || high == null || low <= high;
  }
}
