package ar.com.lamontana.cotizaciones;

import static ar.com.lamontana.cotizaciones.CotizacionController.*;
import ar.com.lamontana.catalogo.*;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CotizacionService {
    private final JdbcTemplate jdbc;
    private final OfertaOperativaService operativa;
    private final CatalogoService catalogo;
    private final EvaluadorPrecioItem precios;
    private final EvaluadorFinanciero finanzas;
    private final Clock clock=Clock.systemUTC();
    private final JsonMapper json=JsonMapper.builder().build();
    public CotizacionService(JdbcTemplate jdbc,OfertaOperativaService operativa,CatalogoService catalogo,EvaluadorPrecioItem precios,EvaluadorFinanciero finanzas){this.jdbc=jdbc;this.operativa=operativa;this.catalogo=catalogo;this.precios=precios;this.finanzas=finanzas;}
    public record Nombre(UUID codigoPublico,String nombre){}
    public record Punto(UUID codigoPublico,UUID sucursal,String nombre,String direccion,String costo){}
    public record Opciones(boolean disponible,String mensaje,UUID configuracion,UUID revisionComercial,List<OfertaOperativaService.Sucursal> sucursales,
        List<RevisionConfiguracionService.Opcion> combinaciones,List<Nombre> formatos,List<Nombre> papeles,List<Nombre> servicios,
        List<Modalidad> modalidades,List<Punto> puntos,List<MedioPago> medios){}
    public record ItemCotizado(UUID codigoPublico,Documento documento,EvaluadorPrecioItem.Item trabajo,String formato,String papel,EvaluadorPrecioItem.Precio precio){}
    public record Condiciones(boolean revisionHumana,boolean cargaRequiereAcreditacion,String pagoPrevioRequerido,String senaRequerida,String saldo,
        String momento,List<MedioPago> mediosAcreditacion,List<String> instrucciones){}
    public record Oferta(UUID configuracion,long numeroConfiguracion,UUID revisionComercial,long numeroRevision,OfertaOperativaService.Sucursal sucursal,
        List<ItemCotizado> items,Modalidad modalidad,UUID punto,Direccion direccion,String destino,MedioPago medioPago,
        String subtotal,String costoEntrega,String total,Condiciones condiciones,EvaluadorCalendario.Simulacion entrega){}
    public record Detalle(UUID codigoPublico,long numero,long version,String estado,Instant generadaEn,Instant vigenteHasta,Instant aceptadaEn,
        Instant canceladaEn,String motivoCancelacion,UUID reemplaza,UUID reemplazadaPor,Oferta oferta){}
    public record Resumen(UUID codigoPublico,long numero,String estado,Instant generadaEn,Instant vigenteHasta,Instant aceptadaEn,String total,String sucursal,int items){}
    public record Pagina(List<Resumen> elementos,long total,int pagina,int tamano){}
    private record Registro(long id,long actor,Detalle detalle){}
    private record HuellaDecision(UUID cotizacion,Decision decision,boolean aceptar){}

    @Transactional
    public Opciones opciones(String correo){
        cliente(correo);bloquear();catalogo.reconciliarProgramaciones();var c=operativa.leer();
        if(c.configuracion()==null||c.catalogo().actual()==null||c.opciones().isEmpty())return new Opciones(false,"La imprenta está en preparación o no tiene opciones disponibles para cotizar.",null,null,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());
        var b=c.configuracion();var formatos=new HashSet<UUID>();var papeles=new HashSet<UUID>();var servicios=new HashSet<UUID>();
        c.opciones().forEach(o->{formatos.add(o.formato());papeles.add(o.papel());servicios.add(o.servicio());servicios.addAll(o.terminaciones());});
        var puntos=c.puntos().stream().filter(p->c.sucursales().stream().anyMatch(s->s.codigoPublico().equals(p.sucursal()))).map(p->{var d=b.entrega().puntos().stream().filter(x->x.codigoPublico().equals(p.punto())).findFirst().orElseThrow();return new Punto(p.punto(),p.sucursal(),p.nombre(),d.calle()+" "+d.numero()+", "+d.localidad()+", "+d.provincia(),p.costo());}).toList();
        var modalidades=b.entrega().modalidades().stream().filter(m->m!=Modalidad.RETIRO_PUNTO_ENTREGA||!puntos.isEmpty()).filter(m->m!=Modalidad.ENVIO_DOMICILIO||b.entrega().zonas().stream().anyMatch(ZonasRepositorio.Zona::utilizable)).toList();
        return new Opciones(!modalidades.isEmpty(),"Los importes y las condiciones se calculan al cotizar. La cotización no reserva capacidad.",b.codigoPublico(),c.catalogo().actual().codigoPublico(),c.sucursales(),c.opciones(),
            c.catalogo().formatos().stream().filter(f->formatos.contains(f.codigoPublico())).map(f->new Nombre(f.codigoPublico(),f.nombre())).toList(),
            c.catalogo().papeles().stream().filter(p->papeles.contains(p.codigoPublico())).map(p->new Nombre(p.codigoPublico(),p.nombre())).toList(),
            c.catalogo().actual().servicios().stream().filter(s->servicios.contains(s.servicio())).map(s->new Nombre(s.servicio(),s.nombreVisible())).toList(),modalidades,puntos,b.pagos().medios());
    }

    @Transactional
    public Detalle crear(Crear in,String correo){
        bloquear();long actor=cliente(correo);String hash=huella(in);var anterior=reintento(in.operacion(),"COTIZAR",hash,actor);if(anterior!=null)return anterior;
        catalogo.reconciliarProgramaciones();var c=operativa.leer();var b=c.configuracion();
        if(b==null||c.catalogo().actual()==null)throw conflicto("La imprenta todavía no está preparada para cotizar.");
        if(!b.codigoPublico().equals(in.configuracion())||!c.catalogo().actual().codigoPublico().equals(in.revisionComercial()))throw conflicto("Cambiaron las opciones o tarifas. Actualizalas antes de solicitar la cotización.");
        var sucursal=c.sucursales().stream().filter(s->s.codigoPublico().equals(in.sucursal())).findFirst().orElseThrow(()->conflicto("La sucursal ya no está disponible para cotizar."));
        exigir(b.pagos().medios().contains(in.medioPago()),"El medio de pago no está habilitado.");
        exigir((in.modalidad()==Modalidad.ENVIO_DOMICILIO)==(in.direccion()!=null),"Completá la dirección sólo para envío a domicilio.");
        Long reemplazada=null;
        if(in.reemplaza()!=null){var vieja=propia(in.reemplaza(),actor);if(vieja.detalle().estado().equals("CONFIRMADA")||vieja.detalle().reemplazadaPor()!=null)throw conflicto("La cotización ya tiene un pedido o una oferta sucesora. Abrí el resultado guardado.");reemplazada=vieja.id();}
        var items=new ArrayList<ItemCotizado>();var hashes=new HashSet<String>();BigDecimal subtotal=BigDecimal.ZERO;int carillas=0,minutos=0;
        for(var item:in.items()){
            exigir(hashes.add(item.documento().sha256()),"Cada PDF debe aparecer una sola vez con una configuración homogénea. Para otra configuración usá una cotización diferente.");
            String nombre=item.documento().nombre().strip();exigir(nombre.toLowerCase(Locale.ROOT).endsWith(".pdf")&&nombre.codePoints().noneMatch(cp->Character.isISOControl(cp)||cp=='/'||cp=='\\'),"El nombre debe identificar un PDF sin rutas ni caracteres de control.");
            var t=item.trabajo();boolean compatible=c.opciones().stream().anyMatch(o->o.sucursal().equals(in.sucursal())&&o.servicio().equals(t.servicio())&&o.formato().equals(t.formato())&&o.papel().equals(t.papel())&&o.color()==t.color()&&(!t.dobleFaz()||o.dobleFaz())&&o.terminaciones().containsAll(t.terminaciones()));
            if(!compatible)throw conflicto("Uno de los trabajos no tiene tarifa, servicio y capacidad compatibles en esa sucursal.");
            var precio=precios.calcular(c.catalogo(),t);subtotal=subtotal.add(new BigDecimal(precio.subtotal()));carillas=Math.addExact(carillas,precio.carillas());minutos=Math.max(minutos,precio.minimoPreparacionMinutos());
            String formato=c.catalogo().formatos().stream().filter(x->x.codigoPublico().equals(t.formato())).findFirst().orElseThrow().nombre();
            String papel=c.catalogo().papeles().stream().filter(x->x.codigoPublico().equals(t.papel())).findFirst().orElseThrow().nombre();
            items.add(new ItemCotizado(UUID.randomUUID(),new Documento(nombre,item.documento().bytes(),item.documento().sha256()),t,formato,papel,precio));
        }
        Instant ahora=ahora();var temporal=operativa.calcularEntrega(c,new SimularEntrega(b.version(),in.sucursal(),in.modalidad(),ahora,in.punto(),in.direccion()==null?null:in.direccion().territorio()),minutos);
        String costo=temporal.destinoPunto()!=null?temporal.destinoPunto().costo():temporal.destinoZona()!=null?temporal.destinoZona().costo():"0.00";
        BigDecimal total=subtotal.add(new BigDecimal(costo));precios.limite(total);
        var economica=finanzas.evaluar(b.modelo(),b.criterio(),b.pagos(),b.version(),total,carillas);
        var instrucciones=economica.instrucciones().stream().filter(x->!x.startsWith("La simulación evalúa")).map(x->x.replace("Este ejemplo","Esta cotización").replace("este ejemplo","esta cotización")).toList();
        var condiciones=new Condiciones(economica.revisionHumana(),economica.cargaRequiereAcreditacion(),economica.pagoPrevioRequerido(),economica.senaRequerida(),economica.saldo(),economica.momento().name(),economica.mediosAcreditacion(),instrucciones);
        String destino=sucursal.direccion();if(in.punto()!=null){var punto=b.entrega().puntos().stream().filter(x->x.codigoPublico().equals(in.punto())).findFirst().orElseThrow();destino=punto.nombre()+" · "+punto.calle()+" "+punto.numero()+", "+punto.localidad()+", "+punto.provincia();}
        if(in.direccion()!=null)destino=in.direccion().calle().strip()+" "+in.direccion().numero().strip()+", "+in.direccion().territorio().localidad()+", "+in.direccion().territorio().provincia();
        var oferta=new Oferta(b.codigoPublico(),b.numero(),c.catalogo().actual().codigoPublico(),c.catalogo().actual().numero(),sucursal,List.copyOf(items),in.modalidad(),in.punto(),in.direccion(),destino,in.medioPago(),subtotal.toPlainString(),costo,total.toPlainString(),condiciones,temporal);
        UUID codigo=UUID.randomUUID();long id=jdbc.queryForObject("""
            INSERT INTO lamontana.cotizacion(codigo_publico,id_usuario_creador,id_sucursal,id_configuracion_version,id_catalogo_revision,id_cotizacion_reemplazada,estado,generada_en,vigente_hasta,subtotal,costo_entrega,total,oferta)
            VALUES (?,?,(SELECT id_sucursal FROM lamontana.sucursal WHERE codigo_publico=?),(SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?),(SELECT id_catalogo_revision FROM lamontana.catalogo_revision WHERE codigo_publico=?),?,'VIGENTE',?,?,?,?,?,?::jsonb) RETURNING id_cotizacion
            """,Long.class,codigo,actor,in.sucursal(),b.codigoPublico(),c.catalogo().actual().codigoPublico(),reemplazada,Timestamp.from(ahora),Timestamp.from(ahora.plusSeconds(b.pagos().vigenciaCotizacionMinutos()*60L)),subtotal,new BigDecimal(costo),total,json.writeValueAsString(oferta));
        int orden=0;for(var item:items){var t=item.trabajo();var p=item.precio();long idItem=jdbc.queryForObject("""
            INSERT INTO lamontana.cotizacion_item(codigo_publico,id_cotizacion,orden,id_formato,id_papel,cantidad_paginas_declaradas,cantidad_copias,cantidad_carillas,cantidad_hojas,modo_color,doble_faz,nombre_archivo_declarado,bytes_declarados,sha256_declarado,subtotal)
            VALUES (?,?,?,(SELECT id_formato FROM lamontana.formato WHERE codigo_publico=?),(SELECT id_papel FROM lamontana.papel WHERE codigo_publico=?),?,?,?,?,?,?,?,?,?,?) RETURNING id_cotizacion_item
            """,Long.class,item.codigoPublico(),id,++orden,t.formato(),t.papel(),t.paginas(),t.copias(),p.carillas(),p.hojas(),t.color().name(),t.dobleFaz(),item.documento().nombre(),item.documento().bytes(),item.documento().sha256(),new BigDecimal(p.subtotal()));
            for(var linea:p.lineas())jdbc.update("INSERT INTO lamontana.cotizacion_item_servicio(id_cotizacion_item,id_servicio,tipo_servicio,nombre_visible,base_precio,cantidad,precio_unitario,subtotal) VALUES (?,(SELECT id_servicio FROM lamontana.servicio WHERE codigo_publico=?),?,?,?,?,?,?)",idItem,linea.servicio(),linea.servicio().equals(t.servicio())?"IMPRESION":"TERMINACION",linea.nombre(),linea.base(),linea.unidades(),new BigDecimal(linea.precioUnitario()),new BigDecimal(linea.importe()));
        }
        if(reemplazada!=null)jdbc.update("UPDATE lamontana.cotizacion SET estado='CANCELADA',version=version+1,cancelada_en=?,motivo_cancelacion='Sustituida por una nueva cotización solicitada por el cliente.' WHERE id_cotizacion=? AND estado='VIGENTE' AND vigente_hasta>?",Timestamp.from(ahora),reemplazada,Timestamp.from(ahora));
        evento(id,actor,in.operacion(),"COTIZAR",hash,null);return propia(codigo,actor).detalle();
    }

    @Transactional
    public Detalle decidir(UUID id,Decision in,boolean aceptar,String correo){
        bloquear();long actor=cliente(correo);String tipo=aceptar?"ACEPTAR_OFERTA":"CANCELAR_OFERTA",hash=huella(new HuellaDecision(id,in,aceptar));
        var anterior=reintento(in.operacion(),tipo,hash,actor);if(anterior!=null)return anterior;
        var r=propia(id,actor);var d=r.detalle();if(d.version()!=in.version())throw conflicto("La cotización cambió. Consultá su estado antes de continuar.");
        if(!d.estado().equals("VIGENTE"))throw conflicto("La cotización ya no está vigente. Solicitá una nueva oferta para continuar.");
        if(aceptar&&d.aceptadaEn()!=null)throw conflicto("La oferta ya fue aceptada. Consultá su estado guardado.");
        if(!aceptar)exigir(in.motivo()!=null&&!in.motivo().isBlank(),"Indicá el motivo de cancelación.");
        Instant decisionEn=ahora();if(!decisionEn.isBefore(d.vigenteHasta()))throw conflicto("La cotización acaba de vencer. Solicitá una nueva oferta.");
        if(aceptar)jdbc.update("UPDATE lamontana.cotizacion SET aceptada_en=?,version=version+1 WHERE id_cotizacion=?",Timestamp.from(decisionEn),r.id());
        else jdbc.update("UPDATE lamontana.cotizacion SET estado='CANCELADA',cancelada_en=?,motivo_cancelacion=?,version=version+1 WHERE id_cotizacion=?",Timestamp.from(decisionEn),in.motivo().strip(),r.id());
        evento(r.id(),actor,in.operacion(),tipo,hash,aceptar?null:in.motivo().strip());return propia(id,actor).detalle();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Detalle detalle(UUID id,String correo){return propia(id,cliente(correo)).detalle();}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Pagina listar(int pagina,String correo){
        exigir(pagina>=0&&pagina<=100000,"Página inválida.");long actor=cliente(correo);long total=jdbc.queryForObject("SELECT count(*) FROM lamontana.cotizacion WHERE id_usuario_creador=?",Long.class,actor);
        var ids=jdbc.query("SELECT codigo_publico FROM lamontana.cotizacion WHERE id_usuario_creador=? ORDER BY id_cotizacion DESC LIMIT 25 OFFSET ?",(r,n)->r.getObject(1,UUID.class),actor,(long)pagina*25);
        var resumen=ids.stream().map(id->{var d=propia(id,actor).detalle();return new Resumen(id,d.numero(),d.estado(),d.generadaEn(),d.vigenteHasta(),d.aceptadaEn(),d.oferta().total(),d.oferta().sucursal().nombre(),d.oferta().items().size());}).toList();
        return new Pagina(resumen,total,pagina,25);
    }
    private Registro propia(UUID id,long actor){
        var rows=jdbc.query("SELECT c.*,p.codigo_publico AS reemplaza,n.codigo_publico AS reemplazada_por FROM lamontana.cotizacion c LEFT JOIN lamontana.cotizacion p ON p.id_cotizacion=c.id_cotizacion_reemplazada LEFT JOIN lamontana.cotizacion n ON n.id_cotizacion_reemplazada=c.id_cotizacion WHERE c.codigo_publico=? AND c.id_usuario_creador=?",(r,n)->{
            Instant vencimiento=r.getTimestamp("vigente_hasta").toInstant();String estado=r.getString("estado");if(estado.equals("VIGENTE")&&!ahora().isBefore(vencimiento))estado="EXPIRADA";
            var d=new Detalle(r.getObject("codigo_publico",UUID.class),r.getLong("id_cotizacion"),r.getLong("version"),estado,r.getTimestamp("generada_en").toInstant(),vencimiento,instante(r.getTimestamp("aceptada_en")),instante(r.getTimestamp("cancelada_en")),r.getString("motivo_cancelacion"),r.getObject("reemplaza",UUID.class),r.getObject("reemplazada_por",UUID.class),json.readValue(r.getString("oferta"),Oferta.class));
            return new Registro(r.getLong("id_cotizacion"),r.getLong("id_usuario_creador"),d);
        },id,actor);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"La cotización no está disponible en tu cuenta.");return rows.get(0);
    }
    private Detalle reintento(UUID operacion,String tipo,String hash,long actor){
        var rows=jdbc.query("SELECT e.tipo,e.huella,e.id_actor,c.codigo_publico FROM lamontana.evento_cotizacion e JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE e.id_operacion=?",(r,n)->new Object[]{r.getString(1),r.getString(2),r.getLong(3),r.getObject(4,UUID.class)},operacion);
        if(rows.isEmpty())return null;var r=rows.get(0);if(!tipo.equals(r[0])||!hash.equals(r[1])||actor!=(long)r[2])throw conflicto("Esa operación ya se utilizó con otros datos.");return propia((UUID)r[3],actor).detalle();
    }
    private long cliente(String correo){var ids=jdbc.query("SELECT u.id_usuario FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.codigo='CLIENTE' AND r.activo",(r,n)->r.getLong(1),correo);if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Se requiere una cuenta activa de cliente particular.");return ids.get(0);}
    private void bloquear(){jdbc.execute("SELECT pg_advisory_xact_lock(764003)");jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");}
    private void evento(long id,long actor,UUID operacion,String tipo,String hash,String motivo){jdbc.update("INSERT INTO lamontana.evento_cotizacion(id_cotizacion,id_actor,id_operacion,tipo,huella,motivo) VALUES (?,?,?,?,?,?)",id,actor,operacion,tipo,hash,motivo);}
    private Instant ahora(){return clock.instant();}
    private Instant instante(Timestamp v){return v==null?null:v.toInstant();}
    private String huella(Object v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(v).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
    private void exigir(boolean valido,String mensaje){if(!valido)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
    private ResponseStatusException conflicto(String mensaje){return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);}
}
