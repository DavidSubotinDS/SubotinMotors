package lithan.abc.cars.error;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleResourceNotFoundException() {

    return "error";
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String handleAccessDeniedException() {
    return "error";
  }

  @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String handleBadRequestException() {
    return "error";
  }
}
