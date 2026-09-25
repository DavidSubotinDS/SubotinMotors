package lithan.autostrada.auctions.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.config.PaymentPolicyProperties;
import lithan.autostrada.auctions.entity.*;
import lithan.autostrada.auctions.error.MissingShippingAddressException;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.identity.CheckoutProfile;
import lithan.autostrada.auctions.payment.StripeCheckoutResult;
import lithan.autostrada.auctions.repository.*;

@Service
public class CheckoutPreparationService {
  static final String STORE = "STORE_ORDER";
  static final String DEPOSIT = "LISTING_DEPOSIT";
  static final List<String> RECONCILABLE = List.of("PROVIDER_PENDING", "RECONCILE_REQUIRED");

  private final CheckoutAttemptRepository attempts;
  private final StockHoldRepository holds;
  private final StoreOrderRepository orders;
  private final ListingDepositRepository deposits;
  private final CartItemRepository cartItems;
  private final CarPartRepository parts;
  private final CarListingRepository listings;
  private final PaymentPolicyProperties paymentPolicy;
  private final Clock clock;

  public CheckoutPreparationService(
      CheckoutAttemptRepository attempts, StockHoldRepository holds,
      StoreOrderRepository orders, ListingDepositRepository deposits,
      CartItemRepository cartItems, CarPartRepository parts,
      CarListingRepository listings, PaymentPolicyProperties paymentPolicy, Clock clock) {
    this.attempts = attempts;
    this.holds = holds;
    this.orders = orders;
    this.deposits = deposits;
    this.cartItems = cartItems;
    this.parts = parts;
    this.listings = listings;
    this.paymentPolicy = paymentPolicy;
    this.clock = clock;
  }

  @Transactional
  public PreparedCheckout prepareStore(
      int userId, String clientRequestId, CheckoutProfile profile) {
    var existing = attempts.findByUserIdAndPurposeAndClientRequestId(userId, STORE, clientRequestId);
    if (existing.isPresent()) {
      rejectReusedStoreKey(existing.get(), userId, profile);
      return prepared(existing.get(), false);
    }
    if (!profile.hasCompleteShippingAddress()) {
      throw new MissingShippingAddressException();
    }
    List<CartItem> cart = cartItems.findByUserIdForUpdate(userId);
    if (cart.isEmpty()) {
      throw new IllegalStateException("Your cart is empty");
    }
    List<Integer> partIds = cart.stream().map(item -> item.getPart().getIdPart()).sorted().toList();
    Map<Integer, CarPart> lockedParts = parts.findAllByIdForUpdate(partIds).stream()
        .collect(Collectors.toMap(CarPart::getIdPart, Function.identity()));
    String requestHash = storeHash(userId, profile, cart, lockedParts);
    Instant now = clock.instant();
    CheckoutAttempt attempt = newAttempt(userId, STORE, clientRequestId, requestHash, profile.email(), now);
    attempts.save(attempt);

    StoreOrder order = new StoreOrder();
    order.setUserId(userId);
    order.setCurrency(paymentPolicy.getCurrency().toLowerCase());
    order.setStatus("PROVIDER_PENDING");
    order.setCheckoutAttemptId(attempt.getAttemptId());
    order.setShippingName(profile.name());
    order.setShippingAddress(profile.formattedAddress());
    order.setShippingStreetAddress(profile.streetAddress().trim());
    order.setShippingCity(profile.city().trim());
    order.setShippingPostalCode(profile.postalCode().trim());
    order.setShippingCountry(profile.country().trim());
    order.setCreatedAt(now);
    order.setUpdatedAt(now);

    long totalMinor = 0;
    for (CartItem cartItem : cart) {
      CarPart part = lockedParts.get(cartItem.getPart().getIdPart());
      if (part == null || !part.isActive()) {
        throw new IllegalStateException(cartItem.getPart().getName() + " is no longer available");
      }
      if (cartItem.getQuantity() > part.getStockQuantity()) {
        throw new IllegalStateException("Only " + part.getStockQuantity() + " units of " + part.getName() + " remain");
      }
      part.setStockQuantity(part.getStockQuantity() - cartItem.getQuantity());
      part.setUpdatedAt(now);
      StoreOrderItem item = new StoreOrderItem();
      item.setPart(part);
      item.setSku(part.getSku());
      item.setPartName(part.getName());
      item.setUnitPriceMinor(part.getPriceMinor());
      item.setQuantity(cartItem.getQuantity());
      order.addItem(item);
      totalMinor = Math.addExact(totalMinor, item.getLineTotalMinor());

      StockHold hold = new StockHold();
      hold.setAttemptId(attempt.getAttemptId());
      hold.setPart(part);
      hold.setQuantity(cartItem.getQuantity());
      hold.setStatus("ACTIVE");
      hold.setCreatedAt(now);
      hold.setUpdatedAt(now);
      holds.save(hold);
    }
    order.setTotalMinor(totalMinor);
    orders.saveAndFlush(order);
    attempt.setAggregateId(order.getIdOrder());
    attempts.save(attempt);
    cartItems.deleteByUserId(userId);
    return new PreparedCheckout(attempt, order, null, true);
  }

