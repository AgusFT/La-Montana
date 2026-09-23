//#region ENCABEZADO · GuiaInstalacion.java
/*
 * ========================================================================
 * FUNCIÓN: Construye el recorrido inicial en orden a partir de mínimos guardados
 * y comprobados. Una configuración activa libera la navegación posterior.
 * CONSTRUCTOR: GuiaInstalacion(), privado (utilidad sin instancias).
 * MÉTODOS: crear(...) calcula etapas, disponibilidad y fase operativa accesible.
 * TIPOS: GuiaInstalacion, Etapa, Estado (records de sólo lectura).
 * ========================================================================
 */
//#endregion
package ar.com.lamontana.configuracion;

import java.util.ArrayList;
import java.util.List;

public final class GuiaInstalacion {
    private GuiaInstalacion() {}
    public record Etapa(String codigo,String titulo,boolean completa,boolean habilitada,String enlace) {}
    public record Estado(boolean primeraInstalacion,int faseDisponible,Long versionBorrador,List<Etapa> etapas,List<String> pendientesFase) {}

    public static Estado crear(boolean activa,boolean sucursal,boolean papeles,boolean servicios,boolean precios,
                               boolean borrador,boolean modelo,boolean pagos,boolean recursos,boolean entrega,
                               Long version,List<String> pendientes) {
        String[] codigos={"sucursal","papeles","servicios","precios","modelo","pagos","recursos","entrega","activacion"};
        String[] titulos={"Crear una sucursal","Elegir papeles","Crear servicios base","Guardar tarifas y servicios",
                "Guardar modelo operativo","Guardar pagos y reglas","Configurar impresoras y asignación","Completar horarios y entrega","Revisar y activar"};
        String[] enlaces={"/administracion/sucursales","/administracion/catalogo#catalogo-base","/administracion/catalogo#catalogo-base",
                "/administracion/catalogo#revision-comercial","/administracion/configuracion","/administracion/configuracion",
                "/administracion/configuracion","/administracion/configuracion","/administracion/configuracion"};
        boolean[] minimos={sucursal,papeles,servicios,precios,modelo,pagos,recursos,entrega,activa};
        var etapas=new ArrayList<Etapa>();boolean anteriores=true;
        for(int i=0;i<codigos.length;i++) {
            boolean completa=activa||anteriores&&minimos[i];
            etapas.add(new Etapa(codigos[i],titulos[i],completa,activa||anteriores,enlaces[i]));
            anteriores=completa;
        }
        int fase=activa?8:!sucursal||!papeles||!servicios||!precios||!borrador?1:!modelo?2:!pagos?3:!recursos?4:!entrega?5:6;
        return new Estado(!activa,fase,version,List.copyOf(etapas),List.copyOf(pendientes));
    }
}
