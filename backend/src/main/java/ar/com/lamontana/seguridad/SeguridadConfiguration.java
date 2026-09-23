package ar.com.lamontana.seguridad;

import ar.com.lamontana.identidad.IdentidadService;
import java.util.Map;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SeguridadConfiguration {
    @Bean
    PasswordEncoder contrasenas() {
        String id = "pbkdf2@SpringSecurity_v5_8";
        return new DelegatingPasswordEncoder(id, Map.of(id, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, IdentidadService identidad, org.springframework.jdbc.core.JdbcTemplate jdbc) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/publico/pagina-web", "/api/sistema/estado", "/actuator/health", "/actuator/health/**", "/api/setup/estado", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/setup/propietario", "/api/auth/login", "/api/auth/registro", "/api/auth/recuperacion/solicitar", "/api/auth/recuperacion/confirmar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/contrasena", "/api/auth/correo/solicitar", "/api/auth/correo/confirmar").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/admin/pagina-web", "/api/admin/pagina-web/revision").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/pagina-web/borrador").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/pagina-web/publicar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/estado", "/api/admin/preparacion").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/sucursales").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/sucursales").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/sucursales/*", "/api/admin/empleados/*").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/empleados").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/empleados").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/catalogo", "/api/admin/catalogo/revisiones/*").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/catalogo/formatos", "/api/admin/catalogo/papeles", "/api/admin/catalogo/servicios", "/api/admin/catalogo/revisiones").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/catalogo/programaciones/*/cancelar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/puntos-entrega/disponibilidad").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/puntos-entrega/disponibilidad/*").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/configuracion/borradores/*/programacion/revision").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/programacion/activacion/solicitar", "/api/admin/configuracion/borradores/*/programacion/activacion/confirmar", "/api/admin/configuracion/borradores/*/programacion/activacion/revocar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/historial/*/base", "/api/admin/configuracion/borradores/*/revision/confirmar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/configuracion/historial", "/api/admin/configuracion/historial/*", "/api/admin/configuracion/historial/*/auditoria", "/api/admin/configuracion/historial/*/comparacion/*").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/configuracion/rollback/revision").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/rollback/solicitar", "/api/admin/configuracion/rollback/confirmar", "/api/admin/configuracion/rollback/revocar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/configuracion").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/configuracion/borradores/*/modelo").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/configuracion/borradores/*/pagos").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/pagos/simular").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/configuracion/borradores/*/recursos", "/api/admin/configuracion/borradores/*/impresoras/*").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/configuracion/borradores/*/entrega").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/configuracion/borradores/*/entrega/validacion").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/configuracion/borradores/*/revision").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/revision/simular").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/entrega/simular").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/entrega/puntos").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/configuracion/borradores/*/entrega/puntos/*").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/entrega/zonas").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/configuracion/borradores/*/entrega/zonas/*").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/impresoras", "/api/admin/configuracion/borradores/*/impresoras/*/estado", "/api/admin/configuracion/borradores/*/impresoras/*/retirar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/activacion/solicitar", "/api/admin/configuracion/borradores/*/activacion/confirmar", "/api/admin/configuracion/borradores/*/activacion/revocar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/programacion/solicitar", "/api/admin/configuracion/borradores/*/programacion/confirmar", "/api/admin/configuracion/borradores/*/programacion/revocar", "/api/admin/configuracion/borradores/*/programacion/cancelacion/solicitar", "/api/admin/configuracion/borradores/*/programacion/cancelacion/confirmar", "/api/admin/configuracion/borradores/*/programacion/cancelacion/revocar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/configuracion/borradores/*/cancelacion/solicitar", "/api/admin/configuracion/borradores/*/cancelacion/confirmar", "/api/admin/configuracion/borradores/*/cancelacion/revocar").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/contexto", "/api/operacion/sucursales/*", "/api/operacion/sucursales/*/archivos").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/cotizaciones/*/archivos", "/api/cliente/cotizaciones/*/archivos/*", "/api/cliente/cotizaciones/*/archivos/*/carga", "/api/cliente/cotizaciones/*/archivos/*/original", "/api/cliente/cotizaciones/*/archivos/*/paginas/*").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/cotizaciones/*/archivos", "/api/cliente/cotizaciones/*/archivos/*/aceptar").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/cliente/cotizaciones/*/archivos/*/contenido").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/cotizaciones/*/archivos", "/api/operacion/cotizaciones/*/archivos/*", "/api/operacion/cotizaciones/*/archivos/*/original", "/api/operacion/cotizaciones/*/archivos/*/paginas/*").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/cotizaciones/opciones", "/api/cliente/cotizaciones", "/api/cliente/cotizaciones/*").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/cotizaciones", "/api/cliente/cotizaciones/*/aceptar", "/api/cliente/cotizaciones/*/cancelar").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/cotizaciones/*/pagos").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/cotizaciones/*/pagos/informar", "/api/cliente/cotizaciones/*/pagos/descartar").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/cotizaciones/*/pagos", "/api/operacion/sucursales/*/pagos").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.POST, "/api/operacion/cotizaciones/*/pagos/recibir", "/api/operacion/cotizaciones/*/pagos/descartar", "/api/operacion/cotizaciones/*/pagos/aplicar", "/api/operacion/cotizaciones/*/pagos/devolver").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/cotizaciones/*/comprobantes", "/api/cliente/cotizaciones/*/comprobantes/*", "/api/cliente/cotizaciones/*/comprobantes/*/carga", "/api/cliente/cotizaciones/*/comprobantes/*/original", "/api/cliente/cotizaciones/*/comprobantes/*/paginas/*").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/cotizaciones/*/comprobantes").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/cliente/cotizaciones/*/comprobantes/*/contenido").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/cotizaciones/*/comprobantes", "/api/operacion/cotizaciones/*/comprobantes/*", "/api/operacion/cotizaciones/*/comprobantes/*/carga", "/api/operacion/cotizaciones/*/comprobantes/*/original", "/api/operacion/cotizaciones/*/comprobantes/*/paginas/*").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.POST, "/api/operacion/cotizaciones/*/comprobantes").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.PUT, "/api/operacion/cotizaciones/*/comprobantes/*/contenido").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/cotizaciones/*/confirmacion", "/api/cliente/pedidos", "/api/cliente/pedidos/*").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/cotizaciones/*/confirmacion").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/pedidos/*", "/api/operacion/sucursales/*/pedidos").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/pedidos/*/reprogramacion").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/pedidos/*/reprogramacion/responder").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/pedidos/*/reprogramacion", "/api/operacion/pedidos/*/reprogramacion/franjas").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.POST, "/api/operacion/pedidos/*/reprogramacion", "/api/operacion/pedidos/*/reprogramacion/retirar").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/pedidos/*/entrega").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/pedidos/*/entrega/codigo").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/pedidos/*/entrega").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.POST, "/api/operacion/pedidos/*/entrega/movimientos", "/api/operacion/pedidos/*/entrega/validar", "/api/operacion/pedidos/*/entrega/confirmar", "/api/operacion/pedidos/*/entrega/cerrar").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/pedidos/*/produccion").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.POST, "/api/operacion/pedidos/*/produccion/iniciar", "/api/operacion/pedidos/*/produccion/trabajos/*/estado", "/api/operacion/pedidos/*/produccion/trabajos/*/calidad").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/pedidos/*/gestion").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.POST, "/api/operacion/pedidos/*/gestion", "/api/operacion/pedidos/*/observaciones").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/pedidos/*/cancelar").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/pedidos/*/correcciones", "/api/cliente/pedidos/*/correcciones/respuesta").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/cliente/pedidos/*/correcciones/respuesta", "/api/cliente/pedidos/*/correcciones/*/cotizaciones").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/operacion/pedidos/*/correcciones").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.POST, "/api/operacion/pedidos/*/correcciones").hasAnyRole("ADMIN_ADMIN", "EMPLEADO")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/estado").hasRole("CLIENTE")
                        .anyRequest().denyAll())
                .addFilterBefore(new SesionVigenteFilter(jdbc), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new LimiteAccesoFilter(), UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.loginPage("/acceso").loginProcessingUrl("/api/auth/login")
                        .successHandler((req, res, auth) -> {
                            identidad.registrarAcceso("LOGIN_CORRECTO", auth.getName());
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"mensaje\":\"Sesión iniciada.\"}");
                        })
                        .failureHandler((req, res, ex) -> {
                            identidad.registrarAcceso("LOGIN_FALLIDO", null);
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"mensaje\":\"Correo o contraseña incorrectos.\"}");
                        }))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> {
                            if (auth != null) identidad.registrarAcceso("LOGOUT", auth.getName());
                            res.setStatus(204);
                        }))
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"mensaje\":\"Iniciá sesión para continuar.\"}");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"mensaje\":\"La operación no está autorizada. Volvé a intentarlo desde la aplicación.\"}");
                        }))
                .build();
    }
}
