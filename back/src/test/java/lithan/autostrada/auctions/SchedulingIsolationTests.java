package lithan.autostrada.auctions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;
import lithan.autostrada.auctions.config.AuctionSchedulingConfig;
import static org.awaitility.Awaitility.await;

class SchedulingIsolationTests {
  @Test void disablingAuctionScanLeavesIndependentScheduledWorkersRunning() {
    new ApplicationContextRunner().withUserConfiguration(AuctionSchedulingConfig.class,ProbeConfig.class)
        .withPropertyValues("auction.notifications.scheduling-enabled=false").run(context -> {
          Probe probe=context.getBean(Probe.class);
          await().atMost(Duration.ofSeconds(3)).until(()->probe.runs.get()>0);
        });
  }
  @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false) static class ProbeConfig {
    @Bean Probe probe(){return new Probe();}
  }
  static class Probe {
    final AtomicInteger runs=new AtomicInteger();
    @Scheduled(fixedDelay=10) public void tick(){runs.incrementAndGet();}
  }
}
