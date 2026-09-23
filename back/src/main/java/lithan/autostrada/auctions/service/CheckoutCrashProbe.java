package lithan.autostrada.auctions.service;

/** Test seam for deterministic process-interruption tests; production always uses the no-op bean. */
public interface CheckoutCrashProbe {
  enum Point { AFTER_LOCAL_COMMIT, AFTER_PROVIDER_CREATE }
  void check(Point point, String attemptId);
}

