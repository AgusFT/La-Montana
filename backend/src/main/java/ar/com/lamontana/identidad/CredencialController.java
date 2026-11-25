//#region ENCABEZADO · CredencialController.java
/*
 * ========================================================================
 * ARCHIVO: CredencialController.java
 * ========================================================================
 * FUNCIÓN
 * Expone verificación de correo, recuperación y cambio de contraseña, además del cierre de sesión
 * asociado al cambio de credenciales.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] CredencialController(CredencialService credenciales)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] Map<String, String> solicitarCorreo(Principal principal)
 *   Entrada HTTP POST · ruta del método: /api/auth/correo/solicitar.
 * - [public] Map<String, String> verificar(Principal principal, Codigo input)
 *   Entrada HTTP POST · ruta del método: /api/auth/correo/confirmar.
 * - [public] Map<String, String> solicitarRecuperacion(Solicitud input)
 *   Entrada HTTP POST · ruta del método: /api/auth/recuperacion/solicitar.
 * - [public] Map<String, String> recuperar(Recuperacion input, HttpServletRequest request)
 *   Entrada HTTP POST · ruta del método: /api/auth/recuperacion/confirmar.
 * - [public] Map<String, String> cambiar(Principal principal, CambioClave input,
 *   HttpServletRequest request)
 *   Entrada HTTP POST · ruta del método: /api/auth/contrasena.
 * - [private] void cerrarSesion(HttpServletRequest request)
 * - [public] Codigo :: String toString()
 * - [public] Recuperacion :: String toString()
 * - [public] CambioClave :: String toString()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CredencialController (clase).
 * - CredencialController.Codigo (record).
 * - CredencialController.Solicitud (record).
 * - CredencialController.Recuperacion (record).
 * - CredencialController.CambioClave (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.identidad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class CredencialController {
    private final CredencialService credenciales;
    public CredencialController(CredencialService credenciales) { this.credenciales=credenciales; }
    @PostMapping("/api/auth/correo/solicitar")
    public Map<String,String> solicitarCorreo(Principal principal) {
        credenciales.solicitarVerificacion(principal.getName());
        return Map.of("mensaje","Revisá tu correo para obtener el código de verificación.");
    }
    @PostMapping("/api/auth/correo/confirmar")
    public Map<String,String> verificar(Principal principal,@Valid @RequestBody Codigo input) {
        credenciales.confirmarVerificacion(principal.getName(),input.token());
        return Map.of("mensaje","Correo verificado.");
    }
    @PostMapping("/api/auth/recuperacion/solicitar")
    public Map<String,String> solicitarRecuperacion(@Valid @RequestBody Solicitud input) {
        try { credenciales.solicitarRecuperacion(input.correo()); }
        catch(CorreoService.CorreoNoDisponible ignored) { /* Respuesta uniforme para no revelar cuentas. */ }
        return Map.of("mensaje","Si la cuenta existe y está activa, recibirás un código. Esperá un minuto antes de solicitar otro.");
    }
    @PostMapping("/api/auth/recuperacion/confirmar")
    public Map<String,String> recuperar(@Valid @RequestBody Recuperacion input,HttpServletRequest request) {
        credenciales.recuperar(input.correo(),input.token(),input.nuevaContrasena());
        cerrarSesion(request);
        return Map.of("mensaje","Contraseña restablecida. Iniciá sesión con tu nueva contraseña.");
    }
    @PostMapping("/api/auth/contrasena")
    public Map<String,String> cambiar(Principal principal,@Valid @RequestBody CambioClave input,HttpServletRequest request) {
        credenciales.cambiarClave(principal.getName(),input.contrasenaActual(),input.nuevaContrasena());
        cerrarSesion(request);
        return Map.of("mensaje","Contraseña actualizada. Iniciá sesión nuevamente.");
    }
    private void cerrarSesion(HttpServletRequest request) {
        var session=request.getSession(false);if(session!=null) session.invalidate();
        SecurityContextHolder.clearContext();
    }
    public record Codigo(@NotBlank @Size(max=128) String token) { @Override public String toString(){return "Codigo[privado]";} }
    public record Solicitud(@NotBlank @Email @Size(max=254) String correo) {}
    public record Recuperacion(@NotBlank @Email @Size(max=254) String correo,@NotBlank @Size(max=128) String token,
                               @NotBlank @Size(min=12,max=128) String nuevaContrasena) { @Override public String toString(){return "Recuperacion[privada]";} }
    public record CambioClave(@NotBlank @Size(max=128) String contrasenaActual,@NotBlank @Size(min=12,max=128) String nuevaContrasena) {
        @Override public String toString(){return "CambioClave[privado]";}
    }
}
