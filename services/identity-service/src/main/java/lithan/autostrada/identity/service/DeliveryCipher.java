package lithan.autostrada.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Versioned AES-GCM envelope, bound to delivery identity and expiry. Never stringify plaintext. */
@Component
public class DeliveryCipher {
  private final SecretKeySpec key;
  public DeliveryCipher(@Value("${notification.delivery-key}") String encoded) {
    try {byte[] bytes=Base64.getDecoder().decode(encoded);if(bytes.length!=32)throw new IllegalArgumentException();key=new SecretKeySpec(bytes,"AES");}
    catch(RuntimeException ignored){throw new IllegalArgumentException("A base64 256-bit notification delivery key is required");}
  }
  public String encrypt(String plain,String context) {
    try {
      byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);
      Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      return "v1."+Base64.getEncoder().encodeToString(nonce)+"."+Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }catch(Exception ignored){throw new IllegalStateException("Delivery encryption failed");}
  }
  public String decrypt(String encoded,String context) {
    try {
      String[] parts=encoded.split("\\.");if(parts.length!=3 || !parts[0].equals("v1"))throw new IllegalArgumentException();
      byte[] nonce=Base64.getDecoder().decode(parts[1]);if(nonce.length!=12)throw new IllegalArgumentException();
      Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,nonce));
      cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
      return new String(cipher.doFinal(Base64.getDecoder().decode(parts[2])),StandardCharsets.UTF_8);
    }catch(Exception ignored){throw new IllegalStateException("Invalid encrypted delivery");}
  }
}
