package lithan.abc.cars.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.entity.UserProfile;

public interface AdminService {

  void editUser(UserProfile profile);

  void markAsAdmin(int idUser);

  UserProfile getProfileById(int idCar);

  Page<UserAccount> listUser(Pageable pageable);

  Page<UserAccount> listAdmin(Pageable pageable);

  Page<Car> listCar(Pageable pageable);

  Page<CarBidding> listCarBid(Pageable pageable);

  void approveCarBid(int idBid);

  void denyCarBid(int idBid);
}
