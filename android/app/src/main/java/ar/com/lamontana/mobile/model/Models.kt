package ar.com.lamontana.mobile.model

data class Profile(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String,
    val debeCambiarContrasena: Boolean,
    val puntoRetiro: Boolean,
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
)

enum class PointSection(val title: String) {
    ARRIVING("Por llegar"), READY("Para retirar"), DELIVERED("Entregados"), FINANCE("Finanzas")
}

enum class MobileDestination { LOGIN, POINT, RIDER_PENDING, UNSUPPORTED }
