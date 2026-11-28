package ar.com.lamontana.instalacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/demo/tutorial")
@ConditionalOnProperty(name="lamontana.instalacion.modo", havingValue="DEMO")
public class TutorialDemoController {
    static final List<String> PASOS = List.of("tarifa", "archivo", "presupuesto", "confirmacion", "cobro",
            "revision", "produccion", "calidad", "entrega");
    private static final Map<String, Set<String>> CAMPOS = Map.of(
            "archivo", Set.of("archivoActivo", "opciones"),
            "presupuesto", Set.of("modalidad","destino","medio","calle","numero","referencias","codigoPostal","localidad","provincia"),
            "confirmacion", Set.of("receptor","telefono"),
            "cobro", Set.of("importe","medio","referencia","observacion","fecha","formulario"),
            "revision", Set.of("observacion","formulario"),
            "produccion", Set.of("impresora","recurso","observacion","formulario"),
            "calidad", Set.of("resultado","observacion","formulario"),
            "entrega", Set.of("observacion","receptor","formulario"));
    private static final Pattern IDENTIFICADOR = Pattern.compile("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}");
    private static final Set<String> OPCIONES_PDF = Set.of("archivo","choice","copies","double","ends");
    private static final TypeReference<Map<String,String>> TEXTO = new TypeReference<>() {};
    private final JdbcTemplate db;
    private final JsonMapper json = JsonMapper.builder().build();

    public TutorialDemoController(JdbcTemplate db) { this.db = db; }

    public record Cambio(@NotBlank @Size(max=16) String accion, @Size(max=20) String paso,
                         UUID preparacion, UUID pedido, @Size(max=12) Map<String,String> campos,
                         @PositiveOrZero Long intento) {}
    public record Vista(String estado, String paso, List<String> completados, UUID preparacion,
                        UUID cotizacion, UUID pedido, UUID destino, Map<String,Map<String,String>> borradores, String bloqueo,
                        long intento) {}
    private record Actor(long id, String rol) {}
    private record Hechos(String estado, String paso, UUID preparacion, UUID cotizacion, UUID pedido, UUID destino,
                         List<String> completos, boolean inactivo, long intento) {
        String accesible(String solicitado) {
            for (String paso : PASOS) {
                if (paso.equals(solicitado) || !completos.contains(paso)) return paso;
            }
            return solicitado;
        }
    }

    @GetMapping
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Vista consultar(Principal sesion) {
        Actor actor = actor(sesion);
        return vista(actor, hechos(actor));
    }

    @PutMapping
    @Transactional
    public Vista modificar(@Valid @RequestBody Cambio cambio, Principal sesion) {
        Actor actor = actor(sesion);
        db.queryForObject("SELECT unico FROM lamontana.tutorial_demo WHERE unico FOR UPDATE", Boolean.class);
        Hechos actual = hechos(actor);
        if ("reiniciar".equals(cambio.accion())) {
            if (debeReiniciar(cambio.intento(),actual.intento())) {
                db.update("""
                        UPDATE lamontana.tutorial_demo
                        SET intento=intento+1, estado='ACTIVO', paso=DEFAULT,
                            tarifa_revisada=DEFAULT, inicio=clock_timestamp(), preparacion=NULL
                        WHERE unico
                        """);
                db.update("DELETE FROM lamontana.tutorial_borrador");
            }
            return vista(actor, hechos(actor));
        }
        exigirIntento(cambio.intento(),actual.intento());
        switch (cambio.accion()) {
            case "iniciar", "retomar" -> db.update("UPDATE lamontana.tutorial_demo SET estado='ACTIVO', inicio=coalesce(inicio,clock_timestamp())");
            case "pausar" -> db.update("UPDATE lamontana.tutorial_demo SET estado='PAUSADO' WHERE estado='ACTIVO'");
            default -> {
                if (!actual.estado().equals("ACTIVO")) throw conflicto("Retomá el tutorial para continuar.");
                switch (cambio.accion()) {
                    case "ir" -> {
                        exigirPaso(cambio.paso());
                        db.update("UPDATE lamontana.tutorial_demo SET paso=?", actual.accesible(cambio.paso()));
                    }
                    case "tarifa" -> {
                        exigirRol(actor, "ADMIN_ADMIN");
                        db.update("UPDATE lamontana.tutorial_demo SET tarifa_revisada=true");
                        if (!hechos(actor).completos().contains("tarifa"))
                            throw conflicto("Publicá una tarifa de impresión vigente antes de continuar.");
                    }
                    case "vincular" -> vincular(actor, cambio.preparacion(), actual);
                    case "borrador" -> guardarBorrador(actor, cambio);
                    default -> throw invalido("Acción desconocida.");
                }
            }
        }
        return vista(actor, hechos(actor));
    }

