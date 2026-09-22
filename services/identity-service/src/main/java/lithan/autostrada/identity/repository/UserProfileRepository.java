package lithan.autostrada.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.identity.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

}