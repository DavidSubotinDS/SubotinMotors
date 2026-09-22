package lithan.autostrada.identity.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lithan.autostrada.identity.entity.UserAccount;

public class CustomUserDetails implements UserDetails, org.springframework.security.core.CredentialsContainer {

  private final java.time.Instant authenticatedAt = java.time.Instant.now();
  private final String securityStamp;
  public static String stamp(String password, java.util.Collection<String> roles) {
    try {
      String roleNames=roles.stream().sorted().collect(java.util.stream.Collectors.joining(","));
      return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
          .digest((password+":"+roleNames).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable"); }
  }
  private static String stamp(UserAccount user) {
    return stamp(user.getPassword(), user.getRoles().stream().map(r -> r.getRole()).toList());
  }
  public java.time.Instant getAuthenticatedAt() { return authenticatedAt; }
  public boolean matchesSecurityState(UserAccount user) { return securityStamp.equals(stamp(user)); }
  public boolean matchesSecurityState(String password, java.util.Collection<String> roles) { return securityStamp.equals(stamp(password, roles)); }
  private final int userId;
  private final String username;
  private final String displayName;
  private String password;
  private final List<SimpleGrantedAuthority> authorities;

  public CustomUserDetails(UserAccount user) {
    super();
    this.securityStamp = stamp(user);
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
