package lithan.autostrada.auctions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import fixtures.identity.repository.UserRepository;

public class WithIdentityFactory implements WithSecurityContextFactory<WithIdentity> {
  @Autowired private UserRepository users;
  @Override public SecurityContext createSecurityContext(WithIdentity annotation) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(TestIdentity.authentication(users, annotation.username(), annotation.roles()));
    return context;
  }
}
