package lithan.abc.cars.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Autowired
  private PaymentService paymentService;

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

  @Override
  public Page<Car> listCar(Pageable pageable) {
    return carRepo.findAll(pageable);
  }

  @Override
  public Page<CarBidding> listCarBid(Pageable pageable) {
    return carBidRepo.findByStatusNot("STARTING", pageable);
  }

  @Override
  @Transactional
  public void approveCarBid(int idBid) {
    paymentService.acceptBidForPayment(idBid);
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
