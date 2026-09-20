package e2e;

import org.springframework.boot.SpringApplication;
import lithan.autostrada.auctions.AutostradaAuctionsApplication;

/** Only in the explicitly built e2e image. No arbitrary JDBC URL or CLI arguments. */
public class ComposeE2eApplication {
  static String database() {
    String database = System.getenv("E2E_DATABASE");
    if (database == null || !database.matches("e2e_[a-f0-9]{32}")) {
      throw new IllegalStateException("A harness-owned MySQL schema is required");
    }
    return database;
  }

  public static void main(String[] ignored) {
    String token = System.getenv("E2E_CONTROL_TOKEN");
    if (token == null || !token.matches("[a-f0-9]{64}")) {
      throw new IllegalStateException("A per-run control token is required");
    }
    String url = "jdbc:mysql://mysql:3306/" + database()
        + "?serverTimezone=UTC&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=true&useSSL=false";
    new SpringApplication(AutostradaAuctionsApplication.class, E2eApplication.Configuration.class).run(
        "--spring.config.location=classpath:/application.properties",
        "--spring.profiles.active=mysql,gateway,container",
        "--spring.datasource.url=" + url, "--spring.flyway.url=" + url,
        "--spring.datasource.username=e2e", "--spring.flyway.user=e2e",
        "--spring.flyway.password=" + System.getenv("DB_PASSWORD"),
        "--payments.stripe.enabled=false", "--app.mail.mode=log",
        "--auction.notifications.scheduling-enabled=false");
  }
}
