package ar.com.lamontana.mobile.model

enum class AdminSection(val title: String, val destination: MobileDestination) {
    BRANCHES("Sucursales · Mostrador", MobileDestination.BRANCH),
    RIDER("Mis repartos", MobileDestination.RIDER),
    PRINTERS("Impresoras", MobileDestination.PRINTERS),
    PAUSES("Pausas de atención", MobileDestination.PAUSES),
}

val Profile.isAdministrator: Boolean get() = rol == "ADMIN_ADMIN" && !debeCambiarContrasena
fun Profile.canOpen(section: AdminSection): Boolean = isAdministrator && (section != AdminSection.RIDER || repartidor)

data class AdminPrinter(
    val id: String, val branch: String?, val location: String, val name: String,
    val configuredState: String, val retired: Boolean, val enabled: Boolean,
    val sheets: Int?, val capacity: Int, val version: Long, val actor: String?, val updatedAt: String?,
) {
    val canEnable: Boolean get() = !retired && configuredState == "OPERATIVA"
    val available: Boolean get() = canEnable && enabled
}
data class AdminPrinterPanel(val canManage: Boolean, val printers: List<AdminPrinter>)
data class AdminAvailability(
    val id: String, val name: String, val address: String, val status: String,
    val version: Long, val actor: String?, val updatedAt: String?,
    val available: Boolean? = null, val reason: String? = null, val origins: List<AdminOrigin> = emptyList(),
) {
    val outOfService: Boolean get() = available == false || status in listOf("PAUSADA", "DESHABILITADO")
}
data class AdminOrigin(val id: String, val name: String, val available: Boolean)
enum class AdminResource { PRINTER, BRANCH, POINT }
data class AdminCommand(
    val resource: AdminResource, val id: String, val name: String,
    val path: String, val method: String, val body: String, val previousVersion: Long,
)

