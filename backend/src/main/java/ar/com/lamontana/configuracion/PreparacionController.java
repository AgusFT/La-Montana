//#region ENCABEZADO · PreparacionController.java
/*
 * ========================================================================
 * ARCHIVO: PreparacionController.java
 * ========================================================================
 * FUNCIÓN
 * Construye el mapa de preparación inicial del dashboard a partir del estado real del configurador
 * y del catálogo, señalando pendientes, bloqueos y pasos disponibles. Adjunta una guía de
 * primera instalación con desbloqueo secuencial y mínimos validados de cada fase.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] PreparacionController(ConfiguracionService configuracion,
 *   RevisionConfiguracionService revision, CatalogoService catalogo, JdbcTemplate jdbc)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Preparacion leer(Principal actor)
 *   Entrada HTTP GET · ruta del método: /api/admin/preparacion.
 * - [private, static] String detalle(List<RevisionConfiguracionService.Hallazgo> h, List<Integer>
 *   fases, String ayuda)
 * - [private, static] void agregar(List<Paso> pasos, String codigo, String titulo, boolean
 *   presente, List<RevisionConfiguracionService.Hallazgo> h, List<Integer> fases, String guardada,
 *   String ayuda)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PreparacionController (clase).
 * - PreparacionController.Paso (record).
 * - PreparacionController.Preparacion (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import ar.com.lamontana.catalogo.CatalogoService;
import ar.com.lamontana.catalogo.CatalogoController.TipoServicio;
import java.security.Principal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class PreparacionController {
    private final ConfiguracionService configuracion;
    private final RevisionConfiguracionService revision;
    private final CatalogoService catalogo;
    private final JdbcTemplate jdbc;
    public PreparacionController(ConfiguracionService configuracion,RevisionConfiguracionService revision,CatalogoService catalogo,JdbcTemplate jdbc){this.configuracion=configuracion;this.revision=revision;this.catalogo=catalogo;this.jdbc=jdbc;}
    public record Paso(String codigo,String titulo,boolean completo,String estado,String detalle,String enlace,boolean complementario){}
    public record Preparacion(String origen,Long numero,boolean activa,Long borradorPendiente,List<Paso> pasos,GuiaInstalacion.Estado guia){}
    @GetMapping("/api/admin/preparacion")
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Preparacion leer(Principal actor){
        var estado=configuracion.estado(actor.getName());
        var b=estado.activa()!=null?estado.activa().configuracion():estado.borrador()!=null?estado.borrador():estado.programada()!=null?estado.programada().configuracion():null;
        var r=b==null?null:revision.revisarPreparacion(b.codigoPublico(),actor.getName());
        var comercial=r==null?catalogo.leerEstado():r.catalogo();
        var bloqueos=r==null?List.<RevisionConfiguracionService.Hallazgo>of():r.hallazgos().stream().filter(h->h.nivel().equals("BLOQUEO")).toList();
        boolean activa=estado.activa()!=null;
        String origen=b==null?"SIN_CONFIGURACION":b.estado(),guardada=activa?"Configurada y activa":b!=null&&b.estado().equals("PROGRAMADA")?"Configurada y programada":"Configurada en borrador";
        boolean sucursal=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.sucursal WHERE estado='ACTIVA')",Boolean.class));
        boolean precios=comercial.actual()!=null&&comercial.actual().tarifas().stream().anyMatch(t->t.habilitada())&&comercial.actual().servicios().stream().anyMatch(s->s.habilitado()&&comercial.servicios().stream().anyMatch(m->m.codigoPublico().equals(s.servicio())&&m.tipo()==TipoServicio.IMPRESION));
        var pasos=new ArrayList<Paso>();
        pasos.add(new Paso("propietario","Crear propietario",true,"Completado","Las cuentas de clientes ya pueden registrarse. Los empleados los crea el propietario.","/cuenta/seguridad",false));
        pasos.add(new Paso("sucursal","Preparar sucursal",sucursal,sucursal?"Configurada":"Pendiente","Necesitás al menos una sucursal activa con sus datos y zona horaria.","/administracion/sucursales",false));
        boolean catalogoListo=precios&&bloqueos.stream().noneMatch(h->h.fase()==0);
        pasos.add(new Paso("catalogo","Servicios y precios",catalogoListo,catalogoListo?"Revisión comercial vigente":"Pendiente",detalle(bloqueos,List.of(0),"Cargá servicios, formatos y papeles y publicá sus tarifas. Los precios se administran por separado de la configuración operativa."),"/administracion/catalogo",false));
        agregar(pasos,"modelo","Modelo y pagos",b!=null&&b.modelo()!=null&&b.pagos()!=null,bloqueos,List.of(2,3),guardada,"Definí aprobación, medios de pago, vigencia y reglas de seña.");
        agregar(pasos,"recursos","Capacidades de producción",b!=null&&b.recursos().metodoAsignacion()!=null,bloqueos,List.of(4),guardada,"Registrá impresoras operativas y los servicios disponibles en cada sucursal.");
        agregar(pasos,"entrega","Horarios y entregas",b!=null,bloqueos,List.of(5),guardada,"Guardá tiempos, horarios, modalidades y sus franjas o destinos.");
        boolean lista=r!=null&&r.bloqueos()==0;
        pasos.add(new Paso("activacion","Revisar y activar",activa&&lista,activa?(lista?"Activa":"Activa · requiere atención"):"Pendiente",activa?(lista?"La versión vigente permite operar. Los nuevos borradores no la reemplazan hasta activarlos.":"Hay condiciones actuales que requieren revisión. Corregí los pasos señalados; los pedidos existentes conservan sus condiciones."):lista?"El contenido supera la revisión. Falta autorizar su activación en el paso 7.":"Completá los pasos anteriores, revisá y autorizá la activación. Guardar no habilita pedidos.","/administracion/configuracion",false));
        boolean empleados=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol) WHERE r.codigo='EMPLEADO' AND u.estado='ACTIVO')",Boolean.class));
        pasos.add(new Paso("empleados","Equipo de trabajo",empleados,empleados?"Cuentas creadas":"Opcional · sin empleados","Podés operar como propietario. Creá empleados y asigná sus sucursales y permisos cuando los necesites.","/administracion/empleados",true));
        boolean publicada=Boolean.TRUE.equals(jdbc.queryForObject("SELECT publicacion IS NOT NULL FROM lamontana.web_borrador WHERE unica",Boolean.class));
        boolean webPreparada=Boolean.TRUE.equals(jdbc.queryForObject("SELECT id_actor IS NOT NULL FROM lamontana.web_borrador WHERE unica",Boolean.class));
        pasos.add(new Paso("web","Página de la imprenta",publicada,publicada?"Publicada":webPreparada?"Borrador sin publicar":"Opcional · sin configurar","Prepará identidad, fichas e imágenes, revisá la vista previa y publicá expresamente. Guardar un borrador no cambia la página pública.","/administracion/pagina-web",true));
        boolean papelesListos=comercial.papelesHabilitados().stream().anyMatch(s->s.habilitado());
        boolean servicioBase=comercial.servicios().stream().anyMatch(s->s.tipo()==TipoServicio.IMPRESION);
        boolean modeloListo=b!=null&&b.modelo()!=null&&bloqueos.stream().noneMatch(h->h.fase()==2);
        boolean pagosListos=b!=null&&b.pagos()!=null&&bloqueos.stream().noneMatch(h->h.fase()==3);
        boolean recursosListos=b!=null&&b.recursos().metodoAsignacion()!=null&&!b.recursos().serviciosPorSucursal().isEmpty()
                &&bloqueos.stream().noneMatch(h->h.fase()==4||h.fase()==0);
        boolean entregaLista=b!=null&&!b.entrega().modalidades().isEmpty()&&bloqueos.stream().noneMatch(h->h.fase()==5)
                &&r.hallazgos().stream().noneMatch(h->h.codigo().equals("MODALIDAD_SIN_DESTINO"));
        var guia=GuiaInstalacion.crear(activa,sucursal,papelesListos,servicioBase,precios,b!=null,modeloListo,pagosListos,
                recursosListos,entregaLista,b==null?null:b.version(),List.of());
        var pendientes=new ArrayList<String>();
        int fase=guia.faseDisponible();
        bloqueos.stream().filter(h->h.fase()==fase||fase==4&&h.fase()==0).map(RevisionConfiguracionService.Hallazgo::mensaje).distinct().forEach(pendientes::add);
        if(fase==4&&b!=null&&b.recursos().serviciosPorSucursal().isEmpty())pendientes.add("Guardá la asignación y al menos un servicio de impresión por sucursal que vaya a operar.");
        if(fase==5&&r!=null)r.hallazgos().stream().filter(h->h.codigo().equals("MODALIDAD_SIN_DESTINO")).map(RevisionConfiguracionService.Hallazgo::mensaje).forEach(pendientes::add);
        guia=new GuiaInstalacion.Estado(guia.primeraInstalacion(),guia.faseDisponible(),guia.versionBorrador(),guia.etapas(),List.copyOf(pendientes));
        return new Preparacion(origen,b==null?null:b.numero(),activa,estado.borrador()==null?null:estado.borrador().numero(),List.copyOf(pasos),guia);
    }
    private static String detalle(List<RevisionConfiguracionService.Hallazgo> h,List<Integer> fases,String ayuda){var errores=h.stream().filter(x->fases.contains(x.fase())).map(RevisionConfiguracionService.Hallazgo::mensaje).distinct().limit(3).toList();return errores.isEmpty()?ayuda:String.join(" ",errores);}
    private static void agregar(List<Paso> pasos,String codigo,String titulo,boolean presente,List<RevisionConfiguracionService.Hallazgo> h,List<Integer> fases,String guardada,String ayuda){boolean listo=presente&&h.stream().noneMatch(x->fases.contains(x.fase()));pasos.add(new Paso(codigo,titulo,listo,listo?guardada:"Pendiente",detalle(h,fases,ayuda),"/administracion/configuracion",false));}
}
