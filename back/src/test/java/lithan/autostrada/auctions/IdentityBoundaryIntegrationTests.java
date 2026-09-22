package lithan.autostrada.auctions;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static lithan.autostrada.auctions.TestIdentity.user;

import java.util.List;
import java.util.Arrays;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import lithan.autostrada.auctions.identity.*;
import lithan.autostrada.auctions.repository.*;
import fixtures.identity.repository.*;
import lithan.autostrada.auctions.controller.api.ApiModelMapper;
import lithan.autostrada.auctions.service.CartService;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class IdentityBoundaryIntegrationTests {
  @Autowired ProfileClient profiles;
  @Autowired CheckoutProfileClient checkoutProfiles;
  @Autowired CurrentIdentity identity;
  @Autowired UserRepository users;
  @Autowired CarRepository cars;
  @Autowired CartItemRepository carts;
  @Autowired CartService cart;
  @Autowired ApiModelMapper mapper;
  @Autowired EntityManager em;
  @Autowired JdbcTemplate sql;
  @Autowired MockMvc mvc;

  @Test void batchIsBoundedDeduplicatedAndDoesNotHydrateIdentityEntities() {
    var ids = users.findAll().stream().map(u -> u.getIdUser()).toList();
    em.flush(); em.clear();
    var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    boolean enabled = statistics.isStatisticsEnabled();
    statistics.setStatisticsEnabled(true); statistics.clear();
    try {
      var result = profiles.findAll(ids);
      assertThat(result).hasSize(ids.size());
      assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
      assertThat(statistics.getEntityLoadCount()).isZero();
      statistics.clear();
      assertThat(profiles.findAll(Arrays.asList(ids.get(0), ids.get(0), null, -1, Integer.MAX_VALUE)))
          .containsOnlyKeys(ids.get(0));
      assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
      statistics.clear();
      assertThat(profiles.findAll(List.of())).isEmpty();
      assertThat(statistics.getPrepareStatementCount()).isZero();
    } finally { statistics.setStatisticsEnabled(enabled); }
  }

  @Test void missingAccountAndMissingProfileHaveDeliberateDisplayFallbacks() {
    sql.update("INSERT INTO tb_user(id_user,username,email,password) VALUES (910001,'boundaryuser','boundary@example.invalid','test-hash')");
    var profile = profiles.display(910001);
    assertThat(profile.profileId()).isNull();
    assertThat(profile.displayName()).isEqualTo("boundaryuser");
    assertThat(profiles.display(Integer.MAX_VALUE).displayName()).isEqualTo("Unavailable user");
    assertThat(profiles.findByProfileId(Integer.MAX_VALUE)).isEmpty();
  }

  @Test void profileIdIsNotAnAccountIdAndProfileAuctionsStayBusinessOwned() throws Exception {
    sql.update("INSERT INTO tb_user(id_user,username,email,password) VALUES (910002,'boundarytwo','private@example.invalid','test-hash')");
    sql.update("INSERT INTO tb_user_profile(id_profile,id_user,first_name,last_name,phone_number,address,street_address,city,postal_code,country,about) VALUES (920002,910002,'Visible','Seller','123456789','Private location','Secret street','Novi Sad','21000','Serbia','Public bio')");
    assertThat(profiles.findByProfileId(920002).orElseThrow().userId()).isEqualTo(910002);
    sql.update("INSERT INTO tb_car(id_car,make,model,production_year,price,status,id_user) VALUES (930002,'Boundary','Car','2024',1000,'ACTIVE',910002)");
    mvc.perform(get("/api/public/profiles/920002/auctions"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(930002));
    mvc.perform(get("/api/public/profiles/910002/auctions"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  }

  @Test @WithIdentity(username = "demo_bidder")
  void privateCheckoutProfileIsOnlyTheAuthenticatedSubjectAndSelfProfileStaysPrivate() throws Exception {
    var user = users.findByUsername("demo_bidder").orElseThrow();
    var checkout = checkoutProfiles.current();
    assertThat(checkout.userId()).isEqualTo(user.getIdUser());
    assertThat(checkout.email()).isEqualTo(user.getEmail());
    assertThat(checkout.toString()).doesNotContain(user.getEmail());
    assertThat(((RemoteProfileClient) checkoutProfiles).self().email()).isEqualTo(user.getEmail());
  }

  @Test void anonymousAndUnsupportedPrincipalsCannotReadPrivateProfiles() {
    var previous = SecurityContextHolder.getContext();
    try {
      SecurityContextHolder.clearContext();
      assertThatThrownBy(checkoutProfiles::current).isInstanceOf(AccessDeniedException.class);
      SecurityContextHolder.getContext().setAuthentication(
          UsernamePasswordAuthenticationToken.authenticated("demo_bidder", null, List.of()));
      assertThatThrownBy(identity::requireUserId).isInstanceOf(AccessDeniedException.class);
    } finally { SecurityContextHolder.setContext(previous); }
  }

  @Test void deletedIdentityCannotCreateCartSideEffects() {
    var previous = SecurityContextHolder.getContext();
    long count = carts.count();
    try {
      var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-only").header("alg","RS256")
          .subject(Integer.toString(Integer.MAX_VALUE)).claim("tokenUse","user")
          .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(60)).build();
      SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt));
      assertThatThrownBy(() -> cart.add(1, 1)).isInstanceOf(AccessDeniedException.class);
      assertThat(carts.count()).isEqualTo(count);
    } finally { SecurityContextHolder.setContext(previous); }
  }

  @Test void clientSuppliedOwnerCannotRedirectCartWrites() throws Exception {
    int bidder = users.findByUsername("demo_bidder").orElseThrow().getIdUser();
    int seller = users.findByUsername("demo_seller").orElseThrow().getIdUser();
    long sellerCount = carts.countByUserId(seller);
    mvc.perform(post("/api/store/cart/items").with(user("demo_bidder").roles("USER")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"idPart\":1,\"quantity\":1,\"userId\":" + seller + "}"))
        .andExpect(status().isOk());
    assertThat(carts.findByUserIdOrderByCreatedAtAsc(bidder)).isNotEmpty();
    assertThat(carts.countByUserId(seller)).isEqualTo(sellerCount);
  }

  @Test void foreignKeysStillRejectOrphanIdentityAndAccountDeletionWithHistory() {
    int owner = cars.findAll().get(0).getUserId();
    assertThatThrownBy(() -> sql.update("UPDATE tb_car SET id_user=2147483647 WHERE id_car=1"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(() -> sql.update("DELETE FROM tb_user WHERE id_user=?", owner))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(users.existsById(owner)).isTrue();
  }

  @Test void businessCollectionMappingBatchesEveryDistinctOwner() {
    var values = cars.findAll();
    var client = org.mockito.Mockito.mock(ProfileClient.class);
    org.mockito.Mockito.when(client.findAll(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(java.util.Map.of());
    var isolatedMapper = new ApiModelMapper(java.time.Clock.systemUTC(), client);
    assertThat(isolatedMapper.map(values.stream(), isolatedMapper::auction).toList()).hasSize(values.size());
    org.mockito.Mockito.verify(client).findAll(new java.util.HashSet<>(values.stream().map(c -> c.getUserId()).toList()));
    org.mockito.Mockito.verifyNoMoreInteractions(client);
  }
}
