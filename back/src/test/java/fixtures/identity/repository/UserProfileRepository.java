package fixtures.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fixtures.identity.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

}