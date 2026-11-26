//#region ENCABEZADO · ErroresIdentidad.java
/*
 * ========================================================================
 * ARCHIVO: ErroresIdentidad.java
 * ========================================================================
 * FUNCIÓN
 * Traduce errores de formato, validación, conflicto, correo y estado HTTP en respuestas JSON
 * controladas para los clientes de la API.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] ResponseEntity<Map<String, String>>
 *   formato(org.springframework.http.converter.HttpMessageNotReadableException ex)
 *   Convierte la excepción indicada en una respuesta HTTP controlada.
 * - [paquete] ResponseEntity<Map<String, String>>
 *   conflicto(org.springframework.dao.DataIntegrityViolationException ex)
 *   Convierte la excepción indicada en una respuesta HTTP controlada.
 * - [paquete] ResponseEntity<Map<String, String>> correo(CorreoService.CorreoNoDisponible ex)
 *   Convierte la excepción indicada en una respuesta HTTP controlada.
 * - [paquete] ResponseEntity<Map<String, String>> estado(ResponseStatusException ex)
 *   Convierte la excepción indicada en una respuesta HTTP controlada.
 * - [paquete] ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException ex)
 *   Convierte la excepción indicada en una respuesta HTTP controlada.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - ErroresIdentidad (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.identidad;

import java.sql.SQLException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErroresIdentidad {
    private static final Map<String, String> MENSAJES_CUPO = Map.of(
            "23D01", "La Demo admite hasta 20 pedidos.",
            "23D02", "La Demo admite hasta 10 clientes.",
            "23D03", "La Demo admite hasta 30 movimientos de caja entre cobros y devoluciones.",
            "23D04", "La Demo admite hasta 2 impresoras simuladas.");
    private static final Map<String, String> MENSAJES_BAJA = Map.of(
            "23D11", "El pedido está dado de baja en la Demo. Elegí un pedido activo para iniciar una impresión.",
            "23D12", "Esta ficha de cliente está dada de baja en la Demo. Podés consultar su historial.",
            "23D14", "La impresora está dada de baja en la Demo. Elegí otra para iniciar la impresión.",
            "23D15", "La ficha de Lucía Papel está reservada para el selector de Cliente. Elegí otra ficha para liberar un lugar.");

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<Map<String,String>> formato(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Revisá el formato y los valores de los datos enviados."));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> conflicto(org.springframework.dao.DataIntegrityViolationException ex) {
        if (ex.getMostSpecificCause() instanceof SQLException sql && sql.getSQLState() != null) {
            if ("23D16".equals(sql.getSQLState())) {
                return ResponseEntity.status(409).body(Map.of("codigo", "MODULO_DEMO",
                        "mensaje", ar.com.lamontana.configuracion.PuntosRepositorio.EXCLUSION_DEMO));
            }
            String mensaje = MENSAJES_CUPO.get(sql.getSQLState());
            if (mensaje != null) {
                return ResponseEntity.status(409).body(Map.of("codigo", "CUPO_DEMO",
                        "mensaje", mensaje + " Liberá un lugar desde los cupos de la Demo."));
            }
            String baja = MENSAJES_BAJA.get(sql.getSQLState());
            if (baja != null) {
                return ResponseEntity.status(409).body(Map.of("codigo", "BAJA_DEMO", "mensaje", baja));
            }
        }
        return ResponseEntity.status(409).body(Map.of("mensaje", "Los datos entran en conflicto con un registro existente. Revisá el correo o el código."));
    }
    @ExceptionHandler(CorreoService.CorreoNoDisponible.class)
    ResponseEntity<Map<String,String>> correo(CorreoService.CorreoNoDisponible ex) {
        return ResponseEntity.status(503).body(Map.of("mensaje", "No pudimos enviar el correo. Volvé a intentar cuando el servicio de correo esté disponible."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> estado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("mensaje", ex.getReason() == null ? "No se pudo completar la operación." : ex.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException ex) {
        String campos = ex.getBindingResult().getFieldErrors().stream().map(error -> error.getField()).distinct().sorted().collect(java.util.stream.Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Revisá los campos: " + campos + "."));
    }
}
