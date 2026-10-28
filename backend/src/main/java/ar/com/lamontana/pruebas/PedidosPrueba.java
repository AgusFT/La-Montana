package ar.com.lamontana.pruebas;

import ar.com.lamontana.archivos.ArchivosPrivados;
import ar.com.lamontana.catalogo.*;
import ar.com.lamontana.configuracion.*;
import ar.com.lamontana.cotizaciones.CotizacionController;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.pedidos.PedidoService;
import java.io.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;
import static ar.com.lamontana.pruebas.DatosPruebaConfiguration.*;
import static ar.com.lamontana.configuracion.EntregaConfiguracionController.*;
import static ar.com.lamontana.configuracion.ConfiguracionController.*;

/**
 * Fábrica de hechos históricos ficticios. Usa los evaluadores reales para importes y calendarios,
 * guarda PDF generados localmente y recorre las transiciones con todos los triggers activos.
 * No modifica relojes, servicios de producción, auditorías existentes ni permisos de validación.
 */
final class PedidosPrueba {
    private final JdbcTemplate jdbc;
    private final OfertaOperativaService operativa;
    private final EvaluadorPrecioItem precios;
    private final EvaluadorFinanciero finanzas;
    private final ArchivosPrivados archivos;
    private final String hashCodigo;
    private final JsonMapper json=JsonMapper.builder().build();
    private int secuencia;
    private static final String HUELLA="0".repeat(64);
    private static final PDType1Font FUENTE=new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final String[] DOCUMENTOS={"Apunte de programación","Trabajo práctico de redes","Material de capacitación","Carpeta de presentaciones","Guía de ejercicios","Catálogo comercial","Proyecto de diseño","Manual de usuario"};
    private record Actor(long id,String nombre,String rol){}
    private record Item(long id,long archivo,long aceptacion,UUID publico,UUID pdf){}
    private static final class Pedido {
        long id,cotizacion,config,version=1,reserva;
        UUID publico;
        Actor cliente;
        String estado="PENDIENTE_REVISION";
        BigDecimal total,anticipo;
        MedioPago medio;
        Modalidad modalidad;
        boolean retiroConTraslado;
        Instant fecha;
        final List<Item> items=new ArrayList<>();
    }

    PedidosPrueba(JdbcTemplate jdbc,OfertaOperativaService operativa,EvaluadorPrecioItem precios,EvaluadorFinanciero finanzas,ArchivosPrivados archivos,PasswordEncoder encoder) {
        this.jdbc=jdbc;this.operativa=operativa;this.precios=precios;this.finanzas=finanzas;this.archivos=archivos;
        hashCodigo=encoder.encode("PRUEBA26");
    }

    void historico(LocalDate dia) {
        int tipo=secuencia%12;
        crear(dia.atTime(9,20).atZone(ZONA).toInstant(),tipo==0?"RECHAZADO":tipo==2?"CANCELADO":"CERRADO",0,false);
        // Otro cliente el mismo día deja un historial de volumen realista.
        crear(dia.atTime(10,10).atZone(ZONA).toInstant(),"CERRADO",0,false);
    }

    void jornada(LocalDate hoy) {
        String[] estados={"PENDIENTE_REVISION","PENDIENTE_REVISION","CORRECCION_SOLICITADA","APROBADO","APROBADO","EN_PRODUCCION","EN_PRODUCCION","EN_PRODUCCION","LISTO_PARA_ENTREGA","LISTO_PARA_ENTREGA","LISTO_PARA_ENTREGA","LISTO_PARA_ENTREGA","ENTREGADO","CERRADO","CANCELADO","RECHAZADO"};
        Instant inicio=Instant.now().minusSeconds(3*3600);
        for(int i=0;i<estados.length;i++)crear(inicio.plusSeconds(i*60L),estados[i],i,true);
        // Cotizaciones sin pedido: una vigente sin aceptar y otras vencidas/canceladas.
        cotizacionSinPedido(hoy.minusDays(25),"EXPIRADA");
        cotizacionSinPedido(hoy.minusDays(7),"CANCELADA");
        cotizacionSinPedido(hoy,"VIGENTE");
    }

