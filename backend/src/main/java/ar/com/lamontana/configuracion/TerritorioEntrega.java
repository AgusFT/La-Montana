//#region ENCABEZADO · TerritorioEntrega.java
/*
 * ========================================================================
 * ARCHIVO: TerritorioEntrega.java
 * ========================================================================
 * FUNCIÓN
 * Representa una cobertura por código postal, localidad y provincia y normaliza esos datos para
 * compararlos y validarlos de forma consistente.
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] TerritorioEntrega normalizado()
 * - [private, static] String normalizar(String s)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - TerritorioEntrega (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import java.text.Normalizer;
import java.util.Locale;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Coincidencia territorial exacta normalizada, sin rangos ni geocodificación. */
public record TerritorioEntrega(@NotBlank @Size(max=24) String codigoPostal,@NotBlank @Size(max=120) String localidad,@NotBlank @Size(max=120) String provincia) {
    public TerritorioEntrega normalizado(){
        String cp=normalizar(codigoPostal).replace(" ",""),l=normalizar(localidad),p=normalizar(provincia);
        if(!cp.matches("[A-Z0-9]{1,12}")||l.isEmpty()||l.length()>120||p.isEmpty()||p.length()>120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Completá código postal alfanumérico, localidad y provincia de la cobertura.");
        return new TerritorioEntrega(cp,l,p);
    }
    private static String normalizar(String s){return s==null?"":Normalizer.normalize(s,Normalizer.Form.NFKC).strip().replaceAll("(?U)\\s+"," ").toUpperCase(Locale.ROOT);}
}
