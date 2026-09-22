package ar.com.lamontana.catalogo;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods=false)
@EnableScheduling
public class CatalogoProgramacionScheduler {
    private final CatalogoService catalogo;
    public CatalogoProgramacionScheduler(CatalogoService catalogo) { this.catalogo=catalogo; }

    // Se invoca el proxy transaccional del servicio; una falla deja pendiente la programación.
    @Scheduled(fixedDelayString="${lamontana.catalogo.reconciliacion-intervalo-ms:5000}",
               initialDelayString="${lamontana.catalogo.reconciliacion-inicio-ms:5000}")
    public void reconciliar() { catalogo.reconciliarProgramaciones(); }
}
