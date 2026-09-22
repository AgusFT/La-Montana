package ar.com.lamontana.seguridad;

import ar.com.lamontana.identidad.IdentidadService;
import java.util.Map;
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
    SecurityFilterChain seguridad(HttpSecurity http, IdentidadService identidad) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/sistema/estado", "/actuator/health", "/actuator/health/**", "/api/setup/estado", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/setup/propietario", "/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/admin/estado").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/sucursales").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/sucursales").hasRole("ADMIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/cliente/estado").hasRole("CLIENTE")
                        .anyRequest().denyAll())
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
