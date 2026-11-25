package ar.com.lamontana.configuracion;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class RecuperacionImpresorasTest {
 private final UUID destino=UUID.randomUUID(),origen=UUID.randomUUID();
 private final JdbcTemplate jdbc=mock(JdbcTemplate.class,call->call.getMethod().getName().equals("queryForObject")?(Arrays.asList(call.getArguments()).contains(origen)?1L:2L):RETURNS_DEFAULTS.answer(call));
 private final ConfiguracionService configuracion=mock(ConfiguracionService.class);
 private final CopiaParametrosConfiguracion copias=mock(CopiaParametrosConfiguracion.class);
 private final RecursosConfiguracionService service=new RecursosConfiguracionService(jdbc,configuracion,copias);
 private final ConfiguracionService.Borrador draft=mock(ConfiguracionService.Borrador.class),source=mock(ConfiguracionService.Borrador.class);
 private final RecursosRepositorio.Recursos resources=mock(RecursosRepositorio.Recursos.class),originals=mock(RecursosRepositorio.Recursos.class);
 private final RecursosConfiguracionController.RecuperarImpresoras input=new RecursosConfiguracionController.RecuperarImpresoras(UUID.randomUUID(),3L);
 private final String correo="propietario@example.test";
 private void preparar(){
  when(configuracion.propietario(correo)).thenReturn(7L);when(configuracion.huella(any())).thenReturn("huella");when(configuracion.cargar(destino)).thenReturn(draft);when(draft.estado()).thenReturn("EN_PREPARACION");when(draft.version()).thenReturn(3L);when(draft.modelo()).thenReturn(ConfiguracionController.Modelo.MANUAL);when(draft.recursos()).thenReturn(resources);when(resources.impresoras()).thenReturn(List.of());
  var activa=mock(ConfiguracionService.Version.class);when(configuracion.activa()).thenReturn(activa);when(activa.configuracion()).thenReturn(source);when(source.codigoPublico()).thenReturn(origen);when(source.numero()).thenReturn(1L);when(source.recursos()).thenReturn(originals);when(originals.impresoras()).thenReturn(List.of(mock(RecursosRepositorio.Impresora.class)));
 }
 @Test void recuperaSoloEquiposYRegistraLaEdicionSinCopiarPagosNiHorarios(){
  preparar();assertThat(service.recuperarVigentes(destino,input,correo)).isSameAs(draft);
  verify(copias).copiarImpresoras(1L,2L);verify(copias,never()).copiar(anyLong(),anyLong());
  verify(configuracion).registrar(eq(input.operacion()),eq("GUARDAR_RECURSOS"),eq(2L),eq(7L),eq("huella"),eq("RECURSOS_CONFIGURADOS"),eq(4L),eq(origen),contains("Impresoras recuperadas de V1"));
 }
 @Test void noReemplazaUnBorradorQueYaTieneEquipos(){
  preparar();when(resources.impresoras()).thenReturn(List.of(mock(RecursosRepositorio.Impresora.class)));
  assertThatThrownBy(()->service.recuperarVigentes(destino,input,correo)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("ya tiene impresoras");verifyNoInteractions(copias);
 }
 @Test void reintentoNoVuelveACopiarLosEquipos(){
  preparar();when(configuracion.reintento(input.operacion(),"GUARDAR_RECURSOS",destino,7L,"huella")).thenReturn(draft);
  assertThat(service.recuperarVigentes(destino,input,correo)).isSameAs(draft);verifyNoInteractions(copias);verify(configuracion,never()).activa();
 }
}
