package ar.com.lamontana.mobile

import ar.com.lamontana.mobile.model.RiderOrder
import ar.com.lamontana.mobile.model.RiderRoute
import java.time.Instant

internal enum class StopType { POINT, HOME, BRANCH }
internal enum class RiderSection(val title: String) {
    CURRENT("Actuales"), POINTS("Puntos"), HOME("A domicilio"), DELIVERED("Entregados")
}
internal data class RiderStop(
    val order: Int, val type: StopType, val name: String, val address: String,
    val orders: List<Int>, val status: String, val completed: Boolean = false,
    val routeId: String = "", val items: List<RiderOrder> = emptyList(), val destinationKey: String = "",
) {
    val key: String get() = "$routeId:$type:${destinationKey.ifBlank { items.firstOrNull()?.id ?: order.toString() }}:$completed"
}
internal data class RiderBoard(val pending: List<RiderStop>, val delivered: List<RiderStop>) {
    val pendingOrders: Int get() = pending.sumOf { it.orders.size }
    val homeOrders: Int get() = pending.filter { it.type == StopType.HOME }.sumOf { it.orders.size }
    val deliveredOrders: Int get() = delivered.sumOf { it.orders.size }
    fun stops(section: RiderSection, query: String = ""): List<RiderStop> {
        val source = when (section) {
            RiderSection.CURRENT -> pending
            RiderSection.POINTS -> pending.filter { it.type != StopType.HOME }
            RiderSection.HOME -> pending.filter { it.type == StopType.HOME }
            RiderSection.DELIVERED -> delivered
        }
        val text = query.trim().lowercase()
        return source.filter { stop -> text.isEmpty() || listOf(stop.name, stop.address).any { text in it.lowercase() } ||
            stop.items.any { text in it.recipient.lowercase() || text == it.number.toString() || text == "ped-${it.number}" } }
    }
}

/** El servidor entrega sólo pendientes y finalizaciones de hoy; no usamos el reloj del celular. */
internal fun riderBoard(routes: List<RiderRoute>): RiderBoard {
    fun stops(completed: Boolean): List<RiderStop> = routes.flatMap { route ->
        route.orders.filter { order ->
            val active = order.status in listOf("EN_VIAJE", "CLIENTE_AUSENTE")
            if (completed) order.status in listOf("DISPONIBLE_PUNTO", "LISTO_RETIRO", "ENTREGADO", "CERRADO") && order.finishedAt != null else active
        }.groupBy { order -> order.mode to if (order.mode == "ENVIO_DOMICILIO") order.id else order.destinationId ?: order.destination }
            .map { (destination, items) ->
                val destinationKey = destination.second
                val first = items.first()
                val type = when (first.mode) { "ENVIO_DOMICILIO" -> StopType.HOME; "RETIRO_PUNTO_ENTREGA" -> StopType.POINT; else -> StopType.BRANCH }
                val status = when {
                    completed -> if (type == StopType.HOME) "Entregado" else "Recepción confirmada"
                    items.any { it.status == "CLIENTE_AUSENTE" } -> "Sin receptor"
                    type == StopType.POINT && items.none { it.canPresent } -> "Pendiente de recepción"
                    else -> "Pendiente"
                }
                RiderStop(0, type, if (type == StopType.HOME) first.recipient else first.destination.substringBefore(" · "),
                    if (type == StopType.HOME) first.destination else first.destination.substringAfter(" · ", ""),
                    items.map { it.number }, status, completed, route.id, items, destinationKey)
            }
    }
    fun instant(value: String?): Instant = runCatching { Instant.parse(value) }.getOrDefault(Instant.MAX)
    val pending = stops(false).sortedWith(compareBy<RiderStop> { if (it.type == StopType.HOME) 0 else 1 }
        .thenBy { it.items.minOfOrNull { order -> instant(order.dueAt) } ?: Instant.MAX }).mapIndexed { i, s -> s.copy(order = i + 1) }
    val delivered = stops(true).sortedByDescending { it.items.maxOfOrNull { order -> instant(order.finishedAt) } }
        .mapIndexed { i, s -> s.copy(order = i + 1) }
    return RiderBoard(pending, delivered)
}
