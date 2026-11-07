package ar.com.lamontana.mobile

import ar.com.lamontana.mobile.data.BranchDeskApi
import ar.com.lamontana.mobile.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BranchDeskTest {
    private val branch = "00000000-0000-0000-0000-000000000001"
    private val orderId = "00000000-0000-0000-0000-000000000002"
    private val operation = "00000000-0000-0000-0000-000000000003"
    private val attempt = "00000000-0000-0000-0000-000000000004"
    private val path get() = "/api/operacion/sucursales/$branch/retiros"
    private fun row(guest: Boolean = false) = BranchOrder(orderId, 156, "Raúl", guest, "LISTO_PARA_ENTREGA", "1529.00", "TRANSFERENCIA")
    private fun delivery(logistics: String = "LISTO_RETIRO", balance: String = "1529.00", cash: Boolean = false, arrival: Boolean = false) =
        BranchDelivery(4, "LISTO_PARA_ENTREGA", logistics, balance, true, arrival,
            if (balance == "0.00") emptyList() else listOf("Falta acreditar y aplicar el saldo."), cash, balance, true, null, null)
    private fun detail(guest: Boolean = false, logistics: String = "LISTO_RETIRO", balance: String = "1529.00", cash: Boolean = false) =
        BranchOrderDetail(row(guest), delivery(logistics, balance, cash), BranchPaymentOptions(balance, "Sucursal La Plata",
            listOf("EFECTIVO", "TRANSFERENCIA"), false, emptyList(), emptyList()))
    private fun api() = BranchDeskApi(read = { error("No se esperaba consultar $it") }, post = { _, _ -> error("No se esperaba escribir") })

    @Test fun localGuestDoesNotNeedAnInventedCodeButTransferredGuestDoes() {
        assertFalse(detail(guest = true).needsCode)
        assertTrue(detail(guest = true).validCode(""))
        assertTrue(detail(guest = true, logistics = "DISPONIBLE_PUNTO").needsCode)
        assertFalse(detail(guest = true, logistics = "DISPONIBLE_PUNTO").validCode(""))
        assertTrue(detail().needsCode)
    }

    @Test fun customerCodeUsesTheSameAlphabetAndLengthAsTheServer() {
        assertTrue(detail().validCode("ABCD2345"))
        listOf("", "ABCD234", "ABCD23456", "ABCD2340", "abcdefgh", "ABCD23I5").forEach { assertFalse(detail().validCode(it)) }
    }

    @Test fun paidOrderOffersDeliveryWithoutAnotherReceipt() {
        val detail = detail(balance = "0.00")
        assertTrue(detail.paid); assertTrue(detail.canDeliver)
        assertFalse(detail.canCollect("EFECTIVO")); assertFalse(detail.canCollect("TRANSFERENCIA"))
        assertThrows(IllegalArgumentException::class.java) { api().command(branch, detail, BranchAction.RECEIPT, operation, method = "EFECTIVO") }
    }

    @Test fun incomingOrderCannotBeDeliveredOrChargedBeforeReception() {
        val detail = detail(logistics = "EN_VIAJE")
        assertFalse(detail.ready); assertFalse(detail.canDeliver); assertFalse(detail.canCollect("TRANSFERENCIA"))
    }

    @Test fun receivedMoneyAndChangedBalancePreventChargingAgain() {
        val detail = detail()
        assertFalse(detail.copy(payment = detail.payment!!.copy(unappliedMoney = true)).canCollect("TRANSFERENCIA"))
        assertFalse(detail.copy(payment = detail.payment.copy(balance = "100.00")).canCollect("TRANSFERENCIA"))
        assertFalse(detail.copy(payment = null).canCollect("EFECTIVO"))
    }

    @Test fun branchContextControlsDeliveryAndCashScope() {
        val employee = Profile("A", "B", "u", "EMPLEADO", false, false, false)
        val context = BranchContext(listOf(Branch(branch, "La Plata", "Calle 7")), setOf("GESTIONAR_ENTREGAS"), branch)
        assertTrue(context.canDeliver); assertTrue(context.canSeeCash(branch, employee))
        assertFalse(context.canSeeCash(orderId, employee))
        assertFalse(context.copy(cashBranch = null).canSeeCash(branch, employee))
        assertTrue(context.copy(cashBranch = null).canSeeCash(branch, employee.copy(rol = "ADMIN_ADMIN")))
        assertFalse(context.copy(permissions = emptySet()).canDeliver)
    }

    @Test fun pickupSearchIsEncodedAndKeepsBranchTabAndPage() {
        var seen = ""
        val api = BranchDeskApi(read = { seen = it; """{"pedidos":[{"pedido":"$orderId","numero":156,"cliente":"Raúl","invitado":true,"estado":"LISTO_PARA_ENTREGA","saldo":"1529.00","medio":"TRANSFERENCIA"}],"total":27,"pagina":1}""" }, post = { _, _ -> error("read only") })
        val page = api.orders(branch, BranchSection.READY, "Raúl & PED-156", 1)
        assertEquals("$path?bandeja=PARA_RETIRAR&texto=Ra%C3%BAl+%26+PED-156&pagina=1", seen)
        assertEquals(27, page.total); assertTrue(page.orders.single().guest); assertEquals(156L, page.orders.single().number)
    }

    @Test fun branchContextComesFromTheAuthenticatedServerResponse() {
        val api = BranchDeskApi(read = { assertEquals("/api/operacion/contexto", it)
            """{"sucursales":[{"codigoPublico":"$branch","nombre":"La Plata","calle":"Calle 7","numero":"1280","localidad":"La Plata"}],"permisos":["GESTIONAR_ENTREGAS"],"cajaSucursal":null}"""
        }, post = { _, _ -> error("read only") })
        assertEquals(listOf(branch), api.context().branches.map { it.id }); assertNull(api.context().cashBranch)
    }

    @Test fun localGuestDeliveryPreservesVersionAndExplicitNullCode() {
        val command = api().command(branch, detail(guest = true, balance = "0.00"), BranchAction.DELIVER, operation)
        assertEquals("$path/$orderId/entregar", command.path)
        val json = JSONObject(command.body)
        assertEquals(operation, json.getString("operacion")); assertEquals(4, json.getLong("version"))
        assertTrue(json.getBoolean("confirmado")); assertTrue(json.has("codigo")); assertTrue(json.isNull("codigo"))
    }

    @Test fun cashAndDeliveryAreOnlyCombinedWhenEnabledByTheServer() {
        assertThrows(IllegalArgumentException::class.java) { api().command(branch, detail(), BranchAction.CASH_DELIVERY, operation, "ABCD2345", "EFECTIVO") }
        val command = api().command(branch, detail(cash = true), BranchAction.CASH_DELIVERY, operation, "ABCD2345", "EFECTIVO")
        assertEquals("$path/$orderId/cobrar-efectivo", command.path)
        val json = JSONObject(command.body)
        assertEquals("1529.00", json.getString("importe")); assertTrue(json.getBoolean("cerrar")); assertEquals("ABCD2345", json.getString("codigo"))
    }

    @Test fun actualPaymentMethodCanDifferFromTheOriginalChoice() {
        val command = api().command(branch, detail(), BranchAction.RECEIPT, operation, method = "EFECTIVO")
        val json = JSONObject(command.body)
        assertEquals("$path/$orderId/pagos/recibir", command.path)
        assertEquals("EFECTIVO", json.getString("medio")); assertEquals("1529.00", json.getString("importe"))
        assertTrue(json.getBoolean("verificado")); assertTrue(json.getBoolean("aplicarAlPedido"))
        assertFalse(command.toString().contains("1529.00"))
    }

    @Test fun transferRequiresReferenceAndCreditsTheMatchingPendingReport() {
        assertThrows(IllegalArgumentException::class.java) { api().command(branch, detail(), BranchAction.RECEIPT, operation, method = "TRANSFERENCIA") }
        val detail = detail().let { it.copy(payment = it.payment!!.copy(transfers = listOf(BranchTransfer(attempt, "1529.00", "BANCO-1")))) }
        val command = api().command(branch, detail, BranchAction.RECEIPT, operation, method = "TRANSFERENCIA", reference = "BANCO-1")
        assertEquals(attempt, JSONObject(command.body).getString("intento"))
    }

    @Test fun ambiguousTransferReportsRequireReviewInsteadOfGuessing() {
        val detail = detail().let { it.copy(payment = it.payment!!.copy(transfers = listOf(BranchTransfer(attempt, "1529.00", "BANCO-1"), BranchTransfer(orderId, "1529.00", "BANCO-1")))) }
        assertThrows(IllegalArgumentException::class.java) { api().command(branch, detail, BranchAction.RECEIPT, operation, method = "TRANSFERENCIA", reference = "BANCO-1") }
    }

    @Test fun arrivalUsesTheReceivingBranchAndCurrentVersion() {
        val detail = detail(logistics = "EN_VIAJE").let { it.copy(delivery = it.delivery.copy(canReceive = true)) }
        val command = api().command(branch, detail, BranchAction.RECEIVE, operation)
        assertEquals("$path/$orderId/llegada", command.path); assertFalse(JSONObject(command.body).has("codigo"))
        assertThrows(IllegalArgumentException::class.java) { api().command(branch, detail.copy(delivery = detail.delivery.copy(canReceive = false)), BranchAction.RECEIVE, operation) }
    }

    @Test fun unsuccessfulCodeValidationNeverLooksLikeACompletedDelivery() {
        val api = BranchDeskApi(read = { error("mutation") }, post = { _, _ -> """{"aceptado":false,"mensaje":"Código incorrecto"}""" })
        val result = api.execute(api.command(branch, detail(balance = "0.00"), BranchAction.DELIVER, operation, "ABCD2345"))
        assertFalse(result.accepted); assertEquals("Código incorrecto", result.message)
    }

    @Test fun recoveringAReceiptReusesExactlyTheSameOperationAndPayload() {
        val bodies = mutableListOf<String>()
        val api = BranchDeskApi(read = { error("mutation") }, post = { path, body ->
            assertEquals("${this.path}/$orderId/pagos/recibir", path); bodies += body; """{"pendiente":"0.00"}"""
        })
        val command = api.command(branch, detail(), BranchAction.RECEIPT, operation, method = "EFECTIVO")
        assertTrue(api.execute(command).accepted); assertTrue(api.execute(command).accepted)
        assertEquals(bodies[0], bodies[1]); assertEquals(operation, JSONObject(bodies[1]).getString("operacion"))
    }

    @Test fun deliveryOnlyAccountDoesNotRequestFinancialEndpoints() {
        val paths = mutableListOf<String>()
        val api = BranchDeskApi(read = { paths += it
            """{"version":4,"estadoPedido":"LISTO_PARA_ENTREGA","estadoLogistico":"LISTO_RETIRO","saldo":"0.00","puedeGestionar":true,"acciones":[],"bloqueosEntrega":[],"cliente":"Raúl","entrega":null,"cobroEfectivo":null}"""
        }, post = { _, _ -> error("read only") })
        val detail = api.detail(branch, row(), financial = false)
        assertTrue(detail.canDeliver); assertNull(detail.payment); assertEquals(listOf("$path/$orderId"), paths)
    }

    @Test fun invalidIdentifiersCannotEscapeTheScopedEndpoint() {
        assertThrows(IllegalArgumentException::class.java) { api().orders("../admin", BranchSection.READY, "", 0) }
    }

    @Test fun paidPickupDoesNotDependOnAnAdditionalPaymentRequest() {
        val paths = mutableListOf<String>()
        val api = BranchDeskApi(read = { path ->
            paths += path
            check(!path.endsWith("/pagos")) { "Un pedido sin deuda no necesita consultar pagos para poder entregarlo." }
            """{"version":4,"estadoPedido":"LISTO_PARA_ENTREGA","estadoLogistico":"LISTO_RETIRO","saldo":"0.00","puedeGestionar":true,"acciones":[],"bloqueosEntrega":[],"cliente":"Lucía Fernández","entrega":null,"cobroEfectivo":null}"""
        }, post = { _, _ -> error("read only") })
        val detail = api.detail(branch, row(), financial = true)
        assertTrue(detail.canDeliver); assertNull(detail.payment)
        assertEquals("Lucía Fernández", detail.order.customer)
        assertEquals(listOf("$path/$orderId"), paths)
    }

    @Test fun paymentDetailUsesActualOriginAndFiltersMethodsByPermissions() {
        val paths = mutableListOf<String>()
        val api = BranchDeskApi(read = { path -> paths += path
            if (!path.endsWith("/pagos")) """{"version":4,"estadoPedido":"LISTO_PARA_ENTREGA","estadoLogistico":"DISPONIBLE_PUNTO","saldo":"1529.00","puedeGestionar":true,"acciones":[],"bloqueosEntrega":["Falta acreditar el saldo."],"cliente":"Raúl","entrega":null,"cobroEfectivo":null}"""
            else """{"pendiente":"1529.00","sucursal":"Casa Matriz Floresta","permisos":["ACREDITAR_PAGO"],"mediosRecepcion":["EFECTIVO","TRANSFERENCIA"],"pagos":[],"intentos":[{"codigoPublico":"$attempt","importe":"1529.00","referencia":"BANCO-1","estado":"PENDIENTE"}]}"""
        }, post = { _, _ -> error("read only") })
        val detail = api.detail(branch, row(guest = true), financial = true)
        assertTrue(detail.needsCode)
        assertEquals("Casa Matriz Floresta", detail.payment!!.originBranch)
        assertEquals(listOf("TRANSFERENCIA"), detail.payment.methods)
        assertTrue(detail.canCollect("TRANSFERENCIA")); assertFalse(detail.canCollect("EFECTIVO"))
        assertEquals(attempt, detail.payment.transfers.single().id)
        assertEquals(listOf("$path/$orderId", "$path/$orderId/pagos"), paths)
    }

    @Test fun dailyCashKeepsTheServerBalanceAndMovementSigns() {
        val api = BranchDeskApi(read = { assertEquals("/api/operacion/sucursales/$branch/caja?pagina=1", it)
            """{"hoy":"2026-11-07","resumen":{"efectivoActual":"1400.00","efectivoIngresos":"2000.00","efectivoEgresos":"600.00","transferenciasIngresos":"500.00"},"movimientos":[{"tipo":"PAGO_PROVEEDOR","medio":"EFECTIVO","importe":"-600.00","fecha":"2026-11-07T15:00:00Z","actor":"Responsable","concepto":"Resmas"}],"total":26,"pagina":1}"""
        }, post = { _, _ -> error("read only") })
        val cash = api.cash(branch, 1)
        assertEquals("1400.00", cash.current); assertEquals("-600.00", cash.movements.single().amount)
        assertEquals("Resmas", cash.movements.single().concept); assertEquals(1, cash.page)
    }
}
