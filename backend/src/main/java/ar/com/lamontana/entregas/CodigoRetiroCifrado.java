package ar.com.lamontana.entregas;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.stereotype.Component;

/** Persistent private-volume key. Codes are recoverable by their owner without storing plaintext in the database. */
@Component
public class CodigoRetiroCifrado {
 private final byte[] key;private final SecureRandom random=new SecureRandom();
 public CodigoRetiroCifrado(Path directorioArchivos)throws java.io.IOException{
  Path file=directorioArchivos.resolve("codigo-retiro.key");
  try(var channel=java.nio.channels.FileChannel.open(directorioArchivos.resolve("codigo-retiro.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock()){
   if(!Files.exists(file)){byte[] created=new byte[32];random.nextBytes(created);if(file.getFileSystem().supportedFileAttributeViews().contains("posix"))Files.createFile(file,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));else Files.createFile(file);Files.write(file,created);}
   key=Files.readAllBytes(file);
  }
  if(key.length!=32)throw new IllegalStateException("La clave privada de códigos de retiro no es válida.");
 }
 public String cifrar(UUID id,String value){try{byte[] nonce=new byte[12];random.nextBytes(nonce);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));cipher.updateAAD(id.toString().getBytes(StandardCharsets.UTF_8));byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] all=Arrays.copyOf(nonce,nonce.length+encrypted.length);System.arraycopy(encrypted,0,all,nonce.length,encrypted.length);return Base64.getEncoder().encodeToString(all);}catch(java.security.GeneralSecurityException e){throw new IllegalStateException("No se pudo proteger el código de retiro.",e);}}
 public String descifrar(UUID id,String value){try{byte[] all=Base64.getDecoder().decode(value);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Arrays.copyOfRange(all,0,12)));cipher.updateAAD(id.toString().getBytes(StandardCharsets.UTF_8));return new String(cipher.doFinal(Arrays.copyOfRange(all,12,all.length)),StandardCharsets.UTF_8);}catch(java.security.GeneralSecurityException|IllegalArgumentException e){throw new IllegalStateException("No se pudo recuperar el código de retiro con su clave privada.",e);}}
}
