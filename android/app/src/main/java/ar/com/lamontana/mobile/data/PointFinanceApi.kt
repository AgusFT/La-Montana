package ar.com.lamontana.mobile.data

import org.json.JSONArray
import org.json.JSONObject

data class FinanceAccount(val id:String,val use:String,val method:String,val name:String,val instructions:String)
data class Movement(val id:String,val quote:String,val order:String,val date:String,val amount:String,val method:String,
    val recipient:String,val state:String,val reference:String,val canVerify:Boolean)
data class Remittance(val date:String,val amount:String,val method:String,val recipient:String,val state:String,val note:String)
data class FinanceView(val amounts:List<Pair<String,String>>,val movements:List<Movement> = emptyList(),
    val settlements:List<Remittance> = emptyList(),val accounts:List<FinanceAccount> = emptyList(),val available:String="0",val total:Int=0)
data class PointCommand(val path:String,val body:String,val message:String)
internal fun JSONArray.accounts()=List(length()){index->getJSONObject(index).run {
    FinanceAccount(getString("cuenta"),getString("uso"),getString("medio"),getString("nombre"),getString("instrucciones"))
}}
private fun JSONObject.textOr(key:String,fallback:String="")=if(isNull(key)) fallback else getString(key)

class PointFinanceApi(private val read:(String)->String){
    fun collections(period:String,page:Int):FinanceView{
        val j=JSONObject(read("/api/punto/finanzas?periodo=$period&pagina=$page"))
        val rows=j.getJSONArray("movimientos")
        return FinanceView(listOf("Recibido por el punto" to j.getString("recibido"),"Efectivo" to j.getString("efectivo"),
            "Transferencias al punto" to j.getString("transferencia"),"Directo a sucursal" to j.getString("directa"),
            "Total gestionado" to j.getString("gestionado"),"Pendiente de cobro al retirar" to j.getString("pendiente")),List(rows.length()){i->
                rows.getJSONObject(i).run { Movement(getString("codigo"),getString("cotizacion"),getLong("pedido").toString(),getString("fecha"),
                    getString("importe"),getString("medio"),textOr("destinoCobro","Destinatario sin determinar"),getString("estado"),
                    textOr("referencia"),optBoolean("puedeVerificar")) }
            },total=j.getInt("total"))
    }
    fun remittances():FinanceView{
        val j=JSONObject(read("/api/punto/rendiciones"))
        val saldo=j.getJSONObject("saldo")
        val rows=j.getJSONArray("rendiciones")
        return FinanceView(listOf("Pendiente acumulado" to saldo.getString("pendiente"),
            "Informado en espera de confirmación" to saldo.getString("reservado"),"Disponible para rendir" to saldo.getString("disponible")),
            accounts=j.getJSONArray("cuentas").accounts(),available=saldo.getString("disponible"),settlements=List(rows.length()){i->
                rows.getJSONObject(i).run { Remittance(getString("fecha"),getString("importe"),getString("medio"),
                    getString("destinatario"),getString("estado"),listOf(textOr("referencia"),textOr("receptor"),textOr("motivo")).filter { it.isNotBlank() }.joinToString(" · ")) }
            })
    }
}
