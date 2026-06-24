package lithan.autostrada.auctions.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.CarPicture;
import lithan.autostrada.auctions.entity.TestDrive;
import lithan.autostrada.auctions.entity.TestDriveStatus;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.CarBiddingRepository;
import lithan.autostrada.auctions.repository.CarPictureRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.TestDriveRepository;
import lithan.autostrada.auctions.validation.ImageUploadValidator;
import lithan.autostrada.auctions.validation.ImageUploadValidator.ValidatedImage;

@Service
public class UserCarServiceImpl implements UserCarService {

  @Autowired
  private UserService userService;

  @Autowired
  private CarRepository carRepo;

  @Autowired
  private CarPictureRepository carPictureRepo;

  @Autowired
  private CarBiddingRepository carBidRepo;

  @Autowired
  private TestDriveRepository testDriveRepo;

  @Override
  public List<Car> listUserCar() {
    return carRepo.findByUser(userService.getUserLogin());
  }

  @Override
  @Transactional
  public void postCar(MultipartFile file, Car car) throws Exception {
    requireFutureAuctionEnd(car);
    ValidatedImage image = ImageUploadValidator.validate(file);
    UserAccount user = userService.getUserLogin();
    CarPicture picture = new CarPicture();

    picture.setFileName(image.fileName());
    picture.setFileType(image.contentType());
    picture.setImage(Base64.getEncoder().encodeToString(image.bytes()));
    picture.setCar(car);

    car.setCarPicture(picture);
    car.setStatus("PENDING");
    car.setUser(user);
    carRepo.save(car);
  }

  @Override
  public Car getOwnedCarById(int id) {
    Car car = getCarById(id);
    if (car.getUser().getIdUser() != userService.getUserLogin().getIdUser()) {
      throw new AccessDeniedException("You do not own this car");
    }
    return car;
  }

  @Override
  @Transactional
  public Car editOwnedCar(Car car) {
    requireFutureAuctionEnd(car);
    Car editedCar = getOwnedCarById(car.getIdCar());
    if ("SOLD".equals(editedCar.getStatus())) {
      throw new IllegalStateException("Sold cars cannot be edited");
    }
    editedCar.setMake(car.getMake());
    editedCar.setModel(car.getModel());
    editedCar.setYear(car.getYear());
    editedCar.setPrice(car.getPrice());
    editedCar.setAuctionEndTime(car.getAuctionEndTime());
    return carRepo.save(editedCar);
  }

  @Override
  @Transactional
  public void changeOwnedCarStatus(int id, String status) {
    Car car = getOwnedCarById(id);
    if ("PENDING".equals(car.getStatus())) {
      throw new IllegalStateException("Listings awaiting approval can only be reviewed by an administrator");
    }
    changeStatus(car, status);
  }

  @Override
  @Transactional
  public void changeCarStatusByAdmin(int id, String status) {
    changeStatus(getCarById(id), status);
  }

  private void changeStatus(Car car, String status) {
    if ("SOLD".equals(car.getStatus())) {
      throw new IllegalStateException("Sold cars cannot be reactivated");
    }
    if (!"ACTIVE".equals(status) && !"DEACTIVE".equals(status)) {
      throw new IllegalArgumentException("Unsupported car status");
    }
    car.setStatus(status);
    carRepo.save(car);
  }

