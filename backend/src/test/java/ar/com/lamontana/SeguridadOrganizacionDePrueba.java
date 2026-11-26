package ar.com.lamontana;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextRepository;

/** Sólo estas suites preparan configuraciones e identidades históricas por HTTP. */
@Configuration(proxyBeanMethods = false)
@Profile("historia-organizacion")
class SeguridadOrganizacionDePrueba {
    @Bean
    @Order(1)
    SecurityFilterChain escenariosDeOrganizacion(HttpSecurity http, SecurityContextRepository sesiones) throws Exception {
        return http.securityMatcher("/fixture/**")
                .securityContext(context -> context.securityContextRepository(sesiones))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN_ADMIN"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
