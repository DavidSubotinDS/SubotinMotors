package lithan.autostrada.payment;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
class PaymentErrors {
  @ExceptionHandler(PaymentStore.NotFound.class) ResponseEntity<?> missing(){return error(HttpStatus.NOT_FOUND,"Payment was not found.");}
  @ExceptionHandler(PaymentStore.Conflict.class) ResponseEntity<?> conflict(RuntimeException e){return error(HttpStatus.CONFLICT,e.getMessage());}
  @ExceptionHandler(PaymentEngine.Invalid.class) ResponseEntity<?> invalid(RuntimeException e){return error(HttpStatus.BAD_REQUEST,e.getMessage());}
  @ExceptionHandler(PaymentEngine.Forbidden.class) ResponseEntity<?> forbidden(RuntimeException e){return error(HttpStatus.FORBIDDEN,e.getMessage());}
  @ExceptionHandler(PaymentEngine.Unavailable.class) ResponseEntity<?> unavailable(RuntimeException e){return error(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage());}
  @ExceptionHandler(PaymentProviderException.class) ResponseEntity<?> provider(){return error(HttpStatus.BAD_REQUEST,"Provider request was rejected.");}
  private static ResponseEntity<?> error(HttpStatus status,String message){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("message",message,"fieldErrors",Map.of()));}
}
