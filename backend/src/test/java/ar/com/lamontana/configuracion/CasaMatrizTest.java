package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.EntregaConfiguracionController.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class CasaMatrizTest {
    private final UUID codigo=UUID.randomUUID(),matriz=UUID.randomUUID(),destino=UUID.randomUUID();
    private final ConfiguracionService configuracion=mock(ConfiguracionService.class);
    private final ConfiguracionService.Borrador b=mock(ConfiguracionService.Borrador.class);
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class,call->{
        if(call.getMethod().getName().equals("queryForObject"))return call.getArgument(1)==Boolean.class?Boolean.TRUE:1L;
        if(call.getMethod().getName().equals("query")&&call.getArgument(0).toString().contains("FROM lamontana.sucursal")){
            @SuppressWarnings("unchecked") RowMapper<Object> mapper=call.getArgument(1);
            var result=new ArrayList<Object>();
            for(var entry:List.of(matriz,destino)){
                var row=mock(ResultSet.class);when(row.getLong("id_sucursal")).thenReturn(entry.equals(matriz)?1L:2L);
                when(row.getObject("codigo_publico",UUID.class)).thenReturn(entry);
                when(row.getString("nombre")).thenReturn(entry.equals(matriz)?"Matriz":"Retiro");
                when(row.getString("zona_horaria")).thenReturn("UTC");
                result.add(mapper.mapRow(row,result.size()));
            }
            return result;
        }
        return RETURNS_DEFAULTS.answer(call);
    });
    private final EntregaConfiguracionService service=new EntregaConfiguracionService(jdbc,configuracion,new EvaluadorCalendario(),new EvaluadorFranjas());
    private List<Dia> semana(boolean abierta){return IntStream.rangeClosed(1,7).mapToObj(d->new Dia(d,abierta&&d<=5,abierta&&d<=5?"09:00":null,abierta&&d<=5?"17:00":null)).toList();}
    private EntregaRepositorio.Entrega datos(UUID principal,boolean confirmada,String traslado){
        var retiro=new PuntoEntregaController.Franja(1,"19:00","20:00",10,true);
        return new EntregaRepositorio.Entrega("2",traslado,List.of(Modalidad.RETIRO_SUCURSAL),List.of(
            new EntregaRepositorio.Horario(matriz,"UTC",semana(true),List.of(new PuntoEntregaController.Franja(1,"09:00","17:00",10,true))),
            new EntregaRepositorio.Horario(destino,"UTC",semana(false),List.of(retiro))),List.of(),List.of(),principal,confirmada);
    }
    private void preparar(EntregaRepositorio.Entrega entrega,String estado){
        when(b.codigoPublico()).thenReturn(codigo);when(b.estado()).thenReturn(estado);when(b.version()).thenReturn(3L);
        when(b.modelo()).thenReturn(ConfiguracionController.Modelo.MANUAL);when(b.entrega()).thenReturn(entrega);
        when(configuracion.cargar(codigo)).thenReturn(b);
    }
    private SimularEntrega ejemplo(UUID sucursal){return new SimularEntrega(3L,sucursal,Modalidad.RETIRO_SUCURSAL,Instant.parse("2026-10-26T09:00:00Z"),null,null);}

    @Test void ningunBorradorEsActivableSinElegirMatriz(){
        preparar(datos(null,false,"1"),"EN_PREPARACION");
        var v=service.validar(codigo,"admin");assertThat(v.valida()).isFalse();
        assertThat(v.problemas()).extracting(EntregaConfiguracionService.Problema::codigo).contains("MATRIZ_PENDIENTE","MATRIZ_SIN_CONFIRMAR");
        assertThatThrownBy(()->service.calcular(b,ejemplo(matriz),0)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("confirmá");
    }
    @Test void matrizHeredadaSinConfirmarBloqueaBorradorYProgramacion(){
        preparar(datos(matriz,false,"1"),"EN_PREPARACION");
        assertThat(service.validar(codigo,"admin").problemas()).extracting(EntregaConfiguracionService.Problema::codigo).contains("MATRIZ_SIN_CONFIRMAR");
        when(b.estado()).thenReturn("PROGRAMADA");
        assertThat(service.evaluar(codigo,"admin",true).valida()).isFalse();
    }
    @Test void destinoSoloRetiraPuedeEstarCerradoParaProduccion(){
        preparar(datos(matriz,true,"1"),"EN_PREPARACION");var v=service.validar(codigo,"admin");
        assertThat(v.valida()).as(v.problemas().toString()).isTrue();
        assertThat(v.avisos()).anyMatch(a->a.contains("producción cerrada"));
    }
    @Test void retiroEnMatrizNoSumaTraslado(){
        preparar(datos(matriz,true,"5"),"EN_PREPARACION");var r=service.calcular(b,ejemplo(matriz),0);
        assertThat(r.finPreparacion()).isEqualTo(Instant.parse("2026-10-26T11:00:00Z"));
        assertThat(r.llegadaEstimada()).isNull();assertThat(r.disponibleDesde()).isEqualTo(r.finPreparacion());
    }
    @Test void otraSucursalSumaTrasladoDesdeCalendarioMatrizYAjustaRetiro(){
        preparar(datos(matriz,true,"1"),"EN_PREPARACION");var r=service.calcular(b,ejemplo(destino),0);
        assertThat(r.finPreparacion()).isEqualTo(Instant.parse("2026-10-26T11:00:00Z"));
        assertThat(r.llegadaEstimada()).isEqualTo(Instant.parse("2026-10-26T12:00:00Z"));
        assertThat(r.disponibleDesde()).isEqualTo(Instant.parse("2026-10-26T19:00:00Z"));
        assertThat(r.destinoSucursal().sucursal()).isEqualTo(destino);
    }
    @Test void aunConTrasladoCeroElRetiroExternoRequiereLlegadaReal(){
        preparar(datos(matriz,true,"0"),"EN_PREPARACION");var r=service.calcular(b,ejemplo(destino),0);
        assertThat(r.llegadaEstimada()).isEqualTo(r.finPreparacion());assertThat(r.llegadaEstimada()).isNotNull();
    }
    @Test void trasladoNoDefinidoBloqueaRetiroEnOtraSucursal(){
        preparar(datos(matriz,true,null),"EN_PREPARACION");
        assertThat(service.validar(codigo,"admin").problemas()).extracting(EntregaConfiguracionService.Problema::codigo).contains("TRASLADO_PENDIENTE");
        assertThatThrownBy(()->service.calcular(b,ejemplo(destino),0)).hasMessageContaining("tiempos estimados");
    }
    @Test void matrizInactivaNoEsCompatible(){
        preparar(datos(UUID.randomUUID(),true,"1"),"EN_PREPARACION");
        assertThat(service.validar(codigo,"admin").problemas()).extracting(EntregaConfiguracionService.Problema::codigo).contains("MATRIZ_INACTIVA");
        assertThatThrownBy(()->service.calcular(b,ejemplo(destino),0)).hasMessageContaining("permanecer activa");
    }
    @Test void versionLegadaNoRecibeUnaMatrizArbitraria(){
        preparar(datos(null,false,"1"),"ACTIVA");var r=service.calcular(b,ejemplo(matriz),0);
        assertThat(r.llegadaEstimada()).isNull();
        assertThat(r.finPreparacion()).isEqualTo(Instant.parse("2026-10-26T11:00:00Z"));
    }
    @Test void copiaDeParametrosHeredaMatrizPeroNuncaConfirmacion(){
        var statements=new ArrayList<String>();var db=mock(JdbcTemplate.class,call->{if(call.getMethod().getName().equals("update"))statements.add(call.getArgument(0));return RETURNS_DEFAULTS.answer(call);});
        new CopiaParametrosConfiguracion(db).copiar(1L,2L);
        assertThat(statements).anyMatch(s->s.contains("configuracion_entrega")&&s.contains("id_sucursal_matriz"));
        assertThat(statements).noneMatch(s->s.contains("matriz_confirmada"));
    }
    @Test void clasificacionNuncaUsaUbicacionDeImpresora(){
        assertThat(CasaMatriz.requiereTraslado(matriz,matriz,Modalidad.RETIRO_SUCURSAL)).isFalse();
        assertThat(CasaMatriz.requiereTraslado(matriz,destino,Modalidad.RETIRO_SUCURSAL)).isTrue();
        assertThat(CasaMatriz.requiereTraslado(matriz,matriz,Modalidad.ENVIO_DOMICILIO)).isTrue();
        assertThat(CasaMatriz.requiereTraslado(matriz,matriz,Modalidad.RETIRO_PUNTO_ENTREGA)).isTrue();
    }
}
