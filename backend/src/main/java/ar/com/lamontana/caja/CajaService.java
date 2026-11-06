package ar.com.lamontana.caja;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CajaService {
    private static final int PAGINA = 25;
    private static final String PERIODO = " WHERE id_sucursal=? AND fecha>=? AND fecha<? AND fecha<=?";
    private final JdbcTemplate jdbc;
    private final Clock clock = Clock.systemUTC();
    private final JsonMapper json = JsonMapper.builder().build();

    public CajaService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public enum Tipo { PAGO_EMPLEADO, PAGO_PROVEEDOR, RENDICION }
    public enum Medio { EFECTIVO, TRANSFERENCIA }
    private record Acceso(long sucursal, long actor, String nombre, ZoneId zona, boolean admin, boolean activa) {}
    private record Periodo(LocalDate hoy, LocalDate desde, LocalDate hasta, Timestamp inicio, Timestamp fin) {}
    public record Movimiento(UUID codigo, String tipo, Medio medio, String importe, Instant fecha,
                             String actor, String concepto, String destinatario, boolean adjunto, UUID cotizacion) {}
    public record Arqueo(UUID codigo, Instant fecha, String actor, String esperado, String contado,
                         String diferencia, String observacion) {}
    public record Resumen(String efectivoInicial, String efectivoIngresos, String efectivoEgresos, String efectivoFinal,
                          String efectivoActual, String transferenciasIngresos, String transferenciasEgresos,
                          String rendiciones) {}
    public record Vista(LocalDate hoy, LocalDate desde, LocalDate hasta, String zonaHoraria, boolean activa,
                        Resumen resumen, List<Movimiento> movimientos, long total, int pagina,
                        List<Arqueo> arqueos, long totalArqueos, int paginaArqueos) {}
    public record Comprobante(String extension, byte[] datos) {}

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Vista consultar(UUID sucursal, String correo, LocalDate desde, LocalDate hasta, int pagina, int paginaArqueos) {
        var acceso = acceso(sucursal, correo);
        var ahora = Timestamp.from(clock.instant());
        var periodo = periodo(acceso, ahora.toInstant(), desde, hasta);
        if (pagina < 0 || paginaArqueos < 0) throw invalido("La página no es válida.");
        var resumen = jdbc.queryForObject("""
            SELECT coalesce(sum(importe) FILTER(WHERE medio='EFECTIVO' AND fecha<?),0) AS inicial,
              coalesce(sum(importe) FILTER(WHERE medio='EFECTIVO' AND importe>0 AND fecha>=? AND fecha<?),0) AS entradas,
              coalesce(-sum(importe) FILTER(WHERE medio='EFECTIVO' AND importe<0 AND fecha>=? AND fecha<?),0) AS salidas,
              coalesce(sum(importe) FILTER(WHERE medio='EFECTIVO'),0) AS actual,
              coalesce(sum(importe) FILTER(WHERE medio='TRANSFERENCIA' AND importe>0 AND fecha>=? AND fecha<?),0) AS transferencias,
              coalesce(-sum(importe) FILTER(WHERE medio='TRANSFERENCIA' AND importe<0 AND fecha>=? AND fecha<?),0) AS pagos,
              coalesce(-sum(importe) FILTER(WHERE tipo='RENDICION' AND fecha>=? AND fecha<?),0) AS rendido
            FROM lamontana.caja_movimiento WHERE id_sucursal=? AND fecha<=?
            """, (rs, n) -> {
                var inicial = rs.getBigDecimal("inicial");
                var entradas = rs.getBigDecimal("entradas");
                var salidas = rs.getBigDecimal("salidas");
                return new Resumen(inicial.toPlainString(), entradas.toPlainString(), salidas.toPlainString(),
                    inicial.add(entradas).subtract(salidas).toPlainString(), rs.getBigDecimal("actual").toPlainString(),
                    rs.getBigDecimal("transferencias").toPlainString(), rs.getBigDecimal("pagos").toPlainString(),
                    rs.getBigDecimal("rendido").toPlainString());
            }, periodo.inicio(),
            periodo.inicio(), periodo.fin(), periodo.inicio(), periodo.fin(), periodo.inicio(), periodo.fin(),
            periodo.inicio(), periodo.fin(), periodo.inicio(), periodo.fin(), acceso.sucursal(), ahora);
        var movimientos = jdbc.query("SELECT * FROM lamontana.caja_movimiento" + PERIODO
                + " ORDER BY fecha DESC,codigo LIMIT ? OFFSET ?", (rs, n) -> new Movimiento(
                rs.getObject("codigo", UUID.class), rs.getString("tipo"), Medio.valueOf(rs.getString("medio")),
                rs.getBigDecimal("importe").toPlainString(), rs.getTimestamp("fecha").toInstant(), rs.getString("actor"),
                rs.getString("concepto"), rs.getString("destinatario"), rs.getBoolean("adjunto"),
                rs.getObject("cotizacion", UUID.class)), acceso.sucursal(), periodo.inicio(), periodo.fin(),
                ahora, PAGINA, (long) pagina * PAGINA);
        var arqueos = jdbc.query("SELECT * FROM lamontana.caja_arqueo" + PERIODO
                + " ORDER BY fecha DESC,id_operacion LIMIT ? OFFSET ?", (rs, n) -> new Arqueo(
                rs.getObject("id_operacion", UUID.class), rs.getTimestamp("fecha").toInstant(),
                rs.getString("actor_nombre"), rs.getBigDecimal("esperado").toPlainString(), rs.getBigDecimal("contado").toPlainString(),
                rs.getBigDecimal("diferencia").toPlainString(), rs.getString("observacion")), acceso.sucursal(),
                periodo.inicio(), periodo.fin(), ahora, PAGINA, (long) paginaArqueos * PAGINA);
        return new Vista(periodo.hoy(), periodo.desde(), periodo.hasta(), acceso.zona().getId(), acceso.activa(),
            resumen, movimientos, contar("caja_movimiento", acceso, periodo, ahora), pagina,
            arqueos, contar("caja_arqueo", acceso, periodo, ahora), paginaArqueos);
    }

    @Transactional
    public void egreso(UUID sucursal, String correo, CajaController.NuevoEgreso in) {
        var acceso = accesoActivo(sucursal, correo);
        bloquearLibro();
        var importe = importe(in.importe(), false);
        String destinatario = in.destinatario().strip(), concepto = in.concepto().strip();
        if (in.tipo() == Tipo.RENDICION && in.medio() != Medio.EFECTIVO)
            throw invalido("Una rendición de caja se registra en efectivo.");
        String extension = extension(in.comprobante());
        String huella = huella(in);
        if (yaRegistrado("caja_egreso", in.operacion(), acceso, huella)) return;
        jdbc.update("""
            INSERT INTO lamontana.caja_egreso(id_operacion,id_sucursal,id_actor,actor_nombre,tipo,medio,importe,
                destinatario,concepto,fecha,huella,comprobante_tipo,comprobante)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, in.operacion(), acceso.sucursal(), acceso.actor(), acceso.nombre(), in.tipo().name(),
            in.medio().name(), importe, destinatario, concepto, Timestamp.from(clock.instant()),
            huella, extension, in.comprobante());
    }

    @Transactional
    public void arqueo(UUID sucursal, String correo, CajaController.NuevoArqueo in) {
        var acceso = accesoActivo(sucursal, correo);
        bloquearLibro();
        var contado = importe(in.contado(), true);
        var huella = huella(in);
        if (yaRegistrado("caja_arqueo", in.operacion(), acceso, huella)) return;
        var ahora = Timestamp.from(clock.instant());
        var esperado = jdbc.queryForObject("""
            SELECT coalesce(sum(importe),0) FROM lamontana.caja_movimiento
            WHERE id_sucursal=? AND medio='EFECTIVO' AND fecha<=?
            """, BigDecimal.class, acceso.sucursal(), ahora);
        if (esperado.compareTo(new BigDecimal(in.esperado())) != 0)
            throw conflicto("El efectivo esperado cambió. Actualizá Caja y volvé a confirmar el arqueo.");
        jdbc.update("""
            INSERT INTO lamontana.caja_arqueo(id_operacion,id_sucursal,id_actor,actor_nombre,fecha,
                esperado,contado,observacion,huella) VALUES (?,?,?,?,?,?,?,?,?)
            """, in.operacion(), acceso.sucursal(), acceso.actor(), acceso.nombre(), ahora, esperado, contado,
            in.observacion() == null ? "" : in.observacion().strip(), huella);
    }

    @Transactional(readOnly=true)
    public Comprobante comprobante(UUID sucursal, UUID movimiento, String correo) {
        var acceso = acceso(sucursal, correo);
        var hoy = LocalDate.ofInstant(clock.instant(), acceso.zona());
        return jdbc.query("""
            SELECT comprobante_tipo,comprobante FROM lamontana.caja_egreso
            WHERE id_operacion=? AND id_sucursal=? AND comprobante IS NOT NULL AND
                  (? OR fecha>=? AND fecha<?)
            """, (rs, n) -> new Comprobante(rs.getString(1),rs.getBytes(2)), movimiento, acceso.sucursal(),
            acceso.admin(), Timestamp.from(hoy.atStartOfDay(acceso.zona()).toInstant()),
            Timestamp.from(hoy.plusDays(1).atStartOfDay(acceso.zona()).toInstant())).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"El comprobante no está disponible."));
    }

    private Acceso acceso(UUID sucursal, String correo) {
        return jdbc.query("""
            SELECT s.id_sucursal,u.id_usuario,u.nombre||' '||u.apellido,s.zona_horaria,
                   r.codigo='ADMIN_ADMIN',s.estado='ACTIVA'
            FROM lamontana.sucursal s
            JOIN lamontana.usuario u ON u.correo=? AND u.estado='ACTIVO'
            JOIN lamontana.rol r ON r.id_rol=u.id_rol AND r.activo
            WHERE s.codigo_publico=? AND (r.codigo='ADMIN_ADMIN' OR
                r.codigo='EMPLEADO' AND s.estado='ACTIVA'
                AND EXISTS(SELECT 1 FROM lamontana.sucursal_responsable sr
                    JOIN lamontana.usuario_sucursal us USING(id_sucursal,id_usuario)
                    WHERE sr.id_sucursal=s.id_sucursal AND sr.id_usuario=u.id_usuario AND us.activa)
                AND NOT EXISTS(SELECT 1 FROM lamontana.cuenta_punto cp WHERE cp.id_usuario=u.id_usuario))
            """, (rs, n) -> new Acceso(rs.getLong(1), rs.getLong(2), rs.getString(3),
                ZoneId.of(rs.getString(4)), rs.getBoolean(5), rs.getBoolean(6)), correo, sucursal).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"No tenés acceso a la caja de esta sucursal."));
    }

    private Acceso accesoActivo(UUID sucursal, String correo) {
        var acceso = acceso(sucursal, correo);
        if (!acceso.activa()) throw conflicto("La sucursal está desactivada. Sólo se puede consultar su historial.");
        return acceso;
    }

    private Periodo periodo(Acceso acceso, Instant ahora, LocalDate desde, LocalDate hasta) {
        var hoy = LocalDate.ofInstant(ahora, acceso.zona());
        var inicio = desde == null ? hoy : desde;
        var fin = hasta == null ? hoy : hasta;
        if (!acceso.admin() && (!inicio.equals(hoy) || !fin.equals(hoy)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Como encargado podés consultar los movimientos del día.");
        if (inicio.getYear()<1900 || inicio.isAfter(fin) || fin.isAfter(hoy) || ChronoUnit.DAYS.between(inicio, fin) > 365)
            throw invalido("Elegí un período válido de hasta 366 días, sin fechas futuras.");
        return new Periodo(hoy,inicio,fin,Timestamp.from(inicio.atStartOfDay(acceso.zona()).toInstant()),
            Timestamp.from(fin.plusDays(1).atStartOfDay(acceso.zona()).toInstant()));
    }

    private long contar(String tabla, Acceso acceso, Periodo periodo, Timestamp ahora) {
        return jdbc.queryForObject("SELECT count(*) FROM lamontana." + tabla + PERIODO, Long.class,
            acceso.sucursal(), periodo.inicio(), periodo.fin(), ahora);
    }

    private boolean yaRegistrado(String tabla, UUID operacion, Acceso acceso, String huella) {
        return jdbc.query("SELECT id_sucursal,id_actor,huella FROM lamontana." + tabla + " WHERE id_operacion=?",
            (rs, n) -> {
                if (rs.getLong(1) != acceso.sucursal() || rs.getLong(2) != acceso.actor() || !huella.equals(rs.getString(3)))
                    throw conflicto("La operación ya se utilizó con otros datos.");
                return true;
            }, operacion).stream().findFirst().orElse(false);
    }

    private void bloquearLibro() { jdbc.execute("SELECT pg_advisory_xact_lock(764004)"); }

    private BigDecimal importe(String valor, boolean permiteCero) {
        var importe = new BigDecimal(valor).setScale(2);
        if (!permiteCero && importe.signum() <= 0) throw invalido("El importe debe ser mayor que cero.");
        return importe;
    }

    private String extension(byte[] bytes) {
        if (bytes == null) return null;
        if (empieza(bytes, 0x25, 0x50, 0x44, 0x46, 0x2d)) return "pdf";
        if (empieza(bytes, 0xff, 0xd8, 0xff)) return "jpg";
        if (empieza(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) return "png";
        throw invalido("El comprobante debe ser un PDF, PNG o JPEG válido.");
    }

    private boolean empieza(byte[] bytes, int... firma) {
        if (bytes.length <= firma.length) return false;
        for (int i=0;i<firma.length;i++) if (Byte.toUnsignedInt(bytes[i]) != firma[i]) return false;
        return true;
    }

    private String huella(Object entrada) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(json.writeValueAsString(entrada).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private ResponseStatusException invalido(String mensaje) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje); }
    private ResponseStatusException conflicto(String mensaje) { return new ResponseStatusException(HttpStatus.CONFLICT,mensaje); }
}
