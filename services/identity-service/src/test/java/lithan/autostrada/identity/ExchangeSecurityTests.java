package lithan.autostrada.identity;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest @Transactional @AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class ExchangeSecurityTests extends IdentityTestBase {
  @Autowired MockMvc mvc; @Autowired ObjectMapper json;
  @Autowired lithan.autostrada.identity.service.PasswordResetService resets;
  @Autowired lithan.autostrada.identity.service.AdminService admins;
  @Autowired lithan.autostrada.identity.config.IdentityTokens tokens;
  record Browser(MockHttpSession session,String token) {}
  Browser browser() throws Exception {
    return browser("demo_bidder","demo123");
  }
  Browser browser(String username,String password) throws Exception {
    var r=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
    var session=(MockHttpSession)r.getRequest().getSession(false);
    String token=json.readTree(r.getResponse().getContentAsByteArray()).get("token").asText();
    mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",token).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsBytes(Map.of("username",username,"password",password)))).andExpect(status().isOk());
    r=mvc.perform(get("/api/csrf").session(session)).andReturn();
    return new Browser(session,json.readTree(r.getResponse().getContentAsByteArray()).get("token").asText());
  }
  String basic(String client,String secret){return "Basic "+Base64.getEncoder().encodeToString((client+":"+secret).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder exchange(Browser b,String audience,String method,String csrf) throws Exception {
    return post("/internal/v1/session-exchange").session(b.session()).header("Authorization",basic("gateway",GATEWAY_SECRET))
        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("audience",audience,"method",method,"csrfToken",csrf)));
  }
  @Test void exchangeRequiresGatewayAndIssuesMinimalAudienceBoundSixtySecondUserAssertion() throws Exception {
    var b=browser();
    mvc.perform(exchange(b,"legacy-backend","GET","").header("Authorization","Basic invalid")).andExpect(status().isUnauthorized());
    mvc.perform(exchange(b,"identity-service","GET","")).andExpect(status().isForbidden());
    var r=mvc.perform(exchange(b,"legacy-backend","GET","")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
    var claims=SignedJWT.parse(json.readTree(r.getResponse().getContentAsByteArray()).get("assertion").asText()).getJWTClaimsSet();
    assertThat(claims.getSubject()).isEqualTo("3");assertThat(claims.getAudience()).containsExactly("legacy-backend");
    assertThat(claims.getStringClaim("tokenUse")).isEqualTo("user");
    assertThat(claims.getExpirationTime().getTime()-claims.getIssueTime().getTime()).isEqualTo(60000);
    assertThat(claims.getClaims()).doesNotContainKeys("email","password","username","address","csrfToken");
  }
  @Test void unsafeExchangeRequiresMaskedCsrfBoundToTheSameSession() throws Exception {
    var b=browser();var other=browser();
    for(String csrf:List.of("","bad",other.token()))mvc.perform(exchange(b,"legacy-backend","POST",csrf))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    mvc.perform(exchange(b,"legacy-backend","POST",b.token())).andExpect(status().isOk());
  }
  @Test void resetAndRoleChangeRevokePreviouslyAuthenticatedSessions() throws Exception {
    var b=browser();String reset=resets.requestReset("demo_bidder").orElseThrow();
    assertThat(resets.resetPassword(reset,"updated123")).isTrue();
    mvc.perform(exchange(b,"legacy-backend","GET","")).andExpect(status().isUnauthorized());
    assertThat(b.session().isInvalid()).isTrue();
    // A separate account session is revoked by a role grant too.
    var c=browser("demo_seller","demo123");admins.markAsAdmin(4);
    mvc.perform(get("/api/session").session(c.session())).andExpect(jsonPath("$.authenticated").value(false));
    assertThat(c.session().isInvalid()).isTrue();
  }
  @Test void serviceGrantHasFixedAudienceScopesAndCannotBecomeInteractiveAdmin() throws Exception {
    mvc.perform(post("/internal/v1/service-token").contentType(MediaType.APPLICATION_JSON).content("{\"audience\":\"identity-service\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/internal/v1/service-token").header("Authorization",basic("gateway",GATEWAY_SECRET))
        .contentType(MediaType.APPLICATION_JSON).content("{\"audience\":\"identity-service\"}")).andExpect(status().isUnauthorized());
    mvc.perform(post("/internal/v1/service-token").header("Authorization",basic("legacy-backend",BACKEND_SECRET))
        .contentType(MediaType.APPLICATION_JSON).content("{\"audience\":\"other\"}")).andExpect(status().isForbidden());
    var r=mvc.perform(post("/internal/v1/service-token").header("Authorization",basic("legacy-backend",BACKEND_SECRET))
        .contentType(MediaType.APPLICATION_JSON).content("{\"audience\":\"identity-service\",\"roles\":[\"ROLE_ADMIN\"],\"sub\":\"1\"}"))
        .andExpect(status().isOk()).andReturn();
    String token=json.readTree(r.getResponse().getContentAsByteArray()).get("accessToken").asText();
    var claims=SignedJWT.parse(token).getJWTClaimsSet();assertThat(claims.getSubject()).isEqualTo("legacy-backend");
    assertThat(claims.getStringListClaim("roles")).isEmpty();assertThat(claims.getStringClaim("tokenUse")).isEqualTo("service");
    mvc.perform(get("/api/admin/dashboard").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    mvc.perform(get("/internal/v1/users/3/checkout-profile").header("Authorization","Bearer "+token))
        .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(3));
    var payment=mvc.perform(post("/internal/v1/service-token").header("Authorization",basic("legacy-backend",BACKEND_SECRET))
        .contentType(MediaType.APPLICATION_JSON).content("{\"audience\":\"payment-service\"}" )).andExpect(status().isOk()).andReturn();
    var paymentClaims=SignedJWT.parse(json.readTree(payment.getResponse().getContentAsByteArray()).get("accessToken").asText()).getJWTClaimsSet();
    assertThat(paymentClaims.getAudience()).containsExactly("payment-service");
    assertThat(paymentClaims.getStringListClaim("scopes")).containsExactly("create-store-payment","create-deposit-payment","payment-lookup","payment-expire");
  }
  @Test void privateEndpointRejectsUserTokensAndPublicLookupOmitsPrivateFields() throws Exception {
    String user=tokens.issue("3","identity-service","user",List.of("ROLE_ADMIN"),List.of("checkout-profile"));
    mvc.perform(get("/internal/v1/users/3/checkout-profile").header("Authorization","Bearer "+user)).andExpect(status().isUnauthorized());
    String service=tokens.issue("legacy-backend","identity-service","service",List.of(),List.of("public-profiles"));
    mvc.perform(get("/internal/v1/users/3/checkout-profile").header("Authorization","Bearer "+service)).andExpect(status().isUnauthorized());
    mvc.perform(get("/internal/v1/profiles").param("ids","3,3,999").header("Authorization","Bearer "+service))
        .andExpect(status().isOk()).andExpect(jsonPath("$.3.userId").value(3)).andExpect(jsonPath("$.3.email").doesNotExist())
        .andExpect(jsonPath("$.3.roles").doesNotExist());
    mvc.perform(get("/api/public/profiles/30")).andExpect(status().isOk()).andExpect(jsonPath("$.idProfile").value(30))
        .andExpect(jsonPath("$.email").isEmpty()).andExpect(jsonPath("$.phoneNumber").isEmpty());
  }
}
