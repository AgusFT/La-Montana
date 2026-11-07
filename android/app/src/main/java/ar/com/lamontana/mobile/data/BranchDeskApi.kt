package ar.com.lamontana.mobile.data

import ar.com.lamontana.mobile.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Usa los mismos retiros, pagos y caja que la web; siempre dentro de la sucursal autorizada. */
class BranchDeskApi(private val read: (String) -> String, private val post: (String, String) -> String) {
    fun context(): BranchContext {
        val json = JSONObject(read("/api/operacion/contexto"))
        return BranchContext(json.getJSONArray("sucursales").objects().map {
            Branch(it.getString("codigoPublico"), it.getString("nombre"),
                "${it.optString("calle")} ${it.optString("numero")}, ${it.optString("localidad")}")
        }, json.getJSONArray("permisos").strings().toSet(), json.textOrNull("cajaSucursal"))
    }

    fun orders(branch: String, section: BranchSection, text: String, page: Int): BranchPage {
        require(section != BranchSection.CASH && page >= 0)
        val query = java.net.URLEncoder.encode(text.trim().take(160), java.nio.charset.StandardCharsets.UTF_8)
        val json = JSONObject(read("${base(branch)}?bandeja=${section.query}&texto=$query&pagina=$page"))
        return BranchPage(json.getJSONArray("pedidos").objects().map {
            BranchOrder(it.getString("pedido"), it.getLong("numero"), it.getString("cliente"),
                it.getBoolean("invitado"), it.getString("estado"), it.getString("saldo"), it.getString("medio"))
        }, json.getInt("total"), json.getInt("pagina"))
    }

    fun detail(branch: String, order: BranchOrder, financial: Boolean): BranchOrderDetail {
        val path = "${base(branch)}/${uuid(order.id)}"
        val json = JSONObject(read(path))
        val cash = json.optJSONObject("cobroEfectivo")
        val receipt = json.optJSONObject("entrega")
        val delivery = BranchDelivery(json.getLong("version"), json.getString("estadoPedido"),
            json.getString("estadoLogistico"), json.getString("saldo"), json.getBoolean("puedeGestionar"),
            json.getJSONArray("acciones").objects().any { it.getString("accion") == "LLEGAR_PUNTO" && it.getBoolean("habilitada") },
            json.getJSONArray("bloqueosEntrega").strings(), cash?.optBoolean("habilitado") == true,
            cash?.textOrNull("importe"), cash?.optBoolean("puedeCerrar") == true,
            receipt?.textOrNull("fecha"), receipt?.textOrNull("responsable"))
        // Una cuenta de entrega sin permisos financieros puede entregar pedidos ya abonados.
        val payment = if (!financial || delivery.balance.toBigDecimal().signum() == 0 || delivery.status != "LISTO_PARA_ENTREGA" ||
            delivery.logistics !in listOf("LISTO_RETIRO", "DISPONIBLE_PUNTO")) null else {
            val data = JSONObject(read("$path/pagos"))
            val permissions = data.getJSONArray("permisos").strings()
            val methods = data.getJSONArray("mediosRecepcion").strings().filter {
                if (it == "EFECTIVO") "REGISTRAR_COBRO" in permissions else it == "TRANSFERENCIA" && "ACREDITAR_PAGO" in permissions
            }
            val payments = data.getJSONArray("pagos").objects()
            BranchPaymentOptions(data.getString("pendiente"), data.getString("sucursal"), methods,
                payments.any { it.getString("disponible").toBigDecimal().signum() > 0 },
                data.getJSONArray("intentos").objects().filter { it.getString("estado") == "PENDIENTE" }.map {
                    BranchTransfer(it.getString("codigoPublico"), it.getString("importe"), it.getString("referencia"))
                }, payments.map { BranchReceipt(it.getString("medio"), it.getString("importe"),
                    it.getString("referencia"), it.getString("recibidoEn")) })
        }
        return BranchOrderDetail(order.copy(customer = json.getString("cliente"), status = delivery.status, balance = delivery.balance), delivery, payment)
    }

