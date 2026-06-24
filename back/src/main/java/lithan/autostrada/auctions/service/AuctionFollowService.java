package lithan.autostrada.auctions.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.AuctionFollow;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.repository.AuctionFollowRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.error.ResourceNotFoundException;

@Service
public class AuctionFollowService {

  private final AuctionFollowRepository followRepository;
  private final CarRepository carRepository;
  private final UserService userService;
  private final AuctionNotificationService notificationService;
  private final Clock clock;

  public AuctionFollowService(
      AuctionFollowRepository followRepository,
      CarRepository carRepository,
      UserService userService,
      AuctionNotificationService notificationService,
      Clock clock) {
    this.followRepository = followRepository;
    this.carRepository = carRepository;
    this.userService = userService;
    this.notificationService = notificationService;
    this.clock = clock;
  }

  public boolean follow(int carId) {
    UserAccount user = userService.getUserLogin();
    Car car = carRepository.findById(carId).orElseThrow(ResourceNotFoundException::new);
    if (!car.isAuctionOpenAt(LocalDateTime.now(clock))) {
      throw new IllegalStateException("Only active auctions can be followed");
    }
    if (followRepository.existsByUserAndCar(user, car)) {
      return false;
    }

    AuctionFollow follow = new AuctionFollow();
    follow.setUser(user);
    follow.setCar(car);
    follow.setFollowedAt(LocalDateTime.now(clock));
    try {
      followRepository.saveAndFlush(follow);
    } catch (DataIntegrityViolationException exception) {
      return false;
    }
    notificationService.createEndingSoonNotification(user, car);
    return true;
  }

  @Transactional
  public boolean unfollow(int carId) {
    UserAccount user = userService.getUserLogin();
    Car car = carRepository.findById(carId).orElseThrow(ResourceNotFoundException::new);
    return followRepository.findByUserAndCar(user, car)
        .map(follow -> {
          followRepository.delete(follow);
          return true;
        })
        .orElse(false);
  }

  public List<AuctionFollow> listCurrentUserFollows() {
    return followRepository.findByUserOrderByCarAuctionEndTimeAsc(
        userService.getUserLogin());
  }

  public boolean isFollowingCurrentUser(Car car) {
    try {
      return followRepository.existsByUserAndCar(userService.getUserLogin(), car);
    } catch (RuntimeException exception) {
      return false;
    }
  }
}
