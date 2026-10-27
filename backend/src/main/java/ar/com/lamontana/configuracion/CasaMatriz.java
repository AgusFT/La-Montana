package ar.com.lamontana.configuracion;

import java.util.UUID;
import ar.com.lamontana.configuracion.EntregaConfiguracionController.Modalidad;

/** Clasifica usando la matriz de la versión o la condición ya capturada; nunca una ubicación de impresora. */
public final class CasaMatriz {
    private CasaMatriz(){}
    public static boolean requiereTraslado(UUID matriz,UUID destino,Modalidad modalidad){
        return modalidad!=Modalidad.RETIRO_SUCURSAL||matriz!=null&&!matriz.equals(destino);
    }
}
