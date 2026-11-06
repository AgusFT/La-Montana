package ar.com.lamontana.mobile.data

import ar.com.lamontana.mobile.BuildConfig
import ar.com.lamontana.mobile.model.PointContext
import ar.com.lamontana.mobile.model.PointOrder
import ar.com.lamontana.mobile.model.PointSection
import ar.com.lamontana.mobile.model.Profile
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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

    fun pointOrders(section: PointSection, text: String = ""): List<PointOrder> {
        require(section != PointSection.FINANCE)
        val state = when (section) {
            PointSection.ARRIVING -> "POR_LLEGAR"
            PointSection.READY -> "DISPONIBLES"
            PointSection.DELIVERED -> "ENTREGADOS"
            PointSection.FINANCE -> error("Finanzas usa su propio contrato.")
        }
        val body = JSONObject().put("estado", state).put("texto", text.trim()).put("pagina", 0).toString()
        val rows = JSONObject(secureRequest("/api/punto/pedidos/buscar", "POST", body)).getJSONArray("pedidos")
        return List(rows.length()) { index ->
            val item = rows.getJSONObject(index)
            PointOrder(
                pedido = item.getString("pedido"),
                numero = item.getInt("numero"),
                cliente = item.getString("cliente"),
                estado = item.getString("estado"),
                recorrido = item.getString("recorrido"),
                saldo = item.getString("saldo"),
                medio = item.getString("medio"),
                movimiento = item.getString("movimiento"),
            )
        }
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
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
