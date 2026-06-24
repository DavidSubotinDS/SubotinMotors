package lithan.autostrada.auctions.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lithan.autostrada.auctions.dto.ListingCommentForm;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.service.CarService;
import lithan.autostrada.auctions.service.ListingCommentService;

@Controller
public class ListingCommentController {

  private final ListingCommentService commentService;
  private final CarService carService;

  public ListingCommentController(
      ListingCommentService commentService,
      CarService carService) {
    this.commentService = commentService;
    this.carService = carService;
  }

  @PostMapping("/cars/{idCar}/comments")
  public String addCarComment(
      @PathVariable int idCar,
      @Valid @ModelAttribute("commentForm") ListingCommentForm commentForm,
      BindingResult bindingResult,
      @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute(
          "commentError", bindingResult.getAllErrors().get(0).getDefaultMessage());
      return carRedirect(idCar);
    }
    try {
      commentService.addCarComment(idCar, commentForm.getBody(), imageFile);
      redirectAttributes.addFlashAttribute("commentMessage", "Your comment was posted.");
    } catch (IllegalArgumentException exception) {
      redirectAttributes.addFlashAttribute("commentError", exception.getMessage());
    }
    return carRedirect(idCar);
  }

  @PostMapping("/parts/{idPart}/comments")
  public String addPartComment(
      @PathVariable int idPart,
      @Valid @ModelAttribute("commentForm") ListingCommentForm commentForm,
      BindingResult bindingResult,
      @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute(
          "commentError", bindingResult.getAllErrors().get(0).getDefaultMessage());
    } else {
      try {
        commentService.addPartComment(idPart, commentForm.getBody(), imageFile);
        redirectAttributes.addFlashAttribute("commentMessage", "Your comment was posted.");
      } catch (IllegalArgumentException exception) {
        redirectAttributes.addFlashAttribute("commentError", exception.getMessage());
      }
    }
    return "redirect:/parts/" + idPart + "#discussion";
  }

  private String carRedirect(int idCar) {
    Car car = carService.getCarById(idCar);
    return "redirect:/cars/" + car.getMake() + "/" + car.getModel() + "/"
        + car.getYear() + "/" + car.getIdCar() + "#discussion";
  }
}
