package ar.com.lamontana.mobile.model

import java.math.BigDecimal

data class Branch(val id: String, val name: String, val address: String)
data class BranchContext(val branches: List<Branch>, val permissions: Set<String>, val cashBranch: String?) {
    val canDeliver: Boolean get() = "GESTIONAR_ENTREGAS" in permissions
    fun canSeeCash(branch: String, profile: Profile?): Boolean =
        branches.any { it.id == branch } && (profile?.rol == "ADMIN_ADMIN" || cashBranch == branch)
}

enum class BranchSection(val title: String, val query: String) {
    READY("Para retirar", "PARA_RETIRAR"), ARRIVING("En camino", "EN_CAMINO"),
    DELIVERED("Entregados", "ENTREGADOS"), CASH("Caja", "")
}

data class BranchOrder(
    val id: String, val number: Long, val customer: String, val guest: Boolean,
    val status: String, val balance: String, val preferredMethod: String,
)
data class BranchPage(val orders: List<BranchOrder>, val total: Int, val page: Int)
data class BranchReceipt(val method: String, val amount: String, val reference: String, val receivedAt: String)
data class BranchTransfer(val id: String, val amount: String, val reference: String)
data class BranchPaymentOptions(
    val balance: String, val originBranch: String, val methods: List<String>, val unappliedMoney: Boolean,
    val transfers: List<BranchTransfer>, val receipts: List<BranchReceipt>,
)
data class BranchDelivery(
    val version: Long, val status: String, val logistics: String, val balance: String,
    val canManage: Boolean, val canReceive: Boolean, val blocks: List<String>,
    val cashEnabled: Boolean, val cashAmount: String?, val cashCanClose: Boolean,
    val deliveredAt: String?, val deliveredBy: String?,
)
data class BranchOrderDetail(val order: BranchOrder, val delivery: BranchDelivery, val payment: BranchPaymentOptions?) {
    val ready: Boolean get() = delivery.status == "LISTO_PARA_ENTREGA" &&
        delivery.logistics in listOf("LISTO_RETIRO", "DISPONIBLE_PUNTO") && delivery.canManage
    val needsCode: Boolean get() = !(order.guest && delivery.logistics == "LISTO_RETIRO")
    val paid: Boolean get() = delivery.balance.toBigDecimalOrNull()?.signum() == 0
    val canDeliver: Boolean get() = ready && delivery.blocks.isEmpty() && paid
    fun canCollect(method: String): Boolean = ready && !paid && payment != null &&
        method in payment.methods && !payment.unappliedMoney &&
        payment.balance.toBigDecimalOrNull()?.compareTo(delivery.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO) == 0
    fun canCollectAndDeliver(method: String): Boolean = canCollect(method) && method == "EFECTIVO" && delivery.cashEnabled
    fun validCode(code: String): Boolean = !needsCode || code.matches(Regex("[A-HJ-NP-Z2-9]{8}"))
}

enum class BranchAction { RECEIVE, DELIVER, CASH_DELIVERY, RECEIPT }
data class BranchCommand(val path: String, val body: String, val action: BranchAction) {
    override fun toString(): String = "BranchCommand(action=$action, datos privados)"
}
data class BranchCommandResult(val accepted: Boolean, val message: String)
data class BranchCashMovement(val type: String, val method: String, val amount: String, val date: String, val actor: String, val concept: String)
data class BranchCash(
    val today: String, val current: String, val incoming: String, val outgoing: String,
    val transfers: String, val movements: List<BranchCashMovement>, val total: Int, val page: Int,
)
