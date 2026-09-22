package ar.com.lamontana.identidad;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErroresIdentidad {
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<Map<String,String>> formato(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Revisá el formato y los valores de los datos enviados."));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> conflicto(org.springframework.dao.DataIntegrityViolationException ex) {
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
