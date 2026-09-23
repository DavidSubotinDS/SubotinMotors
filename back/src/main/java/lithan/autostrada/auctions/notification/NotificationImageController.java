package lithan.autostrada.auctions.notification;

import lithan.autostrada.auctions.repository.CarRepository;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
class NotificationImageController {
  private final CarRepository cars;
  NotificationImageController(CarRepository cars){this.cars=cars;}
  @GetMapping("/api/public/auctions/{id}/notification-image")
  @Transactional(readOnly=true)
  public ResponseEntity<byte[]> image(@PathVariable int id) {
    var car=cars.findById(id);
    if(car.isEmpty() || car.get().getCarPicture()==null)return ResponseEntity.notFound().build();
    var picture=car.get().getCarPicture();
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(picture.getFileType()))
        .header("X-Content-Type-Options","nosniff").body(java.util.Base64.getDecoder().decode(picture.getImage()));
  }
}
