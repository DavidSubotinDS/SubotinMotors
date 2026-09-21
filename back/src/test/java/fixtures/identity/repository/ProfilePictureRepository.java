package fixtures.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fixtures.identity.entity.ProfilePicture;

public interface ProfilePictureRepository extends JpaRepository<ProfilePicture, Integer> {

}