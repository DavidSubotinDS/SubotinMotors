package lithan.autostrada.identity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

/** Test-only authenticated fixture with the same scalar principal as production. */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithIdentityFactory.class)
public @interface WithIdentity {
  String username();
  String[] roles() default {"USER"};
}
