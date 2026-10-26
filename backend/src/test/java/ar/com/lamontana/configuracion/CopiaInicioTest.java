package ar.com.lamontana.configuracion;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Prueba el inicio normal y sus reintentos sin depender de PostgreSQL embebido. */
class CopiaInicioTest {
 private final UUID origen=UUID.randomUUID();
 private final JdbcTemplate jdbc=mock(JdbcTemplate.class,call->{
  if(call.getMethod().getName().equals("queryForObject")){
   String sql=(String)call.getArgument(0);
   if(sql.contains("SELECT EXISTS"))return false;
   return sql.contains("INSERT INTO")?2L:1L;
  }
  return RETURNS_DEFAULTS.answer(call);
 });
 private final ConfiguracionService configuracion=mock(ConfiguracionService.class);
 private final RevisionConfiguracionService revision=mock(RevisionConfiguracionService.class);
 private final CopiaParametrosConfiguracion parametros=mock(CopiaParametrosConfiguracion.class);
 private final CopiaConfiguracionService service=new CopiaConfiguracionService(jdbc,configuracion,revision,parametros);
 private final ConfiguracionController.CrearBorrador input=new ConfiguracionController.CrearBorrador(UUID.randomUUID());
 private final String correo="propietario@example.test";
 private void identidad(){when(configuracion.propietario(correo)).thenReturn(7L);when(configuracion.huella(input)).thenReturn("huella");}

 @Test void primeraInstalacionConservaElInicioVacio(){
  identidad();var draft=mock(ConfiguracionService.Borrador.class);when(configuracion.crear(input,correo)).thenReturn(draft);
  assertThat(service.iniciar(input,correo)).isSameAs(draft);verify(configuracion).crear(input,correo);verifyNoInteractions(parametros,revision);
 }
 @Test void inicioConVigenteReutilizaLaCopiaCompletaYLasIdentidades(){
  identidad();var source=mock(ConfiguracionService.Borrador.class);var active=mock(ConfiguracionService.Version.class);var draft=mock(ConfiguracionService.Borrador.class);
  when(configuracion.activa()).thenReturn(active);when(active.configuracion()).thenReturn(source);when(source.codigoPublico()).thenReturn(origen);when(source.estado()).thenReturn("ACTIVA");when(source.modelo()).thenReturn(ConfiguracionController.Modelo.MANUAL);when(source.numero()).thenReturn(1L);
  when(configuracion.cargar(any(UUID.class))).thenReturn(draft);when(configuracion.cargar(origen)).thenReturn(source);
  var review=mock(RevisionConfiguracionService.Revision.class);when(revision.evaluar(any(UUID.class),eq(correo),eq(false))).thenReturn(review);when(review.hallazgos()).thenReturn(List.of());
  assertThat(service.iniciar(input,correo)).isSameAs(draft);verify(parametros).copiar(1L,2L);verify(configuracion,never()).crear(any(),anyString());
  verify(configuracion).registrar(eq(input.operacion()),eq("CREAR_BORRADOR"),eq(2L),eq(7L),eq("huella"),eq("BORRADOR_DESDE_BASE"),eq(2L),eq(origen),contains("V1"));
 }
 @Test void reintentoRecuperaElMismoBorradorAntesDeConsultarLaVigente(){
  identidad();var draft=mock(ConfiguracionService.Borrador.class);when(configuracion.reintento(input.operacion(),"CREAR_BORRADOR",null,7L,"huella")).thenReturn(draft);
  assertThat(service.iniciar(input,correo)).isSameAs(draft);verify(configuracion,never()).activa();verifyNoInteractions(parametros,revision);
 }
 @Test void unActorNoAutorizadoNoPuedeCrearNiCopiar(){
  when(configuracion.propietario(correo)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
  assertThatThrownBy(()->service.iniciar(input,correo)).isInstanceOf(ResponseStatusException.class);verifyNoInteractions(parametros,revision);verify(configuracion,never()).activa();
 }
}
