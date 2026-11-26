package ar.com.lamontana.pruebas;

import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.caja.*;
import ar.com.lamontana.catalogo.EvaluadorPrecioItem;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.puntos.*;
import ar.com.lamontana.pagos.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/** Escenario opcional sobre tres pedidos nuevos, exclusivamente en la instalación de pruebas. */
@Configuration
@Profile("pruebas")
@ConditionalOnProperty(name="lamontana.pruebas.finanzas",havingValue="true")
class FinanzasPuntosPrueba {
    @Bean @Order(1)
    ApplicationRunner cargarFinanzasPuntos(JdbcTemplate db,TransactionTemplate tx,OfertaOperativaService oferta,EvaluadorPrecioItem precios,
            EvaluadorFinanciero reglas,ArchivosPrivados archivos,PasswordEncoder encoder,PuntoRetiroService puntos,FinanzasPunto finanzas,
            PagoService pagos,CajaService caja){
        return args->tx.executeWithoutResult(status->{
            for(long lock:new long[]{764003,764001,764002,764004,764099})db.execute("SELECT pg_advisory_xact_lock("+lock+")");
            db.execute("CREATE TABLE IF NOT EXISTS lamontana.escenario_finanzas_punto(version integer PRIMARY KEY,fecha timestamptz NOT NULL DEFAULT now())");
            if(db.update("INSERT INTO lamontana.escenario_finanzas_punto(version) VALUES (1) ON CONFLICT DO NOTHING")==0)return;
            String admin=DatosPruebaConfiguration.ADMIN,correo="punto.caballito.retiro@prueba.local";
            var punto=puntos.contexto(correo).punto();
            var fabrica=new PedidosPrueba(db,oferta,precios,reglas,archivos,encoder);
            BigDecimal recibidos=BigDecimal.ZERO;UUID sucursal=null;
            for(String destino:new String[]{"EFECTIVO","COBRO_PUNTO","COBRO_ORIGEN"}){
                var medio=destino.equals("EFECTIVO")?MedioPago.EFECTIVO:MedioPago.TRANSFERENCIA;
                var pedido=fabrica.pedidoFinanciero(punto,medio);
                var ficha=puntos.ficha(pedido,correo);
                sucursal=db.queryForObject("SELECT s.codigo_publico FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion) JOIN lamontana.sucursal s ON s.id_sucursal=c.id_sucursal WHERE p.codigo_publico=?",UUID.class,pedido);
                String monto=ficha.pedido().saldo(),referencia="SIMULACION-"+pedido;
                if(medio==MedioPago.EFECTIVO){
                    puntos.cobrar(pedido,new PuntoRetiroController.Cobro(UUID.randomUUID(),ficha.pedido().version(),medio,monto,referencia,true,PagoService.DestinoCobro.PUNTO_ENTREGA),correo);
                }else{
                    var uso=FinanzasPunto.Uso.valueOf(destino);
                    UUID cuenta=cuenta(finanzas,db,punto,uso==FinanzasPunto.Uso.COBRO_PUNTO?null:sucursal,uso,admin);
                    finanzas.aviso(pedido,new FinanzasPunto.Aviso(UUID.randomUUID(),ficha.pedido().version(),cuenta,monto,referencia),()->correo);
                    var quote=db.queryForObject("SELECT c.codigo_publico FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE p.codigo_publico=?",UUID.class,pedido);
                    var aviso=pagos.vista(quote,admin,true).intentos().stream().filter(i->i.referencia().equals(referencia)).findFirst().orElseThrow();
                    pagos.recibir(quote,new PagoController.Recibir(UUID.randomUUID(),aviso.codigoPublico(),medio,monto,referencia,null,true,"Simulación de acreditación para pruebas.",true),admin);
                }
                if(!destino.equals("COBRO_ORIGEN"))recibidos=recibidos.add(new BigDecimal(monto));
            }
            UUID cuenta=cuenta(finanzas,db,punto,sucursal,FinanzasPunto.Uso.RENDICION,admin);
            String parcial=recibidos.divide(BigDecimal.valueOf(4),2,java.math.RoundingMode.DOWN).toPlainString();
            for(String estado:new String[]{"RECHAZADA","CONFIRMADA","PENDIENTE"}){
                var op=UUID.randomUUID();
                finanzas.informar(new FinanzasPunto.Informe(op,cuenta,parcial,"SIMULACION-"+op,null,"Escenario de pruebas",null),()->correo);
                if(!estado.equals("PENDIENTE"))finanzas.resolver(sucursal,op,new FinanzasPunto.Decision(UUID.randomUUID(),estado,"Verificación de muestra"),()->admin);
            }
            caja.egreso(sucursal,admin,new CajaController.NuevoEgreso(UUID.randomUUID(),CajaService.Tipo.PAGO_PUNTO_ENTREGA,CajaService.Medio.TRANSFERENCIA,
                "80000.00","Responsable de muestra","Servicios de retiro simulados",null,punto,"SIMULACION-SERVICIOS-1"));
        });
    }
    private UUID cuenta(FinanzasPunto servicio,JdbcTemplate db,UUID punto,UUID sucursal,FinanzasPunto.Uso uso,String admin){
        var existentes=db.query("""
          SELECT i.operacion,i.habilitada FROM lamontana.instruccion_punto_actual i JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
          LEFT JOIN lamontana.sucursal s USING(id_sucursal) WHERE pt.codigo_publico=? AND s.codigo_publico IS NOT DISTINCT FROM ?
          AND i.uso=? AND i.medio='TRANSFERENCIA'
          """,(r,n)->{if(!r.getBoolean(2))throw new IllegalStateException("El escenario respeta la cuenta deshabilitada. Desactivá el fixture financiero opcional.");return r.getObject(1,UUID.class);},punto,sucursal,uso.name());
        if(!existentes.isEmpty())return existentes.get(0);
        var id=UUID.randomUUID();
        servicio.configurar(new FinanzasPunto.Config(id,punto,sucursal,uso,MedioPago.TRANSFERENCIA,"Cuenta simulada para pruebas · sin datos bancarios reales",true,false,true,null),()->admin);
        return id;
    }
}
