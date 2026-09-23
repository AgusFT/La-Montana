//#region ENCABEZADO · UbicacionSucursal.java
/*
 * ========================================================================
 * ARCHIVO: UbicacionSucursal.java
 * ========================================================================
 * FUNCIÓN
 * Define provincias argentinas y sus alias para resolver la zona horaria sin conexión. Normaliza
 * la ubicación y devuelve su desfase horario para la interfaz.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [private] UbicacionSucursal()
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [private, static] Provincia provincia(String nombre, String ciudad, String[] alias)
 * - [private, static] String normalizar(String valor)
 * - [public, static] String zona(String provincia, String zonaAnterior, boolean contratoAnterior)
 * - [public, static] String desfase(String zona)
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - UbicacionSucursal (clase).
 * - UbicacionSucursal.Provincia (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.organizacion;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Catálogo local de Argentina. No requiere geolocalización ni consultas externas.
 * Fuente: https://data.iana.org/time-zones/tzdb/zone1970.tab (AR).
 * Conserva IANA internamente: el desplazamiento visible no reemplaza las reglas civiles.
 */
public final class UbicacionSucursal {
    private UbicacionSucursal() {}
    public record Provincia(String nombre, String zonaHoraria, List<String> alias) {}
    private static Provincia provincia(String nombre, String ciudad, String... alias) {
        return new Provincia(nombre, "America/Argentina/" + ciudad, List.of(alias));
    }
    public static final List<Provincia> PROVINCIAS = List.of(
        provincia("Ciudad Autónoma de Buenos Aires", "Buenos_Aires", "CABA", "C.A.B.A.", "Capital Federal", "Ciudad de Buenos Aires"),
        provincia("Buenos Aires", "Buenos_Aires", "Provincia de Buenos Aires", "Bs. As.", "PBA"),
        provincia("Catamarca", "Catamarca"), provincia("Chaco", "Cordoba"),
        provincia("Chubut", "Catamarca"), provincia("Córdoba", "Cordoba"),
        provincia("Corrientes", "Cordoba"), provincia("Entre Ríos", "Cordoba"),
        provincia("Formosa", "Cordoba"), provincia("Jujuy", "Jujuy"),
        provincia("La Pampa", "Salta"), provincia("La Rioja", "La_Rioja"),
        provincia("Mendoza", "Mendoza"), provincia("Misiones", "Cordoba"),
        provincia("Neuquén", "Salta"), provincia("Río Negro", "Salta"),
        provincia("Salta", "Salta"), provincia("San Juan", "San_Juan"),
        provincia("San Luis", "San_Luis"), provincia("Santa Cruz", "Rio_Gallegos"),
        provincia("Santa Fe", "Cordoba"), provincia("Santiago del Estero", "Cordoba"),
        provincia("Tierra del Fuego", "Ushuaia", "Tierra del Fuego, Antártida e Islas del Atlántico Sur"),
        provincia("Tucumán", "Tucuman")
    );
    private static String normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    public static String zona(String provincia, String zonaAnterior, boolean contratoAnterior) {
        String clave = normalizar(provincia);
        var encontrada = PROVINCIAS.stream().filter(p -> normalizar(p.nombre()).equals(clave)
            || p.alias().stream().anyMatch(a -> normalizar(a).equals(clave))).findFirst();
        if (encontrada.isPresent()) return encontrada.get().zonaHoraria();
        // Compatibilidad de la API v6 para integraciones anteriores sin horario de alta.
        // El formulario nuevo siempre envía horarioAtencion y exige provincia reconocida.
        if (contratoAnterior && zonaAnterior != null && ZoneId.getAvailableZoneIds().contains(zonaAnterior)) return zonaAnterior;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Seleccioná una provincia de Argentina o Ciudad Autónoma de Buenos Aires para determinar la zona horaria.");
    }
    public static String desfase(String zona) {
        String offset = ZoneId.of(zona).getRules().getOffset(Instant.now()).getId();
        return "GMT" + (offset.equals("Z") ? "+00:00" : offset);
    }
}
