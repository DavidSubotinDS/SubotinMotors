package lithan.autostrada.identity;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class IdentityTestBase {
  static final String GATEWAY_SECRET="gateway-test-secret-000000000000000000000";
  static final String BACKEND_SECRET="backend-test-secret-000000000000000000000";
  static final com.nimbusds.jose.jwk.RSAKey KEY;
  static {try {KEY=new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).keyID("identity-test").generate();}catch(Exception e){throw new ExceptionInInitializerError(e);}}
  @DynamicPropertySource static void settings(DynamicPropertyRegistry r) {
    r.add("identity.signing-jwk",KEY::toJSONString);r.add("identity.gateway-secret",()->GATEWAY_SECRET);
    r.add("identity.backend-secret",()->BACKEND_SECRET);
    r.add("notification.delivery-key",()->java.util.Base64.getEncoder().encodeToString(new byte[32]));
    r.add("notification.relay.enabled",()->"false");
    r.add("spring.datasource.url",()->"jdbc:h2:mem:identity_tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
  }
  @Autowired JdbcTemplate fixtureSql;
  @Autowired PasswordEncoder fixtureEncoder;
  @BeforeEach void accounts() {
    String[] names={"admin123","user123","demo_bidder","demo_seller","demo_trader","demo_newcomer","sample_one","sample_two"};
    for(int i=0;i<names.length;i++) {
      String name=names[i];
      if(fixtureSql.queryForObject("SELECT COUNT(*) FROM tb_user WHERE username=?",Integer.class,name)>0)continue;
      fixtureSql.update("INSERT INTO tb_user(id_user,username,password,email) VALUES(?,?,?,?)",i+1,name,
          fixtureEncoder.encode(i<2?name:"demo123"),name+"@example.invalid");
      fixtureSql.update("INSERT INTO tb_user_profile(id_profile,id_user,first_name,last_name,phone_number) VALUES(?,?,?,?,?)",(i+1)*10,i+1,name,"Fixture","+381641234567");
      fixtureSql.update("INSERT INTO tb_role(id_user,role) VALUES(?,'ROLE_USER')",i+1);
    }
    if(fixtureSql.queryForObject("SELECT COUNT(*) FROM tb_role WHERE id_user=1 AND role='ROLE_ADMIN'",Integer.class)==0)
      fixtureSql.update("INSERT INTO tb_role(id_user,role) VALUES(1,'ROLE_ADMIN')");
    fixtureSql.execute("ALTER TABLE tb_user ALTER COLUMN id_user RESTART WITH 100");
    fixtureSql.execute("ALTER TABLE tb_user_profile ALTER COLUMN id_profile RESTART WITH 100");
  }
}
