package lithan.autostrada.auctions.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.CarRepository;

@Service
public class CarServiceImpl implements CarService {

  @Autowired
  private CarRepository carRepo;

  @Override
  public List<Car> listCar() {
    return carRepo.findByStatusNot("DEACTIVE");
  }

  @Override
  public Car getCarById(int id) {
    return carRepo.findById(id).orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  public Page<Car> findCatalogCars(String keyword, Integer low, Integer high, Pageable pageable) {
    String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    if (low != null && high != null && low > high) {
      throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
    }
    return carRepo.findCatalogCars(normalizedKeyword, low, high, pageable);
  }

  @Override
  public List<Car> featuredCars() {
    return carRepo.featuredCars(PageRequest.of(0, 3));
  }

}