  @Transactional
  public PreparedCheckout prepareDeposit(
      int userId, int listingId, String clientRequestId, CheckoutProfile profile) {
    var existing = attempts.findByUserIdAndPurposeAndClientRequestId(userId, DEPOSIT, clientRequestId);
    if (existing.isPresent()) {
      ListingDeposit previous = deposits.findByCheckoutAttemptId(existing.get().getAttemptId())
          .orElseThrow(ResourceNotFoundException::new);
      if (previous.getListing().getIdListing() != listingId) {
        throw new IllegalStateException("Idempotency-Key was already used for another checkout");
      }
      return prepared(existing.get(), false);
    }
    CarListing listing = listings.findByIdForUpdate(listingId).orElseThrow(ResourceNotFoundException::new);
    if (listing.getSellerId() == userId) throw new IllegalStateException("You cannot reserve your own listing");
    if (listing.getStatus() != CarListingStatus.ACTIVE) {
      throw new IllegalStateException("This listing is no longer available for reservation");
    }
    if (deposits.existsByListingAndBuyerIdAndStatusIn(
        listing, userId, List.of("PENDING_CHECKOUT", "PROVIDER_PENDING", "CHECKOUT_CREATED", "PAID"))) {
      throw new IllegalStateException("You already have an active deposit for this listing");
    }
    Instant now = clock.instant();
    String hash = sha256(userId + "|" + listingId + "|" + listing.getDepositAmountMinor()
        + "|" + paymentPolicy.getCurrency().toLowerCase());
    CheckoutAttempt attempt = newAttempt(userId, DEPOSIT, clientRequestId, hash, profile.email(), now);
    attempts.save(attempt);
    ListingDeposit deposit = new ListingDeposit();
    deposit.setListing(listing);
    deposit.setBuyerId(userId);
    deposit.setAmountMinor(listing.getDepositAmountMinor());
    deposit.setCurrency(paymentPolicy.getCurrency().toLowerCase());
    deposit.setStatus("PROVIDER_PENDING");
    deposit.setCheckoutAttemptId(attempt.getAttemptId());
    deposit.setCreatedAt(now);
    deposit.setUpdatedAt(now);
    deposits.saveAndFlush(deposit);
    listing.setStatus(CarListingStatus.RESERVED);
    listing.setUpdatedAt(now);
    attempt.setAggregateId(deposit.getIdDeposit());
    attempts.save(attempt);
    return new PreparedCheckout(attempt, null, deposit, true);
  }

  @Transactional(readOnly = true)
  public PreparedCheckout load(String attemptId) {
    return prepared(attempts.findById(attemptId).orElseThrow(ResourceNotFoundException::new), false);
  }

