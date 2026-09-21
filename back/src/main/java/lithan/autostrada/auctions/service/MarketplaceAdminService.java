package lithan.autostrada.auctions.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.CarBiddingRepository;

@Service
public class MarketplaceAdminService {
  private final CarRepository carRepo;
  private final CarBiddingRepository carBidRepo;
  public MarketplaceAdminService(CarRepository carRepo, CarBiddingRepository carBidRepo) {
    this.carRepo = carRepo;
    this.carBidRepo = carBidRepo;
  }

  public Page<Car> listCar(Pageable pageable) {
    return carRepo.findAll(pageable);
  }

  public Page<CarBidding> listCarBid(Pageable pageable) {
    return carBidRepo.findByStatusNot("STARTING", pageable);
  }

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
