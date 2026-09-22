package ar.com.lamontana.archivos;

import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.organizacion.OrganizacionService;
import java.io.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ArchivoService {
    private final JdbcTemplate jdbc;private final TransactionTemplate tx;
    private final ArchivosPrivados storage;private final InspectorPdf inspector;private final Antivirus antivirus;
    private final OrganizacionService organizacion;private final OfertaOperativaService operativa;private final EvaluadorFinanciero finanzas;
    private final JsonMapper json=JsonMapper.builder().build();
    private final Clock clock=Clock.systemUTC();
    public ArchivoService(JdbcTemplate jdbc,PlatformTransactionManager manager,ArchivosPrivados storage,InspectorPdf inspector,Antivirus antivirus,
                          OrganizacionService organizacion,OfertaOperativaService operativa,EvaluadorFinanciero finanzas){
        this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);this.storage=storage;this.inspector=inspector;this.antivirus=antivirus;
        this.organizacion=organizacion;this.operativa=operativa;this.finanzas=finanzas;
    }
    public record Acceso(boolean habilitada,String motivo){}
    public record Archivo(UUID codigoPublico,UUID item,String nombre,String estado,boolean activo,Instant creadoEn,Instant cargarHasta,
                          Long bytes,String sha256,Integer paginas,String codigoResultado,String mensaje,Instant aceptadaEn){}
    public record Item(UUID codigoPublico,String nombre,List<Archivo> archivos){}
    public record Vista(UUID cotizacion,String sucursal,Acceso carga,List<Item> items){}
    private record Cotizacion(long id,long propietario,CotizacionService.Oferta oferta,String estado,Instant vence,Instant aceptada,long version){}
    private record Contexto(Cotizacion cotizacion,long usuario,String rol){}
    public record Contenido(byte[] bytes,String tipo,String nombre){}
    public record Recibida(UUID cotizacion,long numero,String cliente,String estado,long archivos,long validos,long aceptados){}
    public record Bandeja(List<Recibida> elementos,long total,int pagina){}
    public Bandeja bandeja(UUID branch,int page,String correo){return tx.execute(s->{
        organizacion.sucursalAutorizada(correo,branch);
        if(page<0||page>100000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Página inválida.");
        String from=" FROM lamontana.cotizacion c JOIN lamontana.usuario u ON u.id_usuario=c.id_usuario_creador JOIN lamontana.sucursal s ON s.id_sucursal=c.id_sucursal WHERE s.codigo_publico=? AND EXISTS (SELECT 1 FROM lamontana.cotizacion_item ci JOIN lamontana.archivo_trabajo t USING(id_cotizacion_item) WHERE ci.id_cotizacion=c.id_cotizacion)";
        long total=jdbc.queryForObject("SELECT count(*)"+from,Long.class,branch);
        var list=jdbc.query("SELECT c.codigo_publico,c.id_cotizacion,u.nombre,u.apellido,c.estado,c.vigente_hasta"+from+" ORDER BY c.id_cotizacion DESC LIMIT 25 OFFSET ?",(r,n)->{
            long id=r.getLong("id_cotizacion");long[] counts=jdbc.queryForObject("SELECT count(*),count(*) FILTER(WHERE t.activo AND a.estado='VALIDO'),count(*) FILTER(WHERE t.activo AND p.id_aceptacion_vista_previa IS NOT NULL) FROM lamontana.cotizacion_item ci JOIN lamontana.archivo_trabajo t USING(id_cotizacion_item) JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) LEFT JOIN lamontana.aceptacion_vista_previa p USING(id_archivo_almacenado) WHERE ci.id_cotizacion=?",(row,index)->new long[]{row.getLong(1),row.getLong(2),row.getLong(3)},id);
            String state=r.getString("estado");if(state.equals("VIGENTE")&&!clock.instant().isBefore(r.getTimestamp("vigente_hasta").toInstant()))state="EXPIRADA";
            return new Recibida(r.getObject("codigo_publico",UUID.class),id,r.getString("nombre")+" "+r.getString("apellido"),state,counts[0],counts[1],counts[2]);
        },branch,(long)page*25);return new Bandeja(list,total,page);
    });}

    public Vista listar(UUID quote,String correo,boolean interno){return tx.execute(s->{var c=autorizar(quote,correo,interno);return vista(quote,c,interno);});}
    public Archivo consultar(UUID quote,UUID file,String correo,boolean interno){return tx.execute(s->{autorizar(quote,correo,interno);return archivo(quote,file);});}
    public Acceso puedeEnviar(UUID quote,UUID file,String correo){return tx.execute(s->{var c=autorizar(quote,correo,false);var f=archivo(quote,file);if(!f.estado().equals("PENDIENTE"))return new Acceso(false,"El intento ya fue procesado; consultá su resultado.");if(!clock.instant().isBefore(f.cargarHasta()))return new Acceso(false,"El plazo de carga venció. Iniciá otra carga.");return gate(c.cotizacion());});}

    public Archivo crear(UUID quote,UUID item,UUID operation,long version,String correo){return tx.execute(s->{
        bloquear();var c=autorizar(quote,correo,false);
        var prev=jdbc.query("SELECT a.codigo_publico,t.id_cotizacion_item FROM lamontana.archivo_almacenado a JOIN lamontana.archivo_trabajo t USING(id_archivo_almacenado) WHERE a.id_operacion=?",(r,n)->new Object[]{r.getObject(1,UUID.class),r.getLong(2)},operation);
        long idItem=itemId(c.cotizacion().id(),item);
        if(!prev.isEmpty()){
            if((long)prev.get(0)[1]!=idItem)throw conflicto("La operación ya se usó con otro archivo.");
            return archivo(quote,(UUID)prev.get(0)[0]);
        }
        if(c.cotizacion().version()!=version)throw conflicto("La cotización cambió. Actualizá su estado.");
        exigirCarga(c.cotizacion());
        jdbc.update("UPDATE lamontana.archivo_almacenado a SET estado='FALLIDO',fecha_fin=now(),codigo_resultado='CARGA_VENCIDA',mensaje='El plazo para enviar este archivo venció. Iniciá otra carga.' FROM lamontana.archivo_trabajo t WHERE t.id_archivo_almacenado=a.id_archivo_almacenado AND t.id_cotizacion_item=? AND a.estado='PENDIENTE' AND a.cargar_hasta<=now()",idItem);
        if(jdbc.queryForObject("SELECT count(*) FROM lamontana.archivo_almacenado a JOIN lamontana.archivo_trabajo t USING(id_archivo_almacenado) WHERE t.id_cotizacion_item=? AND a.estado IN ('PENDIENTE','VALIDANDO')",Integer.class,idItem)>0)throw conflicto("Ya hay una carga pendiente para este ítem. Recuperá su resultado.");
        if(jdbc.queryForObject("SELECT count(*) FROM lamontana.archivo_almacenado WHERE id_usuario_cargador=? AND fecha_creacion>now()-interval '1 hour'",Integer.class,c.usuario())>=60)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Se alcanzó el límite de cargas por hora. Conservá el PDF e intentá más tarde.");
        String name=c.cotizacion().oferta().items().stream().filter(i->i.codigoPublico().equals(item)).findFirst().orElseThrow().documento().nombre();
        UUID file=UUID.randomUUID();long id=jdbc.queryForObject("INSERT INTO lamontana.archivo_almacenado(codigo_publico,id_usuario_cargador,id_operacion,nombre_original,estado,cargar_hasta) VALUES (?,?,?,?,'PENDIENTE',now()+interval '10 minutes') RETURNING id_archivo_almacenado",Long.class,file,c.usuario(),operation,name);
        jdbc.update("INSERT INTO lamontana.archivo_trabajo(id_archivo_almacenado,id_cotizacion_item,id_archivo_trabajo_reemplazado) VALUES (?,?,(SELECT id_archivo_trabajo FROM lamontana.archivo_trabajo WHERE id_cotizacion_item=? AND activo))",id,idItem,idItem);
        return archivo(quote,file);
    });}

    public Archivo recibir(UUID quote,UUID file,String correo,InputStream input,long declaredLength){
        boolean start=Boolean.TRUE.equals(tx.execute(s->{
            bloquear();var c=autorizar(quote,correo,false);var f=archivo(quote,file);
            if(!f.estado().equals("PENDIENTE"))return false;
            exigirCarga(c.cotizacion());
            if(!clock.instant().isBefore(f.cargarHasta()))throw conflicto("El plazo de carga venció. Iniciá otra carga.");
            if(declaredLength>ArchivosPrivados.MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"El PDF supera el máximo de 10 MiB.");
            jdbc.update("UPDATE lamontana.archivo_almacenado SET estado='VALIDANDO',fecha_inicio=now() WHERE codigo_publico=?",file);return true;
        }));
        if(!start)return consultar(quote,file,correo,false);
        String engine=null;String stage="SEGURIDAD";ArchivosPrivados.Recibido received=null;
        try{
            received=storage.recibir(file,input);engine=antivirus.analizar(received.path());stage="ESTRUCTURA_RENDER";
            int pages=inspector.inspeccionar(received.path(),storage.carpeta(file).resolve("inspeccion.txt"));
            storage.publicar(file);
            final var original=received;final var engineVersion=engine;
            return tx.execute(s->{
                bloquear();var c=autorizar(quote,correo,false);exigirCarga(c.cotizacion());var f=archivo(quote,file);
                if(!f.estado().equals("VALIDANDO"))throw conflicto("El análisis fue interrumpido. Iniciá otra carga.");
                var expected=c.cotizacion().oferta().items().stream().filter(i->i.codigoPublico().equals(f.item())).findFirst().orElseThrow();
                boolean same=expected.trabajo().paginas()==pages;
                boolean identical=expected.documento().bytes()==original.bytes()&&expected.documento().sha256().equals(original.sha256());
                String state=same?"VALIDO":"REQUIERE_COTIZACION",code=same?(identical?"PDF_VALIDADO":"NUEVO_CONTENIDO_MISMO_IMPORTE"):"DATOS_DIFERENTES";
                String message=same?(identical?"PDF inspeccionado. Revisá todas las páginas y aceptá su vista previa.":"El contenido cambió, pero conserva las páginas y opciones cotizadas. El precio no cambia; revisá y aceptá esta nueva vista previa."):"El PDF inspeccionado difiere de los datos cotizados. Necesita una nueva cotización y aceptación; la oferta original no cambió.";
                jdbc.update("UPDATE lamontana.archivo_almacenado SET estado=?,fecha_fin=now(),cantidad_bytes=?,sha256=?,cantidad_paginas=?,codigo_resultado=?,mensaje=? WHERE codigo_publico=?",state,original.bytes(),original.sha256(),pages,code,message,file);
                validacion(file,"SEGURIDAD","ACEPTADO","ClamAV",engineVersion,"LIMPIO");validacion(file,"ESTRUCTURA_RENDER","ACEPTADO","Apache PDFBox","3.0.8","TODAS_LAS_PAGINAS");
                if(same){long itemId=itemId(c.cotizacion().id(),f.item());jdbc.update("UPDATE lamontana.archivo_trabajo SET activo=false WHERE id_cotizacion_item=? AND activo",itemId);jdbc.update("UPDATE lamontana.archivo_trabajo SET activo=true WHERE id_archivo_almacenado=(SELECT id_archivo_almacenado FROM lamontana.archivo_almacenado WHERE codigo_publico=?)",file);}
                return archivo(quote,file);
            });
        }catch(Exception ex){
            boolean rejected=ex instanceof InspectorPdf.Rechazado;String code=rejected?((InspectorPdf.Rechazado)ex).codigo:ex instanceof InspectorPdf.NoDisponible?"ANALISIS_NO_DISPONIBLE":"CARGA_INTERRUMPIDA";
            String message=rejected||ex instanceof InspectorPdf.NoDisponible?ex.getMessage():"La carga o su validación se interrumpió. Consultá el estado y volvé a cargar el PDF.";
            final String engineVersion=engine,failedStage=stage,failedVersion=ex instanceof Antivirus.Deteccion detected?detected.version:stage.equals("SEGURIDAD")?"No disponible":"3.0.8";
            tx.executeWithoutResult(s->{bloquear();int changed=jdbc.update("UPDATE lamontana.archivo_almacenado SET estado=?,fecha_fin=now(),codigo_resultado=?,mensaje=? WHERE codigo_publico=? AND estado='VALIDANDO'",rejected?"RECHAZADO":"FALLIDO",code,message,file);if(changed==1){if(engineVersion!=null)validacion(file,"SEGURIDAD","ACEPTADO","ClamAV",engineVersion,"LIMPIO");if(engineVersion==null||failedStage.equals("ESTRUCTURA_RENDER"))validacion(file,failedStage,rejected?"RECHAZADO":"INCOMPLETO",failedStage.equals("SEGURIDAD")?"ClamAV":"Apache PDFBox",failedVersion,code);}});
            try{storage.descartar(file);}catch(IOException ignored){} // Never served: the durable state is terminal and unsafe.
            return consultar(quote,file,correo,false);
        }
    }

    public Archivo aceptar(UUID quote,UUID file,UUID operation,String hash,String correo){return tx.execute(s->{
        bloquear();var c=autorizar(quote,correo,false);var f=archivo(quote,file);
        var previous=jdbc.query("SELECT a.codigo_publico,p.sha256 FROM lamontana.aceptacion_vista_previa p JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) WHERE p.id_operacion=?",(r,n)->new String[]{r.getString(1),r.getString(2)},operation);
        if(!previous.isEmpty()){if(!previous.get(0)[0].equals(file.toString())||!previous.get(0)[1].equals(hash))throw conflicto("La operación ya se usó para otra vista previa.");return f;}
        exigirCarga(c.cotizacion());
        if(!f.activo()||!f.estado().equals("VALIDO")||!Objects.equals(f.sha256(),hash))throw conflicto("Sólo se acepta la vista previa vigente de un PDF que coincide con esta cotización.");
        if(f.aceptadaEn()!=null)throw conflicto("La vista previa ya fue aceptada. Consultá el estado guardado.");
        try{storage.original(file,f.sha256(),f.bytes());}catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo comprobar la integridad del PDF privado. La vista previa no fue aceptada.");}
        jdbc.update("INSERT INTO lamontana.aceptacion_vista_previa(id_archivo_almacenado,id_usuario,id_operacion,sha256) VALUES ((SELECT id_archivo_almacenado FROM lamontana.archivo_almacenado WHERE codigo_publico=?),?,?,?)",file,c.usuario(),operation,hash);
        return archivo(quote,file);
    });}

    public Contenido contenido(UUID quote,UUID file,Integer pagina,String correo,boolean interno){
        Archivo f=consultar(quote,file,correo,interno);
        if(!Set.of("VALIDO","REQUIERE_COTIZACION").contains(f.estado()))throw conflicto("El archivo todavía no tiene un resultado de inspección seguro.");
        if(pagina!=null&&(pagina<1||pagina>f.paginas()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Página inválida.");
        try{
            Path original=storage.original(file,f.sha256(),f.bytes());
            if(pagina==null){byte[] data=Files.readAllBytes(original);consultar(quote,file,correo,interno);return new Contenido(data,"application/pdf",f.nombre());}
            Path output=storage.carpeta(file).resolve("preview-"+UUID.randomUUID()+".png");
            try{inspector.preview(original,output,pagina-1);consultar(quote,file,correo,interno);return new Contenido(Files.readAllBytes(output),"image/png","pagina-"+pagina+".png");}
            finally{Files.deleteIfExists(output);}
        }catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo recuperar o renderizar el archivo privado. Volvé a intentar.");}
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperar(){var interrupted=tx.execute(s->{bloquear();return jdbc.query("UPDATE lamontana.archivo_almacenado SET estado='FALLIDO',fecha_fin=now(),codigo_resultado='SERVICIO_REINICIADO',mensaje='El servicio se reinició durante la carga. Iniciá otra carga para continuar.' WHERE estado='VALIDANDO' RETURNING codigo_publico",(r,n)->r.getObject(1,UUID.class));});for(UUID id:interrupted)try{storage.descartar(id);}catch(IOException ignored){}}
    private Vista vista(UUID quote,Contexto c,boolean interno){
        var items=c.cotizacion().oferta().items().stream().map(i->{var ids=jdbc.query("SELECT a.codigo_publico FROM lamontana.archivo_almacenado a JOIN lamontana.archivo_trabajo t USING(id_archivo_almacenado) JOIN lamontana.cotizacion_item ci USING(id_cotizacion_item) WHERE ci.codigo_publico=? ORDER BY t.activo DESC,a.id_archivo_almacenado DESC LIMIT 10",(r,n)->r.getObject(1,UUID.class),i.codigoPublico());return new Item(i.codigoPublico(),i.documento().nombre(),ids.stream().map(id->archivo(quote,id)).toList());}).toList();
        return new Vista(quote,c.cotizacion().oferta().sucursal().nombre(),interno?new Acceso(false,"Consulta interna de archivos recibidos; la aprobación operativa se integra con los pedidos."):gate(c.cotizacion()),items);
    }
    private Contexto autorizar(UUID quote,String correo,boolean interno){
        var users=jdbc.query("SELECT u.id_usuario,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND r.activo",(r,n)->new Object[]{r.getLong(1),r.getString(2)},correo);
        if(users.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La cuenta no está habilitada.");long user=(long)users.get(0)[0];String role=(String)users.get(0)[1];
        if(interno?!Set.of("ADMIN_ADMIN","EMPLEADO").contains(role):!role.equals("CLIENTE"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No tenés acceso a este espacio.");
        var rows=jdbc.query("SELECT * FROM lamontana.cotizacion WHERE codigo_publico=? AND (? OR id_usuario_creador=?)",(r,n)->new Cotizacion(r.getLong("id_cotizacion"),r.getLong("id_usuario_creador"),json.readValue(r.getString("oferta"),CotizacionService.Oferta.class),r.getString("estado"),r.getTimestamp("vigente_hasta").toInstant(),r.getTimestamp("aceptada_en")==null?null:r.getTimestamp("aceptada_en").toInstant(),r.getLong("version")),quote,interno,user);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"La cotización no está disponible.");var c=rows.get(0);
        if(interno&&!role.equals("ADMIN_ADMIN"))organizacion.sucursalAutorizada(correo,c.oferta().sucursal().codigoPublico());return new Contexto(c,user,role);
    }
    private Acceso gate(Cotizacion c){
        if(!c.estado().equals("VIGENTE")||!clock.instant().isBefore(c.vence()))return new Acceso(false,"La cotización dejó de estar vigente. Solicitá una nueva oferta para continuar.");
        if(c.aceptada()==null)return new Acceso(false,"Aceptá la cotización antes de enviar los PDF.");
        var current=operativa.leer();var b=current.configuracion();
        if(b==null||current.sucursales().stream().noneMatch(s->s.codigoPublico().equals(c.oferta().sucursal().codigoPublico())))return new Acceso(false,"La sucursal no está disponible para recibir el trabajo.");
        int faces=c.oferta().items().stream().mapToInt(i->i.precio().carillas()).sum();
        var actual=finanzas.evaluar(b.modelo(),b.criterio(),b.pagos(),b.version(),new java.math.BigDecimal(c.oferta().total()),faces);
        if(c.oferta().condiciones().cargaRequiereAcreditacion()||actual.cargaRequiereAcreditacion())return new Acceso(false,"La carga requiere dinero acreditado. La acreditación está En construcción; conservá el PDF en tu dispositivo.");
        return new Acceso(true,"Podés enviar los PDF para su análisis y vista previa. La seña exigible después de aprobar sigue siendo un requisito independiente.");
    }
    private void exigirCarga(Cotizacion c){var a=gate(c);if(!a.habilitada())throw conflicto(a.motivo());}
    private long itemId(long quote,UUID item){var ids=jdbc.query("SELECT id_cotizacion_item FROM lamontana.cotizacion_item WHERE id_cotizacion=? AND codigo_publico=?",(r,n)->r.getLong(1),quote,item);if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El ítem no está disponible.");return ids.get(0);}
    private Archivo archivo(UUID quote,UUID file){
        var rows=jdbc.query("SELECT a.*,ci.codigo_publico AS item,t.activo,p.fecha AS aceptada FROM lamontana.archivo_almacenado a JOIN lamontana.archivo_trabajo t USING(id_archivo_almacenado) JOIN lamontana.cotizacion_item ci USING(id_cotizacion_item) JOIN lamontana.cotizacion c USING(id_cotizacion) LEFT JOIN lamontana.aceptacion_vista_previa p USING(id_archivo_almacenado) WHERE a.codigo_publico=? AND c.codigo_publico=?",(r,n)->new Archivo(r.getObject("codigo_publico",UUID.class),r.getObject("item",UUID.class),r.getString("nombre_original"),r.getString("estado"),r.getBoolean("activo"),r.getTimestamp("fecha_creacion").toInstant(),r.getTimestamp("cargar_hasta").toInstant(),r.getObject("cantidad_bytes",Long.class),r.getString("sha256"),r.getObject("cantidad_paginas",Integer.class),r.getString("codigo_resultado"),r.getString("mensaje"),r.getTimestamp("aceptada")==null?null:r.getTimestamp("aceptada").toInstant()),file,quote);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El archivo no está disponible.");return rows.get(0);
    }
    private void validacion(UUID file,String type,String result,String engine,String version,String code){jdbc.update("INSERT INTO lamontana.validacion_archivo(id_archivo_almacenado,tipo_validacion,resultado,motor,version_motor,codigo_resultado) VALUES ((SELECT id_archivo_almacenado FROM lamontana.archivo_almacenado WHERE codigo_publico=?),?,?,?,?,?)",file,type,result,engine,version,code);}
    private void bloquear(){jdbc.execute("SELECT pg_advisory_xact_lock(764003)");jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");}
    private ResponseStatusException conflicto(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
