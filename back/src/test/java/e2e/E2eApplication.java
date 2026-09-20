package e2e;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import lithan.autostrada.auctions.AutostradaAuctionsApplication;

/** Test classpath only: never included in the deployable JAR or normal startup. */
public class E2eApplication {
  public static final Instant START = Instant.parse("2030-06-15T10:00:00Z");

  public static void main(String[] ignored) {
    String token = System.getenv("E2E_CONTROL_TOKEN");
    if (token == null || token.length() < 32) {
      throw new IllegalStateException("Start with npm run test:e2e; a per-run control token is required");
    }
    String databaseUrl = "jdbc:h2:mem:e2e_" + UUID.randomUUID()
        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    // CLI properties override inherited Spring environment settings. Never read normal config.
    new SpringApplication(AutostradaAuctionsApplication.class, Configuration.class).run(
        "--spring.config.location=classpath:/application-e2e.properties",
        "--spring.profiles.active=e2e",
        "--spring.datasource.url=" + databaseUrl,
        "--spring.datasource.driver-class-name=org.h2.Driver",
        "--spring.datasource.username=sa", "--spring.datasource.password=",
        "--spring.flyway.url=" + databaseUrl, "--spring.flyway.user=sa", "--spring.flyway.password=",
        "--server.address=127.0.0.1", "--server.port=18080",
        "--payments.stripe.enabled=false", "--app.mail.mode=log",
        "--auction.notifications.scheduling-enabled=false");
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Configuration {
    @Bean @Primary
    MutableClock e2eClock() { return new MutableClock(); }

    @Bean @Primary
    SimulatedStripeGateway e2eStripeGateway() { return new SimulatedStripeGateway(); }

    @Bean
    Controls e2eControls(JdbcTemplate jdbc, PasswordEncoder encoder, MutableClock clock) {
      return new Controls(jdbc, encoder, clock);
    }

    @Bean @Order(0)
    SecurityFilterChain e2eControlSecurity(HttpSecurity http) throws Exception {
      return http.securityMatcher("/__e2e/**").csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth.anyRequest().access((authentication, context) ->
              new AuthorizationDecision(System.getenv("E2E_CONTROL_TOKEN")
                  .equals(context.getRequest().getHeader("X-E2E-Control")))))
          .build();
    }
  }

  static class MutableClock extends Clock {
    private volatile Instant time = START;
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(time, zone); }
    @Override public Instant instant() { return time; }
  }

  @RestController
  static class Controls {
    private final JdbcTemplate jdbc;
    private final String passwordHash;
    private final MutableClock clock;

    Controls(JdbcTemplate jdbc, PasswordEncoder encoder, MutableClock clock) {
      this.jdbc = jdbc;
      this.passwordHash = encoder.encode("E2e-pass-123!");
      this.clock = clock;
    }

    @GetMapping("/__e2e/ready")
    public Map<String, String> ready() {
      return Map.of("mode", "isolated-e2e", "stripeApiVersion", Stripe.API_VERSION);
    }

    @PostMapping("/__e2e/clock")
    public Map<String, String> time(@RequestBody Map<String, String> body) {
      clock.time = Instant.parse(body.get("instant"));
      return Map.of("instant", clock.time.toString());
    }

    @PostMapping("/__e2e/reset")
    public Map<String, Object> reset() throws Exception {
      // Same connection for guard, reset and integrity restoration. Never reset normal databases.
      boolean mysql;
      try (var connection = jdbc.getDataSource().getConnection(); var statement = connection.createStatement()) {
        String url = connection.getMetaData().getURL();
        mysql = url.startsWith("jdbc:mysql://mysql:3306/e2e_");
        if (mysql) {
          if (!ComposeE2eApplication.database().equals(connection.getCatalog())
              || !System.getenv("E2E_CONTROL_TOKEN").equals(
                  jdbc.queryForObject("SELECT token FROM e2e_guard", String.class))) {
            throw new IllegalStateException("MySQL database is not owned by this harness");
          }
        } else if (!url.startsWith("jdbc:h2:mem:e2e_")) {
          throw new IllegalStateException("Refusing to reset a non-E2E database");
        }
        var tables = jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema="
                + (mysql ? "DATABASE()" : "'public'") + " AND table_name LIKE 'tb_%'",
            String.class);
        statement.execute(mysql ? "SET FOREIGN_KEY_CHECKS=0" : "SET REFERENTIAL_INTEGRITY FALSE");
        try {
          for (String table : tables) {
            if (!table.matches("tb_[a-z_]+")) throw new IllegalStateException("Unexpected table");
            statement.execute("TRUNCATE TABLE " + table + (mysql ? "" : " RESTART IDENTITY"));
          }
        } finally {
          statement.execute(mysql ? "SET FOREIGN_KEY_CHECKS=1" : "SET REFERENTIAL_INTEGRITY TRUE");
        }
      }
      clock.time = START;
      for (int i = 1; i <= 4; i++) {
        String username = new String[] {"buyer", "seller", "other", "admin"}[i - 1];
        jdbc.update("INSERT INTO tb_user (id_user,username,password,email) VALUES (?,?,?,?)",
            i, username, passwordHash, username + "@e2e.invalid");
        jdbc.update("INSERT INTO tb_role (role,id_user) VALUES ('ROLE_USER',?)", i);
        jdbc.update("INSERT INTO tb_user_profile (id_profile,first_name,last_name,phone_number,address,id_user) VALUES (?,?,?,'+381641234567','Novi Sad',?)",
            i, username, "Fixture", i);
      }
      jdbc.update("INSERT INTO tb_role (role,id_user) VALUES ('ROLE_ADMIN',4)");
      if (!mysql) {
        jdbc.update("ALTER TABLE tb_user ALTER COLUMN id_user RESTART WITH 5");
        jdbc.update("ALTER TABLE tb_user_profile ALTER COLUMN id_profile RESTART WITH 5");
      }
      jdbc.update("INSERT INTO tb_car (make,model,production_year,status,price,id_user,auction_end_time) VALUES ('E2E','Roadster','2024','ACTIVE',10000,2,'2030-06-15 12:00:00')");
      jdbc.update("INSERT INTO tb_car (make,model,production_year,status,price,id_user,auction_end_time) VALUES ('E2E','Coupe','2023','ACTIVE',8000,2,'2030-06-15 13:00:00')");
      jdbc.update("INSERT INTO tb_car_listing (title,make,model,production_year,mileage,fuel_type,transmission,price_minor,deposit_amount_minor,description,status,id_seller,created_at,updated_at) VALUES ('E2E Touring','E2E','Touring','2024',12000,'Petrol','Manual',2500000,50000,'Deterministic test vehicle','ACTIVE',2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
      jdbc.update("INSERT INTO tb_car_part (sku,name,category,description,price_minor,stock_quantity,active,created_at,updated_at) VALUES ('E2E-FILTER','E2E Oil Filter','Filters','Test replacement filter',2500,10,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
      return Map.of("auctionId", 1, "secondAuctionId", 2, "listingId", 1, "partId", 1,
          "instant", START.toString(), "stripeApiVersion", Stripe.API_VERSION);
    }
  }
}
