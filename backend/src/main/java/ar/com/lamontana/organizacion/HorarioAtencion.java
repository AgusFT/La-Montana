//#region ENCABEZADO · HorarioAtencion.java
/*
 * ========================================================================
 * ARCHIVO: HorarioAtencion.java
 * ========================================================================
 * FUNCIÓN
 * Representa y valida la semana de atención de una sucursal: días únicos, habilitación explícita y
 * horas de apertura y cierre coherentes.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [private] HorarioAtencion()
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public, static] void validar(List<Dia> dias)
 * - [private, static] boolean hora(String value)
 * - [private, static] void exigir(boolean condicion, String mensaje)
 *   Rechaza la operación si no se cumple la condición indicada.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - HorarioAtencion (clase).
 * - HorarioAtencion.Dia (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.organizacion;

import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class HorarioAtencion {
    private HorarioAtencion() {}
    public record Dia(@NotNull @Min(1) @Max(7) Integer dia, @NotNull Boolean habilitado,
        @Pattern(regexp="(?:[01][0-9]|2[0-3]):[0-5][0-9]") String apertura,
        @Pattern(regexp="(?:[01][0-9]|2[0-3]):[0-5][0-9]") String cierre) {}

    public static void validar(List<Dia> dias) {
        if (dias == null) return; // Contrato v6: no modifica el horario existente.
        exigir(dias.size() == 7, "Completá los siete días de atención de la sucursal.");
        var vistos = new HashSet<Integer>();
        for (var dia : dias) {
            exigir(dia != null && dia.dia() != null && dia.dia() >= 1 && dia.dia() <= 7 && vistos.add(dia.dia()),
                "Los días de atención deben ser únicos, de lunes a domingo.");
            exigir(dia.habilitado() != null, "Indicá si cada día está abierto o cerrado.");
            if (Boolean.TRUE.equals(dia.habilitado())) {
                exigir(hora(dia.apertura()) && hora(dia.cierre()), "Cada día abierto necesita una hora Desde y una hora Hasta.");
                exigir(dia.apertura().compareTo(dia.cierre()) < 0, "La hora Desde debe ser anterior a Hasta dentro del mismo día.");
            } else exigir(dia.apertura() == null && dia.cierre() == null, "Los días cerrados no deben tener horas de atención.");
        }
        exigir(dias.stream().anyMatch(d -> Boolean.TRUE.equals(d.habilitado())), "Seleccioná al menos un día de atención.");
    }
    private static boolean hora(String value) { return value != null && value.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"); }
    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
