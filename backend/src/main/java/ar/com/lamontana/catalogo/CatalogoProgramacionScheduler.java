//#region ENCABEZADO · CatalogoProgramacionScheduler.java
/*
 * ========================================================================
 * ARCHIVO: CatalogoProgramacionScheduler.java
 * ========================================================================
 * FUNCIÓN
 * Ejecuta periódicamente la reconciliación del catálogo para aplicar las configuraciones
 * comerciales cuya fecha programada ya llegó.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] CatalogoProgramacionScheduler(CatalogoService catalogo, VigenciaDemo vigencia)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] void reconciliar()
 *   Tarea periódica invocada por Spring.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CatalogoProgramacionScheduler (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.catalogo;

import ar.com.lamontana.instalacion.VigenciaDemo;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods=false)
@EnableScheduling
public class CatalogoProgramacionScheduler {
    private final VigenciaDemo vigencia;
    private final CatalogoService catalogo;
    public CatalogoProgramacionScheduler(CatalogoService catalogo, VigenciaDemo vigencia) { this.catalogo=catalogo; this.vigencia = vigencia; }

    // Se invoca el proxy transaccional del servicio; una falla deja pendiente la programación.
    @Scheduled(fixedDelayString="${lamontana.catalogo.reconciliacion-intervalo-ms:5000}",
               initialDelayString="${lamontana.catalogo.reconciliacion-inicio-ms:5000}")
    public void reconciliar() { if (vigencia.operativa()) catalogo.reconciliarProgramaciones(); }
}
