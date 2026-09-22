package lithan.autostrada.identity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lithan.autostrada.identity.entity.UserAccount;

@Controller
public class LoginController {

  @GetMapping("/login")
  public String loginPage(Model model) {
    UserAccount user = new UserAccount();

    model.addAttribute("user", user);

    return "login";
  }

}