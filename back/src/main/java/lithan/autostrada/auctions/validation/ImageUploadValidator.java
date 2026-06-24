package lithan.autostrada.auctions.validation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.springframework.web.multipart.MultipartFile;

public final class ImageUploadValidator {

  private static final byte[] PNG_SIGNATURE = {
      (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };

  private ImageUploadValidator() {
  }

  public static ValidatedImage validate(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Image file is required");
    }

    String declaredType = normalizeContentType(file.getContentType());
    if (!isSupported(declaredType)) {
      throw new IllegalArgumentException("Only JPEG and PNG images are supported");
    }

    byte[] bytes = file.getBytes();
    String detectedType = detectContentType(bytes);
    if (detectedType == null || !declaredType.equals(detectedType) || !isDecodable(bytes)) {
      throw new IllegalArgumentException("Image content does not match a valid JPEG or PNG file");
    }

    return new ValidatedImage(safeFileName(file.getOriginalFilename()), detectedType, bytes);
  }

  private static String normalizeContentType(String contentType) {
    return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
  }

  private static boolean isSupported(String contentType) {
    return "image/jpeg".equals(contentType) || "image/png".equals(contentType);
  }

  private static String detectContentType(byte[] bytes) {
    if (startsWith(bytes, PNG_SIGNATURE)) {
      return "image/png";
    }
    if (bytes.length >= 3
        && (bytes[0] & 0xFF) == 0xFF
        && (bytes[1] & 0xFF) == 0xD8
        && (bytes[2] & 0xFF) == 0xFF) {
      return "image/jpeg";
    }
    return null;
  }

  private static boolean startsWith(byte[] content, byte[] signature) {
    if (content.length < signature.length) {
      return false;
    }
    for (int index = 0; index < signature.length; index++) {
      if (content[index] != signature[index]) {
        return false;
      }
    }
    return true;
  }

  private static boolean isDecodable(byte[] bytes) throws IOException {
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
    return image != null && image.getWidth() > 0 && image.getHeight() > 0;
  }

  private static String safeFileName(String originalFilename) {
    if (originalFilename == null || originalFilename.isBlank()) {
      return "image";
    }
    String normalized = originalFilename.replace("\\", "/");
    return normalized.substring(normalized.lastIndexOf('/') + 1);
  }

  public record ValidatedImage(String fileName, String contentType, byte[] bytes) {
  }
}
