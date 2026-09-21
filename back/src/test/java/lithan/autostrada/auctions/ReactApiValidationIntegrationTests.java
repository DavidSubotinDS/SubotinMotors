package lithan.autostrada.auctions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static lithan.autostrada.auctions.TestIdentity.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.repository.CarRepository;
import fixtures.identity.repository.UserRepository;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReactApiValidationIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;



  @Test
  void validAuctionCanBeCreatedAndUpdatedButInvalidOrForeignEditsAreRejected() throws Exception {
    var endTime = LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);
    var image = new MockMultipartFile("imageFiles", "car.png", "image/png",
        Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));
    var creation = mockMvc.perform(multipart("/api/user/auctions").file(image)
            .param("make", "Api").param("model", "Roadster").param("year", "2025")
            .param("price", "10000").param("auctionEndTime", endTime.toString())
            .with(csrf()).with(user("user123").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();
    int id = objectMapper.readTree(creation.getResponse().getContentAsByteArray()).get("id").asInt();
    var request = new LinkedHashMap<String, Object>(Map.of(
        "make", "Api", "model", "Roadster", "year", "2025", "price", 0,
        "auctionEndTime", endTime.toString()));
    mockMvc.perform(put("/api/user/auctions/{id}", id).with(csrf()).with(user("user123").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.price").isNotEmpty());
    assertThat(carRepository.findById(id).orElseThrow().getPrice()).isEqualTo(10000);

    request.put("price", 11000);
    mockMvc.perform(put("/api/user/auctions/{id}", id).with(csrf()).with(user("demo_bidder").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
    assertThat(carRepository.findById(id).orElseThrow().getPrice()).isEqualTo(10000);
    mockMvc.perform(put("/api/user/auctions/{id}", id).with(csrf()).with(user("user123").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(11000));
    assertThat(carRepository.findById(id).orElseThrow().getPrice()).isEqualTo(11000);
  }

  private Map<String, String> registration() {
    return new LinkedHashMap<>(Map.of(
        "username", "api_driver", "email", "api-driver@example.com", "password", "secret123",
        "firstName", "Api", "lastName", "Driver", "phoneNumber", "+381601234567"));
  }
}