    public record Avance(String lesson, int task, int reached, boolean complete, Map<String,String> draft, long intento) {}
    public record CambioRecorrido(@NotBlank String recorrido, @Size(max=48) String lesson, Integer task,
            Integer reached, Boolean complete, @Size(max=16) Map<String,String> draft,
            @Size(max=16) String accion, @PositiveOrZero Long intento) {}
    private static final Set<String> CAMPOS_CLIENTE = Set.of("preparacion","pedido","archivoActivo","opciones","modalidad",
            "destino","medio","calle","numero","referencias","codigoPostal","localidad","provincia","receptor","telefono");

    @GetMapping("/recorridos")
    @Transactional(readOnly=true)
    public Map<String,Avance> recorridos(Principal sesion) {
        return avances(actor(sesion));
    }

    private Map<String,Avance> avances(Actor actor) {
        Map<String,Avance> lista = new LinkedHashMap<>();
        db.query("""
                SELECT recorrido, leccion, tarea, alcanzada, completo,
                       CASE WHEN recorrido=? THEN borrador::text ELSE '{}' END AS campos, intento
                FROM lamontana.tutorial_recorrido
                """, rs -> {
            lista.put(rs.getString(1), new Avance(rs.getString(2),rs.getInt(3),rs.getInt(4),rs.getBoolean(5),
                    json.readValue(rs.getString(6),TEXTO),rs.getLong(7)));
        }, switch(actor.rol()) { case "ADMIN_ADMIN" -> "administrador"; case "CLIENTE" -> "cliente"; default -> ""; });
        return lista;
    }

    @PutMapping("/recorridos")
    @Transactional
    public Map<String,Avance> cambiarRecorrido(@Valid @RequestBody CambioRecorrido cambio, Principal sesion) {
        Actor actor = actor(sesion);
        boolean admin = "administrador".equals(cambio.recorrido());
        if (!admin && !"cliente".equals(cambio.recorrido())) throw invalido("Recorrido desconocido.");
        exigirRol(actor, admin ? "ADMIN_ADMIN" : "CLIENTE");
        if (cambio.accion()!=null) {
            if (!"reiniciar".equals(cambio.accion())) throw invalido("Acción desconocida.");
            long actual=db.queryForObject("SELECT intento FROM lamontana.tutorial_recorrido WHERE recorrido=? FOR UPDATE",
                    Long.class,cambio.recorrido());
            if (debeReiniciar(cambio.intento(),actual)) {
                db.update("""
                        UPDATE lamontana.tutorial_recorrido
                        SET intento=intento+1, leccion=?, tarea=DEFAULT, alcanzada=DEFAULT,
                            completo=DEFAULT, borrador=DEFAULT WHERE recorrido=?
                        """,admin ? "tablero" : "inicio",cambio.recorrido());
            }
            return avances(actor);
        }
        if ((cambio.lesson()==null)!=(cambio.task()==null) ||
                cambio.lesson()!=null && !cambio.lesson().matches("[a-z0-9-]{1,48}") ||
                cambio.task()!=null && (cambio.task()<0 || cambio.task()>100) ||
                cambio.reached()!=null && (cambio.reached()<0 || cambio.reached()>500))
            throw invalido("La posición del tutorial es inválida.");
        var campos = new LinkedHashMap<String,String>();
        if (cambio.draft()!=null) cambio.draft().forEach((clave, valor) -> {
            var permitidos = admin ? Set.of("tarifa","fase","simulacion") : CAMPOS_CLIENTE;
            int limite = clave.equals("simulacion") ? 8192 : clave.equals("fase") ? 512 : clave.equals("tarifa") ? 40960 : clave.equals("opciones") ? 32000 : 1000;
            if ((!permitidos.contains(clave) && !clave.equals("repaso")) || valor==null || valor.length()>limite)
                throw invalido("Revisá los campos del borrador.");
            if (!valor.isEmpty()) {
                if (Set.of("preparacion","pedido","archivoActivo").contains(clave) && !esId(valor))
                    throw invalido("El identificador del trabajo es inválido.");
                if (clave.equals("opciones")) validarOpciones(valor);
                if (clave.equals("repaso") && !valor.equals("si")) throw invalido("El estado del repaso es inválido.");
                if (admin && !clave.equals("repaso")) {
                    try {
                        if (!json.readTree(valor).isObject()) throw invalido("Los datos de la práctica son inválidos.");
                    } catch (tools.jackson.core.JacksonException ex) { throw invalido("Los datos de la práctica son inválidos."); }
                }
            }
            campos.put(clave,valor.isEmpty() ? null : valor);
        });
        int actualizados=db.update("""
                UPDATE lamontana.tutorial_recorrido
                SET leccion=coalesce(?,leccion), tarea=coalesce(?,tarea),
                    alcanzada=greatest(alcanzada,coalesce(?,0)), completo=completo OR coalesce(?,false),
                    borrador=jsonb_strip_nulls(borrador || ?::jsonb)
                WHERE recorrido=? AND intento=?
                """,cambio.lesson(),cambio.task(),cambio.reached(),cambio.complete(),json.writeValueAsString(campos),
                cambio.recorrido(),intentoEscritura(cambio.intento()));
        if (actualizados==0) throw intentoDistinto();
        return avances(actor);
    }