  @Override
  public Car getCarById(int id) {
    return carRepo.findById(id).orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  @Transactional
  public void placeBid(int carId, int bidPrice) {
    Car car = getCarById(carId);
    UserAccount bidder = userService.getUserLogin();
    if (!"ACTIVE".equals(car.getStatus())) {
      throw new IllegalStateException("Bids are only accepted on active cars");
    }
    if (!car.isAuctionOpen()) {
      throw new IllegalStateException("This auction has ended");
    }
    if (car.getUser().getIdUser() == bidder.getIdUser()) {
      throw new IllegalStateException("You cannot bid on your own car");
    }
    int minimum = Math.max(car.getPrice(), highestBidding(carId));
    if (bidPrice <= minimum) {
      throw new IllegalArgumentException("Bid must be greater than the listed price and current highest bid");
    }

    CarBidding bid = new CarBidding();
    bid.setCar(car);
    bid.setUser(bidder);
    bid.setBidPrice(bidPrice);
    bid.setStatus("ONGOING");
    carBidRepo.save(bid);
  }

  @Override
  public List<CarBidding> listCurrentUserBids() {
    return carBidRepo.findByUserOrderByIdBidDesc(userService.getUserLogin());
  }

  @Override
  @Transactional
  public void cancelCurrentUserBid(int bidId) {
    CarBidding bid = carBidRepo.findById(bidId).orElseThrow(ResourceNotFoundException::new);
    if (bid.getUser().getIdUser() != userService.getUserLogin().getIdUser()) {
      throw new AccessDeniedException("This bid belongs to another user");
    }
    if (!"ONGOING".equals(bid.getStatus())) {
      throw new IllegalStateException("Only ongoing bids can be cancelled");
    }
    bid.setStatus("CANCELLED");
    carBidRepo.save(bid);
  }

  @Override
  public int highestBidding(int idCar) {
    Integer highestBid = carBidRepo.highestBid(idCar);
    return highestBid == null ? 0 : highestBid;
  }

  @Override
  @Transactional
  public void saveTestDrive(LocalDate date, int carId) {
    validateTestDriveDate(date);
    Car car = getCarById(carId);
    UserAccount user = userService.getUserLogin();
    if (!car.isAuctionOpen()) {
      throw new IllegalStateException("Test drives are only available for active cars");
    }
    if (car.getUser().getIdUser() == user.getIdUser()) {
      throw new IllegalStateException("You cannot book a test drive for your own car");
    }
    if (testDriveRepo.existsByUserAndCarAndDate(user, car, date)) {
      throw new IllegalStateException("You already booked this car for that date");
    }

    TestDrive testDrive = new TestDrive();
    testDrive.setDate(date);
    testDrive.setCar(car);
    testDrive.setUser(user);
    testDrive.setStatus(TestDriveStatus.PENDING);
    testDriveRepo.save(testDrive);
  }

  @Override
  public List<TestDrive> listTestDriveForOwnedCars() {
    return testDriveRepo.findByCarUserOrderByDateAsc(userService.getUserLogin());
  }

  @Override
  public List<TestDrive> listCurrentUserTestDrives() {
    return testDriveRepo.findByUserOrderByDateAsc(userService.getUserLogin());
  }

  @Override
  @Transactional
  public void rescheduleCurrentUserTestDrive(int testDriveId, LocalDate date) {
    validateTestDriveDate(date);
    TestDrive testDrive = getCurrentUserTestDrive(testDriveId);
    if (!testDrive.isReschedulable()) {
      throw new IllegalStateException("Only pending or accepted test drives can be rescheduled");
    }
    if (!"ACTIVE".equals(testDrive.getCar().getStatus())) {
      throw new IllegalStateException("Only test drives for active cars can be rescheduled");
    }
    if (testDriveRepo.existsByUserAndCarAndDateAndIdTestDriveNot(
        testDrive.getUser(), testDrive.getCar(), date, testDriveId)) {
      throw new IllegalStateException("You already booked this car for that date");
    }
    testDrive.setDate(date);
    testDrive.setStatus(TestDriveStatus.PENDING);
    testDriveRepo.save(testDrive);
  }

  @Override
  @Transactional
  public void cancelCurrentUserTestDrive(int testDriveId) {
    TestDrive testDrive = getCurrentUserTestDrive(testDriveId);
    if (!testDrive.isCancellable()) {
      throw new IllegalStateException("Only pending or accepted test drives can be cancelled");
    }
    testDrive.setStatus(TestDriveStatus.CANCELLED);
    testDriveRepo.save(testDrive);
  }

  @Override
  @Transactional
  public void acceptTestDriveForOwnedCar(int testDriveId) {
    decideTestDriveForOwnedCar(testDriveId, TestDriveStatus.ACCEPTED);
  }

  @Override
  @Transactional
  public void rejectTestDriveForOwnedCar(int testDriveId) {
    decideTestDriveForOwnedCar(testDriveId, TestDriveStatus.REJECTED);
  }

  @Override
  @Transactional
  public void cancelTestDriveForOwnedCar(int testDriveId) {
    TestDrive testDrive = getTestDriveForOwnedCar(testDriveId);
    if (!testDrive.isAccepted()) {
      throw new IllegalStateException("Only accepted test drives can be cancelled by the car owner");
    }
    testDrive.setStatus(TestDriveStatus.CANCELLED);
    testDriveRepo.save(testDrive);
  }

  private void decideTestDriveForOwnedCar(int testDriveId, TestDriveStatus status) {
    TestDrive testDrive = getTestDriveForOwnedCar(testDriveId);
    if (!testDrive.isPending()) {
      throw new IllegalStateException("Only pending test-drive requests can be accepted or rejected");
    }
    testDrive.setStatus(status);
    testDriveRepo.save(testDrive);
  }

  private TestDrive getCurrentUserTestDrive(int testDriveId) {
    TestDrive testDrive = testDriveRepo.findById(testDriveId)
        .orElseThrow(ResourceNotFoundException::new);
    if (testDrive.getUser().getIdUser() != userService.getUserLogin().getIdUser()) {
      throw new AccessDeniedException("This test drive belongs to another user");
    }
    return testDrive;
  }

  private TestDrive getTestDriveForOwnedCar(int testDriveId) {
    TestDrive testDrive = testDriveRepo.findById(testDriveId)
        .orElseThrow(ResourceNotFoundException::new);
    if (testDrive.getCar().getUser().getIdUser() != userService.getUserLogin().getIdUser()) {
      throw new AccessDeniedException("This test drive is for another owner's car");
    }
    return testDrive;
  }

  private void validateTestDriveDate(LocalDate date) {
    if (date == null || !date.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("Test drive date must be in the future");
    }
  }

  @Override
  @Transactional
  public void saveUploadPicture(MultipartFile file, int carId) throws Exception {
    ValidatedImage image = ImageUploadValidator.validate(file);
    Car car = getOwnedCarById(carId);
    CarPicture picture = car.getCarPicture();
    picture.setFileName(image.fileName());
    picture.setFileType(image.contentType());
    picture.setImage(Base64.getEncoder().encodeToString(image.bytes()));
    carPictureRepo.save(picture);
  }

  private void requireFutureAuctionEnd(Car car) {
    if (car.getAuctionEndTime() == null
        || !car.getAuctionEndTime().isAfter(LocalDateTime.now())) {
      throw new IllegalArgumentException(
          "Auction end date and time must be in the future");
    }
  }
}
