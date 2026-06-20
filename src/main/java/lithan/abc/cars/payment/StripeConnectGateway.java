package lithan.abc.cars.payment;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.model.v2.core.Account;
import com.stripe.model.v2.core.AccountLink;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.v2.core.AccountCreateParams;
import com.stripe.param.v2.core.AccountLinkCreateParams;
import com.stripe.param.v2.core.AccountRetrieveParams;

import lithan.abc.cars.config.StripeProperties;
import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.entity.UserAccount;

@Component
@ConditionalOnProperty(name = "payments.stripe.enabled", havingValue = "true")
public class StripeConnectGateway implements StripeGateway {

  private final StripeProperties properties;
  private final StripeClient stripeClient;

  public StripeConnectGateway(StripeProperties properties) {
    properties.validate();
    this.properties = properties;
    this.stripeClient = new StripeClient(properties.getSecretKey());
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String createConnectedAccount(UserAccount seller) {
    try {
      AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance.StripeTransfers transfers =
          AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance.StripeTransfers.builder()
              .setRequested(true)
              .build();
      AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance stripeBalance =
          AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance.builder()
              .setStripeTransfers(transfers)
              .build();
      AccountCreateParams.Configuration.Recipient.Capabilities capabilities =
          AccountCreateParams.Configuration.Recipient.Capabilities.builder()
              .setStripeBalance(stripeBalance)
              .build();
      AccountCreateParams.Configuration.Recipient recipient =
          AccountCreateParams.Configuration.Recipient.builder()
              .setCapabilities(capabilities)
              .build();

      AccountCreateParams params = AccountCreateParams.builder()
          .setDisplayName(seller.getProfile().getFirstName() + " " + seller.getProfile().getLastName())
          .setDashboard(AccountCreateParams.Dashboard.EXPRESS)
          .setConfiguration(AccountCreateParams.Configuration.builder().setRecipient(recipient).build())
          .setDefaults(AccountCreateParams.Defaults.builder()
              .setCurrency(properties.getCurrency())
              .setResponsibilities(AccountCreateParams.Defaults.Responsibilities.builder()
                  .setFeesCollector(AccountCreateParams.Defaults.Responsibilities.FeesCollector.APPLICATION)
                  .setLossesCollector(AccountCreateParams.Defaults.Responsibilities.LossesCollector.APPLICATION)
                  .build())
              .build())
          .putMetadata("subotin_user_id", Integer.toString(seller.getIdUser()))
          .build();

      return stripeClient.v2().core().accounts().create(params).getId();
    } catch (StripeException exception) {
      throw new PaymentProviderException("Unable to create Stripe connected account", exception);
    }
  }

  @Override
  public String createOnboardingLink(String accountId) {
    try {
      AccountLinkCreateParams.UseCase.AccountOnboarding onboarding =
          AccountLinkCreateParams.UseCase.AccountOnboarding.builder()
              .addConfiguration(AccountLinkCreateParams.UseCase.AccountOnboarding.Configuration.RECIPIENT)
              .setRefreshUrl(properties.getBaseUrl() + "/payments/seller/onboarding")
              .setReturnUrl(properties.getBaseUrl() + "/payments/seller/return")
              .build();
      AccountLinkCreateParams params = AccountLinkCreateParams.builder()
          .setAccount(accountId)
          .setUseCase(AccountLinkCreateParams.UseCase.builder()
              .setType(AccountLinkCreateParams.UseCase.Type.ACCOUNT_ONBOARDING)
              .setAccountOnboarding(onboarding)
              .build())
          .build();
      AccountLink accountLink = stripeClient.v2().core().accountLinks().create(params);
      return accountLink.getUrl();
    } catch (StripeException exception) {
      throw new PaymentProviderException("Unable to create Stripe onboarding link", exception);
    }
  }

  @Override
  public StripeAccountState retrieveAccountState(String accountId) {
    try {
      Account account = stripeClient.v2().core().accounts().retrieve(accountId,
          AccountRetrieveParams.builder()
              .addInclude(AccountRetrieveParams.Include.CONFIGURATION__RECIPIENT)
              .addInclude(AccountRetrieveParams.Include.REQUIREMENTS)
              .build());
      String status = Optional.ofNullable(account.getConfiguration())
          .map(Account.Configuration::getRecipient)
          .map(Account.Configuration.Recipient::getCapabilities)
          .map(Account.Configuration.Recipient.Capabilities::getStripeBalance)
          .map(Account.Configuration.Recipient.Capabilities.StripeBalance::getStripeTransfers)
          .map(Account.Configuration.Recipient.Capabilities.StripeBalance.StripeTransfers::getStatus)
          .orElse("pending");
      return new StripeAccountState(accountId, "active".equalsIgnoreCase(status), status.toUpperCase());
    } catch (StripeException exception) {
      throw new PaymentProviderException("Unable to refresh Stripe account status", exception);
    }
  }

  @Override
  public StripeCheckoutResult createCheckoutSession(PaymentOrder paymentOrder, String destinationAccountId) {
    try {
      Car car = paymentOrder.getBid().getCar();
      SessionCreateParams.LineItem.PriceData.ProductData productData =
          SessionCreateParams.LineItem.PriceData.ProductData.builder()
              .setName(car.getMake() + " " + car.getModel() + " (" + car.getYear() + ")")
              .setDescription("Accepted marketplace bid #" + paymentOrder.getBid().getIdBid())
              .build();
      SessionCreateParams.LineItem.PriceData priceData =
          SessionCreateParams.LineItem.PriceData.builder()
              .setCurrency(paymentOrder.getCurrency())
              .setUnitAmount(paymentOrder.getAmountMinor())
              .setProductData(productData)
              .build();
      SessionCreateParams.PaymentIntentData paymentIntentData =
          SessionCreateParams.PaymentIntentData.builder()
              .setApplicationFeeAmount(paymentOrder.getPlatformFeeMinor())
              .setTransferData(SessionCreateParams.PaymentIntentData.TransferData.builder()
                  .setDestination(destinationAccountId)
                  .build())
              .putMetadata("payment_order_id", Integer.toString(paymentOrder.getIdPayment()))
              .putMetadata("bid_id", Integer.toString(paymentOrder.getBid().getIdBid()))
              .build();
      SessionCreateParams params = SessionCreateParams.builder()
          .setMode(SessionCreateParams.Mode.PAYMENT)
          .setClientReferenceId(Integer.toString(paymentOrder.getIdPayment()))
          .setSuccessUrl(properties.getBaseUrl() + "/payments/success?session_id={CHECKOUT_SESSION_ID}")
          .setCancelUrl(properties.getBaseUrl() + "/user/payments?canceled")
          .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
          .addLineItem(SessionCreateParams.LineItem.builder()
              .setQuantity(1L)
              .setPriceData(priceData)
              .build())
          .setPaymentIntentData(paymentIntentData)
          .putMetadata("payment_order_id", Integer.toString(paymentOrder.getIdPayment()))
          .build();

      Session session = stripeClient.checkout().sessions().create(params);
      return new StripeCheckoutResult(session.getId(), session.getUrl());
    } catch (StripeException exception) {
      throw new PaymentProviderException("Unable to create Stripe checkout", exception);
    }
  }

  @Override
  public StripeWebhookEvent verifyAndParseWebhook(String payload, String signature) {
    Event event;
    try {
      event = stripeClient.constructEvent(payload, signature, properties.getWebhookSecret());
    } catch (Exception exception) {
      throw new PaymentProviderException("Invalid Stripe webhook signature", exception);
    }

    StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
    if (object instanceof Session session) {
      return new StripeWebhookEvent(
          event.getId(),
          event.getType(),
          session.getId(),
          session.getPaymentIntent(),
          session.getPaymentStatus());
    }
    return new StripeWebhookEvent(event.getId(), event.getType(), null, null, null);
  }
}
