package lithan.autostrada.auctions.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import fixtures.identity.entity.UserAccount;

public class CustomUserDetails implements UserDetails, org.springframework.security.core.CredentialsContainer {

  private final int userId;
  private final String username;
  private final String displayName;
  private String password;
  private final List<SimpleGrantedAuthority> authorities;

  public CustomUserDetails(UserAccount user) {
    super();
    this.userId = user.getIdUser();
    this.username = user.getUsername();
    this.password = user.getPassword();
    this.displayName = user.getProfile() == null ? username
        : (user.getProfile().getFirstName() + " " + user.getProfile().getLastName()).trim();
    this.authorities = user.getRoles() == null ? List.of()
        : user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getRole())).toList();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public int getUserId() { return userId; }
  public String getDisplayName() { return displayName; }
  @Override public void eraseCredentials() { password = null; }

}