    fun command(branch: String, detail: BranchOrderDetail, action: BranchAction, operation: String,
                code: String = "", method: String = "", reference: String = ""): BranchCommand {
        val path = "${base(branch)}/${uuid(detail.order.id)}"
        val body = JSONObject().put("operacion", uuid(operation))
        val endpoint = when (action) {
            BranchAction.RECEIVE -> { require(detail.delivery.canReceive); "llegada" }
            BranchAction.DELIVER -> { require(detail.canDeliver && detail.validCode(code)); "entregar" }
            BranchAction.CASH_DELIVERY -> {
                require(detail.canCollectAndDeliver(method) && detail.validCode(code))
                body.put("importe", detail.delivery.cashAmount).put("cerrar", detail.delivery.cashCanClose)
                "cobrar-efectivo"
            }
            BranchAction.RECEIPT -> {
                require(detail.canCollect(method) && reference.length <= 160)
                require(method != "TRANSFERENCIA" || reference.isNotBlank())
                val matching = detail.payment!!.transfers.filter {
                    it.reference.trim().equals(reference.trim(), ignoreCase = true) &&
                        it.amount.toBigDecimal().compareTo(detail.delivery.balance.toBigDecimal()) == 0
                }
                require(matching.size <= 1) { "Hay varios avisos con esa referencia. Revisalos desde la web." }
                body.put("medio", method).put("importe", detail.delivery.balance).put("verificado", true)
                    .put("aplicarAlPedido", true).put("referencia", reference.trim().ifBlank { null })
                if (method == "TRANSFERENCIA" && matching.size == 1) body.put("intento", matching.single().id)
                "pagos/recibir"
            }
        }
        if (action != BranchAction.RECEIPT) body.put("version", detail.delivery.version).put("confirmado", true)
        if (action == BranchAction.DELIVER || action == BranchAction.CASH_DELIVERY) {
            body.put("codigo", if (detail.needsCode) code else JSONObject.NULL)
        }
        return BranchCommand("$path/$endpoint", body.toString(), action)
    }

    fun execute(command: BranchCommand): BranchCommandResult {
        val json = JSONObject(post(command.path, command.body))
        return when (command.action) {
            BranchAction.RECEIVE -> { require(json.getLong("version") > 0); BranchCommandResult(true, "Llegada confirmada. El pedido ya está para retirar.") }
            BranchAction.RECEIPT -> { json.getString("pendiente").toBigDecimal(); BranchCommandResult(true, "Cobro registrado. Saldo pendiente ${json.getString("pendiente")}.") }
            else -> BranchCommandResult(json.getBoolean("aceptado"), json.getString("mensaje"))
        }
    }

    fun cash(branch: String, page: Int): BranchCash {
        val json = JSONObject(read("/api/operacion/sucursales/${uuid(branch)}/caja?pagina=$page"))
        val summary = json.getJSONObject("resumen")
        return BranchCash(json.getString("hoy"), summary.getString("efectivoActual"),
            summary.getString("efectivoIngresos"), summary.getString("efectivoEgresos"), summary.getString("transferenciasIngresos"),
            json.getJSONArray("movimientos").objects().map {
                BranchCashMovement(it.getString("tipo"), it.getString("medio"), it.getString("importe"),
                    it.getString("fecha"), it.getString("actor"), it.optString("concepto"))
            }, json.getInt("total"), json.getInt("pagina"))
    }

    private fun base(branch: String) = "/api/operacion/sucursales/${uuid(branch)}/retiros"
    private fun uuid(value: String): String = java.util.UUID.fromString(value).toString()
    private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
    private fun JSONArray.strings() = List(length()) { getString(it) }
    private fun JSONObject.textOrNull(key: String) = if (isNull(key)) null else getString(key)
}
