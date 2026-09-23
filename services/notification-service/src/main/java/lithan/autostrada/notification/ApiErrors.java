package lithan.autostrada.notification;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiErrors {
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<?> forbidden(){return error(403,"You do not have permission to perform this action.");}
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e){return error(e.getStatusCode().value(),"The requested resource could not be found.");}
  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  ResponseEntity<?> database(){return error(503,"Notifications are temporarily unavailable.");}
  private ResponseEntity<?> error(int status,String message){return ResponseEntity.status(status).header("Cache-Control","no-store").body(Map.of("message",message,"fieldErrors",Map.of()));}
}
