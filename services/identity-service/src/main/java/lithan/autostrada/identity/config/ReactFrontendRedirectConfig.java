package lithan.autostrada.identity.config;
import java.util.Map;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;
@Configuration
public class ReactFrontendRedirectConfig {
 @Bean ViewResolver identityViews(@Value("${app.frontend.base-url}") String origin) {
  Map<String,String> routes=Map.ofEntries(Map.entry("login","/login"),Map.entry("register-account","/register"),
   Map.entry("register-profile","/register"),Map.entry("thank-you","/register/thank-you"),
   Map.entry("forgot-password","/forgot-password"),Map.entry("reset-password","/reset-password"),
   Map.entry("user/my-profile","/user/profile"),Map.entry("user/edit-profile","/user/profile/edit"),
   Map.entry("user/upload-picture","/user/profile"),Map.entry("admin/dashboard","/admin/users"),
   Map.entry("admin/edit-user","/admin/users"));
  return (name,locale) -> {
   if(name==null || name.startsWith("redirect:") || name.startsWith("forward:")) return null;
   var view=new RedirectView(origin+routes.getOrDefault(name,"/"));
   view.setExposeModelAttributes(false);view.setPropagateQueryParams(true);return view;
  };
 }
}
