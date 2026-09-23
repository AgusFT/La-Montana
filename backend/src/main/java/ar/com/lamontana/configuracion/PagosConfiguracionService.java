//#region ENCABEZADO · PagosConfiguracionService.java
/*
 * ========================================================================
 * ARCHIVO: PagosConfiguracionService.java
 * ========================================================================
 * FUNCIÓN
 * Valida y guarda los parámetros financieros del borrador y permite simular sus reglas de pago
 * previo y seña sin registrar dinero real.
 *
 * ------------------------------------------------------------------------
 * CONSTRUCTORES DECLARADOS
 * - [public] PagosConfiguracionService(JdbcTemplate jdbc, ConfiguracionService configuracion,
 *   EvaluadorFinanciero evaluador)
 *
 * ------------------------------------------------------------------------
 * MÉTODOS DECLARADOS
 * Incluye métodos privados, sobrecargas y métodos de tipos internos; los accesores generados
 * automáticamente no se enumeran.
 * - [public] ConfiguracionService.Borrador guardar(UUID codigo, GuardarPagos input, String correo)
 * - [public] EvaluadorFinanciero.Simulacion simular(UUID codigo, SimularPagos input, String
 *   correo)
 * - [private] void exigirEditable(ConfiguracionService.Borrador borrador, long version)
 * - [paquete] void comprobar(ConfiguracionService.Borrador b)
 * - [private] Valores validar(Modelo modelo, Criterio criterio, Pagos p)
 * - [private] BigDecimal decimalPositivo(String texto, int decimales, String campo)
 * - [private] void exigir(boolean condicion, String mensaje)
 * - [private] ResponseStatusException error(HttpStatus status, String mensaje)
 *   Construye un error HTTP controlado.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - PagosConfiguracionService (clase).
 * - PagosConfiguracionService.Valores (record).
 * ========================================================================
 */
//#endregion

package ar.com.lamontana.configuracion;

