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
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages={"lithan.autostrada.auctions.entity","fixtures.identity.entity"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages={"lithan.autostrada.auctions.repository","fixtures.identity.repository"})
public class BusinessIdentityFixtures {
  @Bean PasswordEncoder fixturePasswordEncoder(){return new BCryptPasswordEncoder();}
  @Bean UserService fixtureUsers(){return new UserServiceImpl();}
  @Bean @Primary RemoteProfileClient fixtureProfiles(jakarta.persistence.EntityManager em, CurrentIdentity actor, UserRepository users) {
    var local=new InProcessProfileClient(em,actor);
    var mapper=new IdentityApiMapper();
    return new RemoteProfileClient("http://127.0.0.1:1","fixture-only",actor) {
      @Override public Map<Integer,PublicProfile> findAll(Collection<Integer> ids){return local.findAll(ids);}
      @Override public Optional<PublicProfile> findByProfileId(int id){return local.findByProfileId(id);}
      @Override public CheckoutProfile current(){return local.current();}
      @Override public ProfileResponse self(){return mapper.profile(users.findById(actor.requireUserId()).orElseThrow());}
    };
  }
}
