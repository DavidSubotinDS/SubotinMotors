package lithan.abc.cars.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.Role;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.entity.UserProfile;
import lithan.abc.cars.error.ResourceNotFoundException;
import lithan.abc.cars.repository.CarBiddingRepository;
import lithan.abc.cars.repository.CarRepository;
import lithan.abc.cars.repository.RoleRepository;
import lithan.abc.cars.repository.UserProfileRepository;
import lithan.abc.cars.repository.UserRepository;

@Service
public class AdminServiceImpl implements AdminService {

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private UserProfileRepository userProfileRepo;

  @Autowired
  private CarRepository carRepo;

  @Autowired
  private CarBiddingRepository carBidRepo;

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
  public List<UserAccount> listUser() {
    return userRepo.findAll().stream()
        .filter(user -> user.getRoles().stream().noneMatch(role -> "ROLE_ADMIN".equals(role.getRole())))
        .collect(java.util.stream.Collectors.toList());
  }

  @Override
  public List<UserAccount> listAdmin() {
    return userRepo.findAll().stream()
        .filter(user -> user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getRole())))
        .collect(java.util.stream.Collectors.toList());
  }

  @Override
  public UserProfile getProfileById(int idProfile) {
    return userProfileRepo.findById(idProfile).orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  public List<Car> listCar() {
    return carRepo.findAll();
  }

  @Override
  public List<CarBidding> listCarBid() {
    return carBidRepo.findByStatusNot("STARTING");
  }

  @Override
  @Transactional
  public void approveCarBid(int idBid) {
    CarBidding carBidding = carBidRepo.findById(idBid).orElseThrow(ResourceNotFoundException::new);
    if (!"ONGOING".equals(carBidding.getStatus()) || !"ACTIVE".equals(carBidding.getCar().getStatus())) {
      throw new IllegalStateException("Only ongoing bids on active cars can be approved");
    }
    carBidding.setStatus("APPROVED");
    carBidding.getCar().setStatus("SOLD");
    carBidRepo.findByCarIdCar(carBidding.getCar().getIdCar()).stream()
        .filter(bid -> bid.getIdBid() != carBidding.getIdBid() && "ONGOING".equals(bid.getStatus()))
        .forEach(bid -> bid.setStatus("DENIED"));
    carBidRepo.save(carBidding);
  }

  @Override
  @Transactional
  public void denyCarBid(int idBid) {
    CarBidding carBidding = carBidRepo.findById(idBid).orElseThrow(ResourceNotFoundException::new);
    if (!"ONGOING".equals(carBidding.getStatus())) {
      throw new IllegalStateException("Only ongoing bids can be denied");
    }
    carBidding.setStatus("DENIED");
    carBidRepo.save(carBidding);
  }
}
