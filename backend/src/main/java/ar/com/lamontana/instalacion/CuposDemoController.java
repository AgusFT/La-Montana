package ar.com.lamontana.instalacion;

import ar.com.lamontana.organizacion.OrganizacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "lamontana.instalacion.modo", havingValue = "DEMO")
public class CuposDemoController {
    private final JdbcTemplate jdbc;
    private final OrganizacionService organizacion;

    public CuposDemoController(JdbcTemplate jdbc, OrganizacionService organizacion) {
        this.jdbc = jdbc;
        this.organizacion = organizacion;
    }

    public record Panel(List<Cupo> cupos) {}
    public record Cupo(String tipo, int limite, long usados, List<Registro> registros) {}
    public record Registro(String tipo, UUID codigo, String concepto, String detalle, Instant bajaEn, String bajaPor) {}
    public record Alta(@NotNull UUID operacion, @NotBlank @Size(max=100) String nombre,
                       @NotBlank @Size(max=100) String apellido) {}
    public record Ficha(UUID codigo) {}

    @GetMapping("/cupos")
    public Panel panel() {
        var cupos = new LinkedHashMap<String, Cupo>();
        // Un solo snapshot mantiene el contador y su detalle de acuerdo durante una alta o baja.
        jdbc.query("""
                SELECT c.recurso, c.limite, c.ocupados, r.tipo, r.codigo_publico, r.baja_en,
                       actor.nombre || ' ' || actor.apellido AS baja_por,
                       CASE r.tipo WHEN 'PEDIDO' THEN 'Pedido PED-' || p.id_pedido
                                   WHEN 'CLIENTE' THEN u.nombre || ' ' || u.apellido
                                   WHEN 'COBRO' THEN 'Cobro #' || pago.id_pago
                                   WHEN 'DEVOLUCION' THEN 'Devolución #' || dev.id_reembolso
                                   WHEN 'IMPRESORA' THEN coalesce(imp.nombre, 'Impresora #' || i.id_impresora) END AS concepto,
                       CASE r.tipo WHEN 'PEDIDO' THEN initcap(replace(p.estado, '_', ' '))
                                   WHEN 'CLIENTE' THEN 'Ficha de cliente'
                                   WHEN 'COBRO' THEN 'ARS ' || pago.importe || ' · ' || lower(pago.medio)
                                   WHEN 'DEVOLUCION' THEN 'ARS ' || dev.importe || ' · ' || lower(dev.medio)
                                   WHEN 'IMPRESORA' THEN 'Impresora simulada' END AS detalle
                FROM lamontana.demo_cupo c
                LEFT JOIN lamontana.demo_cupo_registro r ON r.recurso=c.recurso
                LEFT JOIN lamontana.usuario actor ON actor.id_usuario=r.id_actor_baja
                LEFT JOIN lamontana.usuario u ON r.tipo='CLIENTE' AND u.codigo_publico=r.codigo_publico
                LEFT JOIN lamontana.pedido p ON r.tipo='PEDIDO' AND p.codigo_publico=r.codigo_publico
                LEFT JOIN lamontana.pago pago ON r.tipo='COBRO' AND pago.codigo_publico=r.codigo_publico
                LEFT JOIN lamontana.reembolso dev ON r.tipo='DEVOLUCION' AND dev.codigo_publico=r.codigo_publico
                LEFT JOIN lamontana.impresora i ON r.tipo='IMPRESORA' AND i.codigo_publico=r.codigo_publico
                LEFT JOIN LATERAL (SELECT ci.nombre FROM lamontana.configuracion_impresora ci
                                   WHERE ci.id_impresora=i.id_impresora
                                   ORDER BY ci.id_configuracion_version DESC LIMIT 1) imp ON true
                ORDER BY CASE c.recurso WHEN 'PEDIDOS' THEN 1 WHEN 'CLIENTES' THEN 2 WHEN 'CAJA' THEN 3 ELSE 4 END,
                         r.baja_en DESC NULLS FIRST, concepto, r.codigo_publico
                """, rs -> {
            String recurso = rs.getString("recurso");
            var cupo = cupos.get(recurso);
            if (cupo == null) {
                cupo = new Cupo(recurso, rs.getInt("limite"), rs.getLong("ocupados"), new ArrayList<>());
                cupos.put(recurso, cupo);
            }
            if (rs.getString("tipo") != null) {
                var baja = rs.getTimestamp("baja_en");
                cupo.registros().add(new Registro(rs.getString("tipo"), rs.getObject("codigo_publico", UUID.class),
                        rs.getString("concepto"), rs.getString("detalle"), baja == null ? null : baja.toInstant(),
                        rs.getString("baja_por")));
            }
        });
        return new Panel(List.copyOf(cupos.values()));
    }

    @PostMapping("/cupos/{tipo}/{codigo}/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void baja(@PathVariable String tipo, @PathVariable UUID codigo, Principal actor) {
        var aplicado = jdbc.queryForObject("SELECT lamontana.dar_baja_cupo_demo(?, ?, ?)",
                Boolean.class, tipo, codigo, idActor(actor));
        if (aplicado == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El registro no existe en los cupos de la Demo.");
    }

    @PostMapping("/clientes")
    @Transactional
    public Ficha alta(@Valid @RequestBody Alta alta, Principal actor) {
        organizacion.exigirPermiso(actor.getName(), "REGISTRAR_COBRO");
        long idActor = idActor(actor);
        String nombre = alta.nombre().strip();
        String apellido = alta.apellido().strip();
        UUID codigo = UUID.randomUUID();
        int nueva = jdbc.update("""
                INSERT INTO lamontana.demo_alta_cliente(operacion,id_actor,codigo_cliente,nombre,apellido)
                VALUES (?,?,?,?,?) ON CONFLICT (operacion) DO NOTHING
                """, alta.operacion(), idActor, codigo, nombre, apellido);
        if (nueva == 0) {
            return jdbc.query("""
                    SELECT codigo_cliente FROM lamontana.demo_alta_cliente
                    WHERE operacion=? AND id_actor=? AND nombre=? AND apellido=?
                    """, (rs, n) -> new Ficha(rs.getObject(1, UUID.class)), alta.operacion(), idActor, nombre, apellido)
                    .stream().findFirst().orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT, "La operación ya fue utilizada con otros datos."));
        }
        // La ficha comercial participa en el cupo sin habilitar credenciales de acceso.
        jdbc.update("""
                INSERT INTO lamontana.usuario(codigo_publico, id_rol, correo, hash_contrasena, nombre, apellido, estado)
                VALUES (?, (SELECT id_rol FROM lamontana.rol WHERE codigo='CLIENTE'), ?, '!', ?, ?, 'DESACTIVADO')
                """, codigo, "ficha-" + codigo + "@demo.invalid", nombre, apellido);
        return new Ficha(codigo);
    }

    private long idActor(Principal actor) {
        return jdbc.queryForObject("SELECT id_usuario FROM lamontana.usuario WHERE correo=?", Long.class, actor.getName());
    }
}
