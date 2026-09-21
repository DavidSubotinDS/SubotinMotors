package lithan.autostrada.identity.identity;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InProcessProfileClient implements ProfileClient, CheckoutProfileClient {
  private final EntityManager em;
  private final CurrentIdentity identity;
  public InProcessProfileClient(EntityManager em, CurrentIdentity identity) {
    this.em = em;
    this.identity = identity;
  }

  @Override
  public Map<Integer, PublicProfile> findAll(Collection<Integer> userIds) {
    var ids = new java.util.ArrayList<>(new LinkedHashSet<>(userIds));
    ids.removeIf(id -> id == null || id <= 0);
    Map<Integer, PublicProfile> result = new LinkedHashMap<>();
    // Two scalar queries per bounded chunk: never hydrate credential entities,
    // eager role/profile associations, or issue one query per displayed user.
    for (int offset = 0; offset < ids.size(); offset += 500) {
      var chunk = ids.subList(offset, Math.min(offset + 500, ids.size()));
      var admins = new java.util.HashSet<>(em.createQuery(
          "select r.user.idUser from Role r where r.user.idUser in :ids and r.role = 'ROLE_ADMIN'",
          Integer.class).setParameter("ids", chunk).getResultList());
      var rows = em.createQuery("select u.idUser, p.idProfile, u.username, p.firstName, p.lastName, "
          + "p.city, p.country, p.about, pic.fileType, pic.image from UserAccount u "
          + "left join u.profile p left join p.profilePicture pic where u.idUser in :ids", Object[].class)
          .setParameter("ids", chunk).getResultList();
      for (var row : rows) {
        int id = (Integer) row[0];
        result.put(id, new PublicProfile(id, (Integer) row[1], (String) row[2],
            (String) row[3], (String) row[4], (String) row[5], (String) row[6],
            (String) row[7], (String) row[8], (String) row[9], admins.contains(id)));
      }
    }
    return Map.copyOf(result);
  }

  @Override
  public Optional<PublicProfile> findByProfileId(int profileId) {
    return em.createQuery("select p.user.idUser from UserProfile p where p.idProfile = :id", Integer.class)
        .setParameter("id", profileId).getResultStream().findFirst().map(this::display);
  }

  @Override
  public CheckoutProfile current() {
    return checkout(identity.requireUserId());
  }

  public CheckoutProfile checkout(int id) {
    return em.createQuery("select new lithan.autostrada.identity.identity.CheckoutProfile("
        + "u.idUser, u.email, concat(concat(coalesce(p.firstName, u.username), ' '), coalesce(p.lastName, '')), "
        + "p.streetAddress, p.city, p.postalCode, p.country) from UserAccount u left join u.profile p "
        + "where u.idUser = :id", CheckoutProfile.class).setParameter("id", id)
        .getSingleResult();
  }
}
