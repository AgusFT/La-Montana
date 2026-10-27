//#region Política certificada · plazos comerciales separados de retención técnica
package ar.com.lamontana.configuracion;

import java.time.Instant;
import java.time.Duration;
import java.util.Set;
import jakarta.validation.constraints.*;

/** Valores versionados; nunca concede autorización de impresión. */
public record PoliticaPagoArchivos(@NotNull @Min(1) @Max(43200) Integer plazoMinutos,
        @Min(1) @Max(43199) Integer recordatorioMinutos,@NotNull Integer retencionHoras) {
    public static final int RETENCION_RECOMENDADA=72;
    public boolean valida(){return plazoMinutos!=null&&plazoMinutos>=1&&plazoMinutos<=43200
        &&retencionHoras!=null&&Set.of(24,48,72).contains(retencionHoras)
        &&(recordatorioMinutos==null||recordatorioMinutos>0&&recordatorioMinutos<plazoMinutos);}
    public Instant limite(Instant creacion){return creacion.plus(Duration.ofMinutes(plazoMinutos));}
    public Instant retenerHasta(Instant limite){return limite.plus(Duration.ofHours(retencionHoras));}
}
//#endregion
