package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.com.lamontana.archivos.ArchivoService;
import ar.com.lamontana.configuracion.ConfiguracionController.MedioPago;
import ar.com.lamontana.configuracion.CopiaConfiguracionController;
import ar.com.lamontana.configuracion.CopiaConfiguracionService;
import ar.com.lamontana.cotizaciones.CotizacionController;
import ar.com.lamontana.cotizaciones.CotizacionService;
import ar.com.lamontana.instalacion.MigracionesDeInstalacion;
import ar.com.lamontana.pagos.PagoController;
import ar.com.lamontana.pagos.PagoService;
import ar.com.lamontana.pedidos.PedidoController;
import ar.com.lamontana.pedidos.PedidoService;
import ar.com.lamontana.pedidos.RevisionPedidoController;
import ar.com.lamontana.pedidos.RevisionPedidoService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Los contadores se contrastan con altas de negocio desde conexiones PostgreSQL independientes. */
class CuposDemoIntegrationTest {
    private static final String ADMIN = "administracion@demo.invalid";
    private static final String CLIENTE = "cliente@demo.invalid";
    private static final UUID PEDIDO = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0078");
    private static final UUID CONFIG = UUID.fromString("4d9fdf39-1f90-4d08-b7b6-0c77fc9f0075");
    private static final String INSERTAR_CLIENTE = """
            INSERT INTO lamontana.usuario(codigo_publico,id_rol,correo,hash_contrasena,nombre,apellido,estado)
            SELECT ?,id_rol,?,'sin-acceso','Cliente','De prueba','ACTIVO' FROM lamontana.rol WHERE codigo='CLIENTE'
            ON CONFLICT (codigo_publico) DO NOTHING
            """;

    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private ClamavDePrueba antivirus;
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;