    private Actor actor(Principal sesion) {
        // El selector crea tres sesiones distintas. Un cliente registrado por separado conserva su privacidad.
        return db.query("""
                SELECT u.id_usuario,r.codigo FROM lamontana.usuario u JOIN lamontana.rol r USING(id_rol)
                WHERE lower(u.correo)=lower(?) AND u.estado='ACTIVO' AND (u.codigo_publico,r.codigo) IN (
                ('4d9fdf39-1f90-4d08-b7b6-0c77fc9f0071'::uuid,'ADMIN_ADMIN'),
                ('4d9fdf39-1f90-4d08-b7b6-0c77fc9f0072'::uuid,'EMPLEADO'),
                ('4d9fdf39-1f90-4d08-b7b6-0c77fc9f0073'::uuid,'CLIENTE'))
                """, (rs,n) -> new Actor(rs.getLong(1), rs.getString(2)), sesion.getName()).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"Entrá desde el selector de roles de la demo."));
    }

    private Hechos hechos(Actor actor) {
        return db.query("""
                SELECT t.*, d.intento FROM lamontana.tutorial_progreso t CROSS JOIN lamontana.tutorial_demo d WHERE
                (t.id_usuario IS NULL OR ? <> 'CLIENTE' OR t.id_usuario=?)
                AND (t.id_sucursal IS NULL OR ? <> 'EMPLEADO' OR EXISTS
                    (SELECT FROM lamontana.usuario_sucursal us JOIN lamontana.sucursal s USING(id_sucursal)
                     WHERE us.id_usuario=? AND us.id_sucursal=t.id_sucursal AND us.activa AND s.estado='ACTIVA'))
                """, (rs,n) -> {
                    List<String> completos = new ArrayList<>();
                    boolean baja = rs.getBoolean("inactivo");
                    for (String paso : PASOS) {
                        if (rs.getBoolean(paso) && (!baja || paso.equals("tarifa"))) completos.add(paso);
                    }
                    return new Hechos(rs.getString("estado"), rs.getString("paso"), rs.getObject("preparacion",UUID.class),
                            rs.getObject("cotizacion",UUID.class), rs.getObject("pedido",UUID.class), rs.getObject("destino",UUID.class),
                            List.copyOf(completos), baja, rs.getLong("intento"));
                }, actor.rol(),actor.id(),actor.rol(),actor.id()).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,"Este pedido está fuera de tu ámbito de trabajo."));
    }

    private Vista vista(Actor actor, Hechos h) {
        Map<String,Map<String,String>> borradores = new LinkedHashMap<>();
        db.query("SELECT paso,campos::text FROM lamontana.tutorial_borrador WHERE id_usuario=?", rs -> {
            borradores.put(rs.getString(1),json.readValue(rs.getString(2),TEXTO));
        },actor.id());
        String estado = h.completos().size()==PASOS.size() ? "COMPLETO" : h.estado();
        String bloqueo = h.inactivo() ? "El pedido anterior está cancelado o dado de baja. Abrí una preparación nueva como Cliente para continuar." : null;
        return new Vista(estado,h.accesible(h.paso()),h.completos(),h.preparacion(),h.cotizacion(),h.pedido(),h.destino(),borradores,bloqueo,h.intento());
    }

    private void vincular(Actor actor, UUID id, Hechos h) {
        exigirRol(actor,"CLIENTE");
        if (id==null) throw invalido("Elegí la preparación del pedido.");
        if (id.equals(h.preparacion())) return;
        if (!h.completos().contains("tarifa")) throw conflicto("Primero revisá la tarifa en Administración.");
        if (h.preparacion()!=null && !h.inactivo()) throw conflicto("El tutorial ya está vinculado a otra preparación.");
        boolean propia = Boolean.TRUE.equals(db.queryForObject("""
                SELECT EXISTS (SELECT FROM lamontana.preparacion_cliente WHERE codigo_publico=? AND id_usuario=? AND estado='ABIERTA')
                """, Boolean.class, id, actor.id()));
        if (!propia) throw conflicto("Elegí una preparación abierta de tu sesión.");
        if (h.inactivo()) db.update("DELETE FROM lamontana.tutorial_borrador");
        db.update("UPDATE lamontana.tutorial_demo SET preparacion=?,paso='archivo'", id);
    }

    private void guardarBorrador(Actor actor, Cambio cambio) {
        String paso = cambio.paso();
        exigirPaso(paso);
        Set<String> permitidos = CAMPOS.get(paso);
        if (permitidos==null || cambio.campos()==null || !permitidos.containsAll(cambio.campos().keySet()))
            throw invalido("Este paso no admite esos campos.");
        exigirRol(actor, Set.of("archivo","presupuesto","confirmacion").contains(paso) ? "CLIENTE" : "EMPLEADO");
        var valores = new LinkedHashMap<String,String>();
        cambio.campos().forEach((campo,valor) -> {
            int limite = campo.equals("opciones") ? 32000 : campo.equals("formulario") ? 48 : 1000;
            if (valor==null || valor.length()>limite) throw invalido("Revisá el tamaño del campo.");
            if (!valor.isEmpty() && campo.equals("opciones")) validarOpciones(valor);
            if (!valor.isEmpty() && campo.equals("archivoActivo") && !esId(valor)) throw invalido("Archivo desconocido.");
            valores.put(campo, valor.isEmpty() ? null : valor);
        });
        db.update("""
                INSERT INTO lamontana.tutorial_borrador AS b (id_usuario,paso,campos)
                VALUES (?,?,jsonb_strip_nulls(?::jsonb)) ON CONFLICT (id_usuario,paso) DO UPDATE
                SET campos=jsonb_strip_nulls(b.campos || ?::jsonb)
                """,actor.id(),paso,json.writeValueAsString(valores),json.writeValueAsString(valores));
    }

    private void validarOpciones(String texto) {
        JsonNode lista;
        try { lista = json.readTree(texto); }
        catch (tools.jackson.core.JacksonException ex) { throw invalido("Las opciones de los PDF son inválidas."); }
        if (!lista.isArray() || lista.size()>20) throw invalido("Revisá la lista de PDF del borrador.");
        for (var item : lista) {
            if (!item.isObject() || !OPCIONES_PDF.containsAll(item.propertyNames()) || !esId(item.path("archivo").asString("")))
                throw invalido("Revisá el archivo del borrador.");
            if (item.has("double") && !item.get("double").isBoolean()) throw invalido("Revisá la opción de doble faz.");
            if (item.has("copies") && (!item.get("copies").isString() || !item.get("copies").asString().matches("[0-9]{0,5}")))
                throw invalido("Revisá la cantidad de copias.");
            if (item.has("ends")) {
                var extras = item.get("ends");
                if (!extras.isArray() || extras.size()>50) throw invalido("Revisá las terminaciones.");
                for (var extra : extras) if (!esId(extra.asString(""))) throw invalido("Terminación desconocida.");
            }
            if (item.has("choice")) {
                if (!item.get("choice").isString()) throw invalido("Revisá las opciones de impresión.");
                String[] partes = item.get("choice").asString().split(":",-1);
                if (partes.length>4) throw invalido("Revisá las opciones de impresión.");
                for (int i=0;i<partes.length;i++) {
                    if (partes[i].isEmpty()) continue;
                    if (i<3 ? !esId(partes[i]) : !Set.of("COLOR","BLANCO_NEGRO").contains(partes[i]))
                        throw invalido("Revisá el formato, el papel y el color.");
                }
            }
        }
    }
    private static boolean esId(String texto) { return IDENTIFICADOR.matcher(texto).matches(); }

    // Los clientes anteriores a V56 pertenecen al intento 0.
    private static long intentoEscritura(Long intento) { return intento==null ? 0L : intento; }

    private static ResponseStatusException intentoDistinto() {
        return conflicto("El tutorial se reinició en otra pestaña. Volvé a abrirlo para usar el avance actual.");
    }
    private static void exigirIntento(Long solicitado,long actual) {
        if (intentoEscritura(solicitado)!=actual) throw intentoDistinto();
    }
    private static boolean debeReiniciar(Long solicitado,long actual) {
        if (solicitado==null) throw invalido("Indicá el intento que querés reiniciar.");
        // Si se perdió la respuesta al primer reinicio, devolver el nuevo intento tal como está.
        if (actual>0 && solicitado==actual-1) return false;
        exigirIntento(solicitado,actual);
        return true;
    }

    private static void exigirPaso(String paso) {
        if (paso==null || !PASOS.contains(paso)) throw invalido("Paso desconocido.");
    }
    private static void exigirRol(Actor actor, String rol) {
        if (!actor.rol().equals(rol)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cambiá al rol indicado para este paso.");
    }
    private static ResponseStatusException invalido(String texto) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,texto);}
    private static ResponseStatusException conflicto(String texto) {return new ResponseStatusException(HttpStatus.CONFLICT,texto);}
}
