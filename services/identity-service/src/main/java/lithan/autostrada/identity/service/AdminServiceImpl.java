package lithan.autostrada.identity.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.identity.entity.Role;
import lithan.autostrada.identity.entity.UserAccount;
import lithan.autostrada.identity.entity.UserProfile;
import lithan.autostrada.identity.error.ResourceNotFoundException;
import lithan.autostrada.identity.repository.RoleRepository;
import lithan.autostrada.identity.repository.UserProfileRepository;
import lithan.autostrada.identity.repository.UserRepository;

@Service
public class AdminServiceImpl implements AdminService {

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private UserProfileRepository userProfileRepo;



  @Autowired
  private RoleRepository roleRepo;

  @Override
  @Transactional
  public void editUser(UserProfile profile) {
    UserProfile editedProfile = userProfileRepo.findById(profile.getIdProfile())
        .orElseThrow(ResourceNotFoundException::new);

    editedProfile.setFirstName(profile.getFirstName());
    editedProfile.setLastName(profile.getLastName());
    editedProfile.setPhoneNumber(profile.getPhoneNumber());
    editedProfile.setAddress(profile.getAddress());
    editedProfile.setStreetAddress(profile.getStreetAddress());
    editedProfile.setCity(profile.getCity());
    editedProfile.setPostalCode(profile.getPostalCode());
    editedProfile.setCountry(profile.getCountry());
    editedProfile.setAbout(profile.getAbout());

    userProfileRepo.save(editedProfile);
  }

  @Override
  @Transactional
  public void markAsAdmin(int idUser) {
    UserAccount user = userRepo.findById(idUser).orElseThrow(ResourceNotFoundException::new);
    if (!roleRepo.existsByUserAndRole(user, "ROLE_ADMIN")) {
      Role role = new Role();
      role.setRole("ROLE_ADMIN");
      role.setUser(user);
      roleRepo.save(role);
    }
  }

  @Override
  public Page<UserAccount> listUser(Pageable pageable) {
    return userRepo.findCustomers(pageable);
  }

  @Override
  public Page<UserAccount> listAdmin(Pageable pageable) {
    return userRepo.findAdmins(pageable);
  }

  @Override
  public UserProfile getProfileById(int idProfile) {
    return userProfileRepo.findById(idProfile).orElseThrow(ResourceNotFoundException::new);
  }

}
