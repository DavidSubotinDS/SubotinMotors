package lithan.autostrada.auctions.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.Role;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.entity.UserProfile;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.CarBiddingRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.RoleRepository;
import lithan.autostrada.auctions.repository.UserProfileRepository;
import lithan.autostrada.auctions.repository.UserRepository;

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
    CarBidding acceptedBid = carBidRepo.findById(idBid).orElseThrow(ResourceNotFoundException::new);
    Car car = acceptedBid.getCar();
    if (!"ONGOING".equals(acceptedBid.getStatus()) || !"ACTIVE".equals(car.getStatus())) {
      throw new IllegalStateException("Only ongoing bids on active cars can be accepted");
    }
    acceptedBid.setStatus("ACCEPTED");
    car.setStatus("SOLD");
    carBidRepo.findByCarIdCar(car.getIdCar()).stream()
        .filter(other -> other.getIdBid() != acceptedBid.getIdBid() && "ONGOING".equals(other.getStatus()))
        .forEach(other -> other.setStatus("DENIED"));
    carBidRepo.save(acceptedBid);
    carRepo.save(car);
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
