package ar.com.lamontana.instalacion;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VigenciaDemoController {
    private final VigenciaDemo vigencia;
    public VigenciaDemoController(VigenciaDemo vigencia) { this.vigencia = vigencia; }

    @GetMapping("/api/demo/vigencia")
    public ResponseEntity<VigenciaDemo.Estado> consultar() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(vigencia.evaluar());
    }
}
