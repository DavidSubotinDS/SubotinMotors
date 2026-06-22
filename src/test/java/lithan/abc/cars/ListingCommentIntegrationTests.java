package lithan.abc.cars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

import lithan.abc.cars.dto.ListingCommentView;
import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarPart;
import lithan.abc.cars.repository.CarPartRepository;
import lithan.abc.cars.repository.CarRepository;
import lithan.abc.cars.repository.ListingCommentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListingCommentIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private CarPartRepository partRepository;

  @Autowired
  private ListingCommentRepository commentRepository;

  @Test
  void auctionDiscussionShowsSeededSellerAndAdminReplies() throws Exception {
    Car car = demoCar("Toyota", "RAV4", "demo_seller");

    MvcResult result = mockMvc.perform(get(carUrl(car)))
        .andExpect(status().isOk())
        .andExpect(view().name("car-details"))
        .andExpect(model().attributeExists("comments", "commentForm"))
        .andReturn();

    List<ListingCommentView> comments = comments(result);
    assertTrue(comments.stream().anyMatch(comment -> "Seller".equals(comment.getBadgeLabel())));
    assertTrue(comments.stream().anyMatch(comment -> "Admin".equals(comment.getBadgeLabel())));
    assertTrue(comments.stream().anyMatch(comment -> !comment.isHighlighted()));
  }

  @Test
  void partDiscussionShowsSeededStoreTeamReply() throws Exception {
    CarPart part = partRepository.findBySkuIgnoreCase("BRK-PAD-001").orElseThrow();

    MvcResult result = mockMvc.perform(get("/parts/{idPart}", part.getIdPart()))
        .andExpect(status().isOk())
        .andExpect(view().name("store/part-details"))
        .andExpect(model().attributeExists("comments", "commentForm"))
        .andReturn();

    assertTrue(comments(result).stream()
        .anyMatch(comment -> "Store team".equals(comment.getBadgeLabel())));
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

  @SuppressWarnings("unchecked")
  private List<ListingCommentView> comments(MvcResult result) {
    return (List<ListingCommentView>) result.getModelAndView().getModel().get("comments");
  }

  private Car demoCar(String make, String model, String owner) {
    return carRepository.findAll().stream()
        .filter(car -> make.equals(car.getMake()))
        .filter(car -> model.equals(car.getModel()))
        .filter(car -> owner.equals(car.getUser().getUsername()))
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
