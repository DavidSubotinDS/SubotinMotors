package lithan.autostrada.auctions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

}