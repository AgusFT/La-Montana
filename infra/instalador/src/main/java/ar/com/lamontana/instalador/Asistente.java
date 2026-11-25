package ar.com.lamontana.instalador;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

final class Asistente implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService http=Executors.newFixedThreadPool(4);
    private final String token=Util.secreto();
    private final Paquete paquete;
    private final ExecutorService tareas=Executors.newSingleThreadExecutor();
    private volatile Map<String,Object> trabajo=Map.of("estado","LIBRE");
    private final Equipo equipo;
    private final Carpetas carpetas;
    private Instalacion instalacion;
    Asistente(Paquete paquete) throws IOException {this.paquete=paquete;this.equipo=new Equipo(paquete);this.carpetas=new Carpetas(equipo);server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),8);server.setExecutor(http);server.createContext("/",this::atender);}
    void start(){server.start();}
    String token(){return token;}
    String origen(){return "http://127.0.0.1:"+server.getAddress().getPort();}
    @Override public void close(){server.stop(0);carpetas.close();http.shutdownNow();tareas.shutdownNow();if(instalacion!=null)instalacion.cerrar();}
    private void atender(HttpExchange x) throws IOException {
        try{
            String host=x.getRequestHeaders().getFirst("Host"),origin=x.getRequestHeaders().getFirst("Origin");
            if(!("127.0.0.1:"+server.getAddress().getPort()).equals(host)||origin!=null&&!origin.equals(origen())){respuesta(x,403,Map.of("error","Origen no autorizado."));return;}
            String path=x.getRequestURI().getPath();
            if(path.startsWith("/api/")){
                String auth=x.getRequestHeaders().getFirst("Authorization");if(auth==null||!MessageDigest.isEqual(("Bearer "+token).getBytes(StandardCharsets.UTF_8),auth.getBytes(StandardCharsets.UTF_8))){respuesta(x,401,Map.of("error","Abrí el asistente desde su acceso local."));return;}
                if(x.getRequestMethod().equals("POST")&&!origen().equals(origin)){respuesta(x,403,Map.of("error","Falta el origen local."));return;}
                if(path.equals("/api/carpetas/estado")&&x.getRequestMethod().equals("GET")){respuesta(x,200,carpetas.estado());return;}
                if(path.equals("/api/carpetas/cancelar")&&x.getRequestMethod().equals("POST")){formulario(x);carpetas.cancelar();respuesta(x,200,Map.of("mensaje","Cancelación solicitada."));return;}
                if((path.equals("/api/carpetas/examinar")||path.equals("/api/carpetas/buscar"))&&x.getRequestMethod().equals("POST")){
                    var v=formulario(x);synchronized(this){verificarLibre();if(path.endsWith("examinar"))carpetas.examinar(v.getOrDefault("directorio",""));else carpetas.buscar(v.getOrDefault("raiz",""));}
                    respuesta(x,202,carpetas.estado());return;
                }
                if(path.equals("/api/seleccionar")&&x.getRequestMethod().equals("POST")){var v=formulario(x);synchronized(this){verificarLibre();var dir=Carpetas.ruta(Util.requerido(v,"directorio"));if(instalacion==null||!instalacion.raiz.equals(dir)){if(instalacion!=null)instalacion.cerrar();instalacion=new Instalacion(dir,equipo);}respuesta(x,200,instalacion.resumen());}return;}
                if(path.equals("/api/instalacion")&&x.getRequestMethod().equals("GET")){if(instalacion==null)throw new IllegalArgumentException("Seleccioná una carpeta de instalación.");respuesta(x,200,instalacion.resumen());return;}
                if(path.equals("/api/mantener")&&x.getRequestMethod().equals("POST")){var v=formulario(x);Instalacion i=seleccionada();iniciarTrabajo(()->i.mantener(Util.requerido(v,"operacion"),java.nio.file.Path.of(Util.requerido(v,"respaldo"))));respuesta(x,202,trabajo);return;}
                if(path.equals("/api/instalar")&&x.getRequestMethod().equals("POST")){var v=formulario(x);Instalacion i=seleccionada();iniciarTrabajo(()->i.instalar(v));respuesta(x,202,trabajo);return;}
                if(path.equals("/api/sistema")&&x.getRequestMethod().equals("POST")){var v=formulario(x);Instalacion i=seleccionada();String a=Util.requerido(v,"accion");iniciarTrabajo(i,()->{if(a.equals("verificarArranque"))i.verificarArranque();else i.sistema(a);});respuesta(x,202,trabajo);return;}
                if(path.equals("/api/publico/configurar")&&x.getRequestMethod().equals("POST")){var v=formulario(x);Instalacion i=seleccionada();iniciarTrabajo(i,()->i.configurarPublicacion(v));respuesta(x,202,trabajo);return;}
                if(path.equals("/api/publico/iniciar")&&x.getRequestMethod().equals("POST")){Instalacion i=seleccionada();iniciarTrabajo(i,i::iniciarPruebaPublica);respuesta(x,202,trabajo);return;}
                if(path.equals("/api/publico/confirmar")&&x.getRequestMethod().equals("POST")){var v=formulario(x);Instalacion i=seleccionada();iniciarTrabajo(i,()->i.confirmarPublico(Util.requerido(v,"codigo")));respuesta(x,202,trabajo);return;}
                if(path.equals("/api/correo/enviar")&&x.getRequestMethod().equals("POST")){var v=formulario(x);Instalacion i=seleccionada();iniciarTrabajo(i,()->i.probarCorreo(Util.requerido(v,"destino")));respuesta(x,202,trabajo);return;}
                if(path.equals("/api/correo/confirmar")&&x.getRequestMethod().equals("POST")){var v=formulario(x);Instalacion i=seleccionada();iniciarTrabajo(i,()->i.confirmarCorreo(Util.requerido(v,"codigo")));respuesta(x,202,trabajo);return;}
                if(path.equals("/api/dependencias")&&x.getRequestMethod().equals("GET")){respuesta(x,200,Map.of("dependencias",equipo.diagnostico()));return;}
                if(path.equals("/api/trabajo")&&x.getRequestMethod().equals("GET")){respuesta(x,200,trabajo);return;}
                if(path.equals("/api/dependencia")&&x.getRequestMethod().equals("POST")){var v=formulario(x);iniciarTrabajo(()->equipo.dependencia(Util.requerido(v,"accion"),Util.requerido(v,"via")));respuesta(x,202,trabajo);return;}
                if(path.equals("/api/estado")&&x.getRequestMethod().equals("GET")){respuesta(x,200,Map.of("version",paquete.version(),"versionInstalador",paquete.manifiesto.getProperty("installer.version","sin-paquete"),"sistema",System.getProperty("os.name"),"directorioRecomendado",Carpetas.recomendada().toString(),"mensaje","Asistente local listo para verificar dependencias."));return;}
                if(path.equals("/api/cerrar")&&x.getRequestMethod().equals("POST")){if("EJECUTANDO".equals(trabajo.get("estado")))throw new IllegalArgumentException("Esperá a que termine la operación antes de cerrar.");respuesta(x,200,Map.of("mensaje","Asistente cerrado. La app continúa funcionando."));new Thread(this::close).start();return;}
                respuesta(x,404,Map.of("error","Operación desconocida."));return;
            }
            if(!x.getRequestMethod().equals("GET")){respuesta(x,405,Map.of("error","Método no permitido."));return;}
            String resource=switch(path){case "/"->"index.html";case "/app.js"->"app.js";case "/style.css"->"style.css";case "/yg-soluciones.svg"->"yg-soluciones.svg";default->null;};
            if(resource==null){respuesta(x,404,Map.of("error","Recurso no encontrado."));return;}
            try(var in=getClass().getResourceAsStream("/web/"+resource)){byte[] body=Objects.requireNonNull(in).readAllBytes();cabeceras(x,resource.endsWith(".js")?"text/javascript":resource.endsWith(".css")?"text/css":resource.endsWith(".svg")?"image/svg+xml":"text/html");x.sendResponseHeaders(200,body.length);x.getResponseBody().write(body);}
        }catch(IllegalArgumentException e){respuesta(x,400,Map.of("error",e.getMessage()));}catch(Exception e){respuesta(x,500,Map.of("error","No se pudo completar la operación. Revisá los recursos y permisos locales."));}finally{x.close();}
    }
    Instalacion seleccionada(){if(instalacion==null)throw new IllegalArgumentException("Seleccioná una carpeta.");return instalacion;}
    interface Tarea {void ejecutar() throws Exception;}
    void iniciarTrabajo(Instalacion app,Tarea tarea){iniciarTrabajo(()->app.modificar(tarea));}
    synchronized void verificarLibre(){if("EJECUTANDO".equals(trabajo.get("estado"))||carpetas.ocupado())throw new IllegalArgumentException("Esperá a que termine la operación o cancelá la búsqueda/selección de carpeta.");}
    synchronized void iniciarTrabajo(Tarea tarea){verificarLibre();trabajo=Map.of("estado","EJECUTANDO","mensaje","Operación en curso. No apagues el equipo.");tareas.submit(()->{try{tarea.ejecutar();trabajo=Map.of("estado","COMPLETADO","mensaje","Operación completada. Volvé a verificar el estado.");}catch(Exception e){trabajo=Map.of("estado","ERROR","mensaje",Objects.toString(e.getMessage(),"No se pudo completar la operación."));}});}
    static Map<String,String> formulario(HttpExchange x) throws IOException {if(!"application/x-www-form-urlencoded;charset=UTF-8".equalsIgnoreCase(x.getRequestHeaders().getFirst("Content-Type")))throw new IllegalArgumentException("Formato de solicitud no válido.");byte[] b=x.getRequestBody().readNBytes(65537);if(b.length>65536)throw new IllegalArgumentException("Solicitud demasiado grande.");return Util.form(new String(b,StandardCharsets.UTF_8));}
    private void cabeceras(HttpExchange x,String tipo){var h=x.getResponseHeaders();h.set("Content-Type",tipo+";charset=UTF-8");h.set("Cache-Control","no-store");h.set("X-Content-Type-Options","nosniff");h.set("Referrer-Policy","no-referrer");h.set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");}
    private void respuesta(HttpExchange x,int status,Object value) throws IOException {byte[] b=Util.json(value).getBytes(StandardCharsets.UTF_8);cabeceras(x,"application/json");x.sendResponseHeaders(status,b.length);x.getResponseBody().write(b);}
}
