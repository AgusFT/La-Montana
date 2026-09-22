package ar.com.lamontana.pagos;

import ar.com.lamontana.archivos.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ComprobanteService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PagoService pagos;
    private final ArchivosPrivados storage;
    private final Antivirus antivirus;
    private final InspectorPdf inspector;
    private final Clock clock=Clock.systemUTC();
    public ComprobanteService(JdbcTemplate jdbc,PlatformTransactionManager manager,PagoService pagos,ArchivosPrivados storage,Antivirus antivirus,InspectorPdf inspector){
        this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);this.pagos=pagos;this.storage=storage;this.antivirus=antivirus;this.inspector=inspector;
    }
    public record Archivo(UUID codigoPublico,String nombre,String estado,Instant creadoEn,Instant cargarHasta,Long bytes,String sha256,Integer paginas,
                          String codigoResultado,String mensaje,String tipo,String detalle,String actor,String rol,String origen,UUID predecesor,boolean puedeEnviar){}
    public record Lista(List<Archivo> elementos,long total,int pagina,boolean puedeCargar,String motivo){}
    public record Carga(boolean habilitada,String motivo){}
    private record Registro(Archivo archivo,long usuario,UUID intento,UUID pago,PagoService.AccesoComprobante acceso){}

    public Lista listar(UUID quote,UUID intento,UUID pago,int pagina,String correo,boolean interno){return tx.execute(s->{
        if(pagina<0||pagina>100000)throw error(HttpStatus.BAD_REQUEST,"Página inválida.");
        var c=pagos.accesoComprobante(quote,intento,pago,correo,interno);
        String where=" FROM lamontana.comprobante_pago p WHERE p.id_intento_pago=? OR p.id_pago=? OR p.id_intento_pago=?";
        long total=jdbc.queryForObject("SELECT count(*)"+where,Long.class,c.intento(),c.pago(),c.intentoVinculado());
        var ids=jdbc.query("SELECT a.codigo_publico FROM lamontana.comprobante_pago p JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) WHERE p.id_intento_pago=? OR p.id_pago=? OR p.id_intento_pago=? ORDER BY p.id_comprobante_pago DESC LIMIT 25 OFFSET ?",(r,n)->r.getObject(1,UUID.class),c.intento(),c.pago(),c.intentoVinculado(),(long)pagina*25);
        var files=ids.stream().map(id->registro(quote,id,correo,interno).archivo()).toList();
        return new Lista(files,total,pagina,c.puedeCargar(),c.puedeCargar()?"Adjuntá un PDF estático de hasta 10 MiB. Su análisis técnico no confirma la transferencia ni acredita dinero.":"Podés consultar los documentos. Para adjuntar se requiere el permiso del medio; los recibos de efectivo los aporta personal interno.");
    });}
    public Archivo consultar(UUID quote,UUID file,String correo,boolean interno){return tx.execute(s->registro(quote,file,correo,interno).archivo());}
    public Carga puedeEnviar(UUID quote,UUID file,String correo,boolean interno){return tx.execute(s->{var r=registro(quote,file,correo,interno);return carga(r);});}
    public Archivo crear(UUID quote,ComprobanteController.Inicio in,String correo,boolean interno){return tx.execute(s->{
        bloquear();var c=pagos.accesoComprobante(quote,in.intento(),in.pago(),correo,interno);
        String nombre=in.nombre().strip(),detalle=in.detalle().strip();
        if(!nombre.toLowerCase(Locale.ROOT).endsWith(".pdf")||nombre.codePoints().anyMatch(cp->Character.isISOControl(cp)||cp=='/'||cp=='\\'))throw error(HttpStatus.BAD_REQUEST,"Indicá un nombre de PDF sin rutas ni caracteres de control.");
        if(detalle.codePoints().anyMatch(cp->Character.isISOControl(cp)&&cp!='\n'&&cp!='\r'&&cp!='\t'))throw error(HttpStatus.BAD_REQUEST,"El detalle contiene caracteres de control no admitidos.");
        var prev=jdbc.query("SELECT a.codigo_publico,p.id_intento_pago,p.id_pago,a.id_usuario_cargador,a.nombre_original,p.detalle FROM lamontana.archivo_almacenado a LEFT JOIN lamontana.comprobante_pago p USING(id_archivo_almacenado) WHERE a.id_operacion=?",(r,n)->new Object[]{r.getObject(1,UUID.class),r.getObject(2,Long.class),r.getObject(3,Long.class),r.getLong(4),r.getString(5),r.getString(6)},in.operacion());
        if(!prev.isEmpty()){
            var r=prev.get(0);if(!Objects.equals(r[1],c.intento())||!Objects.equals(r[2],c.pago())||(long)r[3]!=c.usuario()||!nombre.equals(r[4])||!detalle.equals(r[5]))throw conflicto("La operación ya se utilizó con otro archivo, movimiento o contenido.");
            return registro(quote,(UUID)r[0],correo,interno).archivo();
        }
        exigirEscritura(c);
        jdbc.update("UPDATE lamontana.archivo_almacenado a SET estado='FALLIDO',fecha_fin=now(),codigo_resultado='CARGA_VENCIDA',mensaje='Venció el plazo para enviar este comprobante. Iniciá otra carga.' FROM lamontana.comprobante_pago p WHERE p.id_archivo_almacenado=a.id_archivo_almacenado AND (p.id_intento_pago=? OR p.id_pago=?) AND a.estado='PENDIENTE' AND a.cargar_hasta<=now()",c.intento(),c.pago());
        if(jdbc.queryForObject("SELECT count(*) FROM lamontana.comprobante_pago p JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) WHERE (p.id_intento_pago=? OR p.id_pago=?) AND a.estado IN ('PENDIENTE','VALIDANDO')",Integer.class,c.intento(),c.pago())>0)throw conflicto("Hay una carga pendiente para este movimiento. Consultá su resultado antes de iniciar otra.");
        if(jdbc.queryForObject("SELECT count(*) FROM lamontana.archivo_almacenado WHERE id_usuario_cargador=? AND fecha_creacion>now()-interval '1 hour'",Integer.class,c.usuario())>=60)throw error(HttpStatus.TOO_MANY_REQUESTS,"Se alcanzó el límite de cargas por hora. Conservá el archivo e intentá más tarde.");
        UUID file=UUID.randomUUID();long id=jdbc.queryForObject("INSERT INTO lamontana.archivo_almacenado(codigo_publico,id_usuario_cargador,id_operacion,nombre_original,estado,cargar_hasta,finalidad) VALUES (?,?,?,?,'PENDIENTE',now()+interval '10 minutes','COMPROBANTE') RETURNING id_archivo_almacenado",Long.class,file,c.usuario(),in.operacion(),nombre);
        jdbc.update("INSERT INTO lamontana.comprobante_pago(id_archivo_almacenado,id_intento_pago,id_pago,tipo,actor_nombre,actor_rol,detalle,id_comprobante_reemplazado) VALUES (?,?,?,?,?,?,?,(SELECT p.id_comprobante_pago FROM lamontana.comprobante_pago p JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado) WHERE (p.id_intento_pago=? OR p.id_pago=?) AND a.estado='VALIDO' ORDER BY p.id_comprobante_pago DESC LIMIT 1))",id,c.intento(),c.pago(),c.tipo(),c.actor(),c.rol(),detalle,c.intento(),c.pago());
        return registro(quote,file,correo,interno).archivo();
    });}

    public Archivo recibir(UUID quote,UUID file,String correo,boolean interno,InputStream input,long declaredLength){
        boolean iniciar=Boolean.TRUE.equals(tx.execute(s->{
            bloquear();var r=registro(quote,file,correo,interno);if(!r.archivo().estado().equals("PENDIENTE"))return false;
            var acceso=carga(r);if(!acceso.habilitada())throw conflicto(acceso.motivo());
            if(declaredLength>ArchivosPrivados.MAX_BYTES)throw error(HttpStatus.PAYLOAD_TOO_LARGE,"El comprobante supera el máximo de 10 MiB.");
            jdbc.update("UPDATE lamontana.archivo_almacenado SET estado='VALIDANDO',fecha_inicio=now() WHERE codigo_publico=?",file);return true;
        }));
        if(!iniciar)return consultar(quote,file,correo,interno);
        String engine=null,stage=null;
        try{
            var received=storage.recibir(file,input);stage="SEGURIDAD";engine=antivirus.analizar(received.path());stage="ESTRUCTURA_RENDER";
            int pages=inspector.inspeccionar(received.path(),storage.carpeta(file).resolve("inspeccion.txt"));storage.publicar(file);
            final String version=engine;
            return tx.execute(s->{
                bloquear();var r=registro(quote,file,correo,interno);exigirEscritura(r.acceso());
                if(r.usuario()!=r.acceso().usuario()||!r.archivo().estado().equals("VALIDANDO"))throw conflicto("El intento de carga cambió o fue interrumpido.");
                jdbc.update("UPDATE lamontana.archivo_almacenado SET estado='VALIDO',fecha_fin=now(),cantidad_bytes=?,sha256=?,cantidad_paginas=?,codigo_resultado='COMPROBANTE_INSPECCIONADO',mensaje='Documento privado inspeccionado. No confirma la transferencia ni acredita dinero.' WHERE codigo_publico=?",received.bytes(),received.sha256(),pages,file);
                validacion(file,"SEGURIDAD","ACEPTADO","ClamAV",version,"LIMPIO");validacion(file,"ESTRUCTURA_RENDER","ACEPTADO","Apache PDFBox","3.0.8","TODAS_LAS_PAGINAS");
                return registro(quote,file,correo,interno).archivo();
            });
        }catch(Exception ex){
            boolean rejected=ex instanceof InspectorPdf.Rechazado;
            String code=rejected?((InspectorPdf.Rechazado)ex).codigo:ex instanceof InspectorPdf.NoDisponible?"ANALISIS_NO_DISPONIBLE":"CARGA_INTERRUMPIDA";
            String message=rejected||ex instanceof InspectorPdf.NoDisponible?ex.getMessage():"Se interrumpió la carga o su publicación. Consultá el estado e iniciá otra carga del comprobante.";
            final String version=engine,failedStage=stage,failedVersion=ex instanceof Antivirus.Deteccion d?d.version:"SEGURIDAD".equals(stage)?"No disponible":"3.0.8";
            tx.executeWithoutResult(s->{
                bloquear();int changed=jdbc.update("UPDATE lamontana.archivo_almacenado SET estado=?,fecha_fin=now(),codigo_resultado=?,mensaje=? WHERE codigo_publico=? AND estado='VALIDANDO'",rejected?"RECHAZADO":"FALLIDO",code,message,file);
                if(changed==1){if(version!=null)validacion(file,"SEGURIDAD","ACEPTADO","ClamAV",version,"LIMPIO");if(failedStage!=null&&(version==null||failedStage.equals("ESTRUCTURA_RENDER")))validacion(file,failedStage,rejected?"RECHAZADO":"INCOMPLETO",failedStage.equals("SEGURIDAD")?"ClamAV":"Apache PDFBox",failedVersion,code);}
            });
            try{storage.descartar(file);}catch(IOException ignored){} // El estado terminal impide servir cualquier parcial.
            return consultar(quote,file,correo,interno);
        }
    }

    public ArchivoService.Contenido contenido(UUID quote,UUID file,Integer page,String correo,boolean interno){
        var f=consultar(quote,file,correo,interno);if(!f.estado().equals("VALIDO"))throw conflicto("El comprobante todavía no tiene un resultado de análisis seguro.");
        if(page!=null&&(page<1||page>f.paginas()))throw error(HttpStatus.BAD_REQUEST,"Página inválida.");
        try{
            Path original=storage.original(file,f.sha256(),f.bytes());
            if(page==null){byte[] bytes=Files.readAllBytes(original);consultar(quote,file,correo,interno);return new ArchivoService.Contenido(bytes,"application/pdf",f.nombre());}
            Path output=storage.carpeta(file).resolve("preview-"+UUID.randomUUID()+".png");
            try{inspector.preview(original,output,page-1);consultar(quote,file,correo,interno);return new ArchivoService.Contenido(Files.readAllBytes(output),"image/png","comprobante-pagina-"+page+".png");}
            finally{Files.deleteIfExists(output);}
        }catch(IOException ex){throw error(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo verificar la integridad o recuperar el comprobante privado. Volvé a intentar.");}
    }
    private Registro registro(UUID quote,UUID file,String correo,boolean interno){
        var rows=jdbc.query("""
            SELECT a.*,p.tipo,p.detalle,p.actor_nombre,p.actor_rol,i.codigo_publico AS intento,x.codigo_publico AS pago,
              anterior.codigo_publico AS anterior
            FROM lamontana.comprobante_pago p JOIN lamontana.archivo_almacenado a USING(id_archivo_almacenado)
            LEFT JOIN lamontana.intento_pago i ON i.id_intento_pago=p.id_intento_pago
            LEFT JOIN lamontana.pago x ON x.id_pago=p.id_pago
            JOIN lamontana.cotizacion c ON c.id_cotizacion=coalesce(i.id_cotizacion,x.id_cotizacion)
            LEFT JOIN lamontana.comprobante_pago v ON v.id_comprobante_pago=p.id_comprobante_reemplazado
            LEFT JOIN lamontana.archivo_almacenado anterior ON anterior.id_archivo_almacenado=v.id_archivo_almacenado
            WHERE c.codigo_publico=? AND a.codigo_publico=? AND a.finalidad='COMPROBANTE'
            """,(r,n)->{
            UUID intento=r.getObject("intento",UUID.class),pago=r.getObject("pago",UUID.class);var c=pagos.accesoComprobante(quote,intento,pago,correo,interno);
            boolean cargar=c.puedeCargar()&&c.usuario()==r.getLong("id_usuario_cargador")&&r.getString("estado").equals("PENDIENTE")&&clock.instant().isBefore(r.getTimestamp("cargar_hasta").toInstant());
            var f=new Archivo(file,r.getString("nombre_original"),r.getString("estado"),r.getTimestamp("fecha_creacion").toInstant(),r.getTimestamp("cargar_hasta").toInstant(),r.getObject("cantidad_bytes",Long.class),r.getString("sha256"),r.getObject("cantidad_paginas",Integer.class),r.getString("codigo_resultado"),r.getString("mensaje"),r.getString("tipo"),r.getString("detalle"),r.getString("actor_nombre"),r.getString("actor_rol"),intento==null?"PAGO":"TRANSFERENCIA_INFORMADA",r.getObject("anterior",UUID.class),cargar);
            return new Registro(f,r.getLong("id_usuario_cargador"),intento,pago,c);
        },quote,file);
        if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"El comprobante no está disponible.");return rows.get(0);
    }
    private Carga carga(Registro r){
        if(!r.archivo().estado().equals("PENDIENTE"))return new Carga(false,"El intento ya fue procesado. Consultá su resultado guardado.");
        if(!clock.instant().isBefore(r.archivo().cargarHasta()))return new Carga(false,"El plazo de carga venció. Iniciá otra carga.");
        if(!r.acceso().puedeCargar()||r.usuario()!=r.acceso().usuario())return new Carga(false,"No tenés permiso para completar esta carga o pertenece a otra persona.");
        return new Carga(true,"Podés enviar el comprobante para su análisis. No acredita dinero.");
    }
    private void exigirEscritura(PagoService.AccesoComprobante c){if(!c.puedeCargar())throw error(HttpStatus.FORBIDDEN,"No tenés permiso para adjuntar documentos de este medio de pago.");}
    private void validacion(UUID file,String type,String result,String motor,String version,String code){jdbc.update("INSERT INTO lamontana.validacion_archivo(id_archivo_almacenado,tipo_validacion,resultado,motor,version_motor,codigo_resultado) VALUES ((SELECT id_archivo_almacenado FROM lamontana.archivo_almacenado WHERE codigo_publico=?),?,?,?,?,?)",file,type,result,motor,version,code);}
    private void bloquear(){jdbc.execute("SELECT pg_advisory_xact_lock(764003)");jdbc.execute("SELECT pg_advisory_xact_lock(764001)");jdbc.execute("SELECT pg_advisory_xact_lock(764002)");jdbc.execute("SELECT pg_advisory_xact_lock(764004)");}
    private ResponseStatusException conflicto(String msg){return error(HttpStatus.CONFLICT,msg);}
    private ResponseStatusException error(HttpStatus status,String msg){return new ResponseStatusException(status,msg);}
}
