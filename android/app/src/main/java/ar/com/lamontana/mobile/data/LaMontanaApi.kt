package ar.com.lamontana.mobile.data

import ar.com.lamontana.mobile.BuildConfig
import ar.com.lamontana.mobile.model.ActionResult
import ar.com.lamontana.mobile.model.OrderVersion
import ar.com.lamontana.mobile.model.PointContext
import ar.com.lamontana.mobile.model.PointOrder
import ar.com.lamontana.mobile.model.PointOrderDetail
import ar.com.lamontana.mobile.model.PointPayment
import ar.com.lamontana.mobile.model.PointReceipt
import ar.com.lamontana.mobile.model.PointPage
import ar.com.lamontana.mobile.model.PointSection
import ar.com.lamontana.mobile.model.PointWork
import ar.com.lamontana.mobile.model.Profile
import ar.com.lamontana.mobile.model.RiderOrder
import ar.com.lamontana.mobile.model.RiderRoute
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

class ApiException(message: String, val status: Int) : Exception(message)

class LaMontanaApi(private val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')) {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)

    init {
        CookieHandler.setDefault(cookies)
    }

    fun login(user: String, password: String): Profile {
        val body = "username=${encode(user.trim())}&password=${encode(password)}"
        val csrf = JSONObject(request("/api/auth/csrf")).getString("token")
        request("/api/auth/login", "POST", "application/x-www-form-urlencoded", body, csrf)
        return profile()
    }
    fun profile(): Profile {
        val json = JSONObject(request("/api/auth/me"))
        return Profile(
            nombre = json.getString("nombre"),
            apellido = json.getString("apellido"),
            correo = json.getString("correo"),
            rol = json.getString("rol"),
            debeCambiarContrasena = json.getBoolean("debeCambiarContrasena"),
            puntoRetiro = json.optBoolean("puntoRetiro", false),
            repartidor = json.optBoolean("repartidor", false),
        )
    }

    fun pointContext(): PointContext {
        val json = JSONObject(request("/api/punto/contexto"))
        return PointContext(
            punto = json.getString("punto"),
            nombre = json.getString("nombre"),
            responsable = json.getString("responsable"),
            cobros = json.getBoolean("cobros"),
            porLlegar = json.getInt("porLlegar"),
            disponibles = json.getInt("disponibles"),
            entregados = json.getInt("entregados"),
        )
    }

    fun pointOrders(section: PointSection, text: String, page: Int): PointPage {
        require(section != PointSection.FINANCE)
        val state = when (section) {
            PointSection.ARRIVING -> "POR_LLEGAR"
            PointSection.READY -> "DISPONIBLES"
            PointSection.DELIVERED -> "ENTREGADOS"
            PointSection.FINANCE -> error("Finanzas usa su propio contrato.")
        }
        val body = JSONObject().put("estado", state).put("texto", text.trim()).put("pagina", page).toString()
        val result = JSONObject(secureRequest("/api/punto/pedidos/buscar", "POST", body))
        val rows = result.getJSONArray("pedidos")
        return PointPage(List(rows.length()) { pointOrder(rows.getJSONObject(it)) }, result.getInt("total"), result.getInt("pagina"))
    }

    private fun pointOrder(json: JSONObject) = PointOrder(
        pedido = json.getString("pedido"),
        numero = json.getInt("numero"),
        cliente = json.getString("cliente"),
        estado = json.getString("estado"),
        recorrido = json.getString("recorrido"),
        saldo = json.getString("saldo"),
        medio = json.getString("medio"),
        movimiento = json.getString("movimiento"),
        version = json.getLong("version"),
        puedeRecibir = json.optBoolean("puedeRecibir", false),
        franja = json.optString("franja"),
    )

    fun pointOrderDetail(id: String): PointOrderDetail {
        val detail = JSONObject(request("/api/punto/pedidos/$id"))
        val delivery = detail.getJSONObject("entrega")
        val work = detail.getJSONArray("trabajos")
        val blocks = delivery.getJSONArray("bloqueosEntrega")
        val methods = detail.optJSONArray("mediosSaldo") ?: JSONArray()
        val payments = detail.optJSONArray("pagos") ?: JSONArray()
        return PointOrderDetail(
            order = pointOrder(detail.getJSONObject("pedido")),
            works = List(work.length()) {
                val item = work.getJSONObject(it)
                PointWork(item.getString("nombre"), item.getInt("paginas"), item.getInt("copias"))
            },
            deliveryBlocks = List(blocks.length()) { blocks.getString(it) },
            paymentMethods = List(methods.length()) { methods.getString(it) },
            originBranch = detail.optString("sucursalOrigen"),
            receipts = List(payments.length()) {
                val payment = payments.getJSONObject(it)
                PointReceipt(
                    payment.getString("medio"),
                    payment.getString("importe"),
                    payment.getJSONObject("destinoCobro").getString("nombre"),
                )
            },
        )
    }

    fun receiveAtPoint(operation: String, orders: List<OrderVersion>) {
        val body = ordersPayload(orders).put("operacion", operation)
        secureRequest("/api/punto/recibir", "POST", body.toString())
    }

    fun deliverAtPoint(operation: String, detail: PointOrderDetail, code: String, payment: PointPayment?): ActionResult {
        val body = confirmedAction(operation, detail.order.version).put("codigo", code)
        val action = if (payment == null) "entregar" else {
            body.put("importe", detail.order.saldo)
                .put("medio", payment.medio)
                .put("destino", payment.destino)
                .put("referencia", payment.referencia)
            "cobro-entrega"
        }
        return actionResult(secureRequest("/api/punto/pedidos/${detail.order.pedido}/$action", "POST", body.toString()))
    }

    fun riderRoutes(): List<RiderRoute> {
        val routes = JSONArray(request("/api/rider/recorridos"))
        return List(routes.length()) { index ->
            val route = routes.getJSONObject(index)
            val orders = route.getJSONArray("pedidos")
            RiderRoute(route.getString("codigoPublico"), List(orders.length()) { orderIndex ->
                val order = orders.getJSONObject(orderIndex)
                RiderOrder(
                    id = order.getString("codigoPublico"),
                    number = order.getInt("numero"),
                    version = order.getLong("version"),
                    mode = order.getString("modalidad"),
                    destination = order.getString("destino"),
                    destinationId = if (order.isNull("destinoPublico")) null else order.getString("destinoPublico"),
                    recipient = order.getString("receptor"),
                    status = order.getString("estadoLogistico"),
                    presented = !order.isNull("presentadoEn"),
                )
            })
        }
    }

    fun presentAtPoint(routeId: String, orders: List<OrderVersion>): ActionResult =
        actionResult(secureRequest("/api/rider/recorridos/$routeId/presentar", "POST", ordersPayload(orders).toString()))

    fun riderAction(operation: String, order: RiderOrder, action: String, code: String): ActionResult {
        val body = confirmedAction(operation, order.version)
        val endpoint = if (action == "ENTREGAR") {
            body.put("codigo", code)
            "entregar"
        } else {
            body.put("accion", action)
            "parada"
        }
        return actionResult(secureRequest("/api/rider/pedidos/${order.id}/$endpoint", "POST", body.toString()))
    }

    private fun ordersPayload(orders: List<OrderVersion>): JSONObject =
        JSONObject().put("pedidos", JSONArray().apply {
            orders.forEach { put(JSONObject().put("pedido", it.id).put("version", it.version)) }
        })

    private fun confirmedAction(operation: String, version: Long): JSONObject =
        JSONObject().put("operacion", operation).put("version", version).put("confirmado", true)

    private fun actionResult(body: String): ActionResult {
        val result = JSONObject(body)
        return ActionResult(result.getBoolean("aceptado"), result.getString("mensaje"))
    }

    fun logout() {
        secureRequest("/api/auth/logout", "POST", null)
        cookies.cookieStore.removeAll()
    }

    fun secureRequest(path: String, method: String, body: String?): String {
        val csrf = JSONObject(request("/api/auth/csrf"))
        val token = csrf.getString("token")
        return request(path, method, "application/json", body, token)
    }

    private fun request(
        path: String,
        method: String = "GET",
        contentType: String? = null,
        body: String? = null,
        csrf: String? = null,
    ): String {
        val connection = URI.create("$baseUrl$path").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 12_000
        connection.readTimeout = 18_000
        connection.setRequestProperty("Accept", "application/json")
        contentType?.let { connection.setRequestProperty("Content-Type", it) }
        csrf?.let { connection.setRequestProperty("X-CSRF-TOKEN", it) }
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optString("mensaje") }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: "No se pudo completar la operación."
                throw ApiException(message, status)
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
