package lithan.abc.cars;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Year;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lithan.abc.cars.dto.CarSearchCriteria;
import lithan.abc.cars.dto.TestDriveRequest;
import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.UserProfile;
import lithan.abc.cars.validation.ImageUploadValidator;

class ValidationImprovementsTests {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    validatorFactory.close();
  }

  @Test
  void productionYearMustBeWithinAutomobileHistoryAndCurrentYear() {
    Car car = new Car();

    car.setYear("1885");
    assertFalse(validator.validateProperty(car, "year").isEmpty());

    car.setYear(String.valueOf(Year.now().getValue() + 1));
    assertFalse(validator.validateProperty(car, "year").isEmpty());

    car.setYear(String.valueOf(Year.now().getValue()));
    assertTrue(validator.validateProperty(car, "year").isEmpty());
  }

  @Test
  void phoneNumberMustUseARecognizedInternationalOrNationalFormat() {
    UserProfile profile = new UserProfile();

    profile.setPhoneNumber("+38112345678");
    assertTrue(validator.validateProperty(profile, "phoneNumber").isEmpty());

    profile.setPhoneNumber("0123456789");
    assertTrue(validator.validateProperty(profile, "phoneNumber").isEmpty());

    profile.setPhoneNumber("123-ABC");
    assertFalse(validator.validateProperty(profile, "phoneNumber").isEmpty());
  }

  @Test
  void bidPriceMustBePositiveOnTheEntity() {
    CarBidding bid = new CarBidding();

    bid.setBidPrice(0);
    assertFalse(validator.validateProperty(bid, "bidPrice").isEmpty());

    bid.setBidPrice(1);
    assertTrue(validator.validateProperty(bid, "bidPrice").isEmpty());
  }

  @Test
  void testDriveDateMustBeStrictlyInTheFuture() {
    TestDriveRequest request = new TestDriveRequest();

    request.setDate(LocalDate.now());
    assertFalse(validator.validate(request).isEmpty());

    request.setDate(LocalDate.now().plusDays(1));
    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void minimumSearchPriceCannotExceedMaximum() {
    CarSearchCriteria criteria = new CarSearchCriteria();
    criteria.setLow(20_000);
    criteria.setHigh(10_000);

    assertFalse(validator.validate(criteria).isEmpty());

    criteria.setHigh(20_000);
    assertTrue(validator.validate(criteria).isEmpty());
  }

  @Test
  void uploadedImageMustHaveMatchingMimeTypeAndDecodableContent() throws Exception {
    byte[] png = pngBytes();
    MockMultipartFile valid = new MockMultipartFile(
        "imageFile", "car.png", "image/png", png);
    MockMultipartFile spoofed = new MockMultipartFile(
        "imageFile", "car.png", "image/png", "not an image".getBytes());
    MockMultipartFile mismatched = new MockMultipartFile(
        "imageFile", "car.jpg", "image/jpeg", png);

    assertDoesNotThrow(() -> ImageUploadValidator.validate(valid));
    assertThrows(IllegalArgumentException.class, () -> ImageUploadValidator.validate(spoofed));
    assertThrows(IllegalArgumentException.class, () -> ImageUploadValidator.validate(mismatched));
  }

  private byte[] pngBytes() throws Exception {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }
}