import static ar.com.lamontana.configuracion.ConfiguracionController.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PagosConfiguracionService {
    private final JdbcTemplate jdbc;
    private final ConfiguracionService configuracion;
    private final EvaluadorFinanciero evaluador;
    public PagosConfiguracionService(JdbcTemplate jdbc,ConfiguracionService configuracion,EvaluadorFinanciero evaluador) {
        this.jdbc=jdbc;this.configuracion=configuracion;this.evaluador=evaluador;
    }
    @Transactional
    public ConfiguracionService.Borrador guardar(UUID codigo,GuardarPagos input,String correo) {
        configuracion.bloquear();long actor=configuracion.propietario(correo);String huella=configuracion.huella(input);
        var repetido=configuracion.reintento(input.operacion(),"GUARDAR_PAGOS",codigo,actor,huella);
        if(repetido!=null)return repetido;
        var borrador=configuracion.cargar(codigo);exigirEditable(borrador,input.version());
        Pagos p=input.pagos();Modelo modelo=borrador.modelo();Criterio criterio=borrador.criterio();
        // Desactivar el anticipo de «Seña previa» conserva la aprobación humana.
        if(criterio==Criterio.SENA&&p!=null&&Boolean.FALSE.equals(p.exigirSena())) {modelo=Modelo.MANUAL;criterio=null;}
        var valores=validar(modelo,criterio,p);
        long id=jdbc.queryForObject("SELECT id_configuracion_version FROM lamontana.configuracion_version WHERE codigo_publico=?",Long.class,codigo);
        jdbc.update("""
                INSERT INTO lamontana.configuracion_financiera(id_configuracion_version,vigencia_cotizacion_minutos,instrucciones_transferencia,
                    exigir_sena,condicion_sena,umbral_sena,tipo_sena,valor_sena,umbral_aprobacion)
                VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT(id_configuracion_version) DO UPDATE SET
                    vigencia_cotizacion_minutos=EXCLUDED.vigencia_cotizacion_minutos,instrucciones_transferencia=EXCLUDED.instrucciones_transferencia,
                    exigir_sena=EXCLUDED.exigir_sena,condicion_sena=EXCLUDED.condicion_sena,umbral_sena=EXCLUDED.umbral_sena,
                    tipo_sena=EXCLUDED.tipo_sena,valor_sena=EXCLUDED.valor_sena,umbral_aprobacion=EXCLUDED.umbral_aprobacion
                """,id,p.vigenciaCotizacionMinutos(),p.instruccionesTransferencia()==null?null:p.instruccionesTransferencia().strip(),
                p.exigirSena(),p.condicionSena()==null?null:p.condicionSena().name(),valores.umbralSena(),
                p.tipoSena()==null?null:p.tipoSena().name(),valores.valorSena(),valores.umbralAprobacion());
        jdbc.update("DELETE FROM lamontana.configuracion_medio_pago WHERE id_configuracion_version=?",id);
        for(MedioPago medio:p.medios())jdbc.update("INSERT INTO lamontana.configuracion_medio_pago(id_configuracion_version,medio_pago) VALUES (?,?)",id,medio.name());
        jdbc.update("UPDATE lamontana.configuracion_version SET modelo=?,criterio=?,version=version+1,fecha_actualizacion=clock_timestamp() WHERE id_configuracion_version=?",modelo.name(),criterio==null?null:criterio.name(),id);
        configuracion.registrar(input.operacion(),"GUARDAR_PAGOS",id,actor,huella,"PAGOS_CONFIGURADOS",borrador.version()+1);
        jdbc.update("UPDATE lamontana.autorizacion_configuracion SET fecha_revocacion=clock_timestamp() WHERE id_configuracion_version=? AND fecha_consumo IS NULL AND fecha_revocacion IS NULL",id);
        return configuracion.cargar(codigo);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public EvaluadorFinanciero.Simulacion simular(UUID codigo,SimularPagos input,String correo) {
        configuracion.propietario(correo);var borrador=configuracion.cargar(codigo);exigirEditable(borrador,input.version());
        if(borrador.pagos()==null)throw error(HttpStatus.CONFLICT,"Guardá los parámetros financieros antes de simular.");
        BigDecimal total=decimalPositivo(input.total(),2,"El total de la simulación");
        if(input.carillas()==null||input.carillas()<1)throw error(HttpStatus.BAD_REQUEST,"Las carillas deben ser un entero positivo.");
        return evaluador.evaluar(borrador.modelo(),borrador.criterio(),borrador.pagos(),input.version(),total,input.carillas());
    }
    private void exigirEditable(ConfiguracionService.Borrador borrador,long version) {
        if(!borrador.estado().equals("EN_PREPARACION")||borrador.version()!=version)
            throw error(HttpStatus.CONFLICT,"El borrador cambió o ya no se puede editar. Consultá su versión actual.");
        if(borrador.modelo()==null)throw error(HttpStatus.CONFLICT,"Seleccioná primero el modelo operativo.");
    }
    void comprobar(ConfiguracionService.Borrador b){validar(b.modelo(),b.criterio(),b.pagos());}
    private record Valores(BigDecimal umbralSena,BigDecimal valorSena,BigDecimal umbralAprobacion) {}
    private Valores validar(Modelo modelo,Criterio criterio,Pagos p) {
        exigir(p!=null&&p.medios()!=null&&!p.medios().isEmpty()&&p.medios().size()<=2,"Elegí al menos un medio de pago habilitado.");
        exigir(p.medios().stream().allMatch(Objects::nonNull)&&new HashSet<>(p.medios()).size()==p.medios().size(),"No puede haber medios de pago nulos ni repetidos.");
        exigir(p.vigenciaCotizacionMinutos()!=null&&p.vigenciaCotizacionMinutos()>0,"Ingresá la vigencia de cotización en minutos, mayor que cero.");
        exigir(p.exigirSena()!=null,"Indicá explícitamente si se exige seña.");
        boolean transferencia=p.medios().contains(MedioPago.TRANSFERENCIA);
        if(transferencia)exigir(p.instruccionesTransferencia()!=null&&!p.instruccionesTransferencia().isBlank()&&p.instruccionesTransferencia().length()<=2000,"Ingresá las instrucciones de transferencia.");
        else exigir(p.instruccionesTransferencia()==null,"Las instrucciones corresponden únicamente a transferencia habilitada.");
        if(modelo==Modelo.MANUAL) {
            exigir(p.umbralAprobacion()==null,"El modelo manual no usa umbral de aprobación.");
            exigir(p.condicionSena()!=CondicionSena.SUPERAR_UMBRAL_APROBACION,"Elegí una condición de seña del modelo manual.");
        } else {
            if(criterio==Criterio.PAGO_PREVIO||p.exigirSena())exigir(transferencia,"Habilitá transferencia para acreditar el pago previo o la seña del modelo condicional.");
            switch(criterio) {
                case PAGO_PREVIO -> {
                    exigir(!p.medios().contains(MedioPago.EFECTIVO),"El pago total previo debe acreditarse por transferencia antes de cargar el PDF.");
                    exigir(!p.exigirSena()&&p.umbralAprobacion()==null,"Pago previo no admite una seña adicional ni umbral de aprobación.");
                }
                case SENA -> {
                    exigir(p.exigirSena()&&p.condicionSena()==CondicionSena.SIEMPRE,"La seña previa es obligatoria para todos los pedidos de este modelo.");
                    exigir(p.umbralAprobacion()==null,"La condición de seña previa no usa umbral de aprobación.");
                }
                case MONTO_TOTAL -> exigir(!p.exigirSena()||p.condicionSena()==CondicionSena.SUPERAR_UMBRAL_APROBACION,"Si habilitás seña por monto, se aplica sobre el umbral después de la aprobación humana.");
            }
        }
        BigDecimal umbralAprobacion=criterio==Criterio.MONTO_TOTAL?decimalPositivo(p.umbralAprobacion(),2,"El umbral de aprobación"):null;
        if(!p.exigirSena()) {
            exigir(p.condicionSena()==null&&p.umbralSena()==null&&p.tipoSena()==null&&p.valorSena()==null,"Sin seña, sus condiciones y valores deben quedar vacíos.");
            return new Valores(null,null,umbralAprobacion);
        }
        exigir(p.condicionSena()!=null&&p.tipoSena()!=null,"Completá condición y tipo de seña.");
        BigDecimal umbralSena=null;
        if(p.condicionSena()==CondicionSena.DESDE_CARILLAS) {
            umbralSena=decimalPositivo(p.umbralSena(),0,"El umbral de carillas");
            exigir(umbralSena.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE))<=0,"El umbral de carillas es demasiado grande.");
        } else if(p.condicionSena()==CondicionSena.DESDE_MONTO) umbralSena=decimalPositivo(p.umbralSena(),2,"El umbral de seña");
        else exigir(p.umbralSena()==null,"Esta condición no admite un segundo umbral de seña.");
        BigDecimal valor=decimalPositivo(p.valorSena(),p.tipoSena()==TipoSena.PORCENTAJE?4:2,"El valor de seña");
        if(p.tipoSena()==TipoSena.PORCENTAJE)exigir(valor.compareTo(BigDecimal.valueOf(100))<=0,"El porcentaje de seña no puede superar 100.");
        return new Valores(umbralSena,valor,umbralAprobacion);
    }
    private BigDecimal decimalPositivo(String texto,int decimales,String campo) {
        exigir(texto!=null&&texto.matches(decimales==0?"[0-9]{1,12}":"[0-9]{1,12}(\\.[0-9]{1,"+decimales+"})?"),campo+" debe ser un número positivo con la precisión permitida.");
        var valor=new BigDecimal(texto);exigir(valor.signum()>0,campo+" debe ser mayor que cero.");return valor;
    }
    private void exigir(boolean condicion,String mensaje){if(!condicion)throw error(HttpStatus.BAD_REQUEST,mensaje);}
    private ResponseStatusException error(HttpStatus status,String mensaje){return new ResponseStatusException(status,mensaje);}
}
