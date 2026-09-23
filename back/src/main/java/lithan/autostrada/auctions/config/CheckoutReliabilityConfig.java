package lithan.autostrada.auctions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lithan.autostrada.auctions.service.CheckoutCrashProbe;

@Configuration
public class CheckoutReliabilityConfig {
  @Bean
  CheckoutCrashProbe checkoutCrashProbe() {
    return (point, attemptId) -> { };
  }
}

