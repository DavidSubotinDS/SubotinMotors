package lithan.autostrada.auctions.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import lithan.autostrada.auctions.dto.CarPartForm;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.CarPartRepository;

@Service
public class CarPartServiceImpl implements CarPartService {

  private final CarPartRepository partRepository;

  public CarPartServiceImpl(CarPartRepository partRepository) {
    this.partRepository = partRepository;
  }

  @Override
  public Page<CarPart> browse(String keyword, String category, Pageable pageable) {
    Specification<CarPart> specification = (root, query, builder) -> {
      Predicate predicate = builder.isTrue(root.get("active"));
      if (keyword != null && !keyword.isBlank()) {
        String search = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        predicate = builder.and(predicate, builder.or(
            builder.like(builder.lower(root.get("name")), search),
            builder.like(builder.lower(root.get("description")), search),
            builder.like(builder.lower(root.get("sku")), search)));
      }
      if (category != null && !category.isBlank()) {
        predicate = builder.and(predicate, builder.equal(root.get("category"), category.trim()));
      }
      return predicate;
    };
    return partRepository.findAll(specification, pageable);
  }

  @Override
  public Page<CarPart> listAll(Pageable pageable) {
    return partRepository.findAll(pageable);
  }

  @Override
  public List<String> categories() {
    return partRepository.findActiveCategories();
  }

  @Override
  public CarPart getActivePart(int idPart) {
    CarPart part = getPart(idPart);
    if (!part.isActive()) {
      throw new ResourceNotFoundException();
    }
    return part;
  }

  @Override
  public CarPart getPart(int idPart) {
    return partRepository.findById(idPart).orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  public CarPartForm formFor(int idPart) {
    CarPart part = getPart(idPart);
    CarPartForm form = new CarPartForm();
    form.setIdPart(part.getIdPart());
    form.setSku(part.getSku());
    form.setName(part.getName());
    form.setCategory(part.getCategory());
    form.setDescription(part.getDescription());
    form.setPriceMinor(part.getPriceMinor());
    form.setStockQuantity(part.getStockQuantity());
    form.setActive(part.isActive());
    form.setImageUrl(part.getImageUrl());
    return form;
  }

  @Override
  @Transactional
  public CarPart save(CarPartForm form) {
    String normalizedSku = form.getSku().trim().toUpperCase(Locale.ROOT);
    if (partRepository.existsBySkuIgnoreCaseAndIdPartNot(normalizedSku, form.getIdPart())) {
      throw new IllegalArgumentException("A product with this SKU already exists");
    }

    Instant now = Instant.now();
    CarPart part = form.getIdPart() == 0 ? new CarPart() : getPart(form.getIdPart());
    if (part.getCreatedAt() == null) {
      part.setCreatedAt(now);
    }
    part.setSku(normalizedSku);
    part.setName(form.getName().trim());
    part.setCategory(form.getCategory().trim());
    part.setDescription(form.getDescription().trim());
    part.setPriceMinor(form.getPriceMinor());
    part.setStockQuantity(form.getStockQuantity());
    part.setActive(form.isActive());
    part.setImageUrl(form.getImageUrl() == null || form.getImageUrl().isBlank()
        ? null : form.getImageUrl().trim());
    part.setUpdatedAt(now);
    return partRepository.save(part);
  }

  @Override
  @Transactional
  public void setActive(int idPart, boolean active) {
    CarPart part = getPart(idPart);
    part.setActive(active);
    part.setUpdatedAt(Instant.now());
  }
}
