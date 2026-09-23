//#region ENCABEZADO · UsuarioSesion.java
/*
 * ========================================================================
 * ARCHIVO: UsuarioSesion.java
 * ========================================================================
 * FUNCIÓN
 * Extiende el usuario de Spring Security con la versión de acceso guardada al autenticarse, para
 * detectar sesiones invalidadas por cambios posteriores.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] UsuarioSesion(String correo, String hash, String rol, boolean activo, long
 *   versionAcceso)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] long versionAcceso()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - UsuarioSesion (clase).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.identidad;

import java.io.Serial;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Conserva la generación de acceso leída junto a la contraseña durante el login. */
public final class UsuarioSesion extends User {
    @Serial private static final long serialVersionUID = 1L;
    private final long versionAcceso;

    public UsuarioSesion(String correo, String hash, String rol, boolean activo, long versionAcceso) {
        super(correo, hash, activo, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        this.versionAcceso = versionAcceso;
    }

    public long versionAcceso() { return versionAcceso; }
}