    private void crear(Instant fecha,String destino,int caso,boolean actual) {
        int indice=secuencia++;
        var ctx=operativa.leer();var b=ctx.configuracion();
        var p=new Pedido();p.fecha=fecha;p.publico=UUID.randomUUID();p.cliente=actor("cliente"+(indice%4+1)+"@prueba.local");
        p.config=jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,b.codigoPublico());
        p.medio=indice%2==0?MedioPago.EFECTIVO:MedioPago.TRANSFERENCIA;
        p.modalidad=Modalidad.values()[indice%3];
        if(actual&&caso>=8&&caso<=11)p.modalidad=caso==8?Modalidad.RETIRO_SUCURSAL:caso==11?Modalidad.ENVIO_DOMICILIO:Modalidad.RETIRO_PUNTO_ENTREGA;
        var matriz=ctx.sucursales().stream().filter(s->s.codigoPublico().equals(b.entrega().casaMatriz())).findFirst().orElseThrow();
        // Retiros locales de las tres sucursales; los puntos/domicilios salen de la matriz.
        var branch=p.modalidad==Modalidad.RETIRO_SUCURSAL?ctx.sucursales().get((indice/3)%ctx.sucursales().size()):matriz;
        p.retiroConTraslado=p.modalidad==Modalidad.RETIRO_SUCURSAL&&!branch.codigoPublico().equals(matriz.codigoPublico());
        UUID punto=p.modalidad==Modalidad.RETIRO_PUNTO_ENTREGA?b.entrega().puntos().get(indice%2).codigoPublico():null;
        var territorio=new TerritorioEntrega("1407","FLORESTA","CABA");
        var direccion=p.modalidad==Modalidad.ENVIO_DOMICILIO?new CotizacionController.Direccion("Av. Rivadavia","8350","Timbre de pruebas",territorio):null;
        var tiempo=operativa.calcularEntrega(ctx,new SimularEntrega(b.version(),branch.codigoPublico(),p.modalidad,fecha,punto,p.modalidad==Modalidad.ENVIO_DOMICILIO?territorio:null),15);
        var cotizados=new ArrayList<CotizacionService.ItemCotizado>();
        var pdfs=new ArrayList<ArchivosPrivados.Recibido>();var idsPdf=new ArrayList<UUID>();
        BigDecimal subtotal=BigDecimal.ZERO;
        int carillas=0;
        for(int numero=0;numero<(indice%5==0?2:1);numero++) {
            var opcion=ctx.opciones().stream().filter(o->o.sucursal().equals(branch.codigoPublico())).toList().get((indice*7+numero)%ctx.opciones().stream().filter(o->o.sucursal().equals(branch.codigoPublico())).toList().size());
            int paginas=4+(indice+numero)%9,copias=indice%7==0?30:1+indice%4;
            if(actual&&(caso==0||caso==1||caso==3))copias=60;
            var terminaciones=indice%3==0&&!opcion.terminaciones().isEmpty()?List.of(opcion.terminaciones().get(indice%opcion.terminaciones().size())):List.<UUID>of();
            var trabajo=new EvaluadorPrecioItem.Item(opcion.servicio(),opcion.formato(),opcion.papel(),opcion.color(),paginas,copias,indice%2==0,terminaciones);
            UUID pdf=UUID.randomUUID();var recibido=pdf(pdf,paginas,"Pedido de prueba "+(indice+1)+" - documento "+(numero+1));
            var precio=precios.calcular(ctx.catalogo(),trabajo);
            String formato=ctx.catalogo().formatos().stream().filter(f->f.codigoPublico().equals(opcion.formato())).findFirst().orElseThrow().nombre();
            String papel=ctx.catalogo().papeles().stream().filter(f->f.codigoPublico().equals(opcion.papel())).findFirst().orElseThrow().nombre();
            cotizados.add(new CotizacionService.ItemCotizado(UUID.randomUUID(),new CotizacionController.Documento(DOCUMENTOS[(indice+numero)%DOCUMENTOS.length]+" "+(indice+1)+".pdf",recibido.bytes(),recibido.sha256()),trabajo,formato,papel,precio));
            subtotal=subtotal.add(new BigDecimal(precio.subtotal()));carillas+=precio.carillas();pdfs.add(recibido);idsPdf.add(pdf);
        }
        String costo=tiempo.destinoPunto()!=null?tiempo.destinoPunto().costo():tiempo.destinoZona()!=null?tiempo.destinoZona().costo():"0.00";
        p.total=subtotal.add(new BigDecimal(costo));
        var financiera=finanzas.evaluar(b.modelo(),b.criterio(),b.pagos(),b.version(),p.total,carillas);
        p.anticipo=new BigDecimal(financiera.pagoPrevioRequerido()).add(new BigDecimal(financiera.senaRequerida()));
        if(p.anticipo.signum()>0&&b.modelo()==Modelo.CONDICIONAL)p.medio=MedioPago.TRANSFERENCIA;
        var condiciones=new CotizacionService.Condiciones(financiera.revisionHumana(),false,financiera.pagoPrevioRequerido(),financiera.senaRequerida(),financiera.saldo(),financiera.momento().name(),financiera.mediosAcreditacion(),financiera.instrucciones(),financiera.politicaArchivos());
        String nombreDestino=tiempo.destinoPunto()!=null?tiempo.destinoPunto().nombre():tiempo.destinoZona()!=null?"Av. Rivadavia 8350, Floresta, CABA":branch.nombre()+" · "+branch.direccion();
        var oferta=new CotizacionService.Oferta(b.codigoPublico(),b.numero(),ctx.catalogo().actual().codigoPublico(),ctx.catalogo().actual().numero(),branch,cotizados,p.modalidad,punto,direccion,nombreDestino,p.medio,subtotal.toPlainString(),costo,p.total.toPlainString(),condiciones,tiempo,b.pagos().medios(),false);
        UUID quote=UUID.randomUUID();
        p.cotizacion=jdbc.queryForObject("INSERT INTO lamontana.cotizacion(codigo_publico,id_usuario_creador,id_sucursal,id_configuracion_version,id_catalogo_revision,id_configuracion_confirmacion,estado,version,generada_en,vigente_hasta,aceptada_en,confirmada_en,subtotal,costo_entrega,total,oferta) SELECT ?,?,id_sucursal,?,?,?,'CONFIRMADA',3,?,?,?,?,?,?,?,?::jsonb FROM lamontana.sucursal WHERE codigo_publico=? RETURNING id_cotizacion",Long.class,quote,p.cliente.id(),p.config,ctx.catalogo().actual().numero(),p.config,ts(fecha.minusSeconds(180)),ts(fecha.plusSeconds(86400)),ts(fecha.minusSeconds(120)),ts(fecha),subtotal,new BigDecimal(costo),p.total,json.writeValueAsString(oferta),branch.codigoPublico());
        eventoCotizacion(p,"COTIZAR",fecha.minusSeconds(180));eventoCotizacion(p,"ACEPTAR_OFERTA",fecha.minusSeconds(120));
        for(int i=0;i<cotizados.size();i++)p.items.add(item(p,cotizados.get(i),i+1,idsPdf.get(i),pdfs.get(i)));
        var finales=new PedidoService.Condiciones(condiciones,financiera,financiera.revisionHumana());
        // Las versiones condicionales conservan la aprobación automática tras pago y controles.
        boolean auto=b.modelo()==Modelo.CONDICIONAL;
        p.id=jdbc.queryForObject("INSERT INTO lamontana.pedido(codigo_publico,id_cotizacion,id_usuario_creador,id_sucursal,id_configuracion_version,estado,modo_aprobacion,confirmada_en,total,condiciones_finales,receptor,telefono) SELECT ?,?,?,id_sucursal,?,'PENDIENTE_REVISION',?,?,?,?::jsonb,?,? FROM lamontana.sucursal WHERE codigo_publico=? RETURNING id_pedido",Long.class,p.publico,p.cotizacion,p.cliente.id(),p.config,auto?"AUTOMATICA":"HUMANA",ts(fecha),p.total,json.writeValueAsString(finales),direccion==null?null:p.cliente.nombre(),direccion==null?null:"11-5555-1234",branch.codigoPublico());
        for(var item:p.items)jdbc.update("INSERT INTO lamontana.pedido_item(id_pedido,id_cotizacion_item,id_archivo_almacenado,id_aceptacion_vista_previa) VALUES (?,?,?,?)",p.id,item.id(),item.archivo(),item.aceptacion());
        reserva(p,tiempo,branch,punto,nombreDestino,costo);
        jdbc.update("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha) VALUES (?,?,?,?,'PENDIENTE_REVISION','Pedido simulado confirmado con PDF de muestra aceptado',?)",p.id,p.cliente.id(),UUID.randomUUID(),HUELLA,ts(fecha));
        cobro(p,financiera.politicaArchivos());
        comprobarRestricciones(jdbc);
        if(destino.equals("PENDIENTE_REVISION")) {
            if(caso==1)informar(p,p.total,fecha.plusSeconds(120));
        } else if(destino.equals("CORRECCION_SOLICITADA"))correccion(p,fecha.plusSeconds(180));
        else if(destino.equals("CANCELADO")||destino.equals("RECHAZADO")) {
            if(destino.equals("CANCELADO")) {
                long pago=recibir(p,p.total,fecha.plusSeconds(120),false);
                devolver(p,pago,p.total,fecha.plusSeconds(600));
            }
            transicion(p,destino,destino.equals("CANCELADO")?"CANCELAR":"RECHAZAR",fecha.plusSeconds(900),"El cliente canceló y recibió el reintegro." ,"finalizada_en=?",ts(fecha.plusSeconds(900)));
            jdbc.update("UPDATE lamontana.reserva_entrega SET estado='LIBERADA' WHERE id_pedido=?",p.id);
            comprobarRestricciones(jdbc);
        } else {
            boolean pagoHistoricoEnDosPartes=!actual&&p.anticipo.signum()>0&&p.anticipo.compareTo(p.total)<0;
            BigDecimal monto=pagoHistoricoEnDosPartes?p.anticipo:actual&&caso==3?p.anticipo.max(p.total.multiply(new BigDecimal("0.30")).setScale(2,java.math.RoundingMode.HALF_UP)):p.total;
            recibir(p,monto,fecha.plusSeconds(120),true);
            if(!auto) {
                transicion(p,p.estado,"REVISAR",fecha.plusSeconds(240),"PDF y datos revisados.","revisada_en=?,id_revisor=?",ts(fecha.plusSeconds(240)),actor(ADMIN).id());
                comprobarRestricciones(jdbc);
            }
            transicion(p,"APROBADO","APROBAR",fecha.plusSeconds(300),auto?"Aprobación condicional después de acreditar el anticipo.":"Pedido aprobado para producción.","aprobada_en=?,id_aprobador=?",ts(fecha.plusSeconds(300)),actor(ADMIN).id());
            comprobarRestricciones(jdbc);
            if(!destino.equals("APROBADO")) {
                for(int numero=0;numero<p.items.size();numero++) {
                    var item=p.items.get(numero);long tiempoTrabajo=600+numero*600L;
                    long trabajo=iniciar(p,item,fecha.plusSeconds(tiempoTrabajo));
                    if(destino.equals("EN_PRODUCCION")&&caso==5)break;
                    completar(p,trabajo,fecha.plusSeconds(tiempoTrabajo+200),actual&&caso==6);
                    if(destino.equals("EN_PRODUCCION")&&caso==6)break;
                    calidad(p,trabajo,fecha.plusSeconds(tiempoTrabajo+400),actual&&caso==7);
                }
                if(destino.equals("ENTREGADO")||destino.equals("CERRADO")) {
                    Instant ventana=tiempo.destinoPunto()!=null?tiempo.destinoPunto().franjaDesde():tiempo.destinoZona()!=null?tiempo.destinoZona().franjaDesde():tiempo.destinoSucursal().franjaDesde();
                    long offset=actual?2400:Math.max(2400,Duration.between(fecha,ventana).getSeconds()+600);
                    if(pagoHistoricoEnDosPartes)recibir(p,p.total.subtract(monto),fecha.plusSeconds(offset-150),true);
                    if(p.modalidad!=Modalidad.RETIRO_SUCURSAL||p.retiroConTraslado) {
                        mover(p,"PREPARAR_ENVIO","PREPARADO",fecha.plusSeconds(offset-1000));
                        mover(p,"SALIR_REPARTO","EN_VIAJE",fecha.plusSeconds(offset-600));
                        if(p.modalidad==Modalidad.RETIRO_PUNTO_ENTREGA||p.retiroConTraslado)mover(p,"LLEGAR_PUNTO","DISPONIBLE_PUNTO",fecha.plusSeconds(offset-200));
                    }
                    entregar(p,fecha.plusSeconds(offset));
                    if(destino.equals("CERRADO"))cerrar(p,fecha.plusSeconds(offset+300));
                } else if(destino.equals("LISTO_PARA_ENTREGA")&&caso>=9) {
                    mover(p,"PREPARAR_ENVIO","PREPARADO",fecha.plusSeconds(1700));
                    if(caso>=10)mover(p,"SALIR_REPARTO","EN_VIAJE",fecha.plusSeconds(1800));
                    if(caso==10)mover(p,"LLEGAR_PUNTO","DISPONIBLE_PUNTO",fecha.plusSeconds(1900));
                }
            }
        }
        if(indice%3==0)jdbc.update("INSERT INTO lamontana.observacion_interna(codigo_publico,id_pedido,id_autor,autor_nombre,id_operacion,huella,importancia,texto,fecha) VALUES (?,?,?,?,?,?,'INFORMATIVA',?,?)",UUID.randomUUID(),p.id,actor(ADMIN).id(),actor(ADMIN).nombre(),UUID.randomUUID(),HUELLA,"Cliente habitual. Controlar orden de páginas y terminación al preparar el retiro.",ts(fecha.plusSeconds(60)));
        comprobarRestricciones(jdbc);
    }

    private Item item(Pedido p,CotizacionService.ItemCotizado i,int orden,UUID pdf,ArchivosPrivados.Recibido bytes) {
        var t=i.trabajo();var precio=i.precio();
        long id=jdbc.queryForObject("INSERT INTO lamontana.cotizacion_item(codigo_publico,id_cotizacion,orden,id_formato,id_papel,cantidad_paginas_declaradas,cantidad_copias,cantidad_carillas,cantidad_hojas,modo_color,doble_faz,nombre_archivo_declarado,bytes_declarados,sha256_declarado,subtotal) SELECT ?,?,?,f.id_formato,p.id_papel,?,?,?,?,?,?,?,?,?,? FROM lamontana.formato f CROSS JOIN lamontana.papel p WHERE f.codigo_publico=? AND p.codigo_publico=? RETURNING id_cotizacion_item",Long.class,i.codigoPublico(),p.cotizacion,orden,t.paginas(),t.copias(),precio.carillas(),precio.hojas(),t.color().name(),t.dobleFaz(),i.documento().nombre(),bytes.bytes(),bytes.sha256(),new BigDecimal(precio.subtotal()),t.formato(),t.papel());
        for(var l:precio.lineas())jdbc.update("INSERT INTO lamontana.cotizacion_item_servicio SELECT ?,id_servicio,?,?,?,?,?,? FROM lamontana.servicio WHERE codigo_publico=?",id,l.servicio().equals(t.servicio())?"IMPRESION":"TERMINACION",l.nombre(),l.base(),l.unidades(),new BigDecimal(l.precioUnitario()),new BigDecimal(l.importe()),l.servicio());
        long file=jdbc.queryForObject("INSERT INTO lamontana.archivo_almacenado(codigo_publico,id_usuario_cargador,id_operacion,nombre_original,estado,fecha_creacion,cargar_hasta,fecha_inicio,fecha_fin,cantidad_bytes,sha256,cantidad_paginas,codigo_resultado,mensaje) VALUES (?,?,?,?,'VALIDO',?,?,?,?,?,?,?,'PDF_FICTICIO','PDF generado por el escenario de pruebas; validaciones simuladas identificadas como tales.') RETURNING id_archivo_almacenado",Long.class,pdf,p.cliente.id(),UUID.randomUUID(),i.documento().nombre(),ts(p.fecha.minusSeconds(110)),ts(p.fecha.plusSeconds(600)),ts(p.fecha.minusSeconds(105)),ts(p.fecha.minusSeconds(100)),bytes.bytes(),bytes.sha256(),t.paginas());
        jdbc.update("INSERT INTO lamontana.archivo_trabajo(id_archivo_almacenado,id_cotizacion_item,activo,fecha_asociacion) VALUES (?,?,true,?)",file,id,ts(p.fecha.minusSeconds(100)));
        for(String validacion:new String[]{"SEGURIDAD","ESTRUCTURA_RENDER"})jdbc.update("INSERT INTO lamontana.validacion_archivo(id_archivo_almacenado,tipo_validacion,resultado,motor,version_motor,codigo_resultado,fecha) VALUES (?,?,'ACEPTADO','Escenario ficticio','2','PDF_GENERADO_LOCALMENTE',?)",file,validacion,ts(p.fecha.minusSeconds(100)));
        long aceptacion=jdbc.queryForObject("INSERT INTO lamontana.aceptacion_vista_previa(id_archivo_almacenado,id_usuario,id_operacion,sha256,fecha) VALUES (?,?,?,?,?) RETURNING id_aceptacion_vista_previa",Long.class,file,p.cliente.id(),UUID.randomUUID(),bytes.sha256(),ts(p.fecha.minusSeconds(60)));
        return new Item(id,file,aceptacion,i.codigoPublico(),pdf);
    }

    private void reserva(Pedido p,EvaluadorCalendario.Simulacion t,OfertaOperativaService.Sucursal branch,UUID punto,String nombre,String costo) {
        UUID dest;LocalDate fecha;String apertura,cierre;Instant desde,hasta;int cupo;
        if(t.destinoPunto()!=null){var d=t.destinoPunto();dest=punto;fecha=d.fecha();apertura=d.apertura();cierre=d.cierre();desde=d.franjaDesde();hasta=d.franjaHasta();cupo=d.cupoConfigurado();}
        else if(t.destinoZona()!=null){var d=t.destinoZona();dest=d.zona();fecha=d.fecha();apertura=d.apertura();cierre=d.cierre();desde=d.franjaDesde();hasta=d.franjaHasta();cupo=d.cupoConfigurado();}
        else {var d=t.destinoSucursal();dest=branch.codigoPublico();fecha=d.fecha();apertura=d.apertura();cierre=d.cierre();desde=d.franjaDesde();hasta=d.franjaHasta();cupo=d.cupoConfigurado();}
        p.reserva=jdbc.queryForObject("INSERT INTO lamontana.reserva_entrega(id_pedido,modalidad,destino_publico,origen_publico,nombre_destino,zona_horaria,fecha_local,apertura,cierre,franja_desde,franja_hasta,cupo_confirmado,costo) VALUES (?,?,?,?,?,'America/Argentina/Buenos_Aires',?::date,?::time,?::time,?,?,?,?) RETURNING id_reserva_entrega",Long.class,p.id,p.modalidad.name(),dest,branch.codigoPublico(),nombre,fecha.toString(),apertura,cierre,ts(desde),ts(hasta),cupo,new BigDecimal(costo));
    }

    private long transicion(Pedido p,String destino,String accion,Instant fecha,String mensaje,String adicionales,Object... valores) {
        var args=new ArrayList<Object>();args.add(destino);args.addAll(Arrays.asList(valores));args.add(p.id);
        jdbc.update("UPDATE lamontana.pedido SET estado=?,version=version+1"+(adicionales.isEmpty()?"":","+adicionales)+" WHERE id_pedido=?",args.toArray());
        var a=actor(ADMIN);long evento=jdbc.queryForObject("INSERT INTO lamontana.historial_estado_pedido(id_pedido,id_actor,id_operacion,huella,estado_destino,motivo,fecha,estado_origen,version_pedido,accion,mensaje_cliente,actor_nombre,actor_tipo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id_evento",Long.class,p.id,a.id(),UUID.randomUUID(),HUELLA,destino,mensaje,ts(fecha),p.estado,++p.version,accion,mensaje,a.nombre(),a.rol());
        p.estado=destino;return evento;
    }

    private long iniciar(Pedido p,Item item,Instant fecha) {
        long id=jdbc.queryForObject("INSERT INTO lamontana.trabajo_impresion(codigo_publico,id_pedido,id_cotizacion_item,id_archivo_almacenado,id_aceptacion_vista_previa,id_impresora,id_configuracion_version,impresora_nombre,id_operador,operador_nombre,tipo,estado,fecha_inicio) SELECT ?,?,?,?,?,ci.id_impresora,?,ci.nombre,?,?,'NORMAL','IMPRIMIENDO',? FROM lamontana.configuracion_impresora ci WHERE ci.id_configuracion_version=? AND ci.admite_color ORDER BY ci.id_impresora LIMIT 1 RETURNING id_trabajo_impresion",Long.class,UUID.randomUUID(),p.id,item.id(),item.archivo(),item.aceptacion(),p.config,actor(ADMIN).id(),actor(ADMIN).nombre(),ts(fecha),p.config);
        trabajoEvento(p,id,"INICIAR_TRABAJO",null,"IMPRIMIENDO","EN_PRODUCCION",fecha,"Impresión iniciada manualmente.");return id;
    }
    private void completar(Pedido p,long trabajo,Instant fecha,boolean error) {
        jdbc.update("UPDATE lamontana.trabajo_impresion SET estado=?,fecha_fin=? WHERE id_trabajo_impresion=?",error?"ERROR":"COMPLETADO",ts(fecha),trabajo);
        trabajoEvento(p,trabajo,error?"ERROR_TRABAJO":"COMPLETAR_TRABAJO","IMPRIMIENDO",error?"ERROR":"COMPLETADO","EN_PRODUCCION",fecha,error?"Atasco de papel. Pendiente de nuevo intento.":"Impresión completa; pendiente de calidad.");
    }
    private void calidad(Pedido p,long trabajo,Instant fecha,boolean incidencia) {
        var a=actor(ADMIN);
        jdbc.update("INSERT INTO lamontana.control_calidad(id_trabajo_impresion,id_inspector,inspector_nombre,resultado,impresion_completa,calidad_correcta,alineacion_correcta,orden_correcto,terminaciones_correctas,observaciones,fecha) VALUES (?,?,?,?,true,?,true,true,true,?,?)",trabajo,a.id(),a.nombre(),incidencia?"REIMPRESION_REQUERIDA":"APROBADO",!incidencia,incidencia?"Bandas de impresión. Repetir el trabajo.":"Orden, impresión y terminación correctos.",ts(fecha));
        boolean lista=Boolean.TRUE.equals(jdbc.queryForObject("SELECT lamontana.calidad_pedido_aprobada(?)",Boolean.class,p.id));
        trabajoEvento(p,trabajo,"CONTROLAR_CALIDAD","COMPLETADO","COMPLETADO",lista?"LISTO_PARA_ENTREGA":"EN_PRODUCCION",fecha,incidencia?"Bandas de impresión. Repetir el trabajo.":"Orden, impresión y terminación correctos.");
    }
    private void trabajoEvento(Pedido p,long trabajo,String accion,String origen,String destino,String estado,Instant fecha,String mensaje) {
        long event=transicion(p,estado,accion,fecha,mensaje,"");var a=actor(ADMIN);
        jdbc.update("INSERT INTO lamontana.historial_trabajo_impresion(id_trabajo_impresion,id_evento_pedido,accion,estado_origen,estado_destino,id_actor,actor_nombre,motivo,fecha) VALUES (?,?,?,?,?,?,?,?,?)",trabajo,event,accion,origen,destino,a.id(),a.nombre(),mensaje,ts(fecha));comprobarRestricciones(jdbc);
    }

    private void mover(Pedido p,String accion,String destino,Instant fecha) {
        String origen=jdbc.queryForObject("SELECT lamontana.estado_logistico(?)",String.class,p.id);
        long e=transicion(p,p.estado,accion,fecha,"Movimiento de entrega: "+destino,"");
        jdbc.update("INSERT INTO lamontana.movimiento_entrega(id_pedido,id_reserva_entrega,id_evento_pedido,estado_origen,estado_destino) VALUES (?,?,?,?,?)",p.id,p.reserva,e,origen,destino);comprobarRestricciones(jdbc);
    }

    private void entregar(Pedido p,Instant fecha) {
        var a=actor(ADMIN);
        long codigo=jdbc.queryForObject("INSERT INTO lamontana.codigo_entrega(codigo_publico,id_pedido,id_reserva_entrega,id_emisor,id_operacion,huella,codigo_hash,emitido_en,vence_en) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id_codigo_entrega",Long.class,UUID.randomUUID(),p.id,p.reserva,p.cliente.id(),UUID.randomUUID(),HUELLA,hashCodigo,ts(fecha.minusSeconds(120)),ts(fecha.plusSeconds(1800)));
        long validacion=jdbc.queryForObject("INSERT INTO lamontana.validacion_codigo_entrega(codigo_publico,id_codigo_entrega,id_actor,actor_nombre,id_operacion,huella,version_pedido,resultado,fecha,valido_hasta) VALUES (?,?,?,?,?,?,?,'VALIDO',?,?) RETURNING id_validacion_codigo",Long.class,UUID.randomUUID(),codigo,a.id(),a.nombre(),UUID.randomUUID(),HUELLA,p.version,ts(fecha.minusSeconds(60)),ts(fecha.plusSeconds(600)));
        long evento=transicion(p,"ENTREGADO","ENTREGAR",fecha,"Pedido entregado a "+p.cliente.nombre(),"");
        jdbc.update("UPDATE lamontana.reserva_entrega SET estado='CUMPLIDA' WHERE id_reserva_entrega=?",p.reserva);
        jdbc.update("UPDATE lamontana.codigo_entrega SET utilizado_en=? WHERE id_codigo_entrega=?",ts(fecha),codigo);
        jdbc.update("INSERT INTO lamontana.entrega(codigo_publico,id_pedido,id_reserva_entrega,id_evento_pedido,id_validacion_codigo,receptor_nombre,nota,fecha_entrega) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),p.id,p.reserva,evento,validacion,p.cliente.nombre(),"Entrega presencial simulada con validación de código y saldo cubierto.",ts(fecha));comprobarRestricciones(jdbc);
    }
    private void cerrar(Pedido p,Instant fecha) {
        long evento=transicion(p,"CERRADO","CERRAR",fecha,"Entrega y caja conciliadas. Pedido cerrado.","finalizada_en=?",ts(fecha));
        jdbc.update("INSERT INTO lamontana.cierre_pedido VALUES (?,?,?,?,?)",p.id,evento,p.total,p.total,ts(fecha));comprobarRestricciones(jdbc);
    }

    private void correccion(Pedido p,Instant fecha) {
        var a=actor(ADMIN);
        long id=jdbc.queryForObject("INSERT INTO lamontana.solicitud_correccion(codigo_publico,id_pedido,id_solicitante,solicitante_nombre,tipo,motivo,mensaje_cliente,fecha_solicitud,version_pedido_inicio,id_cotizacion_base,id_configuracion_base,condiciones_base,total_base,receptor_base,telefono_base) SELECT ?,id_pedido,?,?,'ARCHIVO','Texto próximo al margen de corte','Por favor, enviá el PDF con márgenes de 5 mm.',?,version+1,id_cotizacion,id_configuracion_version,condiciones_finales,total,receptor,telefono FROM lamontana.pedido WHERE id_pedido=? RETURNING id_solicitud_correccion",Long.class,UUID.randomUUID(),a.id(),a.nombre(),ts(fecha),p.id);
        for(var i:p.items)jdbc.update("INSERT INTO lamontana.solicitud_correccion_item VALUES (?,?,?,true)",id,i.id(),i.archivo());
        transicion(p,"CORRECCION_SOLICITADA","PEDIR_CORRECCION",fecha,"Por favor, enviá el PDF con márgenes de 5 mm.","");comprobarRestricciones(jdbc);
    }

    private long financiero(Pedido p,String tipo,Instant fecha,String motivo,Actor a) {
        return jdbc.queryForObject("INSERT INTO lamontana.evento_financiero(id_operacion,id_cotizacion,id_actor,actor_nombre,actor_rol,tipo,huella,motivo,fecha) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id_evento",Long.class,UUID.randomUUID(),p.cotizacion,a.id(),a.nombre(),a.rol(),tipo,HUELLA,motivo,ts(fecha));
    }
    private long informar(Pedido p,BigDecimal monto,Instant fecha) {
        long e=financiero(p,"INFORMAR",fecha,"Transferencia simulada informada por el cliente.",p.cliente);
        eventoCobro(p,"PAGO_INFORMADO",fecha,p.cliente);
        return jdbc.queryForObject("INSERT INTO lamontana.intento_pago(codigo_publico,id_cotizacion,id_evento,importe_informado,referencia_informada) VALUES (?,?,?,?,?) RETURNING id_intento_pago",Long.class,UUID.randomUUID(),p.cotizacion,e,monto,"PRUEBA-TRANSFERENCIA-"+p.cotizacion);
    }
    private long recibir(Pedido p,BigDecimal monto,Instant fecha,boolean aplicar) {
        var a=actor("caja@prueba.local");Long intento=p.medio==MedioPago.TRANSFERENCIA?informar(p,monto,fecha.minusSeconds(30)):null;
        long e=financiero(p,"RECIBIR",fecha,"Recepción verificada de dinero ficticio.",a);
        if(intento!=null)jdbc.update("INSERT INTO lamontana.resolucion_intento_pago VALUES (?,?,'ACREDITADO')",intento,e);
        long id=jdbc.queryForObject("INSERT INTO lamontana.pago(codigo_publico,id_cotizacion,id_intento_pago,id_evento,medio,importe,referencia,clave_referencia,recibido_en) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id_pago",Long.class,UUID.randomUUID(),p.cotizacion,intento,e,p.medio.name(),monto,"PRUEBA-"+p.cotizacion,"pago-prueba-"+UUID.randomUUID(),ts(fecha));
        if(aplicar)jdbc.update("INSERT INTO lamontana.aplicacion_pago_cotizacion(id_pago,id_cotizacion,id_evento,importe) VALUES (?,?,?,?)",id,p.cotizacion,financiero(p,"APLICAR",fecha.plusSeconds(1),"Pago aplicado al pedido.",a),monto);
        if(aplicar)jdbc.update("UPDATE lamontana.cobro_pedido SET cobertura_confirmada_en=coalesce(cobertura_confirmada_en,?) WHERE id_pedido=? AND lamontana.produccion_cubierta(?)",ts(fecha.plusSeconds(1)),p.id,p.id);
        eventoCobro(p,aplicar?"PAGO_APLICADO":"DINERO_RECIBIDO",fecha.plusSeconds(1),a);
        comprobarRestricciones(jdbc);return id;
    }
    private void devolver(Pedido p,long pago,BigDecimal monto,Instant fecha) {
        long e=financiero(p,"DEVOLVER",fecha,"Devolución total por cancelación solicitada.",actor("caja@prueba.local"));
        jdbc.update("INSERT INTO lamontana.reembolso(codigo_publico,id_pago,id_evento,importe,medio,referencia,clave_referencia,devuelto_en) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),pago,e,monto,p.medio.name(),"REINTEGRO-PRUEBA-"+p.cotizacion,"devolucion-prueba-"+UUID.randomUUID(),ts(fecha));comprobarRestricciones(jdbc);
        eventoCobro(p,"DEVOLUCION_REGISTRADA",fecha,actor("caja@prueba.local"));
    }

    private void cobro(Pedido p,PoliticaPagoArchivos politica) {
        boolean cubierto=Boolean.TRUE.equals(jdbc.queryForObject("SELECT lamontana.produccion_cubierta(?)",Boolean.class,p.id));
        jdbc.update("INSERT INTO lamontana.cobro_pedido(id_pedido,politica_original,fecha_limite,recordatorio_minutos,retencion_horas,retener_hasta,cobertura_confirmada_en) VALUES (?,?::jsonb,?,?,?,?,?)",p.id,json.writeValueAsString(politica),ts(p.fecha.plusSeconds(politica.plazoMinutos()*60L)),politica.recordatorioMinutos(),politica.retencionHoras(),ts(p.fecha.plusSeconds(politica.plazoMinutos()*60L+politica.retencionHoras()*3600L)),cubierto?ts(p.fecha.plusSeconds(121)):null);
        jdbc.update("INSERT INTO lamontana.evento_cobro_pedido(id_pedido,id_operacion,id_actor,tipo,motivo,resultado,fecha) VALUES (?,?,?,'PEDIDO_CREADO','Política de cobro capturada en escenario ficticio',?::jsonb,?)",p.id,UUID.randomUUID(),p.cliente.id(),json.writeValueAsString(Map.of("simulado",true,"estado",p.estado)),ts(p.fecha));
    }

    private void cotizacionSinPedido(LocalDate dia,String estado) {
        // Reutiliza una fotografía completa de una oferta, conservando todos sus importes.
        Instant fecha=estado.equals("VIGENTE")?Instant.now().minusSeconds(600):dia.atTime(11,0).atZone(ZONA).toInstant();
        var fuente=jdbc.queryForMap("SELECT * FROM lamontana.cotizacion WHERE generada_en<=? ORDER BY generada_en DESC LIMIT 1",ts(fecha));
        var original=json.readValue(fuente.get("oferta").toString(),CotizacionService.Oferta.class);
        var nuevos=original.items().stream().map(i->new CotizacionService.ItemCotizado(UUID.randomUUID(),i.documento(),i.trabajo(),i.formato(),i.papel(),i.precio())).toList();
        var oferta=new CotizacionService.Oferta(original.configuracion(),original.numeroConfiguracion(),original.revisionComercial(),original.numeroRevision(),original.sucursal(),nuevos,original.modalidad(),original.punto(),original.direccion(),original.destino(),original.medioPago(),original.subtotal(),original.costoEntrega(),original.total(),original.condiciones(),original.entrega(),original.mediosGenerales(),false);
        long id=jdbc.queryForObject("INSERT INTO lamontana.cotizacion(codigo_publico,id_usuario_creador,id_sucursal,id_configuracion_version,id_catalogo_revision,estado,generada_en,vigente_hasta,cancelada_en,motivo_cancelacion,subtotal,costo_entrega,total,oferta) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb) RETURNING id_cotizacion",Long.class,UUID.randomUUID(),fuente.get("id_usuario_creador"),fuente.get("id_sucursal"),fuente.get("id_configuracion_version"),fuente.get("id_catalogo_revision"),estado,ts(fecha),ts(fecha.plusSeconds(86400)),estado.equals("CANCELADA")?ts(fecha.plusSeconds(600)):null,estado.equals("CANCELADA")?"El cliente eligió otras cantidades.":null,fuente.get("subtotal"),fuente.get("costo_entrega"),fuente.get("total"),json.writeValueAsString(oferta));
        int orden=0;
        for(var i:nuevos) {
            long item=jdbc.queryForObject("INSERT INTO lamontana.cotizacion_item(codigo_publico,id_cotizacion,orden,id_formato,id_papel,cantidad_paginas_declaradas,cantidad_copias,cantidad_carillas,cantidad_hojas,modo_color,doble_faz,nombre_archivo_declarado,bytes_declarados,sha256_declarado,subtotal) SELECT ?,?,?,id_formato,id_papel,cantidad_paginas_declaradas,cantidad_copias,cantidad_carillas,cantidad_hojas,modo_color,doble_faz,nombre_archivo_declarado,bytes_declarados,sha256_declarado,subtotal FROM lamontana.cotizacion_item WHERE id_cotizacion=? AND orden=? RETURNING id_cotizacion_item",Long.class,i.codigoPublico(),id,++orden,fuente.get("id_cotizacion"),orden);
            jdbc.update("INSERT INTO lamontana.cotizacion_item_servicio SELECT ?,s.id_servicio,s.tipo_servicio,s.nombre_visible,s.base_precio,s.cantidad,s.precio_unitario,s.subtotal FROM lamontana.cotizacion_item_servicio s JOIN lamontana.cotizacion_item ci USING(id_cotizacion_item) WHERE ci.id_cotizacion=? AND ci.orden=?",item,fuente.get("id_cotizacion"),orden);
        }
        jdbc.update("INSERT INTO lamontana.evento_cotizacion(id_cotizacion,id_actor,id_operacion,tipo,huella,fecha) VALUES (?,?,?,'COTIZAR',?,?)",id,fuente.get("id_usuario_creador"),UUID.randomUUID(),HUELLA,ts(fecha));
        if(estado.equals("CANCELADA"))jdbc.update("INSERT INTO lamontana.evento_cotizacion(id_cotizacion,id_actor,id_operacion,tipo,huella,fecha,motivo) VALUES (?,?,?,'CANCELAR_OFERTA',?,?,'El cliente eligió otras cantidades.')",id,fuente.get("id_usuario_creador"),UUID.randomUUID(),HUELLA,ts(fecha.plusSeconds(600)));
    }
    private void eventoCotizacion(Pedido p,String tipo,Instant fecha) {
        jdbc.update("INSERT INTO lamontana.evento_cotizacion(id_cotizacion,id_actor,id_operacion,tipo,huella,fecha) VALUES (?,?,?,?,?,?)",p.cotizacion,p.cliente.id(),UUID.randomUUID(),tipo,HUELLA,ts(fecha));
    }
    private void eventoCobro(Pedido p,String tipo,Instant fecha,Actor actor) {
        jdbc.update("INSERT INTO lamontana.evento_cobro_pedido(id_pedido,id_operacion,id_actor,tipo,motivo,resultado,fecha) VALUES (?,?,?,?,?,to_jsonb((SELECT c FROM lamontana.cobro_pedido c WHERE id_pedido=?)),?)",p.id,UUID.randomUUID(),actor.id(),tipo,"Movimiento financiero simulado: "+tipo,p.id,ts(fecha));
    }
    private Actor actor(String correo){return jdbc.queryForObject("SELECT u.id_usuario,u.nombre||' '||u.apellido,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE correo=?",(r,n)->new Actor(r.getLong(1),r.getString(2),r.getString(3)),correo);}
    private static Timestamp ts(Instant fecha){return Timestamp.from(fecha);}
    private ArchivosPrivados.Recibido pdf(UUID id,int paginas,String titulo) {
        try(var doc=new PDDocument();var output=new ByteArrayOutputStream()) {
            for(int i=1;i<=paginas;i++) {
                var pagina=new PDPage();doc.addPage(pagina);
                try(var texto=new PDPageContentStream(doc,pagina)) {
                    texto.beginText();texto.setFont(FUENTE,14);texto.newLineAtOffset(50,740);
                    texto.showText("LA MONTANA - DOCUMENTO FICTICIO DE PRUEBA");texto.newLineAtOffset(0,-35);texto.showText(titulo);texto.newLineAtOffset(0,-25);texto.showText("Pagina "+i+" de "+paginas+". Sin datos personales reales.");texto.endText();
                }
            }
            doc.save(output);var recibido=archivos.recibir(id,new ByteArrayInputStream(output.toByteArray()));archivos.publicar(id);return recibido;
        } catch(IOException ex){throw new IllegalStateException("No se pudo generar el PDF de prueba",ex);}
    }
}
