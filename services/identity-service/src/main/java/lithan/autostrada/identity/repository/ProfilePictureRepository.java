package lithan.autostrada.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.identity.entity.ProfilePicture;

public interface ProfilePictureRepository extends JpaRepository<ProfilePicture, Integer> {

}