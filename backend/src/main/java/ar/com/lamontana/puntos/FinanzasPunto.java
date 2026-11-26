package ar.com.lamontana.puntos;

import ar.com.lamontana.organizacion.OrganizacionService;
import ar.com.lamontana.pagos.PagoController;
import ar.com.lamontana.pagos.PagoService;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@RestController
@Transactional
public class FinanzasPunto {
    private final JdbcTemplate jdbc;
    private final PuntoRetiroService puntos;
    private final OrganizacionService organizacion;
    private final PagoService pagos;
    private static final String IMPORTE="[0-9]{1,17}(\\.[0-9]{1,2})?";
    public FinanzasPunto(JdbcTemplate jdbc,PuntoRetiroService puntos,OrganizacionService organizacion,PagoService pagos){
        this.jdbc=jdbc;this.puntos=puntos;this.organizacion=organizacion;this.pagos=pagos;
    }
    public enum Uso { COBRO_PUNTO, COBRO_ORIGEN, RENDICION }
    public record Cuenta(UUID cuenta,String uso,String medio,String nombre,String instrucciones,boolean verificaPunto){}
    public record Config(@NotNull UUID operacion,@NotNull UUID punto,UUID sucursal,@NotNull Uso uso,@NotNull MedioPago medio,
        @Size(max=500) @NotNull String instrucciones,boolean habilitada,boolean verificaPunto,Boolean verificaAdmin,UUID anterior){}
    public record Aviso(@NotNull UUID operacion,@Positive long version,@NotNull UUID cuenta,@NotNull @Pattern(regexp=IMPORTE) String importe,
        @NotBlank @Size(max=160) String referencia){}
    public record Verificar(@NotNull UUID operacion,@NotNull UUID cotizacion,@NotNull UUID aviso){}
    public record Informe(@NotNull UUID operacion,@NotNull UUID cuenta,@NotNull @Pattern(regexp=IMPORTE) String importe,
        @Size(max=160) String referencia,@Size(max=140) String receptor,@Size(max=300) String observacion,@Size(min=1,max=2097152) byte[] comprobante){}
    public record Decision(@NotNull UUID operacion,@NotNull @Pattern(regexp="CONFIRMADA|RECHAZADA") String estado,@Size(max=300) String motivo){}
    public record Saldo(String recibido,String confirmado,String pendiente,String reservado,String disponible){}
    public record Rendiciones(Saldo saldo,List<Map<String,Object>> rendiciones,List<Cuenta> cuentas){}
    private void bloquear(){jdbc.execute("SELECT pg_advisory_xact_lock(764004)");}
    private static ResponseStatusException conflicto(String msg){return new ResponseStatusException(HttpStatus.CONFLICT,msg);}
    private static ResponseStatusException prohibido(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"Tu cuenta no tiene autorización para esta operación.");}
    private static boolean vacio(String s){return s==null||s.isBlank();}
    static List<Cuenta> cuentas(JdbcTemplate jdbc,long punto,UUID origen,boolean rendir){
        return jdbc.query("""
          SELECT i.*,coalesce(s.nombre,pt.codigo) nombre FROM lamontana.instruccion_punto_actual i
          JOIN lamontana.punto_entrega pt USING(id_punto_entrega) LEFT JOIN lamontana.sucursal s USING(id_sucursal)
          WHERE i.id_punto_entrega=? AND i.habilitada AND (s.id_sucursal IS NULL OR s.estado='ACTIVA') AND
          """+(rendir?" i.uso='RENDICION'":" (i.uso='COBRO_PUNTO' OR i.uso='COBRO_ORIGEN' AND s.codigo_publico=?)")+" ORDER BY nombre,medio",
          (r,n)->new Cuenta(r.getObject("operacion",UUID.class),r.getString("uso"),r.getString("medio"),r.getString("nombre"),r.getString("instrucciones"),r.getBoolean("verifica_punto")),
          rendir?new Object[]{punto}:new Object[]{punto,origen});
    }
    @PostMapping("/api/punto/pedidos/{pedido}/avisos")
    public PuntoRetiroService.Ficha aviso(@PathVariable UUID pedido,@Valid @RequestBody Aviso in,Principal p){
        var a=puntos.acceso(p.getName());
        if(!a.cobros())throw prohibido();
        CuentaPuntoAcceso.pedido(jdbc,p.getName(),pedido);
        UUID quote=jdbc.queryForObject("SELECT c.codigo_publico FROM lamontana.pedido_actual p JOIN lamontana.cotizacion c USING(id_cotizacion) WHERE p.codigo_publico=?",UUID.class,pedido);
        pagos.informarDesdePunto(quote,pedido,in.version(),in.cuenta(),new PagoController.Informar(in.operacion(),in.importe(),in.referencia(),"Transferencia informada en el punto de entrega."),p.getName());
        return puntos.ficha(pedido,p.getName());
    }
    @PostMapping("/api/punto/finanzas/verificar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificar(@RequestBody @Valid Verificar in,Principal p){
        var a=puntos.acceso(p.getName());
        if(!a.cobros())throw prohibido();
        var datos=jdbc.query("""
          SELECT i.importe_informado,i.referencia_informada FROM lamontana.intento_pago i JOIN lamontana.aviso_punto av USING(id_intento_pago)
          JOIN lamontana.instruccion_punto ins ON ins.operacion=av.instruccion JOIN lamontana.cotizacion c USING(id_cotizacion)
          WHERE ins.id_punto_entrega=? AND ins.uso='COBRO_PUNTO' AND i.codigo_publico=? AND c.codigo_publico=?
          """,(r,n)->List.of(r.getBigDecimal(1).toPlainString(),r.getString(2)),a.punto(),in.aviso(),in.cotizacion()).stream().findFirst().orElseThrow(FinanzasPunto::prohibido);
        pagos.recibir(in.cotizacion(),new PagoController.Recibir(in.operacion(),in.aviso(),MedioPago.TRANSFERENCIA,datos.get(0),datos.get(1),null,true,"Acreditación verificada en la cuenta del punto.",true),p.getName());
    }
    private Saldo saldo(long punto){
        return jdbc.queryForObject("SELECT *,pendiente-reservado disponible FROM lamontana.saldo_punto WHERE id_punto_entrega=?",
          (r,n)->new Saldo(r.getString("recibido"),r.getString("confirmado"),r.getString("pendiente"),r.getString("reservado"),r.getString("disponible")),punto);
    }
    static final String RENDICIONES="""
       SELECT r.operacion codigo,pt.codigo_publico punto,pt.codigo nombre,s.nombre destinatario,s.codigo_publico sucursal,
        i.medio,r.importe::text importe,r.fecha,r.referencia,r.receptor,r.observacion,r.comprobante IS NOT NULL adjunto,
        coalesce(x.estado,'PENDIENTE') estado,x.motivo,x.fecha resuelta,u.nombre||' '||u.apellido responsable,
        v.nombre||' '||v.apellido verificador FROM lamontana.rendicion_punto r JOIN lamontana.instruccion_punto i ON i.operacion=r.instruccion
        JOIN lamontana.sucursal s USING(id_sucursal) JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
        JOIN lamontana.usuario u ON u.id_usuario=r.id_actor LEFT JOIN lamontana.resolucion_rendicion x ON x.rendicion=r.operacion
        LEFT JOIN lamontana.usuario v ON v.id_usuario=x.id_actor
       """;
    @GetMapping("/api/punto/rendiciones")
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Rendiciones rendiciones(Principal p){
        var a=puntos.acceso(p.getName());
        return new Rendiciones(saldo(a.punto()),jdbc.queryForList(RENDICIONES+" WHERE i.id_punto_entrega=? ORDER BY r.fecha DESC,r.operacion LIMIT 25",a.punto()),cuentas(jdbc,a.punto(),null,true));
    }
    private String huella(Object in){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JsonMapper.builder().build().writeValueAsBytes(in)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    @PostMapping("/api/punto/rendiciones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void informar(@Valid @RequestBody Informe in,Principal p){
        bloquear();
        var a=puntos.acceso(p.getName());
        registrarInforme(a.punto(),a.usuario(),null,in);
    }
    @PostMapping("/api/operacion/sucursales/{sucursal}/rendiciones-puntos/informes/{punto}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void informarPorPunto(@PathVariable UUID sucursal,@PathVariable UUID punto,@RequestBody @Valid Informe in,Principal p){
        bloquear();
        if(CuentaPuntoAcceso.punto(jdbc,p.getName())!=null)throw prohibido();
        organizacion.sucursalAutorizada(p.getName(),sucursal);
        organizacion.exigirPermiso(p.getName(),"INFORMAR_RENDICION_PUNTO");
        var actor=jdbc.queryForObject("SELECT id_usuario FROM lamontana.usuario WHERE correo=?",Long.class,p.getName());
        var id=jdbc.query("SELECT id_punto_entrega FROM lamontana.punto_entrega WHERE codigo_publico=?",(r,n)->r.getLong(1),punto).stream().findFirst().orElseThrow(FinanzasPunto::prohibido);
        registrarInforme(id,actor,sucursal,in);
    }
    @GetMapping("/api/operacion/sucursales/{sucursal}/rendiciones-puntos/cuentas")
    public List<Map<String,Object>> acuerdos(@PathVariable UUID sucursal,Principal p){
        if(CuentaPuntoAcceso.punto(jdbc,p.getName())!=null)throw prohibido();
        organizacion.sucursalAutorizada(p.getName(),sucursal);
        organizacion.exigirPermiso(p.getName(),"INFORMAR_RENDICION_PUNTO");
        return jdbc.queryForList("""
            SELECT i.operacion cuenta,pt.codigo_publico punto,pt.codigo nombre,i.medio,i.instrucciones,
             (saldo.pendiente-saldo.reservado)::text disponible FROM lamontana.instruccion_punto_actual i
             JOIN lamontana.sucursal s USING(id_sucursal) JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
             JOIN lamontana.saldo_punto saldo USING(id_punto_entrega)
             WHERE i.uso='RENDICION' AND i.habilitada AND s.codigo_publico=? ORDER BY pt.codigo,i.medio
            """,sucursal);
    }
    private void registrarInforme(long punto,long actor,UUID sucursal,Informe in){
        String hash=huella(in);
        var previa=jdbc.query("SELECT r.huella,r.id_actor,i.id_punto_entrega,s.codigo_publico FROM lamontana.rendicion_punto r JOIN lamontana.instruccion_punto i ON i.operacion=r.instruccion JOIN lamontana.sucursal s USING(id_sucursal) WHERE r.operacion=?",
            (r,n)->r.getString(1).equals(hash)&&r.getLong(2)==actor&&r.getLong(3)==punto&&(sucursal==null||sucursal.equals(r.getObject(4))),in.operacion());
        if(!previa.isEmpty()){if(!previa.get(0))throw conflicto("La operación tiene otros datos.");return;}
        var cuenta=cuentas(jdbc,punto,null,true).stream().filter(c->c.cuenta().equals(in.cuenta())).findFirst().orElseThrow(FinanzasPunto::prohibido);
        if(sucursal!=null&&!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.instruccion_punto i JOIN lamontana.sucursal s USING(id_sucursal) WHERE i.operacion=? AND s.codigo_publico=?)",Boolean.class,in.cuenta(),sucursal)))throw prohibido();
        if(cuenta.medio().equals("EFECTIVO")?vacio(in.receptor()):vacio(in.referencia()))throw conflicto("Falta la persona receptora o la referencia de transferencia.");
        if(cuenta.medio().equals("TRANSFERENCIA")&&Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM lamontana.rendicion_punto r JOIN lamontana.instruccion_punto i ON i.operacion=r.instruccion
              JOIN lamontana.instruccion_punto nueva ON nueva.operacion=? AND nueva.id_sucursal=i.id_sucursal
              LEFT JOIN lamontana.resolucion_rendicion x ON x.rendicion=r.operacion
              WHERE i.id_punto_entrega=? AND i.medio='TRANSFERENCIA' AND x.estado IS DISTINCT FROM 'RECHAZADA'
              AND lower(trim(r.referencia))=lower(trim(?)))
            """,Boolean.class,in.cuenta(),punto,in.referencia())))throw conflicto("La transferencia ya figura en una rendición.");
        var monto=new BigDecimal(in.importe());
        if(monto.signum()<=0||monto.compareTo(new BigDecimal(saldo(punto).disponible()))>0)throw conflicto("El importe excede los fondos disponibles o es cero.");
        jdbc.update("INSERT INTO lamontana.rendicion_punto(operacion,instruccion,importe,referencia,receptor,observacion,comprobante,id_actor,huella) VALUES (?,?,?,?,?,?,?,?,?)",
            in.operacion(),in.cuenta(),monto,in.referencia(),in.receptor(),in.observacion(),in.comprobante(),actor,hash);
        jdbc.update("""
            WITH consumido AS (SELECT id_evento,sum(a.importe) importe FROM lamontana.imputacion_rendicion a
                LEFT JOIN lamontana.resolucion_rendicion x USING(rendicion) WHERE x.estado IS DISTINCT FROM 'RECHAZADA' GROUP BY id_evento),
            fondos AS (SELECT m.id_evento,m.fecha,m.importe-coalesce(c.importe,0) libre
                FROM lamontana.movimiento_punto m LEFT JOIN consumido c USING(id_evento)
                WHERE m.id_punto_entrega=? AND m.tipo='COBRO' AND m.destino='PUNTO_ENTREGA'),
            orden AS (SELECT *,coalesce(sum(libre) OVER(ORDER BY fecha,id_evento ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING),0) anterior
                FROM fondos WHERE libre>0)
            INSERT INTO lamontana.imputacion_rendicion SELECT ?,id_evento,least(libre,?-anterior) FROM orden WHERE anterior<?
            """,punto,in.operacion(),monto,monto);
    }
    private void alcance(String actor,UUID sucursal){
        if(CuentaPuntoAcceso.punto(jdbc,actor)!=null)throw prohibido();
        organizacion.exigirPermiso(actor,"CONFIRMAR_RENDICION_PUNTO");
        organizacion.sucursalAutorizada(actor,sucursal);
    }
    @GetMapping("/api/operacion/sucursales/{sucursal}/rendiciones-puntos")
    public List<Map<String,Object>> bandeja(@PathVariable UUID sucursal,Principal p){
        alcance(p.getName(),sucursal);
        return jdbc.queryForList(RENDICIONES+" WHERE s.codigo_publico=? AND x.estado IS NULL ORDER BY r.fecha",sucursal);
    }
    @PostMapping("/api/operacion/sucursales/{sucursal}/rendiciones-puntos/{rendicion}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolver(@PathVariable UUID sucursal,@PathVariable UUID rendicion,@RequestBody @Valid Decision in,Principal p){
        bloquear();alcance(p.getName(),sucursal);
        var actor=jdbc.queryForObject("SELECT id_usuario FROM lamontana.usuario WHERE correo=?",Long.class,p.getName());
        var origen=jdbc.query("SELECT r.id_actor FROM lamontana.rendicion_punto r JOIN lamontana.instruccion_punto i ON i.operacion=r.instruccion JOIN lamontana.sucursal s USING(id_sucursal) WHERE r.operacion=? AND s.codigo_publico=?",(r,n)->r.getLong(1),rendicion,sucursal);
        if(origen.isEmpty()||origen.get(0).equals(actor))throw prohibido();
        var previa=jdbc.query("SELECT * FROM lamontana.resolucion_rendicion WHERE operacion=? OR rendicion=?",
          (r,n)->r.getObject("operacion").equals(in.operacion())&&r.getObject("rendicion").equals(rendicion)&&r.getLong("id_actor")==actor&&r.getString("estado").equals(in.estado())&&Objects.equals(r.getString("motivo"),in.motivo()),in.operacion(),rendicion);
        if(!previa.isEmpty()){if(!previa.get(0))throw conflicto("La rendición ya fue resuelta u operación utilizada.");return;}
        if(in.estado().equals("RECHAZADA")&&vacio(in.motivo()))throw conflicto("Indicá el motivo del rechazo.");
        jdbc.update("INSERT INTO lamontana.resolucion_rendicion(operacion,rendicion,estado,motivo,id_actor) VALUES (?,?,?,?,?)",in.operacion(),rendicion,in.estado(),in.motivo(),actor);
    }
    @GetMapping("/api/operacion/sucursales/{sucursal}/rendiciones-puntos/{rendicion}/comprobante")
    public ResponseEntity<byte[]> comprobante(@PathVariable UUID sucursal,@PathVariable UUID rendicion,Principal p){
        alcance(p.getName(),sucursal);
        var bytes=jdbc.query("SELECT r.comprobante FROM lamontana.rendicion_punto r JOIN lamontana.instruccion_punto i ON i.operacion=r.instruccion JOIN lamontana.sucursal s USING(id_sucursal) WHERE r.operacion=? AND s.codigo_publico=? AND r.comprobante IS NOT NULL",
            (r,n)->r.getBytes(1),rendicion,sucursal).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition","attachment; filename=comprobante").body(bytes);
    }
    @GetMapping("/api/operacion/sucursales/{sucursal}/puntos-beneficiarios")
    public List<Map<String,Object>> beneficiarios(@PathVariable UUID sucursal,Principal p){
        if(CuentaPuntoAcceso.punto(jdbc,p.getName())!=null)throw prohibido();
        organizacion.sucursalAutorizada(p.getName(),sucursal);organizacion.exigirPermiso(p.getName(),"PAGAR_PUNTO");
        return jdbc.queryForList("SELECT pt.codigo_publico punto,"+PuntoRetiroService.NOMBRE+" nombre FROM lamontana.punto_entrega pt ORDER BY pt.codigo");
    }
    @GetMapping("/api/admin/finanzas-puntos/configuracion")
    public List<Map<String,Object>> configuracion(Principal p){
        puntos.admin(p.getName());
        return jdbc.queryForList("""
           SELECT i.operacion cuenta,pt.codigo_publico punto,s.codigo_publico sucursal,i.uso,i.medio,i.habilitada,
            i.instrucciones,i.verifica_punto,i.verifica_admin FROM lamontana.instruccion_punto_actual i JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
            LEFT JOIN lamontana.sucursal s USING(id_sucursal) ORDER BY pt.codigo,s.nombre,i.uso,i.medio
           """);
    }
    @PostMapping("/api/admin/finanzas-puntos/configuracion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void configurar(@Valid @RequestBody Config in,Principal p){
        bloquear();long actor=puntos.admin(p.getName());
        if((in.uso()==Uso.COBRO_PUNTO)!=(in.sucursal()==null)||in.verificaPunto()&&in.uso()!=Uso.COBRO_PUNTO||
           in.medio()==MedioPago.EFECTIVO&&in.uso()!=Uso.RENDICION||in.habilitada()&&in.medio()==MedioPago.TRANSFERENCIA&&vacio(in.instrucciones()))
            throw conflicto("Revisá el uso, el destinatario y las instrucciones de la cuenta.");
        var historial=jdbc.query("""
          SELECT i.* FROM lamontana.instruccion_punto i JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
          LEFT JOIN lamontana.sucursal s USING(id_sucursal)
          WHERE pt.codigo_publico=? AND s.codigo_publico IS NOT DISTINCT FROM ? AND i.uso=? AND i.medio=? ORDER BY secuencia DESC
          """,(r,n)->Map.entry(r.getObject("operacion",UUID.class),r.getBoolean("habilitada")==in.habilitada()&&r.getBoolean("verifica_punto")==in.verificaPunto()&&r.getBoolean("verifica_admin")==Boolean.TRUE.equals(in.verificaAdmin())&&r.getLong("id_actor")==actor&&r.getString("instrucciones").equals(in.instrucciones().strip())),in.punto(),in.sucursal(),in.uso().name(),in.medio().name());
        for(var fila:historial)if(fila.getKey().equals(in.operacion())){if(!fila.getValue())throw conflicto("Operación utilizada con otros datos.");return;}
        if(!Objects.equals(historial.isEmpty()?null:historial.get(0).getKey(),in.anterior()))throw conflicto("La autorización cambió. Volvé a consultarla.");
        int cambios=jdbc.update("""
           INSERT INTO lamontana.instruccion_punto(operacion,id_punto_entrega,id_sucursal,uso,medio,instrucciones,habilitada,verifica_punto,verifica_admin,id_actor)
           SELECT ?,pt.id_punto_entrega,s.id_sucursal,?,?,?,?,?,?,? FROM lamontana.punto_entrega pt
           LEFT JOIN lamontana.sucursal s ON s.codigo_publico=? WHERE pt.codigo_publico=? AND (CAST(? AS uuid) IS NULL OR s.id_sucursal IS NOT NULL)
           """,in.operacion(),in.uso().name(),in.medio().name(),in.instrucciones().strip(),in.habilitada(),in.verificaPunto(),Boolean.TRUE.equals(in.verificaAdmin()),actor,in.sucursal(),in.punto(),in.sucursal());
        if(cambios!=1)throw conflicto("El destinatario no existe.");
    }
    @GetMapping("/api/admin/finanzas-puntos")
    public List<Map<String,Object>> saldos(Principal p){
        puntos.admin(p.getName());return jdbc.queryForList("SELECT pt.codigo_publico punto,"+PuntoRetiroService.NOMBRE+" nombre,recibido::text,confirmado::text,pendiente::text,reservado::text,(SELECT coalesce(sum(m.importe),0)::text FROM lamontana.movimiento_punto m WHERE m.id_punto_entrega=pt.id_punto_entrega AND m.tipo='COBRO' AND m.destino='SUCURSAL_ORIGEN') directa,(SELECT coalesce(sum(g.importe),0)::text FROM lamontana.caja_egreso g WHERE g.id_punto_beneficiario=pt.id_punto_entrega) servicios FROM lamontana.saldo_punto JOIN lamontana.punto_entrega pt USING(id_punto_entrega) ORDER BY pt.codigo");
    }
    public record Revision(@NotNull UUID operacion,@NotNull UUID pago,@NotNull PagoService.DestinoCobro destino,@NotBlank @Size(max=300) String motivo){}
    @PostMapping("/api/admin/finanzas-puntos/revisiones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revisar(@RequestBody @Valid Revision in,Principal principal){
        bloquear();long actor=puntos.admin(principal.getName());
        var previa=jdbc.query("SELECT h.*,p.codigo_publico FROM lamontana.revision_destino_historico h JOIN lamontana.pago p USING(id_pago) WHERE h.operacion=? OR p.codigo_publico=?",
          (r,n)->r.getObject("operacion").equals(in.operacion())&&r.getObject("codigo_publico").equals(in.pago())&&r.getLong("id_actor")==actor&&r.getString("destino").equals(in.destino().name())&&r.getString("motivo").equals(in.motivo()),in.operacion(),in.pago());
        if(!previa.isEmpty()){if(!previa.get(0))throw conflicto("Este movimiento ya fue revisado.");return;}
        int filas=jdbc.update("""
          INSERT INTO lamontana.revision_destino_historico(operacion,id_pago,destino,motivo,id_actor)
          SELECT ?,p.id_pago,?,?,? FROM lamontana.pago p JOIN lamontana.movimiento_punto m ON m.codigo=p.codigo_publico
          WHERE p.codigo_publico=? AND m.destino IS NULL AND m.tipo='COBRO' AND (p.medio='TRANSFERENCIA' OR ?='PUNTO_ENTREGA')
          """,in.operacion(),in.destino().name(),in.motivo(),actor,in.pago(),in.destino().name());
        if(filas!=1)throw conflicto("El pago no admite esa revisión histórica.");
    }
    @GetMapping("/api/admin/finanzas-puntos/historial")
    public List<Map<String,Object>> historial(Principal p,@RequestParam(required=false) UUID punto,@RequestParam(required=false) UUID sucursal,
        @RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta,@RequestParam(defaultValue="") String medio,
        @RequestParam(defaultValue="") String destino,@RequestParam(defaultValue="") String estado,@RequestParam(defaultValue="") String responsable,
        @RequestParam(defaultValue="0") int pagina){
        puntos.admin(p.getName());if(pagina<0||pagina>1000000)throw conflicto("Página inválida.");
        String query="""
          SELECT * FROM (
           SELECT pt.codigo_publico punto,pt.codigo nombre,s.codigo_publico sucursal,m.codigo,m.tipo,m.fecha,
            m.importe::text importe,m.medio,m.estado,m.destinatario,m.destino,m.referencia,m.responsable,coalesce(m.verificador,rev.nombre) verificador,
            (SELECT p.id_pedido FROM lamontana.pedido_actual p WHERE lamontana.cotizacion_descendiente(p.id_cotizacion,m.id_cotizacion) LIMIT 1) pedido,
            c.codigo_publico cotizacion,rev.motivo,false adjunto
           FROM lamontana.movimiento_punto m JOIN lamontana.punto_entrega pt USING(id_punto_entrega)
           JOIN lamontana.cotizacion c USING(id_cotizacion) JOIN lamontana.sucursal s USING(id_sucursal)
           LEFT JOIN LATERAL (SELECT u.nombre||' '||u.apellido nombre,'Revisión '||h.fecha||' · '||u.nombre||' '||u.apellido||' · '||h.motivo motivo
              FROM lamontana.revision_destino_historico h JOIN lamontana.pago p USING(id_pago)
              JOIN lamontana.usuario u ON u.id_usuario=h.id_actor WHERE p.codigo_publico=m.codigo) rev ON true
           UNION ALL SELECT punto,nombre,sucursal,codigo,'RENDICION',fecha,importe,medio,estado,destinatario,'SUCURSAL',
           coalesce(referencia,receptor),responsable,verificador,null,null,motivo,adjunto FROM (
          """+RENDICIONES+"""
           ) r UNION ALL SELECT pt.codigo_publico,pt.codigo,s.codigo_publico,g.id_operacion,'PAGO_PUNTO_ENTREGA',g.fecha,
            g.importe::text,g.medio,'PAGADO',g.destinatario,'PUNTO_ENTREGA',g.referencia,g.actor_nombre,null,null,null,g.concepto,g.comprobante IS NOT NULL
            FROM lamontana.caja_egreso g JOIN lamontana.punto_entrega pt ON pt.id_punto_entrega=g.id_punto_beneficiario JOIN lamontana.sucursal s USING(id_sucursal)
          ) h WHERE true
          """;
        var args=new ArrayList<Object>();
        if(punto!=null){query+=" AND punto=?";args.add(punto);}
        if(sucursal!=null){query+=" AND sucursal=?";args.add(sucursal);}
        var zona=ZoneId.of("America/Argentina/Buenos_Aires");
        if(desde!=null){query+=" AND fecha>=?";args.add(java.sql.Timestamp.from(desde.atStartOfDay(zona).toInstant()));}
        if(hasta!=null){query+=" AND fecha<?";args.add(java.sql.Timestamp.from(hasta.plusDays(1).atStartOfDay(zona).toInstant()));}
        if(!medio.isBlank()){query+=" AND medio=?";args.add(medio);}
        if(!destino.isBlank()){query+=" AND destino=?";args.add(destino);}
        if(!estado.isBlank()){query+=" AND estado=?";args.add(estado);}
        if(!responsable.isBlank()){query+=" AND responsable ILIKE ?";args.add("%"+responsable+"%");}
        args.add(pagina*50);return jdbc.queryForList(query+" ORDER BY fecha DESC,codigo LIMIT 50 OFFSET ?",args.toArray());
    }
}
