package lithan.autostrada.auctions;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lithan.autostrada.auctions.identity.*;
import lithan.autostrada.auctions.service.*;
import fixtures.identity.repository.UserRepository;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileResponse;
import java.util.*;

/** Historical H2 dataset builders and local peer stub for business unit/integration tests.
 * No login filter, session owner or production authentication bypass is installed. */
@TestConfiguration
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages={"lithan.autostrada.auctions.entity","fixtures.identity.entity","fixtures.notification.entity"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages={"lithan.autostrada.auctions.repository","fixtures.identity.repository","fixtures.notification.repository"})
public class BusinessIdentityFixtures {
  @Bean @Primary lithan.autostrada.auctions.notification.NotificationClient fixtureNotificationCounts(CurrentIdentity actor) {
    return new lithan.autostrada.auctions.notification.NotificationClient("http://127.0.0.1:1","http://127.0.0.1:1","fixture-only",actor,org.springframework.web.client.RestClient.builder()) {
      @Override public long unreadCount(){return 0;}
    };
  }
  @Bean @org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
  org.springframework.beans.factory.InitializingBean fixtureOutbox(javax.sql.DataSource ds) {
    return ()->{
      var jdbc=new org.springframework.jdbc.core.JdbcTemplate(ds);
      if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name='tb_notification_outbox'",Integer.class)==0)
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.ClassPathResource("db/migration/V20__notification_outbox.sql")).execute(ds);
    };
  }
  @Bean PasswordEncoder fixturePasswordEncoder(){return new BCryptPasswordEncoder();}
  @Bean UserService fixtureUsers(){return new UserServiceImpl();}
  @Bean @Primary RemoteProfileClient fixtureProfiles(jakarta.persistence.EntityManager em, CurrentIdentity actor, UserRepository users) {
    var local=new InProcessProfileClient(em,actor);
    var mapper=new IdentityApiMapper();
    return new RemoteProfileClient("http://127.0.0.1:1","fixture-only",actor,
      org.springframework.web.client.RestClient.builder()) {
      @Override public Map<Integer,PublicProfile> findAll(Collection<Integer> ids){return local.findAll(ids);}
      @Override public Optional<PublicProfile> findByProfileId(int id){return local.findByProfileId(id);}
      @Override public CheckoutProfile current(){return local.current();}
      @Override public ProfileResponse self(){return mapper.profile(users.findById(actor.requireUserId()).orElseThrow());}
    };
  }
}
