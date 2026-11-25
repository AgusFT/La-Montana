//#region ENCABEZADO · ProgramacionConfiguracionScheduler.java
/*
 * ========================================================================
 * ARCHIVO: ProgramacionConfiguracionScheduler.java
 * ========================================================================
 * FUNCIÓN
 * Recupera al iniciar la aplicación y revisa periódicamente las activaciones operativas pendientes
 * mediante EjecucionProgramadaService.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] ProgramacionConfiguracionScheduler(EjecucionProgramadaService ejecucion)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] void recuperar()
 *   Responde al evento de inicio de la aplicación.
 * - [public] void ejecutar()
 *   Tarea periódica invocada por Spring.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ProgramacionConfiguracionScheduler (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="lamontana.configuracion.ejecucion-automatica",havingValue="true",matchIfMissing=true)
public class ProgramacionConfiguracionScheduler {
    private final EjecucionProgramadaService ejecucion;
    public ProgramacionConfiguracionScheduler(EjecucionProgramadaService ejecucion){this.ejecucion=ejecucion;}
    @EventListener(ApplicationReadyEvent.class) public void recuperar(){ejecucion.reconciliar(true);}
    @Scheduled(fixedDelayString="${lamontana.configuracion.reconciliacion-intervalo-ms:5000}",initialDelayString="${lamontana.configuracion.reconciliacion-inicio-ms:5000}")
    public void ejecutar(){ejecucion.reconciliar(false);}
}
