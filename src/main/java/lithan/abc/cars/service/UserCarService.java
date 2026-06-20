package lithan.abc.cars.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.TestDrive;

public interface UserCarService {

  List<Car> listUserCar();

  void postCar(MultipartFile file, Car car) throws Exception;

  Car getOwnedCarById(int id);

  Car editOwnedCar(Car car);

  void changeOwnedCarStatus(int id, String status);

  void changeCarStatusByAdmin(int id, String status);

  Car getCarById(int id);

  void placeBid(int carId, int bidPrice);

  List<CarBidding> listCurrentUserBids();

  void cancelCurrentUserBid(int bidId);

  int highestBidding(int idCar);

  void saveTestDrive(LocalDate date, int carId);

  List<TestDrive> listTestDriveForOwnedCars();

  List<TestDrive> listCurrentUserTestDrives();

  void rescheduleCurrentUserTestDrive(int testDriveId, LocalDate date);

  void cancelCurrentUserTestDrive(int testDriveId);

  void acceptTestDriveForOwnedCar(int testDriveId);

  void rejectTestDriveForOwnedCar(int testDriveId);

  void cancelTestDriveForOwnedCar(int testDriveId);

  void saveUploadPicture(MultipartFile file, int carId) throws Exception;
}
