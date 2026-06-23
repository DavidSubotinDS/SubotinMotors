package lithan.autostrada.auctions.service;

import java.time.Instant;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.config.StripeProperties;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.CarListingStatus;
import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.entity.PaymentWebhookEvent;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.payment.StripeCheckoutResult;
import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.CarListingRepository;
import lithan.autostrada.auctions.repository.ListingDepositRepository;
import lithan.autostrada.auctions.repository.PaymentWebhookEventRepository;

@Service
public class ListingDepositServiceImpl implements ListingDepositService {

  private static final Set<String> ACTIVE_DEPOSIT_STATUSES =
      Set.of("PENDING_CHECKOUT", "CHECKOUT_CREATED", "PAID");

  private final ListingDepositRepository depositRepository;
  private final CarListingRepository listingRepository;
  private final PaymentWebhookEventRepository webhookEventRepository;
  private final UserService userService;
  private final StripeGateway stripeGateway;
  private final StripeProperties stripeProperties;

  public ListingDepositServiceImpl(
      ListingDepositRepository depositRepository,
      CarListingRepository listingRepository,
      PaymentWebhookEventRepository webhookEventRepository,
      UserService userService,
      StripeGateway stripeGateway,
      StripeProperties stripeProperties) {
    this.depositRepository = depositRepository;
    this.listingRepository = listingRepository;
    this.webhookEventRepository = webhookEventRepository;
    this.userService = userService;
    this.stripeGateway = stripeGateway;
    this.stripeProperties = stripeProperties;
  }

  @Override
  public boolean isStripeEnabled() {
    return stripeGateway.isEnabled();
  }

  @Override
  @Transactional
  public String startCheckout(int listingId) {
    if (!stripeGateway.isEnabled()) {
      throw new IllegalStateException("Stripe sandbox checkout is not currently enabled");
    }
    CarListing listing = listingRepository.findByIdForUpdate(listingId)
        .orElseThrow(ResourceNotFoundException::new);
    UserAccount buyer = userService.getUserLogin();
    if (listing.getSeller().getIdUser() == buyer.getIdUser()) {
      throw new IllegalStateException("You cannot reserve your own listing");
    }
    if (listing.getStatus() != CarListingStatus.ACTIVE) {
      throw new IllegalStateException("This listing is no longer available for reservation");
    }
    if (depositRepository.existsByListingAndBuyerAndStatusIn(
        listing, buyer, ACTIVE_DEPOSIT_STATUSES)) {
      throw new IllegalStateException("You already have an active deposit for this listing");
    }

    Instant now = Instant.now();
    ListingDeposit deposit = new ListingDeposit();
    deposit.setListing(listing);
    deposit.setBuyer(buyer);
    deposit.setAmountMinor(listing.getDepositAmountMinor());
    deposit.setCurrency(stripeProperties.getCurrency().toLowerCase());
    deposit.setStatus("PENDING_CHECKOUT");
    deposit.setCreatedAt(now);
    deposit.setUpdatedAt(now);
    depositRepository.saveAndFlush(deposit);

    listing.setStatus(CarListingStatus.RESERVED);
    listing.setUpdatedAt(now);
    listingRepository.save(listing);

    StripeCheckoutResult checkout = stripeGateway.createListingDepositCheckoutSession(deposit);
    deposit.setCheckoutSessionId(checkout.sessionId());
    deposit.setCheckoutUrl(checkout.checkoutUrl());
    deposit.setStatus("CHECKOUT_CREATED");
    deposit.setUpdatedAt(Instant.now());
    depositRepository.save(deposit);
    return checkout.checkoutUrl();
  }

  @Override
  public Page<ListingDeposit> currentUserDeposits(Pageable pageable) {
    return depositRepository.findByBuyer(userService.getUserLogin(), pageable);
  }

  @Override
  public ListingDeposit currentUserDepositBySession(String sessionId) {
    ListingDeposit deposit = depositRepository.findByCheckoutSessionId(sessionId)
        .orElseThrow(ResourceNotFoundException::new);
    if (deposit.getBuyer().getIdUser() != userService.getUserLogin().getIdUser()) {
      throw new ResourceNotFoundException();
    }
    return deposit;
  }

  @Override
  @Transactional
  public boolean processWebhook(StripeWebhookEvent event) {
    if (webhookEventRepository.existsByProviderEventId(event.eventId())) {
      return true;
    }
    if (event.checkoutSessionId() == null) {
      return false;
    }
    ListingDeposit deposit = depositRepository.findByCheckoutSessionId(
        event.checkoutSessionId()).orElse(null);
    if (deposit == null) {
      return false;
    }

    switch (event.eventType()) {
      case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
        if ("paid".equalsIgnoreCase(event.paymentStatus())) {
          deposit.setStatus("PAID");
          deposit.setPaymentIntentId(event.paymentIntentId());
          deposit.setPaidAt(Instant.now());
        }
      }
      case "checkout.session.async_payment_failed" -> release(deposit, "PAYMENT_FAILED");
      case "checkout.session.expired" -> release(deposit, "EXPIRED");
      default -> {
        return false;
      }
    }
    deposit.setUpdatedAt(Instant.now());
    depositRepository.save(deposit);

    PaymentWebhookEvent processed = new PaymentWebhookEvent();
    processed.setProviderEventId(event.eventId());
    processed.setEventType(event.eventType());
    processed.setProcessedAt(Instant.now());
    webhookEventRepository.save(processed);
    return true;
  }

  private void release(ListingDeposit deposit, String status) {
    if ("PAID".equals(deposit.getStatus())
        || "PAYMENT_FAILED".equals(deposit.getStatus())
        || "EXPIRED".equals(deposit.getStatus())) {
      return;
    }
    deposit.setStatus(status);
    if (deposit.getListing().getStatus() == CarListingStatus.RESERVED) {
      deposit.getListing().setStatus(CarListingStatus.ACTIVE);
      deposit.getListing().setUpdatedAt(Instant.now());
    }
  }
}
