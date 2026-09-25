package lithan.autostrada.payment;

import java.time.Instant;

final class PaymentContracts {
  private PaymentContracts() {}
  record CreatePayment(String attemptId,String sourceService,String businessType,String businessId,
      long businessVersion,String buyerId,long amountMinor,String currency,String description,
      String returnRoute,String customerEmail) { }
  record PaymentView(String paymentId,String attemptId,String sourceService,String businessType,
      String businessId,long businessVersion,String buyerId,long amountMinor,String currency,
      String status,String checkoutUrl,Instant expiresAt,long version) { }
  record Capability(boolean enabled,java.util.List<String> acceptedCurrencies,String mode) { }
  record ProviderResult(String sessionId,String checkoutUrl,String paymentIntentId) { }
  record ProviderEvent(String eventId,String eventType,String sessionId,String paymentIntentId,
      String paymentStatus,String payloadHash) { }
  record ProviderState(String sessionId,String paymentIntentId,String paymentStatus,String sessionStatus) { }
  record Prepared(PaymentStore.Attempt attempt,boolean created) { }
}
