package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.repository.CarBiddingRepository;
import lithan.autostrada.auctions.repository.CarListingRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.PaymentOrderRepository;
import lithan.autostrada.auctions.repository.CarPartRepository;
import lithan.autostrada.auctions.repository.ListingCommentRepository;
import lithan.autostrada.auctions.repository.ListingDepositRepository;
import lithan.autostrada.auctions.repository.ListingTestRideRepository;
import lithan.autostrada.auctions.repository.UserRepository;

@SpringBootTest
@Transactional
class DemoSeedDataIntegrationTests {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private CarListingRepository carListingRepository;

  @Autowired
  private CarBiddingRepository bidRepository;

  @Autowired
  private PaymentOrderRepository paymentRepository;

  @Autowired
  private CarPartRepository partRepository;

  @Autowired
  private ListingCommentRepository commentRepository;

  @Autowired
  private ListingTestRideRepository listingTestRideRepository;

  @Autowired
  private ListingDepositRepository listingDepositRepository;

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
  void fixedPriceDemoListingsHaveDedicatedSellerAccounts() {
    long listingDemoUsers = userRepository.findAll().stream()
        .filter(user -> user.getUsername().startsWith("demo_list_"))
        .peek(user -> assertTrue(user.getProfile().hasCompleteShippingAddress()))
        .count();

    assertEquals(24, listingDemoUsers);
    assertEquals(24, carListingRepository.findAll().stream()
        .filter(listing -> listing.getSeller().getUsername().startsWith("demo_list_"))
        .count());
    assertEquals(24, carListingRepository.findAll().stream()
        .filter(listing -> listing.getSeller().getUsername().startsWith("demo_list_"))
        .map(listing -> listing.getSeller().getUsername())
        .distinct()
        .count());
    assertTrue(carListingRepository.findAll().stream()
        .filter(listing -> listing.getSeller().getUsername().startsWith("demo_list_"))
        .allMatch(listing -> listing.getPicture() != null));
    assertTrue(carListingRepository.findAll().stream()
        .anyMatch(listing -> "ACTIVE".equals(listing.getStatus().name())));
    assertTrue(carListingRepository.findAll().stream()
        .anyMatch(listing -> "RESERVED".equals(listing.getStatus().name())));
    assertTrue(carListingRepository.findAll().stream()
        .anyMatch(listing -> "SOLD".equals(listing.getStatus().name())));
    assertTrue(carListingRepository.findAll().stream()
        .anyMatch(listing -> "INACTIVE".equals(listing.getStatus().name())));
  }

  @Test
  void fixedPriceDemoListingsIncludeReservationAndTestRideExamples() {
    assertTrue(listingTestRideRepository.findAll().stream()
        .anyMatch(ride -> ride.getListing().getSeller().getUsername().startsWith("demo_list_")
            && "PENDING".equals(ride.getStatus().name())));
    assertTrue(listingTestRideRepository.findAll().stream()
        .anyMatch(ride -> ride.getListing().getSeller().getUsername().startsWith("demo_list_")
            && "ACCEPTED".equals(ride.getStatus().name())));
    assertTrue(listingTestRideRepository.findAll().stream()
        .anyMatch(ride -> ride.getListing().getSeller().getUsername().startsWith("demo_list_")
            && "REJECTED".equals(ride.getStatus().name())));
    assertTrue(listingTestRideRepository.findAll().stream()
        .anyMatch(ride -> ride.getListing().getSeller().getUsername().startsWith("demo_list_")
            && "CANCELLED".equals(ride.getStatus().name())));

    assertTrue(listingDepositRepository.findAll().stream()
        .anyMatch(deposit -> deposit.getListing().getSeller().getUsername().startsWith("demo_list_")
            && "PENDING_CHECKOUT".equals(deposit.getStatus())));
    assertTrue(listingDepositRepository.findAll().stream()
        .anyMatch(deposit -> deposit.getListing().getSeller().getUsername().startsWith("demo_list_")
            && "PAID".equals(deposit.getStatus())));
    assertTrue(listingDepositRepository.findAll().stream()
        .anyMatch(deposit -> deposit.getListing().getSeller().getUsername().startsWith("demo_list_")
            && "EXPIRED".equals(deposit.getStatus())));
  }

  @Test
  void demoPartsCatalogIsReadyForStoreCheckout() {
    assertTrue(partRepository.count() >= 10);
    assertTrue(partRepository.findActiveCategories().size() >= 5);
    assertTrue(partRepository.findAll().stream().allMatch(part -> part.getPriceMinor() > 0));
    assertTrue(partRepository.findAll().stream().allMatch(part -> part.getStockQuantity() >= 0));
  }

  @Test
  void demoDiscussionsIncludeAuctionAndStoreConversations() {
    assertTrue(commentRepository.findAll().stream().anyMatch(comment -> comment.getCar() != null));
    assertTrue(commentRepository.findAll().stream().anyMatch(comment -> comment.getPart() != null));
    assertTrue(commentRepository.findAll().stream()
        .anyMatch(comment -> "admin123".equals(comment.getAuthor().getUsername())));
    assertTrue(commentRepository.findAll().stream()
        .anyMatch(comment -> "demo_seller".equals(comment.getAuthor().getUsername())));
  }
}
