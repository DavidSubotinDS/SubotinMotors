package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static lithan.autostrada.auctions.TestIdentity.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.repository.CarPartRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.ListingCommentRepository;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListingCommentIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private fixtures.identity.repository.UserRepository userRepository;

  @Autowired
  private CarPartRepository partRepository;

  @Autowired
  private ListingCommentRepository commentRepository;

  @Test
  void auctionDiscussionShowsSeededSellerAndAdminReplies() throws Exception {
    Car car = demoCar("Toyota", "RAV4", "demo_seller");

    mockMvc.perform(get("/api/public/auctions/{idCar}", car.getIdCar()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comments[*].badgeLabel", hasItems("Seller", "Admin")))
        .andExpect(jsonPath("$.comments[*].highlightClass", hasItem("")));
  }

  @Test
  void partDiscussionShowsSeededStoreTeamReply() throws Exception {
    CarPart part = partRepository.findBySkuIgnoreCase("BRK-PAD-001").orElseThrow();

    mockMvc.perform(get("/api/public/parts/{idPart}", part.getIdPart()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comments[*].badgeLabel", hasItem("Store team")));
  }

  @Test
  void signedInUserCanPostOnAuctionAndPartWhileAnonymousCannot() throws Exception {
    Car car = demoCar("BMW", "330i", "demo_trader");
    CarPart part = partRepository.findBySkuIgnoreCase("FLT-OIL-101").orElseThrow();
    long initialCount = commentRepository.count();

    mockMvc.perform(post("/cars/{idCar}/comments", car.getIdCar())
            .param("body", "Could you confirm whether a second key is included?")
            .with(user("demo_bidder").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(carUrl(car) + "#discussion"))
        .andExpect(flash().attribute("commentMessage", "Your comment was posted."));

    mockMvc.perform(post("/parts/{idPart}/comments", part.getIdPart())
            .param("body", "Is a replacement sealing washer included?")
            .with(user("demo_bidder").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/parts/" + part.getIdPart() + "#discussion"));

    assertEquals(initialCount + 2, commentRepository.count());

    mockMvc.perform(post("/parts/{idPart}/comments", part.getIdPart())
            .param("body", "Anonymous comment")
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
    assertEquals(initialCount + 2, commentRepository.count());
  }

  @Test
  void blankCommentIsRejectedWithoutWritingData() throws Exception {
    CarPart part = partRepository.findBySkuIgnoreCase("BAT-AGM-070").orElseThrow();
    long initialCount = commentRepository.count();

    mockMvc.perform(post("/parts/{idPart}/comments", part.getIdPart())
            .param("body", "   ")
            .with(user("demo_bidder").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/parts/" + part.getIdPart() + "#discussion"))
        .andExpect(flash().attribute("commentError", "Write a comment before posting"));

    assertEquals(initialCount, commentRepository.count());
  }

  @Test
  void signedInUserCanAttachValidatedPictureToComment() throws Exception {
    CarPart part = partRepository.findBySkuIgnoreCase("BRK-PAD-001").orElseThrow();
    MockMultipartFile image = new MockMultipartFile(
        "imageFile", "fitment.png", "image/png", pngBytes());

    mockMvc.perform(multipart("/parts/{idPart}/comments", part.getIdPart())
            .file(image)
            .param("body", "Here is a photo of the current brake setup.")
            .with(user("demo_bidder").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/parts/" + part.getIdPart() + "#discussion"))
        .andExpect(flash().attribute("commentMessage", "Your comment was posted."));

    var saved = commentRepository.findByPartOrderByCreatedAtAscIdCommentAsc(part).stream()
        .filter(comment -> "Here is a photo of the current brake setup.".equals(comment.getBody()))
        .findFirst()
        .orElseThrow();
    assertEquals("fitment.png", saved.getImageFileName());
    assertEquals("image/png", saved.getImageFileType());
    assertTrue(saved.hasImage());
  }

  @Test
  void spoofedCommentPictureIsRejectedWithoutWritingData() throws Exception {
    CarPart part = partRepository.findBySkuIgnoreCase("BAT-AGM-070").orElseThrow();
    long initialCount = commentRepository.count();
    MockMultipartFile spoofed = new MockMultipartFile(
        "imageFile", "battery.png", "image/png", "not an image".getBytes());

    mockMvc.perform(multipart("/parts/{idPart}/comments", part.getIdPart())
            .file(spoofed)
            .param("body", "This attachment should be rejected.")
            .with(user("demo_bidder").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/parts/" + part.getIdPart() + "#discussion"))
        .andExpect(flash().attribute(
            "commentError", "Image content does not match a valid JPEG or PNG file"));

    assertEquals(initialCount, commentRepository.count());
  }

  private Car demoCar(String make, String model, String owner) {
    return carRepository.findAll().stream()
        .filter(car -> make.equals(car.getMake()))
        .filter(car -> model.equals(car.getModel()))
        .filter(car -> owner.equals(userRepository.findById(car.getUserId()).orElseThrow().getUsername()))
        .findFirst()
        .orElseThrow();
  }

  private String carUrl(Car car) {
    return "/cars/" + car.getMake() + "/" + car.getModel() + "/"
        + car.getYear() + "/" + car.getIdCar();
  }

  private byte[] pngBytes() throws Exception {
    BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }
}
