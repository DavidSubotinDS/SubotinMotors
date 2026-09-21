package lithan.autostrada.auctions.identity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ProfileClient {
  /** Missing accounts are omitted; an account without a profile retains its username. */
  Map<Integer, PublicProfile> findAll(Collection<Integer> userIds);
  Optional<PublicProfile> findByProfileId(int profileId);
  default PublicProfile display(int userId) {
    return findAll(java.util.List.of(userId)).getOrDefault(userId, PublicProfile.missing(userId));
  }
}
