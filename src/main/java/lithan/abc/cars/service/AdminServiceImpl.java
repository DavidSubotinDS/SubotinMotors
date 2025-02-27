package lithan.abc.cars.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.entity.UserProfile;
import lithan.abc.cars.repository.CarBiddingRepository;
import lithan.abc.cars.repository.CarRepository;
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

  @Override
  public void editUser(UserProfile profile) {
    UserProfile editedProfile = userProfileRepo.findById(profile.getIdProfile()).get();

    editedProfile.setFirstName(profile.getFirstName());
    editedProfile.setLastName(profile.getLastName());
    editedProfile.setPhoneNumber(profile.getPhoneNumber());
    editedProfile.setAddress(profile.getAddress());
    editedProfile.setAbout(profile.getAbout());

    userProfileRepo.save(editedProfile);
  }

  @Override
  public void markAsAdmin(int idUser) {
    UserAccount user = userRepo.findById(idUser).get();
    user.setRole("ROLE_ADMIN");
    userRepo.save(user);
  }

  @Override
  public List<UserAccount> listUser() {
    return userRepo.findAll().stream()
                   .filter(user -> user.getRole().equals("ROLE_USER"))
                   .collect(Collectors.toList());
  }

  @Override
  public List<UserAccount> listAdmin() {
    return userRepo.findAll().stream()
                   .filter(admin -> admin.getRole().equals("ROLE_ADMIN"))
                   .collect(Collectors.toList());
  }

  @Override
  public UserProfile getProfileById(int idProfile) {
    return userProfileRepo.findById(idProfile).orElse(null);
  }

  @Override
  public List<Car> listCar() {
    return carRepo.findAll();
  }

  @Override
  public List<CarBidding> listCarBid() {
    return carBidRepo.findAll().stream()
                     .filter(bid -> !bid.getStatus().equals("STARTING"))
                     .collect(Collectors.toList());
  }

  @Override
  public void approveCarBid(int idBid) {
    CarBidding carBidding = carBidRepo.findById(idBid).orElse(null);
    if (carBidding != null) {
      carBidding.setStatus("APPROVED");
      carBidding.getCar().setStatus("SOLD");
      carBidRepo.save(carBidding);
    }
  }

  @Override
  public void denyCarBid(int idBid) {
    CarBidding carBidding = carBidRepo.findById(idBid).orElse(null);
    if (carBidding != null) {
      carBidding.setStatus("DENIED");
      carBidRepo.save(carBidding);
    }
  }
}
