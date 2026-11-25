//#region ENCABEZADO · LaMontanaApplication.java
/*
 * ========================================================================
 * ARCHIVO: LaMontanaApplication.java
 * ========================================================================
 * FUNCIÓN
 * Inicia la aplicación Spring Boot y su autoconfiguración, con detección de los componentes
 * del backend.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public, static] void main(String[] args)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - LaMontanaApplication (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LaMontanaApplication {
    public static void main(String[] args) {
        SpringApplication.run(LaMontanaApplication.class, args);
    }
}
