package ar.com.lamontana;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import ar.com.lamontana.entregas.*;
import ar.com.lamontana.identidad.CorreoService;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class CodigoRetiroServiceTest{
 @TempDir Path files;private JdbcTemplate jdbc;private CorreoService mail;private CodigoRetiroCifrado cipher;private CodigoRetiroService service;
 private boolean eligible=true,created=false;private Object[] inserted;private List<Object[]> notifications=List.of();
 @BeforeEach @SuppressWarnings("unchecked") void setup()throws Exception{
  jdbc=mock(JdbcTemplate.class);mail=mock(CorreoService.class);cipher=new CodigoRetiroCifrado(files);var encoder=mock(PasswordEncoder.class);when(encoder.encode(anyString())).thenReturn("HASH");
  when(jdbc.query(anyString(),any(RowMapper.class),any(Object[].class))).thenAnswer(i->{String sql=i.getArgument(0);if(sql.startsWith("SELECT r.id_reserva"))return eligible?Collections.singletonList(new Object[]{10L,"cliente@prueba.local",11L}):List.of();if(sql.startsWith("SELECT id_codigo_entrega,codigo_cifrado"))return created?Collections.singletonList(new Object[]{50L,inserted[9],10L}):List.of();return List.of();});
  when(jdbc.query(anyString(),any(RowMapper.class))).thenAnswer(i->notifications);
  when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenAnswer(i->{inserted=((Object[])i.getRawArguments()[2]).clone();created=true;return 50L;});
  service=new CodigoRetiroService(jdbc,encoder,cipher,mail);
 }
 @Test void alDespacharCreaUnaVezCodigoCifradoYAviso(){service.asegurar(7);assertTrue(created);String plain=cipher.descifrar((UUID)inserted[0],(String)inserted[9]);assertTrue(plain.matches("[A-HJ-NP-Z2-9]{8}"));assertNotEquals(plain,inserted[9]);assertEquals(11L,inserted[3]);service.asegurar(7);verify(jdbc,times(1)).queryForObject(startsWith("INSERT INTO lamontana.codigo_entrega"),eq(Long.class),any(Object[].class));verify(jdbc,times(1)).update(startsWith("INSERT INTO lamontana.aviso_codigo_entrega"),eq(50L),eq("cliente@prueba.local"));}
 @Test void antesDelDespachoNoEmiteNiNotifica(){eligible=false;service.asegurar(7);assertFalse(created);verify(jdbc,never()).update(anyString(),any(Object[].class));}
 @Test void clavePersistenteRecuperaCodigoYRechazaCopiarloAOtraIdentidad()throws Exception{UUID id=UUID.randomUUID();String encrypted=cipher.cifrar(id,"ABCD2345");assertEquals("ABCD2345",new CodigoRetiroCifrado(files).descifrar(id,encrypted));assertThrows(IllegalStateException.class,()->cipher.descifrar(UUID.randomUUID(),encrypted));}
 @Test void falloDeCorreoSeReintentaSinCrearOtroCodigo(){service.asegurar(7);notifications=Collections.singletonList(new Object[]{50L,"cliente@prueba.local",inserted[0],inserted[9],7L,"Punto Palermo",null,null});doThrow(new CorreoService.CorreoNoDisponible()).when(mail).enviarAviso(anyString(),anyString(),anyString());service.avisar();verify(jdbc).update(contains("proximo_intento=clock_timestamp()"),eq(50L));verify(jdbc,never()).update(contains("estado='ENVIADO'"),anyLong());}
 @Test void avisoEnviadoIncluyeElMismoCodigoSinExigirCelular(){service.asegurar(7);notifications=Collections.singletonList(new Object[]{50L,"cliente@prueba.local",inserted[0],inserted[9],7L,"Punto Palermo",null,null});service.avisar();verify(mail).enviarAviso(eq("cliente@prueba.local"),contains("PED-7"),contains(cipher.descifrar((UUID)inserted[0],(String)inserted[9])));verify(jdbc).update(contains("estado='ENVIADO'"),eq(50L));}
}
