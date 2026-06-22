package lithan.abc.cars.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lithan.abc.cars.dto.CarPartForm;
import lithan.abc.cars.entity.CarPart;

public interface CarPartService {
  Page<CarPart> browse(String keyword, String category, Pageable pageable);

  Page<CarPart> listAll(Pageable pageable);

  List<String> categories();

  CarPart getActivePart(int idPart);

  CarPart getPart(int idPart);

  CarPartForm formFor(int idPart);

  CarPart save(CarPartForm form);

  void setActive(int idPart, boolean active);
}