  @Transactional
  public CheckoutOutcome complete(String attemptId, StripeCheckoutResult checkout) {
    CheckoutAttempt attempt = attempts.findById(attemptId).orElseThrow(ResourceNotFoundException::new);
    if ("CHECKOUT_CREATED".equals(attempt.getStatus())) return outcome(attempt);
    Instant now = clock.instant();
    attempt.setProviderSessionId(checkout.sessionId());
    attempt.setProviderCheckoutUrl(checkout.checkoutUrl());
    attempt.setStatus("CHECKOUT_CREATED");
    attempt.setLastFailureCode(null);
    attempt.setNextReconcileAt(null);
    attempt.setUpdatedAt(now);
    if (STORE.equals(attempt.getPurpose())) {
      StoreOrder order = orders.findByCheckoutAttemptId(attemptId).orElseThrow(ResourceNotFoundException::new);
      order.setCheckoutSessionId(checkout.sessionId());
      order.setCheckoutUrl(checkout.checkoutUrl());
      order.setStatus("CHECKOUT_CREATED");
      order.setUpdatedAt(now);
    } else {
      ListingDeposit deposit = deposits.findByCheckoutAttemptId(attemptId).orElseThrow(ResourceNotFoundException::new);
      deposit.setCheckoutSessionId(checkout.sessionId());
      deposit.setCheckoutUrl(checkout.checkoutUrl());
      deposit.setStatus("CHECKOUT_CREATED");
      deposit.setUpdatedAt(now);
    }
    return outcome(attempts.save(attempt));
  }

  @Transactional
  public CheckoutOutcome markForReconciliation(String attemptId, String failureCode) {
    CheckoutAttempt attempt = attempts.findById(attemptId).orElseThrow(ResourceNotFoundException::new);
    if ("CHECKOUT_CREATED".equals(attempt.getStatus())) return outcome(attempt);
    int failures = attempt.getFailureCount() + 1;
    attempt.setFailureCount(failures);
    attempt.setLastFailureCode(safeFailureCode(failureCode));
    attempt.setStatus("RECONCILE_REQUIRED");
    Instant now = clock.instant();
    attempt.setNextReconcileAt(now.plusSeconds(Math.min(300, 1L << Math.min(failures, 8))));
    attempt.setUpdatedAt(now);
    return outcome(attempts.save(attempt));
  }

  @Transactional
  public void recordTerminal(String attemptId, String status, String paymentIntentId) {
    if (attemptId == null) return;
    CheckoutAttempt attempt = attempts.findById(attemptId).orElse(null);
    if (attempt == null || (attempt.getStatus().startsWith("PAID") && !status.startsWith("PAID"))) return;
    Instant now = clock.instant();
    attempt.setStatus(status);
    attempt.setPaymentIntentId(paymentIntentId);
    attempt.setNextReconcileAt(null);
    attempt.setUpdatedAt(now);
    attempts.save(attempt);
    if (STORE.equals(attempt.getPurpose())) {
      String holdStatus = "PAID".equals(status) ? "CONSUMED" : "RELEASED";
      for (StockHold hold : holds.findByAttemptId(attemptId)) {
        if ("ACTIVE".equals(hold.getStatus())
            || ("PAID".equals(status) && "RELEASED".equals(hold.getStatus()))) {
          hold.setStatus(holdStatus);
          hold.setUpdatedAt(now);
          hold.setReleasedAt("RELEASED".equals(holdStatus) ? now : null);
        }
      }
    }
  }

  @Transactional(readOnly = true)
  public List<String> dueAttemptIds(int limit) {
    return attempts.findByStatusInAndNextReconcileAtLessThanEqualOrderByCreatedAtAsc(
        RECONCILABLE, clock.instant(), PageRequest.of(0, limit)).stream()
        .map(CheckoutAttempt::getAttemptId).toList();
  }

  @Transactional
  public void expireAbandoned(int limit) {
    Instant now = clock.instant();
    for (CheckoutAttempt attempt : attempts
        .findByStatusInAndExpiresAtLessThanEqualOrderByCreatedAtAsc(
            RECONCILABLE, now, PageRequest.of(0, limit))) {
      attempt.setStatus("EXPIRED");
      attempt.setNextReconcileAt(null);
      attempt.setUpdatedAt(now);
      if (STORE.equals(attempt.getPurpose())) {
        StoreOrder order = orders.findByCheckoutAttemptId(attempt.getAttemptId()).orElse(null);
        if (order != null && !"PAID".equals(order.getStatus())) {
          order.setStatus("EXPIRED");
          order.setUpdatedAt(now);
        }
        for (StockHold hold : holds.findByAttemptId(attempt.getAttemptId())) {
          if ("ACTIVE".equals(hold.getStatus())) {
            hold.getPart().setStockQuantity(hold.getPart().getStockQuantity() + hold.getQuantity());
            hold.getPart().setUpdatedAt(now);
            hold.setStatus("RELEASED");
            hold.setReleasedAt(now);
            hold.setUpdatedAt(now);
          }
        }
      } else {
        ListingDeposit deposit = deposits.findByCheckoutAttemptId(attempt.getAttemptId()).orElse(null);
        if (deposit != null && !"PAID".equals(deposit.getStatus())) {
          deposit.setStatus("EXPIRED");
          deposit.setUpdatedAt(now);
          if (deposit.getListing().getStatus() == CarListingStatus.RESERVED) {
            deposit.getListing().setStatus(CarListingStatus.ACTIVE);
            deposit.getListing().setUpdatedAt(now);
          }
        }
      }
    }
  }

