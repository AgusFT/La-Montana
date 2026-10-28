package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.lamontana.configuracion.OfertaOperativaService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class DatosPruebaIntegrationTest {
    @Test
    void cargaUnEntornoAisladoOperativo() throws Exception {
        var files=Files.createTempDirectory("lamontana-datos-prueba-");
        try(var pg=EmbeddedPostgres.builder().start();var app=new SpringApplicationBuilder(LaMontanaApplication.class).run(
                "--server.port=0","--spring.profiles.active=pruebas","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),
                "--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.archivos.directorio="+files,
                "--lamontana.pruebas.clave=MontanaPrueba2026!")){
            var jdbc=app.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.usuario",Integer.class)).isEqualTo(7);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.sucursal",Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.punto_entrega",Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_version WHERE estado='ACTIVA'",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.catalogo_revision WHERE vigente",Integer.class)).isEqualTo(1);
            String hash=jdbc.queryForObject("SELECT hash_contrasena FROM lamontana.usuario WHERE correo='cliente1@prueba.local'",String.class);
            assertThat(app.getBean(PasswordEncoder.class).matches("MontanaPrueba2026!",hash)).isTrue();
            var oferta=app.getBean(OfertaOperativaService.class).leer();
            assertThat(oferta.sucursales()).hasSize(3);
            assertThat(oferta.opciones()).isNotEmpty();
            assertThat(oferta.puntos()).hasSize(2);
        } finally { Files.walk(files).sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}}); }
    }
}
