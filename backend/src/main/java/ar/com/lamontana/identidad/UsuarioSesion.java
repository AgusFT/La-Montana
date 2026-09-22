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
