package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static lithan.autostrada.auctions.TestIdentity.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.repository.CarListingRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import fixtures.identity.repository.UserRepository;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VehicleGalleryIntegrationTests {

  private static final byte[] PNG = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private CarListingRepository listingRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void auctionCreateAndGalleryAppendExposeOrderedImages() throws Exception {
    mockMvc.perform(multipart("/api/user/auctions")
            .file(image("imageFiles", "front.png"))
            .file(image("imageFiles", "side.png"))
            .with(csrf()).with(user("demo_newcomer").roles("USER"))
            .param("make", "Gallery")
            .param("model", "Auction")
            .param("year", "2025")
            .param("price", "21000")
            .param("auctionEndTime", LocalDateTime.now().plusDays(5).withNano(0).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrls.length()").value(2))
        .andExpect(jsonPath("$.imageUrl").isNotEmpty());

    Car car = carRepository.findByUserId(userRepository.findByUsername("demo_newcomer").orElseThrow().getIdUser()).stream()
        .filter(candidate -> "Gallery".equals(candidate.getMake()))
        .findFirst()
        .orElseThrow();
    assertNotNull(car.getCarPicture());
    assertEquals(1, car.getGalleryPictures().size());

    mockMvc.perform(multipart("/api/user/auctions/{idCar}/pictures", car.getIdCar())
            .file(image("imageFiles", "interior.png"))
            .with(csrf()).with(user("demo_newcomer").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Auction gallery updated."));

    assertEquals(2, carRepository.findById(car.getIdCar()).orElseThrow()
        .getGalleryPictures().size());
  }

  @Test
  void fixedPriceListingCreateReturnsEveryUploadedImage() throws Exception {
    mockMvc.perform(multipart("/api/user/listings")
            .file(image("imageFiles", "front.png"))
            .file(image("imageFiles", "rear.png"))
            .file(image("imageFiles", "interior.png"))
            .with(csrf()).with(user("demo_newcomer").roles("USER"))
            .param("title", "Gallery listing")
            .param("make", "Gallery")
            .param("model", "Listing")
            .param("year", "2025")
            .param("mileage", "12000")
            .param("fuelType", "Electric")
            .param("transmission", "Automatic")
            .param("price", "32000.00")
            .param("depositAmount", "1200.00")
            .param("description", "A listing created to verify its multi-image gallery."))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrls.length()").value(3))
        .andExpect(jsonPath("$.imageUrl").isNotEmpty());

    CarListing listing = listingRepository.findBySellerIdOrderByCreatedAtDesc(userRepository.findByUsername("demo_newcomer").orElseThrow().getIdUser()).stream()
        .filter(candidate -> "Gallery listing".equals(candidate.getTitle()))
        .findFirst()
        .orElseThrow();
    assertNotNull(listing.getPicture());
    assertEquals(2, listing.getGalleryPictures().size());
  }

  private MockMultipartFile image(String field, String name) {
    return new MockMultipartFile(field, name, "image/png", PNG);
  }
}
