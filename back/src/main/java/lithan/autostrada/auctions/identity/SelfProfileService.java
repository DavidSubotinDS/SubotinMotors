package lithan.autostrada.auctions.identity;
import org.springframework.stereotype.Service;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileResponse;
/** Read-only adapter for the still mixed workspace response. */
@Service
public class SelfProfileService {
  private final RemoteProfileClient profiles;
  public SelfProfileService(RemoteProfileClient profiles) {this.profiles=profiles;}
  public ProfileResponse profile() {return profiles.self();}
}
