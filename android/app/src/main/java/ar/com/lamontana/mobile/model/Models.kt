package ar.com.lamontana.mobile.model

data class Profile(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String,
    val debeCambiarContrasena: Boolean,
    val puntoRetiro: Boolean,
    val repartidor: Boolean,
)

data class PointContext(
    val punto: String,
    val nombre: String,
    val responsable: String,
    val cobros: Boolean,
    val porLlegar: Int,
    val disponibles: Int,
    val entregados: Int,
)

data class PointOrder(
    val pedido: String,
    val numero: Int,
    val cliente: String,
    val estado: String,
    val recorrido: String,
    val saldo: String,
    val medio: String,
    val movimiento: String,
    val version: Long = 0,
    val puedeRecibir: Boolean = false,
    val franja: String = "",
)

data class PointPage(val orders: List<PointOrder>, val total: Int, val page: Int)
data class PointWork(val nombre: String, val paginas: Int, val copias: Int)
data class PointOrderDetail(
    val order: PointOrder,
    val works: List<PointWork>,
    val deliveryBlocks: List<String>,
    val cashAmount: String?,
    val canCloseWithCash: Boolean,
)

data class RiderRoute(val id: String, val orders: List<RiderOrder>)
data class RiderOrder(
    val id: String,
    val number: Int,
    val version: Long,
    val mode: String,
    val destination: String,
    val destinationId: String?,
    val recipient: String,
    val status: String,
    val presented: Boolean,
) {
    val canPresent: Boolean get() = mode == "RETIRO_PUNTO_ENTREGA" && status == "EN_VIAJE" && !presented
}

data class OrderVersion(val id: String, val version: Long)
data class ActionResult(val accepted: Boolean, val message: String)

enum class PointSection(val title: String) {
    ARRIVING("Por llegar"), READY("Para retirar"), DELIVERED("Entregados"), FINANCE("Finanzas")
}

enum class MobileDestination { LOGIN, POINT, RIDER, UNSUPPORTED }
