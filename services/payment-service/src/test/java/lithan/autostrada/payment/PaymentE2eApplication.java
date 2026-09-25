package lithan.autostrada.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;

public final class PaymentE2eApplication {
  public static void main(String[] ignored){
    String token=System.getenv("E2E_CONTROL_TOKEN");if(token==null||token.length()<32)throw new IllegalStateException("A private E2E control token is required");
    boolean mysql=System.getenv("PAYMENT_DB_URL")!=null;String url=mysql?System.getenv("PAYMENT_DB_URL"):"jdbc:h2:mem:payment_e2e_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    new SpringApplication(PaymentApplication.class,Configuration.class).run("--spring.datasource.url="+url,"--spring.datasource.username="+(mysql?System.getenv("PAYMENT_DB_USERNAME"):"sa"),"--spring.datasource.password="+(mysql?System.getenv("PAYMENT_DB_PASSWORD"):""),"--identity.verification-jwks="+System.getenv("IDENTITY_VERIFICATION_JWKS"),"--payment.broker.enabled="+System.getenv().getOrDefault("E2E_PAYMENT_BROKER_ENABLED","false"),"--server.address=0.0.0.0","--server.port="+System.getenv().getOrDefault("PAYMENT_PORT","8080"));
  }
  @TestConfiguration(proxyBeanMethods=false)
  static class Configuration {
    @Bean @Primary PaymentProvider e2eProvider(ObjectMapper json){return new SimulatedProvider(json);}
    @Bean @Order(0) SecurityFilterChain controls(HttpSecurity http)throws Exception{return http.securityMatcher("/__e2e/**").csrf(c->c.disable()).authorizeHttpRequests(a->a.anyRequest().access((auth,context)->new AuthorizationDecision(System.getenv("E2E_CONTROL_TOKEN").equals(context.getRequest().getHeader("X-E2E-Control"))))).build();}
  }
  static final class SimulatedProvider implements PaymentProvider {
    private final ObjectMapper json;SimulatedProvider(ObjectMapper json){this.json=json;}
    public boolean enabled(){return true;}
    public PaymentContracts.ProviderResult create(PaymentStore.Attempt a){String prefix="STORE_ORDER".equals(a.businessType())?"store":"deposit";String suffix=a.attemptId().substring(0,8);String session="cs_e2e_"+prefix+"_"+a.businessId()+"_"+suffix;String base=System.getenv().getOrDefault("PUBLIC_URL","http://127.0.0.1:18081");return new PaymentContracts.ProviderResult(session,base+"/__provider/checkout?session_id="+session,"pi_e2e_"+prefix+"_"+a.businessId()+"_"+suffix);}
    public PaymentContracts.ProviderEvent verify(String payload,String signature){
      try{Map<String,String> parts=new HashMap<>();for(String p:signature.split(",")){String[] kv=p.split("=",2);if(kv.length==2)parts.put(kv[0],kv[1]);}String signed=parts.get("t")+"."+payload;Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec("whsec_e2e_public_fixture_secret".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));String expected=java.util.HexFormat.of().formatHex(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));if(!java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),Objects.toString(parts.get("v1"),"").getBytes(StandardCharsets.US_ASCII)))throw new IllegalArgumentException();var root=json.readTree(payload);var object=root.path("data").path("object");return new PaymentContracts.ProviderEvent(root.path("id").asText(),root.path("type").asText(),object.path("id").asText(null),object.path("payment_intent").asText(null),object.path("payment_status").asText(null),sha(payload));}catch(Exception e){throw new PaymentProviderException("Invalid simulated signature",e);}
    }
    private static String sha(String value)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
  }
  @RestController static class Controls {
    private final JdbcTemplate db;Controls(JdbcTemplate db){this.db=db;}
    @GetMapping("/__e2e/ready")Map<String,String> ready(){return Map.of("mode","isolated-e2e");}
    @GetMapping("/__e2e/outbox")List<Map<String,Object>> outbox(){return db.queryForList("SELECT event_id,payload_json FROM payment_outbox WHERE status='PENDING' ORDER BY created_at");}
    @PostMapping("/__e2e/outbox-ack")void ack(@RequestBody Map<String,String> body){db.update("UPDATE payment_outbox SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP WHERE event_id=?",body.get("eventId"));}
  }
}
