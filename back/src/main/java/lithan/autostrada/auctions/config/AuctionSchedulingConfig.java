package lithan.autostrada.auctions.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
// Individual workers own their switches. Disabling auction scans must not stop outbox recovery.
public class AuctionSchedulingConfig {
}
