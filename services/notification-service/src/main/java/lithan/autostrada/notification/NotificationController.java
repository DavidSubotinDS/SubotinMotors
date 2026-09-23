package lithan.autostrada.notification;

import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
public class NotificationController {
  private final Inbox inbox;
  public NotificationController(Inbox inbox) { this.inbox=inbox; }
  @GetMapping("/internal/v1/users/{id}/unread-count") public Map<String,Long> internalCount(@PathVariable int id) {
    if(id<=0)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
    return Map.of("count",inbox.unread(id));
  }
  @GetMapping("/api/user/notifications") public List<Map<String,Object>> list(@AuthenticationPrincipal Jwt actor) {return inbox.list(user(actor));}
  @GetMapping("/api/user/notifications/unread-count") public Map<String,Long> count(@AuthenticationPrincipal Jwt actor) {return Map.of("count",inbox.unread(user(actor)));}
  @PostMapping("/api/user/notifications/{id}/read") public Map<String,Object> read(@AuthenticationPrincipal Jwt actor,@PathVariable int id) {
    inbox.read(user(actor),id);return message("Notification marked read.");
  }
  @PostMapping("/api/user/notifications/read-all") public Map<String,Object> all(@AuthenticationPrincipal Jwt actor) {
    inbox.readAll(user(actor));return message("All notifications marked read.");
  }
  @PostMapping("/user/notifications/{id}/read") public ResponseEntity<Void> legacyRead(@AuthenticationPrincipal Jwt actor,@PathVariable int id) {
    inbox.read(user(actor),id);return ResponseEntity.status(302).header("Location","/user/notifications").build();
  }
  @PostMapping("/user/notifications/read-all") public ResponseEntity<Void> legacyAll(@AuthenticationPrincipal Jwt actor) {
    inbox.readAll(user(actor));return ResponseEntity.status(302).header("Location","/user/notifications").build();
  }
  private int user(Jwt actor) {return Integer.parseInt(actor.getSubject());}
  private Map<String,Object> message(String value) {var result=new LinkedHashMap<String,Object>();result.put("message",value);result.put("redirectUrl",null);return result;}
}
