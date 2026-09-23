//#region ENCABEZADO · DisponibilidadPuntoController.java
/*
 * ========================================================================
 * ARCHIVO: DisponibilidadPuntoController.java
 * ========================================================================
 * FUNCIÓN
 * Expone la consulta y modificación de la disponibilidad temporal de los puntos de entrega,
 * separada de sus parámetros versionados.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] DisponibilidadPuntoController(DisponibilidadPuntoService disponibilidad)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] DisponibilidadPuntoService.Panel listar(Principal actor)
 *   Entrada HTTP GET sobre la ruta del controlador.
 * - [public] DisponibilidadPuntoService.Disponibilidad cambiar(UUID punto, Cambiar input,
 *   Principal actor)
 *   Entrada HTTP PUT · ruta del método: /{punto}.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - DisponibilidadPuntoController (clase).
 * - DisponibilidadPuntoController.Estado (enumeración).
 * - DisponibilidadPuntoController.Cambiar (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/puntos-entrega/disponibilidad")
public class DisponibilidadPuntoController {
    private final DisponibilidadPuntoService disponibilidad;
    public DisponibilidadPuntoController(DisponibilidadPuntoService disponibilidad){this.disponibilidad=disponibilidad;}
    @GetMapping public DisponibilidadPuntoService.Panel listar(Principal actor){return disponibilidad.listar(actor.getName());}
    @PutMapping("/{punto}") public DisponibilidadPuntoService.Disponibilidad cambiar(@PathVariable UUID punto,@Valid @RequestBody Cambiar input,Principal actor){return disponibilidad.cambiar(punto,input,actor.getName());}
    public enum Estado { HABILITADO,DESHABILITADO }
    public record Cambiar(@NotNull UUID operacion,@NotNull @PositiveOrZero Long version,@NotNull Estado estado){}
}
