package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.CartItem;
import lithan.autostrada.auctions.identity.CheckoutProfile;
import lithan.autostrada.auctions.repository.CarPartRepository;
import lithan.autostrada.auctions.repository.CartItemRepository;
import lithan.autostrada.auctions.service.CheckoutPreparationService;
import fixtures.identity.repository.UserRepository;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
class CheckoutConcurrencyIntegrationTests {
  @Autowired CheckoutPreparationService preparation;
  @Autowired CarPartRepository parts;
  @Autowired CartItemRepository carts;
  @Autowired UserRepository users;
  @Autowired PlatformTransactionManager transactions;

  @Test
  void concurrentLastItemCheckoutCreatesExactlyOneStockHold() throws Exception {
    Instant now = Instant.now();
    CarPart part = new CarPart();
    part.setSku("S7-LAST-ITEM-" + System.nanoTime());
    part.setName("S7 last item");
    part.setCategory("Tests");
    part.setDescription("Disposable concurrency fixture");
    part.setPriceMinor(1000);
    part.setStockQuantity(1);
    part.setActive(true);
    part.setCreatedAt(now);
    part.setUpdatedAt(now);
    part = parts.saveAndFlush(part);
    final CarPart lastItem = part;
    int firstUser = users.findByUsername("user123").orElseThrow().getIdUser();
    int secondUser = users.findByUsername("admin123").orElseThrow().getIdUser();
    addCart(firstUser, part, now);
    addCart(secondUser, part, now);
    CheckoutProfile firstProfile = profile(firstUser, "first@example.test");
    CheckoutProfile secondProfile = profile(secondUser, "second@example.test");

    var executor = Executors.newFixedThreadPool(2);
    try {
      List<Callable<Boolean>> tasks = List.of(
          () -> tryPrepare(firstUser, "last-item-a-" + lastItem.getIdPart(), firstProfile),
          () -> tryPrepare(secondUser, "last-item-b-" + lastItem.getIdPart(), secondProfile));
      var results = executor.invokeAll(tasks);
      long successes = 0;
      for (var result : results) if (result.get()) successes++;
      assertEquals(1, successes);
    } finally {
      executor.shutdownNow();
    }
    assertEquals(0, parts.findById(part.getIdPart()).orElseThrow().getStockQuantity());
    new TransactionTemplate(transactions).executeWithoutResult(ignored -> {
      carts.deleteByUserId(firstUser);
      carts.deleteByUserId(secondUser);
    });
  }

  private boolean tryPrepare(int userId, String requestId, CheckoutProfile profile) {
    try {
      preparation.prepareStore(userId, requestId, profile);
      return true;
    } catch (IllegalStateException expectedStockConflict) {
      return false;
    }
  }

  private void addCart(int userId, CarPart part, Instant now) {
    CartItem item = new CartItem();
    item.setUserId(userId);
    item.setPart(part);
    item.setQuantity(1);
    item.setCreatedAt(now);
    item.setUpdatedAt(now);
    carts.saveAndFlush(item);
  }

  private CheckoutProfile profile(int userId, String email) {
    return new CheckoutProfile(userId, email, "S7 Buyer", "1 Test Road", "Szeged", "6720", "Hungary");
  }
}
