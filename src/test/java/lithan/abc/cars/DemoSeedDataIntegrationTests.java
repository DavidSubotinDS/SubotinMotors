package lithan.abc.cars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.repository.CarBiddingRepository;
import lithan.abc.cars.repository.CarRepository;
import lithan.abc.cars.repository.PaymentOrderRepository;
import lithan.abc.cars.repository.CarPartRepository;
import lithan.abc.cars.repository.UserRepository;

@SpringBootTest
@Transactional
class DemoSeedDataIntegrationTests {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private CarBiddingRepository bidRepository;

  @Autowired
  private PaymentOrderRepository paymentRepository;

  @Autowired
  private CarPartRepository partRepository;

  @Test
  void demoPersonasHaveDistinctMarketplaceHistories() {
    UserAccount bidder = userRepository.findByUsername("demo_bidder").orElseThrow();
    UserAccount seller = userRepository.findByUsername("demo_seller").orElseThrow();
    UserAccount trader = userRepository.findByUsername("demo_trader").orElseThrow();
    UserAccount newcomer = userRepository.findByUsername("demo_newcomer").orElseThrow();

    assertTrue(carRepository.findByUser(bidder).isEmpty());
    assertFalse(bidRepository.findByUserOrderByIdBidDesc(bidder).isEmpty());
    assertFalse(paymentRepository.findByBuyerOrderByCreatedAtDesc(bidder).isEmpty());

    assertFalse(carRepository.findByUser(seller).isEmpty());
    assertTrue(bidRepository.findByUserOrderByIdBidDesc(seller).isEmpty());
    assertFalse(paymentRepository.findBySellerOrderByCreatedAtDesc(seller).isEmpty());

    assertFalse(carRepository.findByUser(trader).isEmpty());
    assertFalse(bidRepository.findByUserOrderByIdBidDesc(trader).isEmpty());
    assertFalse(paymentRepository.findByBuyerOrderByCreatedAtDesc(trader).isEmpty());
    assertFalse(paymentRepository.findBySellerOrderByCreatedAtDesc(trader).isEmpty());

    assertTrue(carRepository.findByUser(newcomer).isEmpty());
    assertTrue(bidRepository.findByUserOrderByIdBidDesc(newcomer).isEmpty());
  }

  @Test
  void demoListingsCoverAuctionAndModerationStates() {
    assertEquals(4, carRepository.findAll().stream()
        .filter(car -> "ACTIVE".equals(car.getStatus()))
        .filter(car -> car.getUser().getUsername().startsWith("demo_"))
        .count());
    assertEquals(2, carRepository.findAll().stream()
        .filter(car -> "PENDING".equals(car.getStatus()))
        .count());
    assertTrue(bidRepository.findAll().stream()
        .anyMatch(bid -> "ONGOING".equals(bid.getStatus())));
    assertTrue(bidRepository.findAll().stream()
        .anyMatch(bid -> "ACCEPTED_PENDING_PAYMENT".equals(bid.getStatus())));
  }

  @Test
  void demoPartsCatalogIsReadyForStoreCheckout() {
    assertTrue(partRepository.count() >= 10);
    assertTrue(partRepository.findActiveCategories().size() >= 5);
    assertTrue(partRepository.findAll().stream().allMatch(part -> part.getPriceMinor() > 0));
    assertTrue(partRepository.findAll().stream().allMatch(part -> part.getStockQuantity() >= 0));
  }
}
