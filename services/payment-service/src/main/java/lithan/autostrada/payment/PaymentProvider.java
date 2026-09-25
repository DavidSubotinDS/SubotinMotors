package lithan.autostrada.payment;

interface PaymentProvider {
  boolean enabled();
  PaymentContracts.ProviderResult create(PaymentStore.Attempt attempt);
  PaymentContracts.ProviderEvent verify(String payload,String signature);
  default void expire(PaymentStore.Attempt attempt) { }
  default java.util.Optional<PaymentContracts.ProviderState> retrieve(PaymentStore.Attempt attempt) { return java.util.Optional.empty(); }
}
