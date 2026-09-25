package lithan.autostrada.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class PaymentServiceTests {
  static final com.nimbusds.jose.jwk.RSAKey KEY;
  static {try{KEY=new RSAKeyGenerator(2048).keyID("payment-tests").generate();}catch(Exception e){throw new ExceptionInInitializerError(e);}}
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
    r.add("spring.datasource.url",()->"jdbc:h2:mem:payments;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    r.add("spring.datasource.username",()->"sa");r.add("spring.datasource.password",()->"");
    r.add("identity.verification-jwks",()->new JWKSet(KEY.toPublicJWK()).toString());
    r.add("payment.broker.enabled",()->"false");
  }
  @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;@Autowired PaymentEngine engine;
  @MockitoBean PaymentProvider provider;
  JwtEncoder encoder;
  @BeforeEach void resetDatabase(){
    db.update("DELETE FROM payment_outbox");db.update("DELETE FROM payment_webhook_receipt");db.update("DELETE FROM payment_attempt");
    encoder=new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY)));
    when(provider.enabled()).thenReturn(true);
    when(provider.create(any())).thenReturn(new PaymentContracts.ProviderResult("cs_test_1","https://checkout.stripe.test/session","pi_test_1"));
  }
  @Test void serviceStartsWithIndependentSchema(){
    assertThat(db.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=SCHEMA() AND table_name IN ('payment_attempt','payment_webhook_receipt','payment_outbox','payment_legacy_audit','payment_provider_account_audit')",Integer.class)).isEqualTo(5);
  }
  @Test void creationIsAuthenticatedPurposeCheckedAndIdempotent() throws Exception {
    String request=request("100",19900);
    mvc.perform(post("/internal/v1/payments").contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key",ID).content(request)).andExpect(status().isUnauthorized());
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+token("other","service",SCOPES)).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+token("payment-service","user",List.of())).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.attemptId").value(ID)).andExpect(jsonPath("$.status").value("CHECKOUT_CREATED"));
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isOk()).andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.test/session"));
    verify(provider,times(1)).create(any());
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request("100",20000)))
        .andExpect(status().isConflict());
  }
  @Test void webhookIsDurableDeduplicatedAndEmitsNormalizedResult() throws Exception {
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request("100",19900))).andExpect(status().isCreated());
    when(provider.verify("{\"id\":\"evt_1\"}","valid")).thenReturn(new PaymentContracts.ProviderEvent("evt_1","checkout.session.completed","cs_test_1","pi_test_1","paid","abcd"));
    mvc.perform(post("/webhooks/stripe").header("Stripe-Signature","valid").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"evt_1\"}" )).andExpect(status().isOk());
    mvc.perform(post("/webhooks/stripe").header("Stripe-Signature","valid").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"evt_1\"}" )).andExpect(status().isOk());
    assertThat(db.queryForObject("SELECT status FROM payment_attempt WHERE attempt_id=?",String.class,ID)).isEqualTo("SUCCEEDED");
    assertThat(db.queryForObject("SELECT delivery_count FROM payment_webhook_receipt WHERE provider_event_id='evt_1'",Integer.class)).isEqualTo(2);
    assertThat(db.queryForObject("SELECT COUNT(*) FROM payment_outbox WHERE routing_key='payment.succeeded.v1'",Integer.class)).isEqualTo(1);
  }
  @Test void ownerAndAdminReadsRejectCrossUserAndServiceTokensCannotBecomeUsers() throws Exception {
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request("100",19900))).andExpect(status().isCreated());
    mvc.perform(get("/api/payments/"+ID).header("Authorization","Bearer "+user("12",List.of("ROLE_USER")))).andExpect(status().isOk()).andExpect(jsonPath("$.buyerId").value("12"));
    mvc.perform(get("/api/payments/"+ID).header("Authorization","Bearer "+user("13",List.of("ROLE_USER")))).andExpect(status().isForbidden());
    mvc.perform(get("/api/payments/"+ID).header("Authorization","Bearer "+user("1",List.of("ROLE_ADMIN")))).andExpect(status().isOk());
    mvc.perform(get("/api/payments/"+ID).header("Authorization","Bearer "+service())).andExpect(status().isForbidden());
  }
  @Test void rejectsWrongIssuerExpiredWrongAlgorithmAndIncompleteGrant() throws Exception {
    String request=request("100",19900);
    for(String bad:List.of(custom("payment-service","service",SCOPES,"wrong-issuer",Instant.now(),"legacy-backend"),
        custom("payment-service","service",SCOPES,"autostrada-identity",Instant.now().minusSeconds(120),"legacy-backend"),
        custom("payment-service","service",List.of("create-store-payment"),"autostrada-identity",Instant.now(),"legacy-backend"),hmacToken()))
      mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+bad).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
  }
  @Test void earlyWebhookIsMatchedAfterLostCreateResponseRecovery() throws Exception {
    when(provider.create(any())).thenThrow(new PaymentProviderException("lost",new java.io.IOException()));
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request("100",19900))).andExpect(status().isAccepted());
    when(provider.verify(anyString(),eq("valid"))).thenReturn(new PaymentContracts.ProviderEvent("evt_early","checkout.session.completed","cs_lost","pi_lost","paid","beef"));
    mvc.perform(post("/webhooks/stripe").header("Stripe-Signature","valid").content("{}" )).andExpect(status().isOk());
    assertThat(db.queryForObject("SELECT status FROM payment_webhook_receipt WHERE provider_event_id='evt_early'",String.class)).isEqualTo("UNMATCHED");
    doReturn(new PaymentContracts.ProviderResult("cs_lost","https://checkout/recovered","pi_lost")).when(provider).create(any());
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request("100",19900))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCEEDED"));
  }
  @Test void asyncFailureIsTerminalAndExpiryWaitsForProviderEvidence() throws Exception {
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request("100",19900))).andExpect(status().isCreated());
    when(provider.verify(eq("failed"),eq("valid"))).thenReturn(new PaymentContracts.ProviderEvent("evt_async_failed","checkout.session.async_payment_failed","cs_test_1","pi_test_1","unpaid","cafe"));
    mvc.perform(post("/webhooks/stripe").header("Stripe-Signature","valid").content("failed")).andExpect(status().isOk());
    assertThat(db.queryForObject("SELECT status FROM payment_attempt WHERE attempt_id=?",String.class,ID)).isEqualTo("FAILED");

    resetDatabase();
    mvc.perform(post("/internal/v1/payments").header("Authorization","Bearer "+service()).header("Idempotency-Key",ID).contentType(MediaType.APPLICATION_JSON).content(request("100",19900))).andExpect(status().isCreated());
    mvc.perform(post("/internal/v1/payments/"+ID+"/expire").header("Authorization","Bearer "+service()).header("Idempotency-Key","expire-"+ID)).andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("EXPIRY_REQUESTED"));
    assertThat(db.queryForObject("SELECT COUNT(*) FROM payment_outbox WHERE routing_key='payment.expired.v1'",Integer.class)).isZero();
    when(provider.retrieve(any())).thenReturn(Optional.of(new PaymentContracts.ProviderState("cs_test_1","pi_test_1","unpaid","expired")));
    engine.reconcile();
    assertThat(db.queryForObject("SELECT status FROM payment_attempt WHERE attempt_id=?",String.class,ID)).isEqualTo("EXPIRED");
    assertThat(db.queryForObject("SELECT COUNT(*) FROM payment_outbox WHERE routing_key='payment.expired.v1'",Integer.class)).isEqualTo(1);
  }
  static final String ID="ad7343cb-c784-45cb-b9d0-4a5428bdf881";
  static final List<String> SCOPES=List.of("create-store-payment","create-deposit-payment","payment-lookup","payment-expire");
  String request(String business,long amount)throws Exception{return json.writeValueAsString(Map.ofEntries(Map.entry("attemptId",ID),Map.entry("sourceService","commerce-service"),Map.entry("businessType","STORE_ORDER"),Map.entry("businessId",business),Map.entry("businessVersion",1),Map.entry("buyerId","12"),Map.entry("amountMinor",amount),Map.entry("currency","eur"),Map.entry("description","Order "+business),Map.entry("returnRoute","STORE_ORDER"),Map.entry("customerEmail","buyer@example.test")));}
  String service(){return token("payment-service","service",SCOPES);}
  String user(String subject,List<String> roles){Instant now=Instant.now();var claims=JwtClaimsSet.builder().issuer("autostrada-identity").audience(List.of("payment-service")).subject(subject).issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(60)).id(UUID.randomUUID().toString()).claim("tokenUse","user").claim("roles",roles).claim("scopes",List.of()).build();return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY.getKeyID()).build(),claims)).getTokenValue();}
  String token(String audience,String use,List<String> scopes){return custom(audience,use,scopes,"autostrada-identity",Instant.now(),"service".equals(use)?"legacy-backend":"12");}
  String custom(String audience,String use,List<String> scopes,String issuer,Instant now,String subject){var claims=JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience)).subject(subject).issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(60)).id(UUID.randomUUID().toString()).claim("tokenUse",use).claim("roles","user".equals(use)?List.of("ROLE_USER"):List.of()).claim("scopes",scopes).build();return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY.getKeyID()).build(),claims)).getTokenValue();}
  String hmacToken()throws Exception{var claims=new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer("autostrada-identity").audience("payment-service").subject("legacy-backend").issueTime(new Date()).notBeforeTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(60))).jwtID(UUID.randomUUID().toString()).claim("tokenUse","service").claim("roles",List.of()).claim("scopes",SCOPES).build();var jwt=new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),claims);jwt.sign(new com.nimbusds.jose.crypto.MACSigner(new byte[32]));return jwt.serialize();}
}
