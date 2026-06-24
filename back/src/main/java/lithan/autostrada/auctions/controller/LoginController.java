package lithan.autostrada.auctions.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lithan.autostrada.auctions.entity.UserAccount;

@Controller
public class LoginController {

  @GetMapping("/login")
  public String loginPage(Model model) {
    UserAccount user = new UserAccount();

    model.addAttribute("user", user);

    return "login";
  }

}