//#region ENCABEZADO · CorreoIntegrationTest.java
/*
 * ========================================================================
 * ARCHIVO: CorreoIntegrationTest.java
 * ========================================================================
 * FUNCIÓN
 * Comprueba entrega SMTP, verificación y recuperación de credenciales, expiración y consumo de
 * códigos, revocación de sesiones, concurrencia, fallos y atributos de cookies.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [paquete] CapturaSmtp :: CapturaSmtp() throws IOException
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [paquete] void iniciar(TestInfo test) throws Exception
 *   Preparación antes de cada prueba.
 * - [paquete] void cerrar() throws Exception
 *   Limpieza después de cada prueba.
 * - [paquete] void verificacionEntregaSmtpHashPrivadoPropositosSeparadosYUnSoloUso() throws
 *   Exception
 *   Caso de prueba.
 * - [paquete] void intentosPersistidosExpiracionYReenvioSinReactivarCodigoAnterior(boolean
 *   verificacion) throws Exception
 * - [paquete] void recuperacionNoRevelaSiCorreoExisteYNoReenviaDuranteCooldown() throws Exception
 *   Caso de prueba.
 * - [paquete] void
 *   cambioYRecuperacionInvalidanTodasLasSesionesClavesAnterioresYCodigosPendientes() throws
 *   Exception
 *   Caso de prueba.
 * - [paquete] void dosConfirmacionesConcurrentesConsumenRecuperacionUnaSolaVez() throws Exception
 *   Caso de prueba.
 * - [paquete] void falloSmtpRevierteEmisionRevocacionYAuditoriaSinRevelarCuenta() throws Exception
 *   Caso de prueba.
 * - [paquete] void editarCorreoYDarDeBajaEmpleadoRevocanCredencialesYSesiones() throws Exception
 *   Caso de prueba.
 * - [paquete] void principalLeidoAntesDeCambiarClaveNoAutorizaUnaSesionTardia() throws Exception
 *   Caso de prueba.
 * - [paquete] void configuracionCookieRespetaSecureHttpOnlyYSameSite()
 *   Caso de prueba.
 * - [private] void assertSecretoSoloEnCorreo(String tabla, String columna, String token, String
 *   api) throws Exception
 * - [private] void assertRevocados(String empleado)
 * - [private] boolean verificado(String email)
 * - [private] int sesiones(String email)
 * - [private] int contar(String tabla)
 * - [private] int intentos(String tabla)
 * - [private] void envejecer(String tabla)
 * - [private] Map<String, String> recuperacion(String email, String token, String nueva)
 * - [private] void solicitar(HttpClient actor, boolean verificar) throws Exception
 * - [private] HttpResponse<String> confirmar(HttpClient actor, boolean verificar, String token)
 *   throws Exception
 * - [private] Map<String, Object> editarEmpleado(String id) throws Exception
 * - [private] HttpClient cliente()
 * - [private] URI uri(String path)
 * - [private] HttpResponse<String> get(HttpClient actor, String path) throws Exception
 * - [private] String csrf(HttpClient actor) throws Exception
 * - [private] HttpRequest request(HttpClient actor, String method, String path, Object body)
 *   throws Exception
 * - [private] HttpResponse<String> enviar(HttpClient actor, String method, String path, Object
 *   body) throws Exception
 * - [private] HttpResponse<String> post(HttpClient actor, String path, Object body) throws
 *   Exception
 * - [private] void login(HttpClient actor, String email, String clave, int estado) throws
 *   Exception
 * - [private] void assertStatus(HttpResponse<String> response, int esperado)
 * - [paquete] CapturaSmtp :: int port()
 * - [paquete] CapturaSmtp :: int pendientes()
 * - [paquete] CapturaSmtp :: Mensaje recibir() throws Exception
 * - [private] CapturaSmtp :: void aceptar()
 * - [private] CapturaSmtp :: void responder(BufferedWriter escritor, String respuesta) throws
 *   IOException
 * - [public] CapturaSmtp :: void close() throws Exception
 * - [paquete] Mensaje :: String token()
 * - [public] Mensaje :: String toString()
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - CorreoIntegrationTest (clase).
 * - CorreoIntegrationTest.CapturaSmtp (clase).
 * - CorreoIntegrationTest.Mensaje (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.com.lamontana.identidad.CredencialService;
import ar.com.lamontana.identidad.IdentidadController;
import ar.com.lamontana.identidad.IdentidadService;
import ar.com.lamontana.seguridad.SesionVigenteFilter;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** HTTP real, PostgreSQL 18 embebido y SMTP local de captura; ningún correo sale a Internet. */
class CorreoIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String EMAIL = "propietario@example.test";
    private static final String CLAVE = "ClaveCorreoInicial123!";
    private static final String NUEVA = "ClaveCorreoActualizada456!";
    private static final String TOKEN_INSTALACION = "token-local-de-correo-exclusivo-de-pruebas";
    private EmbeddedPostgres pg;
    private ConfigurableApplicationContext app;
    private CapturaSmtp smtp;
    private Path archivos;
    private JdbcTemplate jdbc;
    private HttpClient propietario;
    private int port;

    @BeforeEach
    void iniciar(TestInfo test) throws Exception {
        archivos = Files.createTempDirectory("lamontana-correo-");
        smtp = new CapturaSmtp();
        pg = EmbeddedPostgres.builder().start();
        app = new SpringApplicationBuilder(LaMontanaApplication.class).run(
                "--server.port=0", "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=",
                "--lamontana.archivos.directorio=" + archivos, "--lamontana.instalacion.token=" + TOKEN_INSTALACION,
                "--spring.mail.host=127.0.0.1", "--spring.mail.port=" + smtp.port(),
                "--lamontana.correo.remitente=noreply@example.test",
                "--SESSION_COOKIE_SECURE=" + test.getTestMethod().orElseThrow().getName().equals("configuracionCookieRespetaSecureHttpOnlyYSameSite"));
        port = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
        jdbc = app.getBean(JdbcTemplate.class);
        app.getBean(IdentidadService.class).crearPropietario(new IdentidadController.AltaPropietario(
                TOKEN_INSTALACION, "Propietario", "Correo", EMAIL, CLAVE));
        propietario = cliente();
    }

    @AfterEach
    void cerrar() throws Exception {
        SecurityContextHolder.clearContext();
        if (app != null) app.close();
        if (pg != null) pg.close();
        if (smtp != null) smtp.close();
        if (archivos != null) Files.deleteIfExists(archivos);
    }

    @Test
    void verificacionEntregaSmtpHashPrivadoPropositosSeparadosYUnSoloUso() throws Exception {
        login(propietario, EMAIL, CLAVE, 200);
        var anonimo = cliente();
        assertStatus(post(anonimo, "/api/auth/correo/solicitar", Map.of()), 401);
        var sinCsrf = HttpRequest.newBuilder(uri("/api/auth/correo/solicitar"))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        assertStatus(propietario.send(sinCsrf, HttpResponse.BodyHandlers.ofString()), 403);

        var respuesta = post(propietario, "/api/auth/correo/solicitar", Map.of());
        assertStatus(respuesta, 200);
        var mensaje = smtp.recibir();
        assertThat(mensaje.destino()).isEqualTo(EMAIL);
        assertThat(mensaje.remitente()).isEqualTo("noreply@example.test");
        assertThat(mensaje.asunto()).isEqualTo("La Montaña · Verificar correo");
        String verificacion = mensaje.token();
        assertSecretoSoloEnCorreo("token_verificacion_correo", "hash_token", verificacion, respuesta.body());

        var recuperar = post(anonimo, "/api/auth/recuperacion/solicitar", Map.of("correo", EMAIL));
        assertStatus(recuperar, 200);
        String recuperacion = smtp.recibir().token();
        assertThat(recuperacion).isNotEqualTo(verificacion);
        assertSecretoSoloEnCorreo("credencial_temporal", "hash_codigo", recuperacion, recuperar.body());
        assertThat(jdbc.queryForObject("SELECT proposito FROM lamontana.credencial_temporal", String.class))
                .isEqualTo("RESTABLECIMIENTO");
        assertStatus(post(propietario, "/api/auth/correo/confirmar", Map.of("token", recuperacion)), 400);
        assertStatus(post(anonimo, "/api/auth/recuperacion/confirmar", recuperacion(EMAIL, verificacion, NUEVA)), 400);
        assertThat(intentos("token_verificacion_correo")).isEqualTo(1);
        assertThat(intentos("credencial_temporal")).isEqualTo(1);
        assertThat(verificado(EMAIL)).isFalse();

        assertStatus(post(propietario, "/api/auth/correo/confirmar", Map.of("token", verificacion)), 200);
        assertThat(verificado(EMAIL)).isTrue();
        var perfil = get(propietario, "/api/auth/me");
        assertStatus(perfil, 200);
        assertThat(JSON.readTree(perfil.body()).get("correoVerificado").asBoolean()).isTrue();
        assertThat(perfil.body()).doesNotContain(verificacion, recuperacion, "hash_token", "hash_codigo", CLAVE);
        assertStatus(post(propietario, "/api/auth/correo/confirmar", Map.of("token", verificacion)), 400);
        assertStatus(post(propietario, "/api/auth/correo/solicitar", Map.of()), 200);
        assertThat(smtp.pendientes()).isZero();
        assertThat(contar("token_verificacion_correo")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_acceso WHERE tipo='CORREO_VERIFICADO'", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest(name = "cinco intentos, vencimiento y reenvío: verificacion={0}")
    @ValueSource(booleans = {true, false})
    void intentosPersistidosExpiracionYReenvioSinReactivarCodigoAnterior(boolean verificacion) throws Exception {
        login(propietario, EMAIL, CLAVE, 200);
        HttpClient actor = verificacion ? propietario : cliente();
        String tabla = verificacion ? "token_verificacion_correo" : "credencial_temporal";
        solicitar(actor, verificacion);
        String primero = smtp.recibir().token();
        for (int intento = 1; intento <= 5; intento++) {
            assertStatus(confirmar(actor, verificacion, "codigo-incorrecto-" + intento), 400);
            assertThat(intentos(tabla)).isEqualTo(intento);
        }
        assertStatus(confirmar(actor, verificacion, primero), 400);
        assertThat(intentos(tabla)).isEqualTo(5);
        assertThat(verificado(EMAIL)).isFalse();

        // Reenvío inmediato: verificación informa cooldown; recuperación mantiene respuesta genérica.
        var limitado = post(actor, verificacion ? "/api/auth/correo/solicitar" : "/api/auth/recuperacion/solicitar",
                verificacion ? Map.of() : Map.of("correo", EMAIL));
        assertStatus(limitado, verificacion ? 429 : 200);
        assertThat(contar(tabla)).isEqualTo(1);
        assertThat(smtp.pendientes()).isZero();

        envejecer(tabla);
        solicitar(actor, verificacion);
        String segundo = smtp.recibir().token();
        assertThat(segundo).isNotEqualTo(primero);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana." + tabla + " WHERE fecha_revocacion IS NOT NULL", Integer.class)).isEqualTo(1);
        assertStatus(confirmar(actor, verificacion, primero), 400);
        assertThat(intentos(tabla)).isEqualTo(1);

        jdbc.update("UPDATE lamontana." + tabla + " SET fecha_emision=now()-interval '20 minutes',fecha_vencimiento=now()-interval '1 minute' WHERE fecha_revocacion IS NULL");
        assertStatus(confirmar(actor, verificacion, segundo), 400);
        assertThat(intentos(tabla)).isEqualTo(1);
        solicitar(actor, verificacion);
        String tercero = smtp.recibir().token();
        assertStatus(confirmar(actor, verificacion, tercero), 200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana." + tabla + " WHERE fecha_consumo IS NOT NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana." + tabla + " WHERE fecha_revocacion IS NOT NULL", Integer.class)).isEqualTo(2);
    }

    @Test
    void recuperacionNoRevelaSiCorreoExisteYNoReenviaDuranteCooldown() throws Exception {
        var publico = cliente();
        var inexistente = post(publico, "/api/auth/recuperacion/solicitar", Map.of("correo", "inexistente@example.test"));
        assertStatus(inexistente, 200);
        assertThat(contar("credencial_temporal")).isZero();
        assertThat(smtp.pendientes()).isZero();
        var existente = post(publico, "/api/auth/recuperacion/solicitar", Map.of("correo", "  PROPIETARIO@example.test  ".strip()));
        assertStatus(existente, 200);
        assertThat(existente.body()).isEqualTo(inexistente.body());
        var email = smtp.recibir();
        assertThat(email.destino()).isEqualTo(EMAIL);
        assertThat(email.asunto()).isEqualTo("La Montaña · Recuperar acceso");
        assertSecretoSoloEnCorreo("credencial_temporal", "hash_codigo", email.token(), existente.body());
        var repetida = post(publico, "/api/auth/recuperacion/solicitar", Map.of("correo", EMAIL));
        assertStatus(repetida, 200);
        assertThat(repetida.body()).isEqualTo(inexistente.body());
        assertThat(contar("credencial_temporal")).isEqualTo(1);
        assertThat(smtp.pendientes()).isZero();
        assertStatus(post(publico, "/api/auth/recuperacion/confirmar", recuperacion("inexistente@example.test", email.token(), NUEVA)), 400);
        assertThat(jdbc.queryForObject("SELECT fecha_consumo IS NULL FROM lamontana.credencial_temporal", Boolean.class)).isTrue();
    }

    @Test
    void cambioYRecuperacionInvalidanTodasLasSesionesClavesAnterioresYCodigosPendientes() throws Exception {
        var segunda = cliente();
        login(propietario, EMAIL, CLAVE, 200);
        login(segunda, EMAIL, CLAVE, 200);
        assertThat(sesiones(EMAIL)).isEqualTo(2);
        assertStatus(post(propietario, "/api/auth/contrasena", Map.of("contrasenaActual", "incorrecta", "nuevaContrasena", NUEVA)), 400);
        assertStatus(post(propietario, "/api/auth/contrasena", Map.of("contrasenaActual", CLAVE, "nuevaContrasena", CLAVE)), 400);
        assertThat(sesiones(EMAIL)).isEqualTo(2);
        solicitar(propietario, true); String verificar = smtp.recibir().token();
        var publico = cliente();
        solicitar(publico, false); String recuperar = smtp.recibir().token();
        assertStatus(post(propietario, "/api/auth/contrasena", Map.of("contrasenaActual", CLAVE, "nuevaContrasena", NUEVA)), 200);
        assertThat(sesiones(EMAIL)).isZero();
        assertStatus(get(propietario, "/api/auth/me"), 401);
        assertStatus(get(segunda, "/api/auth/me"), 401);
        assertThatThrownBy(() -> app.getBean(CredencialService.class).confirmarVerificacion(EMAIL, verificar))
                .isInstanceOf(ResponseStatusException.class);
        assertStatus(post(publico, "/api/auth/recuperacion/confirmar", recuperacion(EMAIL, recuperar, CLAVE)), 400);
        login(cliente(), EMAIL, CLAVE, 401);
        login(propietario, EMAIL, NUEVA, 200);
        login(segunda, EMAIL, NUEVA, 200);
        assertThat(sesiones(EMAIL)).isEqualTo(2);

        envejecer("credencial_temporal");
        solicitar(publico, false); String vigente = smtp.recibir().token();
        assertStatus(post(publico, "/api/auth/recuperacion/confirmar", recuperacion(EMAIL, vigente, NUEVA)), 400);
        assertThat(jdbc.queryForObject("SELECT fecha_consumo IS NULL FROM lamontana.credencial_temporal ORDER BY id_credencial_temporal DESC LIMIT 1", Boolean.class)).isTrue();
        String finalClave = "ClaveRestablecidaFinal789!";
        assertStatus(post(publico, "/api/auth/recuperacion/confirmar", recuperacion(EMAIL, vigente, finalClave)), 200);
        assertThat(sesiones(EMAIL)).isZero();
        assertStatus(get(propietario, "/api/auth/me"), 401);
        assertStatus(get(segunda, "/api/auth/me"), 401);
        login(cliente(), EMAIL, NUEVA, 401);
        login(propietario, EMAIL, finalClave, 200);
        assertStatus(post(publico, "/api/auth/recuperacion/confirmar", recuperacion(EMAIL, vigente, CLAVE)), 400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_acceso WHERE tipo='CONTRASENA_CAMBIADA'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_acceso WHERE tipo='ACCESO_RESTABLECIDO'", Integer.class)).isEqualTo(1);
    }

    @Test
    void dosConfirmacionesConcurrentesConsumenRecuperacionUnaSolaVez() throws Exception {
        var a = cliente(); var b = cliente();
        solicitar(a, false); String codigo = smtp.recibir().token();
        var solicitudA = request(a, "POST", "/api/auth/recuperacion/confirmar", recuperacion(EMAIL, codigo, NUEVA));
        var solicitudB = request(b, "POST", "/api/auth/recuperacion/confirmar", recuperacion(EMAIL, codigo, "OtraClaveConcurrente789!"));
        var respuestaA = a.sendAsync(solicitudA, HttpResponse.BodyHandlers.ofString());
        var respuestaB = b.sendAsync(solicitudB, HttpResponse.BodyHandlers.ofString());
        int estadoA = respuestaA.get(20, TimeUnit.SECONDS).statusCode();
        int estadoB = respuestaB.get(20, TimeUnit.SECONDS).statusCode();
        assertThat(List.of(estadoA, estadoB)).containsExactlyInAnyOrder(200, 400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.credencial_temporal WHERE fecha_consumo IS NOT NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_acceso WHERE tipo='ACCESO_RESTABLECIDO'", Integer.class)).isEqualTo(1);
        login(propietario, EMAIL, estadoA == 200 ? NUEVA : "OtraClaveConcurrente789!", 200);
        login(cliente(), EMAIL, estadoA == 200 ? "OtraClaveConcurrente789!" : NUEVA, 401);
    }

    @Test
    void falloSmtpRevierteEmisionRevocacionYAuditoriaSinRevelarCuenta() throws Exception {
        login(propietario, EMAIL, CLAVE, 200);
        solicitar(propietario, true); String anterior = smtp.recibir().token();
        envejecer("token_verificacion_correo");
        smtp.rechazar = true;
        assertStatus(post(propietario, "/api/auth/correo/solicitar", Map.of()), 503);
        assertThat(contar("token_verificacion_correo")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT fecha_revocacion IS NULL FROM lamontana.token_verificacion_correo", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_acceso WHERE tipo='VERIFICACION_SOLICITADA'", Integer.class)).isEqualTo(1);
        assertThat(smtp.pendientes()).isZero();

        var publico = cliente();
        var falla = post(publico, "/api/auth/recuperacion/solicitar", Map.of("correo", EMAIL));
        var desconocido = post(publico, "/api/auth/recuperacion/solicitar", Map.of("correo", "ausente@example.test"));
        assertStatus(falla, 200); assertStatus(desconocido, 200);
        assertThat(falla.body()).isEqualTo(desconocido.body());
        assertThat(contar("credencial_temporal")).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana.evento_acceso WHERE tipo='RECUPERACION_SOLICITADA'", Integer.class)).isZero();
        smtp.rechazar = false;
        // Un envío fallido no invalida el código anterior ni consume el cooldown de recuperación.
        assertStatus(post(propietario, "/api/auth/correo/confirmar", Map.of("token", anterior)), 200);
        solicitar(publico, false); assertThat(smtp.recibir().destino()).isEqualTo(EMAIL);
    }

    @Test
    void editarCorreoYDarDeBajaEmpleadoRevocanCredencialesYSesiones() throws Exception {
        login(propietario, EMAIL, CLAVE, 200);
        var sucursal = post(propietario, "/api/admin/sucursales", Map.of("codigo", "CORREO", "nombre", "Sucursal correo", "calle", "Calle", "numero", "1", "localidad", "Ciudad", "provincia", "Provincia", "codigoPostal", "1234", "zonaHoraria", "America/Argentina/Buenos_Aires"));
        assertStatus(sucursal, 201);
        String sucursalId = JSON.readTree(sucursal.body()).get("codigoPublico").asString();
        String email = "empleado@example.test";
        var alta = post(propietario, "/api/admin/empleados", Map.of("nombre", "Empleado", "apellido", "Correo", "correo", email, "contrasena", CLAVE, "sucursales", List.of(sucursalId), "permisos", List.of()));
        assertStatus(alta, 201);
        String empleadoId = JSON.readTree(alta.body()).get("codigoPublico").asString();
        var empleado = cliente(); login(empleado, email, CLAVE, 200);
        assertStatus(get(empleado, "/api/operacion/contexto"), 403);
        assertStatus(post(empleado, "/api/auth/correo/solicitar", Map.of()), 200);
        String verificarViejo = smtp.recibir().token();
        var anonimo = cliente();
        assertStatus(post(anonimo, "/api/auth/recuperacion/solicitar", Map.of("correo", email)), 200);
        String recuperarViejo = smtp.recibir().token();
        String nuevoEmail = "empleado.nuevo@example.test";
        var editado = editarEmpleado(empleadoId); editado.put("correo", nuevoEmail);
        assertStatus(enviar(propietario, "PUT", "/api/admin/empleados/" + empleadoId, editado), 200);
        assertThat(sesiones(email)).isZero();
        assertStatus(get(empleado, "/api/auth/me"), 401);
        assertThat(verificado(nuevoEmail)).isFalse();
        assertRevocados(empleadoId);
        var servicio = app.getBean(CredencialService.class);
        assertThatThrownBy(() -> servicio.confirmarVerificacion(nuevoEmail, verificarViejo)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servicio.recuperar(nuevoEmail, recuperarViejo, NUEVA)).isInstanceOf(ResponseStatusException.class);
        login(cliente(), email, CLAVE, 401);
        login(empleado, nuevoEmail, CLAVE, 200);
        envejecer("token_verificacion_correo"); envejecer("credencial_temporal");
        assertStatus(post(empleado, "/api/auth/correo/solicitar", Map.of()), 200);
        String verificarActual = smtp.recibir().token();
        assertStatus(post(anonimo, "/api/auth/recuperacion/solicitar", Map.of("correo", nuevoEmail)), 200);
        String recuperarActual = smtp.recibir().token();
        var baja = editarEmpleado(empleadoId); baja.put("estado", "DESACTIVADO"); baja.put("sucursales", List.of());
        assertStatus(enviar(propietario, "PUT", "/api/admin/empleados/" + empleadoId, baja), 200);
        assertThat(sesiones(nuevoEmail)).isZero();
        assertStatus(get(empleado, "/api/auth/me"), 401);
        assertRevocados(empleadoId);
        assertThatThrownBy(() -> servicio.confirmarVerificacion(nuevoEmail, verificarActual)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> servicio.recuperar(nuevoEmail, recuperarActual, NUEVA)).isInstanceOf(ResponseStatusException.class);
        var cuentaInactiva = post(anonimo, "/api/auth/recuperacion/solicitar", Map.of("correo", nuevoEmail));
        var cuentaInexistente = post(anonimo, "/api/auth/recuperacion/solicitar", Map.of("correo", "ausente@example.test"));
        assertStatus(cuentaInactiva, 200); assertStatus(cuentaInexistente, 200);
        assertThat(cuentaInactiva.body()).isEqualTo(cuentaInexistente.body());
        assertThat(smtp.pendientes()).isZero();
    }

    @Test
    void principalLeidoAntesDeCambiarClaveNoAutorizaUnaSesionTardia() throws Exception {
        var identidad = app.getBean(IdentidadService.class);
        var antes = identidad.loadUserByUsername(EMAIL);
        // Reproduce el login que leyó la credencial vieja antes de que concluyera el cambio.
        app.getBean(CredencialService.class).cambiarClave(EMAIL, CLAVE, NUEVA);
        var request = new MockHttpServletRequest("GET", "/api/admin/estado");
        request.setServletPath("/api/admin/estado"); request.getSession(true);
        var response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(antes, null, antes.getAuthorities()));
        var cadenaInvocada = new AtomicBoolean();
        new SesionVigenteFilter(jdbc).doFilter(request, response, (req, res) -> cadenaInvocada.set(true));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(cadenaInvocada).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        var despues = identidad.loadUserByUsername(EMAIL);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(despues, null, despues.getAuthorities()));
        new SesionVigenteFilter(jdbc).doFilter(new MockHttpServletRequest("GET", "/api/admin/estado"), new MockHttpServletResponse(), (req, res) -> cadenaInvocada.set(true));
        assertThat(cadenaInvocada).isTrue();
    }

    @Test
    void configuracionCookieRespetaSecureHttpOnlyYSameSite() {
        var cookie = app.getBean(ServerProperties.class).getServlet().getSession().getCookie();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getHttpOnly()).isTrue();
        assertThat(cookie.getSameSite().name()).isEqualTo("LAX");
    }

    private void assertSecretoSoloEnCorreo(String tabla, String columna, String token, String api) throws Exception {
        String hash = jdbc.queryForObject("SELECT " + columna + " FROM lamontana." + tabla + " ORDER BY fecha_emision DESC LIMIT 1", String.class);
        assertThat(hash).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
        assertThat(hash).isNotEqualTo(token);
        assertThat(api).doesNotContain(token, hash, "hash_token", "hash_codigo");
        assertThat(jdbc.queryForObject("SELECT row_to_json(t)::text FROM lamontana." + tabla + " t ORDER BY fecha_emision DESC LIMIT 1", String.class)).doesNotContain(token);
    }
    private void assertRevocados(String empleado) {
        for (String tabla : List.of("token_verificacion_correo", "credencial_temporal")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lamontana." + tabla + " WHERE id_usuario=(SELECT id_usuario FROM lamontana.usuario WHERE codigo_publico=?) AND fecha_consumo IS NULL AND fecha_revocacion IS NULL", Integer.class, UUID.fromString(empleado))).isZero();
        }
    }
    private boolean verificado(String email) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT correo_verificado_en IS NOT NULL FROM lamontana.usuario WHERE correo=?", Boolean.class, email)); }
    private int sesiones(String email) { return jdbc.queryForObject("SELECT count(*) FROM lamontana.sesion_http WHERE principal_name=?", Integer.class, email); }
    private int contar(String tabla) { return jdbc.queryForObject("SELECT count(*) FROM lamontana." + tabla, Integer.class); }
    private int intentos(String tabla) { return jdbc.queryForObject("SELECT intentos FROM lamontana." + tabla + " WHERE fecha_consumo IS NULL AND fecha_revocacion IS NULL ORDER BY fecha_emision DESC LIMIT 1", Integer.class); }
    private void envejecer(String tabla) { jdbc.update("UPDATE lamontana." + tabla + " SET fecha_emision=now()-interval '2 minutes'"); }
    private Map<String, String> recuperacion(String email, String token, String nueva) { return Map.of("correo", email, "token", token, "nuevaContrasena", nueva); }
    private void solicitar(HttpClient actor, boolean verificar) throws Exception {
        assertStatus(post(actor, verificar ? "/api/auth/correo/solicitar" : "/api/auth/recuperacion/solicitar", verificar ? Map.of() : Map.of("correo", EMAIL)), 200);
    }
    private HttpResponse<String> confirmar(HttpClient actor, boolean verificar, String token) throws Exception {
        return post(actor, verificar ? "/api/auth/correo/confirmar" : "/api/auth/recuperacion/confirmar", verificar ? Map.of("token", token) : recuperacion(EMAIL, token, NUEVA));
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> editarEmpleado(String id) throws Exception {
        var response = get(propietario, "/api/admin/empleados"); assertStatus(response, 200);
        for (var node : JSON.readTree(response.body())) if (node.get("codigoPublico").asString().equals(id)) {
            Map<String, Object> data = new HashMap<>(JSON.readValue(node.toString(), Map.class)); data.remove("codigoPublico"); return data;
        }
        throw new AssertionError("Empleado de prueba inexistente");
    }
    private HttpClient cliente() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build(); }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    private HttpResponse<String> get(HttpClient actor, String path) throws Exception {
        return actor.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private String csrf(HttpClient actor) throws Exception {
        var response = get(actor, "/api/auth/csrf"); assertStatus(response, 200);
        return JSON.readTree(response.body()).get("token").asString();
    }
    private HttpRequest request(HttpClient actor, String method, String path, Object body) throws Exception {
        return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json").header("X-CSRF-TOKEN", csrf(actor))
                .method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();
    }
    private HttpResponse<String> enviar(HttpClient actor, String method, String path, Object body) throws Exception { return actor.send(request(actor, method, path, body), HttpResponse.BodyHandlers.ofString()); }
    private HttpResponse<String> post(HttpClient actor, String path, Object body) throws Exception { return enviar(actor, "POST", path, body); }
    private void login(HttpClient actor, String email, String clave, int estado) throws Exception {
        String body = "username=" + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&password=" + URLEncoder.encode(clave, StandardCharsets.UTF_8);
        var request = HttpRequest.newBuilder(uri("/api/auth/login")).timeout(Duration.ofSeconds(15)).header("Content-Type", "application/x-www-form-urlencoded").header("X-CSRF-TOKEN", csrf(actor))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        assertStatus(actor.send(request, HttpResponse.BodyHandlers.ofString()), estado);
    }
    private void assertStatus(HttpResponse<String> response, int esperado) {
        assertThat(response.statusCode()).withFailMessage("HTTP %s esperado %s: %s", response.statusCode(), esperado, response.body()).isEqualTo(esperado);
    }

    /** Servidor SMTP mínimo que recibe MIME auténtico de JavaMail y puede rechazar RCPT. */
    private static final class CapturaSmtp implements AutoCloseable {
        private final ServerSocket servidor = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
        private final BlockingQueue<Mensaje> mensajes = new LinkedBlockingQueue<>();
        private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "smtp-correo-test"); t.setDaemon(true); return t; });
        private volatile boolean rechazar;
        private volatile Exception fallo;
        CapturaSmtp() throws IOException { hilo.submit(this::aceptar); }
        int port() { return servidor.getLocalPort(); }
        int pendientes() { return mensajes.size(); }
        Mensaje recibir() throws Exception {
            var mensaje = mensajes.poll(5, TimeUnit.SECONDS);
            if (fallo != null) throw new AssertionError("Falló el servidor SMTP de prueba", fallo);
            assertThat(mensaje).as("El correo debe recibirse realmente por SMTP").isNotNull();
            return mensaje;
        }
        private void aceptar() {
            while (!servidor.isClosed()) {
                try (var socket = servidor.accept()) {
                    socket.setSoTimeout(5000);
                    var lector = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    var escritor = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                    responder(escritor, "220 localhost ESMTP test");
                    for (String linea; (linea = lector.readLine()) != null;) {
                        String comando = linea.toUpperCase(Locale.ROOT);
                        if (comando.startsWith("EHLO") || comando.startsWith("HELO")) responder(escritor, "250 localhost");
                        else if (comando.startsWith("MAIL FROM:")) responder(escritor, "250 Sender OK");
                        else if (comando.startsWith("RCPT TO:")) responder(escritor, rechazar ? "451 Temporary SMTP failure" : "250 Recipient OK");
                        else if (comando.equals("DATA")) {
                            responder(escritor, "354 End data with <CRLF>.<CRLF>");
                            var datos = new StringBuilder();
                            while ((linea = lector.readLine()) != null && !linea.equals(".")) datos.append(linea.startsWith("..") ? linea.substring(1) : linea).append("\r\n");
                            var mime = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(datos.toString().getBytes(StandardCharsets.UTF_8)));
                            mensajes.add(new Mensaje(mime.getAllRecipients()[0].toString(), mime.getFrom()[0].toString(), mime.getSubject(), mime.getContent().toString()));
                            responder(escritor, "250 Queued");
                        } else if (comando.equals("QUIT")) { responder(escritor, "221 Bye"); break; }
                        else if (comando.equals("RSET") || comando.equals("NOOP")) responder(escritor, "250 OK");
                        else responder(escritor, "502 Command not implemented");
                    }
                } catch (Exception ex) { if (!servidor.isClosed()) fallo = ex; }
            }
        }
        private void responder(BufferedWriter escritor, String respuesta) throws IOException { escritor.write(respuesta + "\r\n"); escritor.flush(); }
        @Override public void close() throws Exception { servidor.close(); hilo.shutdownNow(); assertThat(hilo.awaitTermination(5, TimeUnit.SECONDS)).isTrue(); }
    }
    private record Mensaje(String destino, String remitente, String asunto, String cuerpo) {
        String token() {
            var encontrado = Pattern.compile("(?m)^[A-Za-z0-9_-]{43}\\r?$").matcher(cuerpo);
            assertThat(encontrado.find()).as("Código opaco presente en el correo MIME").isTrue();
            return encontrado.group().strip();
        }
        @Override public String toString() { return "Mensaje SMTP [contenido privado de prueba]"; }
    }
}
