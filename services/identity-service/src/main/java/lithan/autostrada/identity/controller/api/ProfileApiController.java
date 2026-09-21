package lithan.autostrada.identity.controller.api;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lithan.autostrada.identity.identity.*;
import lithan.autostrada.identity.dto.api.ApiModels.*;

@RestController
public class ProfileApiController {
  private final SelfProfileService self;
  private final ProfileClient profiles;
  public ProfileApiController(SelfProfileService self, ProfileClient profiles) { this.self=self; this.profiles=profiles; }
  @GetMapping("/api/user/profile") public ProfileResponse self() { return self.profile(); }
  @PutMapping("/api/user/profile") public ProfileResponse update(@RequestBody ProfileRequest body, HttpSession session) {
    return self.updateProfile(body, session);
  }
  @PostMapping("/api/user/profile/picture") public ProfileResponse picture(@RequestParam("imageFile") MultipartFile file,
      HttpSession session) throws Exception { return self.updateProfilePicture(file, session); }
  @GetMapping("/api/public/profiles/{id}") public ProfileResponse profile(@PathVariable int id) {
    PublicProfile p=profiles.findByProfileId(id).orElseThrow(lithan.autostrada.identity.error.ResourceNotFoundException::new);
    return new ProfileResponse(p.profileId(), null, null, null, p.firstName(), p.lastName(), null, null,
        null, p.city(), null, p.country(), p.about(), false, "", p.displayLocation(),
        p.pictureType()==null || p.picture()==null ? null : "data:"+p.pictureType()+";base64,"+p.picture());
  }
}
