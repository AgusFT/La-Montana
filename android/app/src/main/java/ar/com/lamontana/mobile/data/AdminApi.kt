package ar.com.lamontana.mobile.data

import ar.com.lamontana.mobile.model.*
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class AdminApi(private val read: (String) -> String, private val mutate: (String, String, String) -> String) {
    fun printers(): AdminPrinterPanel {
        val json = JSONObject(read("/api/operacion/impresoras"))
        return AdminPrinterPanel(json.getBoolean("puedeGestionar"), json.getJSONArray("impresoras").rows().map { p ->
            val operation = p.getJSONObject("operacion")
            AdminPrinter(p.getString("codigoPublico"), p.nullText("sucursal"), p.nullText("ubicacion") ?: "Sin sucursal asignada", p.getString("nombre"),
                p.getString("estadoConfigurado"), p.getBoolean("retirada"), operation.getBoolean("habilitada"),
                if (operation.isNull("hojas")) null else operation.getInt("hojas"), p.getInt("capacidadHojas"),
                operation.getLong("version"), operation.nullText("actor"), operation.nullText("actualizadaEn"))
        })
    }

    fun branches(): List<AdminAvailability> = JSONArray(read(BRANCHES)).rows().map {
        availability(it, "estado", "version", "actor", "actualizadaEn")
    }

    fun points(): List<AdminAvailability> = JSONObject(read(POINTS)).getJSONArray("puntos").rows().map { p ->
        val d = p.getJSONObject("disponibilidad")
        AdminAvailability(p.getString("codigoPublico"), p.getString("nombre"), p.getString("direccion"),
            d.getString("estado"), d.getLong("version"), d.nullText("actor"), d.nullText("actualizadaEn"))
    }

    fun printerCommand(printer: AdminPrinter, enable: Boolean, operation: String): AdminCommand {
        require(!printer.retired && (!enable || printer.canEnable) && printer.enabled != enable)
        return AdminCommand(AdminResource.PRINTER, uuid(printer.id), printer.name,
            "/api/operacion/impresoras/${uuid(printer.id)}", "POST",
            JSONObject().put("operacion", uuid(operation)).put("version", printer.version)
                .put("accion", if (enable) "HABILITAR" else "DESHABILITAR").toString(), printer.version)
    }

    fun availabilityCommand(resource: AdminResource, item: AdminAvailability, pause: Boolean, operation: String): AdminCommand {
        require(resource in listOf(AdminResource.BRANCH, AdminResource.POINT))
        val target = if (resource == AdminResource.BRANCH) (if (pause) "PAUSADA" else "ABIERTA")
            else (if (pause) "DESHABILITADO" else "HABILITADO")
        require(item.status != target && item.version >= 0)
        return AdminCommand(resource, uuid(item.id), item.name,
            "${if (resource == AdminResource.BRANCH) BRANCHES else POINTS}/${uuid(item.id)}", "PUT",
            JSONObject().put("operacion", uuid(operation)).put("version", item.version).put("estado", target).toString(), item.version)
    }

    // El comando conserva UUID, versión y contenido al recuperar una respuesta perdida.
    fun execute(command: AdminCommand) {
        val json = JSONObject(mutate(command.path, command.method, command.body))
        val key = when (command.resource) { AdminResource.PRINTER -> "impresora"; AdminResource.BRANCH -> "sucursal"; AdminResource.POINT -> "punto" }
        require(json.getString(key) == command.id && json.getLong("version") > command.previousVersion)
        if (command.resource == AdminResource.PRINTER) json.getBoolean("habilitada")
        else require(json.getString("estado") in if (command.resource == AdminResource.BRANCH) listOf("ABIERTA", "PAUSADA") else listOf("HABILITADO", "DESHABILITADO"))
    }

    private fun availability(p: JSONObject, status: String, version: String, actor: String, date: String) = AdminAvailability(
        p.getString("codigoPublico"), p.getString("nombre"), p.getString("direccion"), p.getString(status),
        p.getLong(version), p.nullText(actor), p.nullText(date))
    private fun JSONArray.rows(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.nullText(key: String): String? = if (isNull(key)) null else getString(key)
    private fun uuid(value: String): String = UUID.fromString(value).toString()

    companion object {
        const val BRANCHES = "/api/admin/sucursales/disponibilidad"
        const val POINTS = "/api/admin/puntos-entrega/disponibilidad"
    }
}
