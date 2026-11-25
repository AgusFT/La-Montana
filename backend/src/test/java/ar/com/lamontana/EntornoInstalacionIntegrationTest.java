package ar.com.lamontana;
import static org.assertj.core.api.Assertions.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
class EntornoInstalacionIntegrationTest {
    @Test void modoPersistidoImpideAdoptarPruebasEnStandard() throws Exception {
        try(var pg=EmbeddedPostgres.builder().start()){
            try(var app=iniciar(pg,"DESARROLLO")){assertThat(app.getBean(JdbcTemplate.class).queryForObject("SELECT modo FROM lamontana.entorno_instalacion",String.class)).isEqualTo("DESARROLLO");}
            assertThatThrownBy(()->iniciar(pg,"STANDARD")).hasStackTraceContaining("Standard requiere una base nueva");
        }
        try(var pg=EmbeddedPostgres.builder().start();var app=iniciar(pg,"STANDARD")){
            assertThat(app.getBean(ar.com.lamontana.instalacion.EntornoInstalacion.class).desarrollo()).isFalse();
            assertThat(app.getBean(JdbcTemplate.class).queryForObject("SELECT count(*) FROM lamontana.usuario",Integer.class)).isZero();
        }
    }
    private ConfigurableApplicationContext iniciar(EmbeddedPostgres pg,String modo){return new SpringApplicationBuilder(LaMontanaApplication.class).run("--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--lamontana.instalacion.modo="+modo);}
}