  private PreparedCheckout prepared(CheckoutAttempt attempt, boolean created) {
    StoreOrder order = STORE.equals(attempt.getPurpose())
        ? orders.findByCheckoutAttemptId(attempt.getAttemptId()).orElse(null) : null;
    ListingDeposit deposit = DEPOSIT.equals(attempt.getPurpose())
        ? deposits.findByCheckoutAttemptId(attempt.getAttemptId()).orElse(null) : null;
    return new PreparedCheckout(attempt, order, deposit, created);
  }

  private void rejectReusedStoreKey(
      CheckoutAttempt existing, int userId, CheckoutProfile profile) {
    List<CartItem> currentCart = cartItems.findByUserIdForUpdate(userId);
    if (currentCart.isEmpty()) return;
    List<Integer> partIds = currentCart.stream()
        .map(item -> item.getPart().getIdPart()).sorted().toList();
    Map<Integer, CarPart> lockedParts = parts.findAllByIdForUpdate(partIds).stream()
        .collect(Collectors.toMap(CarPart::getIdPart, Function.identity()));
    if (!existing.getRequestHash().equals(storeHash(userId, profile, currentCart, lockedParts))) {
      throw new IllegalStateException("Idempotency-Key was already used for another checkout");
    }
  }

  private CheckoutAttempt newAttempt(int userId, String purpose, String clientRequestId,
      String requestHash, String email, Instant now) {
    CheckoutAttempt attempt = new CheckoutAttempt();
    attempt.setAttemptId(UUID.randomUUID().toString());
    attempt.setUserId(userId);
    attempt.setPurpose(purpose);
    attempt.setClientRequestId(clientRequestId);
    attempt.setRequestHash(requestHash);
    attempt.setCustomerEmail(email);
    attempt.setStatus("PROVIDER_PENDING");
    attempt.setNextReconcileAt(now);
    attempt.setExpiresAt(now.plus(Duration.ofHours(24)));
    attempt.setCreatedAt(now);
    attempt.setUpdatedAt(now);
    return attempt;
  }

  private String storeHash(int userId, CheckoutProfile profile, List<CartItem> cart,
      Map<Integer, CarPart> lockedParts) {
    StringBuilder canonical = new StringBuilder().append(userId).append('|')
        .append(paymentPolicy.getCurrency().toLowerCase()).append('|')
        .append(profile.formattedAddress()).append('|');
    cart.stream().sorted((a, b) -> Integer.compare(a.getPart().getIdPart(), b.getPart().getIdPart()))
        .forEach(item -> {
          CarPart part = lockedParts.get(item.getPart().getIdPart());
          canonical.append(part.getIdPart()).append(':').append(part.getSku()).append(':')
              .append(part.getPriceMinor()).append(':').append(item.getQuantity()).append('|');
        });
    return sha256(canonical.toString());
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static String safeFailureCode(String value) {
    if (value == null || value.isBlank()) return "PROVIDER_ERROR";
    return value.substring(0, Math.min(value.length(), 80));
  }

  static CheckoutOutcome outcome(CheckoutAttempt attempt) {
    boolean retryable = RECONCILABLE.contains(attempt.getStatus());
    return new CheckoutOutcome(attempt.getProviderCheckoutUrl(), attempt.getAttemptId(),
        attempt.getStatus(), retryable);
  }

  public record PreparedCheckout(
      CheckoutAttempt attempt, StoreOrder order, ListingDeposit deposit, boolean created) {
    public CheckoutOutcome outcome() { return CheckoutPreparationService.outcome(attempt); }
  }
}
