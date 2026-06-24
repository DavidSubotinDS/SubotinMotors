package lithan.autostrada.auctions.validation;

import java.time.Year;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProductionYearValidator implements ConstraintValidator<ProductionYear, String> {

  private static final int FIRST_AUTOMOBILE_YEAR = 1886;

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || !value.matches("\\d{4}")) {
      return false;
    }

    int year = Integer.parseInt(value);
    return year >= FIRST_AUTOMOBILE_YEAR && year <= Year.now().getValue();
  }
}
