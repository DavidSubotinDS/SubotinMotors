package lithan.abc.cars.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.error.ResourceNotFoundException;
import lithan.abc.cars.repository.CarRepository;

@Service
public class CarServiceImpl implements CarService {

  @Autowired
  private CarRepository carRepo;

  @Override
  public List<Car> listCar() {
    return carRepo.findByStatusNot("DEACTIVE");
  }

  @Override
  public List<Car> searchCar(String keyword) {
    return carRepo.searchCar(keyword.trim());
  }

  @Override
  public List<Car> searchCarByPriceRange(int low, int high) {
    return carRepo.searchCarByPriceRange(low, high);
  }

  @Override
  public Car getCarById(int id) {
    return carRepo.findById(id).orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  public List<Car> searchCarByKeywordAndPriceRange(String keyword, int low, int high) {
    return carRepo.searchCarByKeywordAndPriceRange(keyword.trim(), low, high);
  }

  @Override
  public List<Car> featuredCars() {
    return carRepo.featuredCars(PageRequest.of(0, 3));
  }

}
