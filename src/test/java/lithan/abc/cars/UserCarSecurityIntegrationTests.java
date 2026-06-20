package lithan.abc.cars;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.repository.CarRepository;
import lithan.abc.cars.repository.UserRepository;
import lithan.abc.cars.service.UserCarService;

@SpringBootTest
@Transactional
class UserCarSecurityIntegrationTests {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private UserCarService userCarService;

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void userCannotEditAnotherUsersCar() {
    UserAccount admin = userRepository.findByUsername("admin123").orElseThrow();
    Car car = new Car();
    car.setMake("Security");
    car.setModel("Check");
    car.setYear("2024");
    car.setPrice(10000);
    car.setStatus("ACTIVE");
    car.setUser(admin);
    carRepository.saveAndFlush(car);

    assertThrows(AccessDeniedException.class, () -> userCarService.getOwnedCarById(car.getIdCar()));
  }
}
