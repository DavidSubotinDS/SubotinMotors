package lithan.autostrada.auctions.controller.api;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lithan.autostrada.auctions.dto.CarPartForm;
import lithan.autostrada.auctions.dto.api.ApiModels.ApiMessageResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.PartActiveRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.StoreOrderResponse;
import lithan.autostrada.auctions.dto.api.PageResponse;
import lithan.autostrada.auctions.dto.api.PartSummaryResponse;
import lithan.autostrada.auctions.service.CarPartService;
import lithan.autostrada.auctions.service.StoreOrderService;

@RestController
@RequestMapping("/api/admin/store")
public class StoreAdminApiController {

  private final CarPartService partService;
  private final StoreOrderService orderService;
  private final ApiModelMapper mapper;

  public StoreAdminApiController(
      CarPartService partService,
      StoreOrderService orderService,
      ApiModelMapper mapper) {
    this.partService = partService;
    this.orderService = orderService;
    this.mapper = mapper;
  }

  @GetMapping("/parts")
  public PageResponse<PartSummaryResponse> parts(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "asc") String direction) {
    var parts = partService.listAll(PageRequest.of(
        Math.max(page, 0),
        10,
        Sort.by(sortDirection(direction), partSort(sort))));
    return PageResponse.from(parts.map(mapper::part));
  }

  @GetMapping("/parts/{idPart}")
  public CarPartForm partForm(@PathVariable int idPart) {
    return partService.formFor(idPart);
  }

  @PostMapping("/parts")
  public PartSummaryResponse createPart(@Valid @RequestBody CarPartForm form) {
    return mapper.part(partService.save(form));
  }

  @PutMapping("/parts/{idPart}")
  public PartSummaryResponse updatePart(
      @PathVariable int idPart,
      @Valid @RequestBody CarPartForm form) {
    form.setIdPart(idPart);
    return mapper.part(partService.save(form));
  }

  @PostMapping("/parts/{idPart}/active")
  public ApiMessageResponse setActive(
      @PathVariable int idPart,
      @RequestBody PartActiveRequest request) {
    partService.setActive(idPart, request.active());
    return new ApiMessageResponse(
        request.active() ? "Product activated." : "Product hidden from the catalog.",
        null);
  }

  @GetMapping("/orders")
  public PageResponse<StoreOrderResponse> orders(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    var orders = orderService.allOrders(PageRequest.of(
        Math.max(page, 0),
        10,
        Sort.by(sortDirection(direction), orderSort(sort))));
    return PageResponse.from(orders.map(mapper::storeOrder));
  }

  @GetMapping("/orders/{idOrder}")
  public StoreOrderResponse order(@PathVariable int idOrder) {
    return mapper.storeOrder(orderService.adminOrder(idOrder));
  }

  private Sort.Direction sortDirection(String direction) {
    return "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
  }

  private String partSort(String sort) {
    return switch (sort) {
      case "name", "sku", "category", "priceMinor", "stockQuantity", "active" -> sort;
      default -> "name";
    };
  }

  private String orderSort(String sort) {
    return switch (sort) {
      case "createdAt", "paidAt", "totalMinor", "status", "user.username" -> sort;
      default -> "createdAt";
    };
  }
}
