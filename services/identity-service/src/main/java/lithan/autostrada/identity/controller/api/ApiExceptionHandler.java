package lithan.autostrada.identity.controller.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lithan.autostrada.identity.dto.api.ApiModels.ApiErrorResponse;
import lithan.autostrada.identity.error.ResourceNotFoundException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "lithan.autostrada.identity.controller.api")
public class ApiExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse notFound() {
    return new ApiErrorResponse("The requested resource could not be found.", Map.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiErrorResponse forbidden() {
    return new ApiErrorResponse("You do not have permission to perform this action.", Map.of());
  }

  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiErrorResponse authenticationFailed() {
    return new ApiErrorResponse("Invalid username or password.", Map.of());
  }


  @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse badRequest(RuntimeException exception) {
    return new ApiErrorResponse(exception.getMessage(), Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse validation(MethodArgumentNotValidException exception) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
    }
    return new ApiErrorResponse("Please correct the highlighted fields.", fieldErrors);
  }

}
