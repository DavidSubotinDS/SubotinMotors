package lithan.autostrada.auctions.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lithan.autostrada.auctions.dto.CarPartForm;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.service.CarPartService;
import lithan.autostrada.auctions.service.StoreOrderService;

@Controller
@RequestMapping("/admin/store")
public class StoreAdminController {

  private final CarPartService partService;
  private final StoreOrderService orderService;

  public StoreAdminController(CarPartService partService, StoreOrderService orderService) {
    this.partService = partService;
    this.orderService = orderService;
  }

  @GetMapping("/parts")
  public String parts(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "asc") String direction,
      Model model) {
    String safeSort = partSort(sort);
    Sort.Direction safeDirection = sortDirection(direction);
    Page<CarPart> parts = partService.listAll(
        PageRequest.of(Math.max(page, 0), 10, Sort.by(safeDirection, safeSort)));
    model.addAttribute("partPage", parts);
    model.addAttribute("parts", parts.getContent());
    model.addAttribute("sort", safeSort);
    model.addAttribute("direction", safeDirection.name().toLowerCase());
    return "admin/store-parts";
  }

  @GetMapping("/parts/new")
  public String newPart(Model model) {
    model.addAttribute("partForm", new CarPartForm());
    return "admin/store-part-form";
  }

  @GetMapping("/parts/{idPart}/edit")
  public String editPart(@PathVariable int idPart, Model model) {
    model.addAttribute("partForm", partService.formFor(idPart));
    return "admin/store-part-form";
  }

  @PostMapping("/parts")
  public String savePart(
      @Valid @ModelAttribute("partForm") CarPartForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (!bindingResult.hasErrors()) {
      try {
        partService.save(form);
      } catch (IllegalArgumentException exception) {
        bindingResult.rejectValue("sku", "duplicate", exception.getMessage());
      }
    }
    if (bindingResult.hasErrors()) {
      return "admin/store-part-form";
    }
    redirectAttributes.addFlashAttribute("storeMessage", "Product saved.");
    return "redirect:/admin/store/parts";
  }

  @PostMapping("/parts/{idPart}/active")
  public String setActive(
      @PathVariable int idPart,
      @RequestParam boolean active,
      RedirectAttributes redirectAttributes) {
    partService.setActive(idPart, active);
    redirectAttributes.addFlashAttribute(
        "storeMessage", active ? "Product activated." : "Product hidden from the catalog.");
    return "redirect:/admin/store/parts";
  }

  @GetMapping("/orders")
  public String orders(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      Model model) {
    String safeSort = orderSort(sort);
    Sort.Direction safeDirection = sortDirection(direction);
    Page<StoreOrder> orders = orderService.allOrders(
        PageRequest.of(Math.max(page, 0), 10, Sort.by(safeDirection, safeSort)));
    model.addAttribute("orderPage", orders);
    model.addAttribute("orders", orders.getContent());
    model.addAttribute("sort", safeSort);
    model.addAttribute("direction", safeDirection.name().toLowerCase());
    return "admin/store-orders";
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
