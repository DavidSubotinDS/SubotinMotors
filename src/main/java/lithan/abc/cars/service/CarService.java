package lithan.abc.cars.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lithan.abc.cars.entity.Car;

public interface CarService {

  Car getCarById(int id);

  List<Car> listCar();

  Page<Car> findCatalogCars(String keyword, Integer low, Integer high, Pageable pageable);

  List<Car> featuredCars();
}
