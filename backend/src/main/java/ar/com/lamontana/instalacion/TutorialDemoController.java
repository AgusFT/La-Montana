package ar.com.lamontana.instalacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
                         UUID preparacion, UUID pedido, @Size(max=12) Map<String,String> campos) {}
    public record Vista(String estado, String paso, List<String> completados, UUID preparacion,
                        UUID cotizacion, UUID pedido, UUID destino, Map<String,Map<String,String>> borradores, String bloqueo) {}
    private record Actor(long id, String rol) {}
    private record Hechos(String estado, String paso, UUID preparacion, UUID cotizacion, UUID pedido, UUID destino,
                         List<String> completos, boolean inactivo) {
        String accesible(String solicitado) {
            for (String paso : PASOS) {
                if (paso.equals(solicitado) || !completos.contains(paso)) return paso;
            }
            return solicitado;
        }
    }

    @GetMapping
    @Transactional(readOnly=true)
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
                SELECT t.* FROM lamontana.tutorial_progreso t WHERE
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
                            List.copyOf(completos), baja);
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
        return new Vista(estado,h.accesible(h.paso()),h.completos(),h.preparacion(),h.cotizacion(),h.pedido(),h.destino(),borradores,bloqueo);
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

    private static void exigirPaso(String paso) {
        if (paso==null || !PASOS.contains(paso)) throw invalido("Paso desconocido.");
    }
    private static void exigirRol(Actor actor, String rol) {
        if (!actor.rol().equals(rol)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cambiá al rol indicado para este paso.");
    }
    private static ResponseStatusException invalido(String texto) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,texto);}
    private static ResponseStatusException conflicto(String texto) {return new ResponseStatusException(HttpStatus.CONFLICT,texto);}
}
