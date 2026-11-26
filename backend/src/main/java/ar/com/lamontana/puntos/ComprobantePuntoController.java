package ar.com.lamontana.puntos;

import ar.com.lamontana.pagos.ComprobanteController;
import ar.com.lamontana.pagos.ComprobanteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ComprobantePuntoController {
    private final JdbcTemplate jdbc;
    private final PuntoRetiroService puntos;
    private final ComprobanteService comprobantes;
    public ComprobantePuntoController(JdbcTemplate jdbc,PuntoRetiroService puntos,ComprobanteService comprobantes){
        this.jdbc=jdbc;this.puntos=puntos;this.comprobantes=comprobantes;
    }
    public record Adjunto(@NotNull UUID operacion,@NotNull @Size(min=1,max=2097152) byte[] pdf){}
    @PostMapping("/api/punto/avisos/{aviso}/comprobante")
    public ComprobanteService.Archivo adjuntar(@PathVariable UUID aviso,@RequestBody @Valid Adjunto in,Principal p)throws NoSuchAlgorithmException{
        var a=puntos.acceso(p.getName());if(!a.cobros())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cobros deshabilitados.");
        var cotizacion=jdbc.query("""
          SELECT c.codigo_publico FROM lamontana.aviso_punto av JOIN lamontana.instruccion_punto ins ON ins.operacion=av.instruccion
          JOIN lamontana.intento_pago i USING(id_intento_pago) JOIN lamontana.cotizacion c USING(id_cotizacion)
          WHERE ins.id_punto_entrega=? AND i.codigo_publico=?
          """,(r,n)->r.getObject(1,UUID.class),a.punto(),aviso).stream().findFirst()
          .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Aviso no disponible."));
        var hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.pdf()));
        var archivo=comprobantes.crear(cotizacion,new ComprobanteController.Inicio(in.operacion(),aviso,null,"transferencia.pdf",
            "Comprobante presentado en el punto. SHA-256 "+hash),p.getName(),true);
        archivo=comprobantes.recibir(cotizacion,archivo.codigoPublico(),p.getName(),true,new ByteArrayInputStream(in.pdf()),in.pdf().length);
        if(archivo.estado().equals("VALIDANDO"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Continúa la verificación del archivo. Recuperá el registro.");
        if(!archivo.estado().equals("VALIDO"))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,archivo.mensaje());
        return archivo;
    }
}
