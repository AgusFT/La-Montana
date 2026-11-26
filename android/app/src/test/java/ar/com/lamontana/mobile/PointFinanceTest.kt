package ar.com.lamontana.mobile

import ar.com.lamontana.mobile.data.PointFinanceApi
import org.junit.Assert.*
import org.junit.Test

class PointFinanceTest {
    @Test fun collectionsUseTheSelectedPeriodAndKeepBankReceiptsSeparate(){
        val api=PointFinanceApi { path->
            assertEquals("/api/punto/finanzas?periodo=SEMANA&pagina=0",path)
            """{"recibido":"45000","efectivo":"25000","transferencia":"20000","directa":"15000",
                "gestionado":"60000","pendiente":"2600","total":1,"movimientos":[{"codigo":"m","cotizacion":"c",
                "pedido":186,"fecha":"2026-11-25T15:00:00Z","importe":"15000","medio":"TRANSFERENCIA",
                "destinoCobro":"La Plata","estado":"PENDIENTE","referencia":"REF-1","puedeVerificar":false}]}"""
        }
        val data=api.collections("SEMANA",0)
        assertEquals("45000",data.amounts.toMap()["Recibido por el punto"])
        assertEquals("15000",data.amounts.toMap()["Directo a sucursal"])
        assertEquals("186",data.movements.single().order)
        assertFalse(data.movements.single().canVerify)
    }
    @Test fun remittanceBalanceIsIndependentOfTheSelectedCollectionPeriod(){
        val api=PointFinanceApi { path->
            assertEquals("/api/punto/rendiciones",path)
            """{"saldo":{"pendiente":"45000","reservado":"15000","disponible":"30000"},"rendiciones":[
                {"fecha":"2026-11-25","importe":"15000","medio":"TRANSFERENCIA","destinatario":"Sucursal","estado":"CONFIRMADA","referencia":"REF-1","motivo":""}],
                "cuentas":[{"cuenta":"1","uso":"RENDICION","medio":"TRANSFERENCIA","nombre":"Sucursal","instrucciones":"Cuenta autorizada"}]}"""
        }
        val data=api.remittances()
        assertEquals("30000",data.available)
        assertEquals("TRANSFERENCIA",data.accounts.single().method)
        assertEquals("REF-1",data.settlements.single().note)
    }
}
