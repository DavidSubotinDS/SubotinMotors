package lithan.autostrada.identity.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Handles failures raised before Spring can select an API controller. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class MultipartExceptionHandler {
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Void> uploadTooLarge() {
    return ResponseEntity.badRequest().build();
  }
}