    @BeforeEach void iniciar(@TempDir Path directorio) throws Exception {
        pg = EmbeddedPostgres.builder().start();
        antivirus = new ClamavDePrueba();
        app = new SpringApplicationBuilder(LaMontanaApplication.class).run(
                "--server.port=0", "--spring.datasource.url=" + pg.getJdbcUrl("postgres","postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--lamontana.archivos.directorio=" + directorio, "--lamontana.archivos.antivirus.port=" + antivirus.port());
        jdbc = app.getBean(JdbcTemplate.class);
        tx = new TransactionTemplate(app.getBean(PlatformTransactionManager.class));
    }

    @AfterEach void cerrar() throws Exception {
        try {
            if (app != null) app.close();
        } finally {
            try { if (antivirus != null) antivirus.close(); }
            finally { if (pg != null) pg.close(); }
        }
    }

    @Test void clientesCompartenDiezLugaresEntreLasFichasSinContarAdministradorNiEmpleado() throws Exception {
        matriz("CLIENTES", "CLIENTE", 10, "23D02", () -> {
            var codigo = UUID.randomUUID();
            return () -> {
                jdbc.update(INSERTAR_CLIENTE,codigo,codigo + "@demo.invalid");
                return codigo;
            };
        });
    }

    @Test void impresorasCompartenDosLugaresPorIdentidadAlCopiarLaConfiguracion() throws Exception {
        var copia = app.getBean(CopiaConfiguracionService.class).copiar(CONFIG,
                new CopiaConfiguracionController.Crear(UUID.randomUUID()),ADMIN);
        assertThat(copia.codigoPublico()).isNotEqualTo(CONFIG);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_impresora",Integer.class)).isEqualTo(2);
        matriz("IMPRESORAS", "IMPRESORA", 2, "23D04", () -> {
            var codigo = UUID.randomUUID();
            return () -> {
                jdbc.update("""
                        INSERT INTO lamontana.impresora(codigo_publico,id_usuario_creador)
                        SELECT ?,id_usuario FROM lamontana.usuario WHERE correo=? ON CONFLICT (codigo_publico) DO NOTHING
                        """,codigo,ADMIN);
                return codigo;
            };
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.configuracion_impresora",Integer.class)).isEqualTo(2);
    }

    @Test void cajaSumaCobrosYDevolucionesInclusoSinAlterarLasAplicacionesNiElSaldo() throws Exception {
        var pagos = app.getBean(PagoService.class);
        UUID cotizacion = cotizar().codigoPublico();
        var cobro = prepararCobro(cotizacion).get();
        var anterior = pagos.vista(cotizacion,ADMIN,true).pagos().get(0);
        var aplicar = new PagoController.Aplicar(UUID.randomUUID(),cobro,anterior.version(),"2.00","Aplicación de prueba");
        var aplicado = pagos.aplicar(cotizacion,aplicar,ADMIN).pagos().get(0);
        assertThat(usados("CAJA")).isEqualTo(2);
        var devolver = new PagoController.Devolver(UUID.randomUUID(),cobro,aplicado.version(),MedioPago.EFECTIVO,
                "1.00",UUID.randomUUID().toString(),Instant.now().minusSeconds(1),true,"Devolución de prueba");
        var pago = pagos.devolver(cotizacion,devolver,ADMIN).pagos().get(0);
        assertThat(usados("CAJA")).isEqualTo(3);
        UUID devolucion = pago.devoluciones().get(0).codigoPublico();
        assertThat(baja("DEVOLUCION",devolucion)).isTrue();
        assertThat(baja("COBRO",cobro)).isTrue();
        assertThat(pagos.vista(cotizacion,ADMIN,true).pagos()).containsExactly(pago);
        matriz("CAJA", "COBRO", 30, "23D03", () -> prepararCobro(cotizar().codigoPublico()));
    }

    @Test void losVeintePedidosIncluyenCanceladosYConservanSusVinculosAlDarDeBaja() throws Exception {
        var pedidos = app.getBean(PedidoService.class);
        var codigo = prepararPedido().get();
        var creado = pedidos.detalle(codigo,CLIENTE,false);
        var cancelado = app.getBean(RevisionPedidoService.class).cancelar(codigo,
                new RevisionPedidoController.Cancelacion(UUID.randomUUID(),creado.version(),"Cancelación de prueba"),CLIENTE);
        assertThat(cancelado.estado()).isEqualTo("CANCELADO");
        assertThat(usados("PEDIDOS")).isEqualTo(2);
        assertThat(baja("PEDIDO",codigo)).isTrue();
        assertThat(pedidos.detalle(codigo,CLIENTE,false)).isEqualTo(cancelado);
        matriz("PEDIDOS", "PEDIDO", 20, "23D01", this::prepararPedido);
    }

    @Test void actualizarDesdeV48ConservaCadaAltaAunqueYaSupereElLimite() throws Exception {
        try (var anterior = EmbeddedPostgres.builder().start()) {
            var cfg = Flyway.configure().dataSource(anterior.getPostgresDatabase())
                    .schemas("lamontana").defaultSchema("lamontana").locations("classpath:db/migration","classpath:db/imprenta");
            new MigracionesDeInstalacion("DEMO").customize(cfg);
            cfg.target("48").load().migrate();
            var db = new JdbcTemplate(anterior.getPostgresDatabase());
            for (int i=0;i<11;i++) db.update(INSERTAR_CLIENTE,UUID.randomUUID(),"previo"+i+"@demo.invalid");
            var cuentas = db.queryForList("SELECT codigo_publico FROM lamontana.usuario ORDER BY id_usuario",UUID.class);
            cfg.target("latest").load().migrate();
            assertThat(db.queryForList("SELECT codigo_publico FROM lamontana.usuario ORDER BY id_usuario",UUID.class)).isEqualTo(cuentas);
            assertThat(db.queryForObject("SELECT ocupados FROM lamontana.demo_cupo WHERE recurso='CLIENTES'",Integer.class)).isEqualTo(12);
            assertThatThrownBy(() -> db.update(INSERTAR_CLIENTE,UUID.randomUUID(),"otra@demo.invalid"))
                    .isInstanceOf(DataAccessException.class).satisfies(e -> assertThat(sqlState(e)).isEqualTo("23D02"));
            String archivar = """
                    SELECT lamontana.dar_baja_cupo_demo('CLIENTE', codigo_publico,
                        (SELECT id_usuario FROM lamontana.usuario WHERE correo=?)) FROM lamontana.usuario WHERE correo=?
                    """;
            assertThat(db.queryForObject(archivar,Boolean.class,ADMIN,"previo0@demo.invalid")).isTrue();
            assertThat(db.queryForObject(archivar,Boolean.class,ADMIN,"previo1@demo.invalid")).isTrue();
            assertThatThrownBy(() -> db.update(INSERTAR_CLIENTE,UUID.randomUUID(),"otra@demo.invalid"))
                    .isInstanceOf(DataAccessException.class).satisfies(e -> assertThat(sqlState(e)).isEqualTo("23D02"));
            assertThat(db.queryForObject(archivar,Boolean.class,ADMIN,"previo2@demo.invalid")).isTrue();
            assertThat(db.update(INSERTAR_CLIENTE,UUID.randomUUID(),"otra@demo.invalid")).isOne();
            assertThat(db.queryForObject("SELECT ocupados FROM lamontana.demo_cupo WHERE recurso='CLIENTES'",Integer.class)).isEqualTo(10);
        }
    }

    @Test void elMostradorComparteCupoConRegistroYLaBajaEsAdministrativaYAuditable() throws Exception {
        String panel = "/api/demo/cupos";
        String fichas = "/api/demo/clientes";
        var cliente = new SesionHttp();
        assertThat(cliente.enviar("GET",panel,null,null).statusCode()).isEqualTo(401);
        cliente.elegir("CLIENTE");
        var operacion = Map.of("operacion",UUID.randomUUID(),"nombre","Ada","apellido","Prueba");
        cliente.post(fichas,operacion,403);
        cliente.get(panel,403);
        var empleado = new SesionHttp();
        empleado.elegir("EMPLEADO");
        String codigo = empleado.post(fichas,operacion,200).get("codigo").asString();
        assertThat(empleado.post(fichas,operacion,200).get("codigo").asString()).isEqualTo(codigo);
        empleado.post(fichas,Map.of("operacion",operacion.get("operacion"),"nombre","Otra","apellido","Prueba"),409);
        String archivar = panel+"/CLIENTE/"+codigo+"/baja";
        empleado.get(panel,403);
        empleado.post(archivar,null,403);
        var admin = new SesionHttp();
        admin.elegir("ADMINISTRADOR");
        var cuentaMuestra = admin.get(panel,200).get("cupos").valueStream()
                .filter(c -> c.get("tipo").asString().equals("CLIENTES")).findFirst().orElseThrow()
                .get("registros").valueStream().filter(c -> c.get("reservado").asBoolean()).findFirst().orElseThrow();
        assertThat(cuentaMuestra.get("concepto").asString()).isEqualTo("Lucía Papel");
        admin.post(panel+"/CLIENTE/"+cuentaMuestra.get("codigo").asString()+"/baja",null,409);
        assertThat(usados("CLIENTES")).isEqualTo(2);
        cliente.elegir("CLIENTE");
        assertThat(cliente.get("/api/auth/me",200).get("codigoPublico")).isEqualTo(cuentaMuestra.get("codigo"));
        assertThat(admin.enviar("POST",archivar,null,null).statusCode()).isEqualTo(403);
        for (int i=2;i<10;i++) admin.post(fichas,Map.of("operacion",UUID.randomUUID(),"nombre","Cliente","apellido","Ficha "+i),200);
        var excesiva = Map.of("operacion",UUID.randomUUID(),"nombre","Nueva","apellido","Ficha");
        assertThat(admin.post(fichas,excesiva,409).get("codigo").asString()).isEqualTo("CUPO_DEMO");
        assertThat(cliente.post("/api/auth/registro",Map.of("nombre","Otro","apellido","Cliente",
                "correo","otro@demo.invalid","contrasena","PruebaLarga123!"),409).get("codigo").asString()).isEqualTo("CUPO_DEMO");
        assertThat(empleado.post(fichas,operacion,200).get("codigo").asString()).isEqualTo(codigo);
        admin.post(archivar,null,204);
        admin.post(archivar,null,204);
        var cupos = admin.get(panel,200).get("cupos");
        assertThat(cupos.size()).isEqualTo(4);
        var clientes = cupos.valueStream().filter(c -> c.get("tipo").asString().equals("CLIENTES")).findFirst().orElseThrow();
        assertThat(clientes.get("limite").asInt()).isEqualTo(10);
        assertThat(clientes.get("usados").asInt()).isEqualTo(9);
        var ficha = clientes.get("registros").valueStream().filter(c -> c.get("codigo").asString().equals(codigo)).findFirst().orElseThrow();
        assertThat(ficha.get("bajaPor").asString()).isEqualTo("Elena Senderos");
        assertThat(ficha.get("bajaEn").isString()).isTrue();
        admin.post(panel+"/CLIENTE/"+UUID.randomUUID()+"/baja",null,404);
        admin.post(fichas,excesiva,200);
        assertThat(usados("CLIENTES")).isEqualTo(10);
    }

    private final class SesionHttp {
        private final HttpClient cliente = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        private final String url = "http://127.0.0.1:"+app.getEnvironment().getRequiredProperty("local.server.port");
        private final JsonMapper json = JsonMapper.builder().build();
        private String token;

        void elegir(String rol) throws Exception {
            token = get("/api/auth/csrf",200).get("token").asString();
            post("/api/demo/sesion",Map.of("rol",rol),200);
            token = get("/api/auth/csrf",200).get("token").asString();
        }

        JsonNode get(String ruta,int estado) throws Exception {
            return leer(enviar("GET",ruta,null,null),estado);
        }

        JsonNode post(String ruta,Object cuerpo,int estado) throws Exception {
            return leer(enviar("POST",ruta,cuerpo,token),estado);
        }

        HttpResponse<String> enviar(String verbo,String ruta,Object cuerpo,String csrf) throws Exception {
            var req = HttpRequest.newBuilder(URI.create(url+ruta)).header("Content-Type","application/json");
            if (csrf!=null) req.header("X-CSRF-TOKEN",csrf);
            return cliente.send(req.method(verbo,cuerpo==null ? HttpRequest.BodyPublishers.noBody() :
                    HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo))).build(),HttpResponse.BodyHandlers.ofString());
        }

        JsonNode leer(HttpResponse<String> r,int estado) {
            assertThat(r.statusCode()).as("%s %s",r.uri(),r.body()).isEqualTo(estado);
            return r.body().isBlank() ? null : json.readTree(r.body());
        }
    }

    private Supplier<UUID> prepararCobro(UUID cotizacion) {
        var input = new PagoController.Recibir(UUID.randomUUID(),null,MedioPago.EFECTIVO,"5.00",
                UUID.randomUUID().toString(),Instant.now().minusSeconds(1),true,"Cobro de prueba");
        return () -> app.getBean(PagoService.class).recibir(cotizacion,input,ADMIN).pagos().stream()
                .filter(p -> p.referencia().equals(input.referencia())).findFirst().orElseThrow().codigoPublico();
    }

    @Test void bajaDeClienteConservaHistorialYReintentosPeroImpideNuevasCotizacionesYPedidos() {
        UUID ficha = UUID.randomUUID();
        String correo = ficha + "@demo.invalid";
        jdbc.update(INSERTAR_CLIENTE, ficha, correo);
        var cotizaciones = app.getBean(CotizacionService.class);
        var solicitudPrevia = solicitudCotizacion();
        var cotizacionPrevia = cotizaciones.crear(solicitudPrevia, correo);
        var confirmarAnterior = prepararPedido(correo);
        var pedidoAnterior = confirmarAnterior.get();
        var pendiente = prepararPedido(correo);
        var solicitudNueva = solicitudCotizacion();

        var otraConexion = new TransactionTemplate(app.getBean(PlatformTransactionManager.class));
        otraConexion.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(baja -> {
            assertThat(baja("CLIENTE", ficha)).isTrue();
            // Mientras la primera conexión no confirma la baja, la segunda debe esperar.
            assertThatThrownBy(() -> otraConexion.executeWithoutResult(alta -> {
                jdbc.execute("SET LOCAL lock_timeout = '300ms'");
                cotizaciones.crear(solicitudNueva, correo);
            })).satisfies(e -> assertThat(sqlState(e)).isEqualTo("55P03"));
        });

        assertThatThrownBy(() -> cotizaciones.crear(solicitudNueva, correo))
                .satisfies(e -> assertThat(sqlState(e)).isEqualTo("23D12"));
        assertThatThrownBy(pendiente::get).satisfies(e -> assertThat(sqlState(e)).isEqualTo("23D12"));
        assertThat(cotizaciones.detalle(cotizacionPrevia.codigoPublico(), correo).codigoPublico())
                .isEqualTo(cotizacionPrevia.codigoPublico());
        assertThat(cotizaciones.crear(solicitudPrevia, correo).codigoPublico()).isEqualTo(cotizacionPrevia.codigoPublico());
        assertThat(app.getBean(PedidoService.class).detalle(pedidoAnterior, correo, false).codigoPublico()).isEqualTo(pedidoAnterior);
        assertThat(confirmarAnterior.get()).isEqualTo(pedidoAnterior);
        assertThat(usados("PEDIDOS")).isEqualTo(2);
    }

    private CotizacionController.Crear solicitudCotizacion() {
        var oferta = app.getBean(PedidoService.class).detalle(PEDIDO,CLIENTE,false).oferta();
        var item = oferta.items().get(0);
        return new CotizacionController.Crear(UUID.randomUUID(),CONFIG,oferta.revisionComercial(),
                oferta.sucursal().codigoPublico(),List.of(new CotizacionController.Item(item.documento(),item.trabajo())),
                oferta.modalidad(),null,null,oferta.medioPago(),null);
    }

    private CotizacionService.Detalle cotizar() { return cotizar(CLIENTE); }

    private CotizacionService.Detalle cotizar(String correo) {
        var servicio = app.getBean(CotizacionService.class);
        var cotizacion = servicio.crear(solicitudCotizacion(), correo);
        return servicio.decidir(cotizacion.codigoPublico(),new CotizacionController.Decision(UUID.randomUUID(),
                cotizacion.version(),null),true,correo);
    }

    private Supplier<UUID> prepararPedido() { return prepararPedido(CLIENTE); }

    private Supplier<UUID> prepararPedido(String correo) {
        var oferta = cotizar(correo);
        var cotizacion = oferta.codigoPublico();
        var archivos = app.getBean(ArchivoService.class);
        var item = oferta.oferta().items().get(0);
        var archivo = archivos.crear(cotizacion,item.codigoPublico(),UUID.randomUUID(),oferta.version(),null,correo);
        try (var pdf = new ClassPathResource("demo/afiche-senderos.pdf").getInputStream()) {
            var recibido = archivos.recibir(cotizacion,archivo.codigoPublico(),correo,pdf,item.documento().bytes());
            assertThat(recibido.estado()).as(recibido.mensaje()).isEqualTo("VALIDO");
            archivos.aceptar(cotizacion,archivo.codigoPublico(),UUID.randomUUID(),recibido.sha256(),correo);
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        return new Supplier<>() {
            PedidoController.Confirmar confirmacion;
            @Override public UUID get() {
                var pedidos = app.getBean(PedidoService.class);
                if (confirmacion == null) {
                    var rev = pedidos.revisar(cotizacion,correo);
                    assertThat(rev.habilitada()).as(rev.bloqueos().toString()).isTrue();
                    confirmacion = new PedidoController.Confirmar(UUID.randomUUID(),rev.version(),rev.huella(),true,null);
                }
                return pedidos.confirmar(cotizacion,confirmacion,correo).codigoPublico();
            }
        };
    }

    private void matriz(String recurso, String tipo, int limite, String error, Supplier<Supplier<UUID>> preparar) throws Exception {
        assertThat(usados(recurso)).isOne();
        assertThat(jdbc.queryForObject("SELECT limite FROM lamontana.demo_cupo WHERE recurso=?",Integer.class,recurso))
                .isEqualTo(limite);
        for (int i=1;i<limite-1;i++) preparar.get().get();
        var revertida = preparar.get();
        tx.executeWithoutResult(status -> {
            revertida.get();
            assertThat(usados(recurso)).isEqualTo(limite);
            status.setRollbackOnly();
        });
        assertThat(usados(recurso)).isEqualTo(limite-1);

        var izquierda = preparar.get();
        var derecha = preparar.get();
        var barrera = new CyclicBarrier(2);
        var pares = Executors.newFixedThreadPool(2);
        Supplier<UUID> ganadora;
        UUID codigo;
        try {
            var primera = pares.submit(competir(izquierda,barrera,error));
            var segunda = pares.submit(competir(derecha,barrera,error));
            var a = primera.get(30,TimeUnit.SECONDS);
            var b = segunda.get(30,TimeUnit.SECONDS);
            assertThat((a == null) ^ (b == null)).as("una sola alta obtuvo el último cupo de "+recurso).isTrue();
            ganadora = a != null ? izquierda : derecha;
            codigo = a != null ? a : b;
        } finally { pares.shutdownNow(); }
        assertThat(usados(recurso)).isEqualTo(limite);
        assertThat(ganadora.get()).isEqualTo(codigo);
        var excedida = preparar.get();
        assertThatThrownBy(excedida::get).isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(sqlState(e)).isEqualTo(error));
        assertThat(usados(recurso)).isEqualTo(limite);
        assertThat(baja(tipo,codigo)).isTrue();
        var auditoria = jdbc.queryForMap("""
                SELECT baja_en,id_actor_baja FROM lamontana.demo_cupo_registro WHERE tipo=? AND codigo_publico=?
                """,tipo,codigo);
        assertThat(auditoria).doesNotContainValue(null);
        assertThat(baja(tipo,codigo)).isFalse();
        assertThat(jdbc.queryForMap("""
                SELECT baja_en,id_actor_baja FROM lamontana.demo_cupo_registro WHERE tipo=? AND codigo_publico=?
                """,tipo,codigo)).isEqualTo(auditoria);
        assertThat(usados(recurso)).isEqualTo(limite-1);
        assertThat(excedida.get()).isNotEqualTo(codigo);
        assertThat(usados(recurso)).isEqualTo(limite);
    }

    private Callable<UUID> competir(Supplier<UUID> alta, CyclicBarrier inicio, String error) {
        return () -> {
            try { return tx.execute(status -> {
                // Las dos conexiones están dentro de sus transacciones antes de intentar el último lugar.
                jdbc.queryForObject("SELECT pg_backend_pid()",Integer.class);
                try { inicio.await(15,TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                catch (java.util.concurrent.BrokenBarrierException | java.util.concurrent.TimeoutException e) {
                    throw new AssertionError("Las altas no llegaron juntas al punto de partida",e);
                }
                return alta.get();
            }); }
            catch (DataAccessException e) {
                assertThat(sqlState(e)).isEqualTo(error);
                return null;
            }
        };
    }

    private static String sqlState(Throwable error) {
        for (var causa = error; causa != null; causa = causa.getCause())
            if (causa instanceof SQLException sql) return sql.getSQLState();
        throw new AssertionError("Falta el error de PostgreSQL",error);
    }

    private int usados(String recurso) {
        return jdbc.queryForObject("SELECT ocupados FROM lamontana.demo_cupo WHERE recurso=?",Integer.class,recurso);
    }

    private Boolean baja(String tipo, UUID codigo) {
        return jdbc.queryForObject("""
                SELECT lamontana.dar_baja_cupo_demo(?,?,(SELECT id_usuario FROM lamontana.usuario WHERE correo=?))
                """,Boolean.class,tipo,codigo,ADMIN);
    }
}
