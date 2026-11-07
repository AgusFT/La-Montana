package ar.com.lamontana.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.lamontana.mobile.data.LaMontanaApi
import ar.com.lamontana.mobile.data.ApiException
import ar.com.lamontana.mobile.model.*
import ar.com.lamontana.mobile.model.ActionResult
import ar.com.lamontana.mobile.model.MobileDestination
import ar.com.lamontana.mobile.model.OrderVersion
import ar.com.lamontana.mobile.model.PointContext
import ar.com.lamontana.mobile.model.PointOrder
import ar.com.lamontana.mobile.model.PointOrderDetail
import ar.com.lamontana.mobile.model.PointPayment
import ar.com.lamontana.mobile.model.PointPage
import ar.com.lamontana.mobile.model.PointSection
import ar.com.lamontana.mobile.model.Profile
import ar.com.lamontana.mobile.model.RiderOrder
import ar.com.lamontana.mobile.model.RiderRoute
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUiState(
    val destination: MobileDestination = MobileDestination.LOGIN,
    val loading: Boolean = false,
    val profile: Profile? = null,
    val point: PointContext? = null,
    val pointPage: PointPage = PointPage(emptyList(), 0, 0),
    val pointSection: PointSection = PointSection.READY,
    val pointQuery: String = "",
    val pointDetail: PointOrderDetail? = null,
    val riderRoutes: List<RiderRoute> = emptyList(),
    val branchContext: BranchContext? = null,
    val branchId: String? = null,
    val branchSection: BranchSection = BranchSection.READY,
    val branchPage: BranchPage = BranchPage(emptyList(), 0, 0),
    val branchQuery: String = "",
    val branchDetail: BranchOrderDetail? = null,
    val branchCash: BranchCash? = null,
    val branchUncertain: Boolean = false,
    val adminPrinters: AdminPrinterPanel? = null,
    val adminBranches: List<AdminAvailability>? = null,
    val adminPoints: List<AdminAvailability>? = null,
    val adminUncertain: Boolean = false,
    val adminConflict: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class AppViewModel(private val api: LaMontanaApi = LaMontanaApi()) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()
    private var retry: Pair<Any, String>? = null
    private data class PendingBranch(val command: BranchCommand, val branch: String, val order: BranchOrder)
    private var pendingBranch: PendingBranch? = null
    private var pendingAdmin: AdminCommand? = null

    fun selectAdminSection(section: AdminSection) {
        val s = state.value
        if (s.loading || s.branchUncertain || s.adminUncertain || s.profile?.canOpen(section) != true) return
        mutableState.value = s.copy(destination = section.destination, branchDetail = null, pointDetail = null,
            branchContext = if (section == AdminSection.BRANCHES) null else s.branchContext,
            message = null, error = null)
        when (section) {
            AdminSection.BRANCHES -> refreshBranch()
            AdminSection.RIDER -> refreshRider()
            else -> refreshAdmin()
        }
    }

    fun refreshAdmin() {
        val s = state.value
        if (s.profile?.isAdministrator != true || s.loading || s.adminUncertain) return
        if (s.destination == MobileDestination.PRINTERS) {
            mutableState.value = s.copy(adminPrinters = null)
            request({ api.admin.printers() }) { mutableState.value = state.value.copy(adminPrinters = it, adminConflict = false) }
        } else if (s.destination == MobileDestination.PAUSES) {
            mutableState.value = s.copy(adminBranches = null, adminPoints = null)
            request({ api.admin.branches() to api.admin.points() }) { (branches, points) ->
                mutableState.value = state.value.copy(adminBranches = branches, adminPoints = points, adminConflict = false)
            }
        }
    }

    fun changePrinter(id: String, enable: Boolean) {
        val s = state.value
        if (s.destination != MobileDestination.PRINTERS || s.adminPrinters?.canManage != true) return
        val printer = s.adminPrinters.printers.firstOrNull { it.id == id } ?: return
        startAdmin { api.admin.printerCommand(printer, enable, UUID.randomUUID().toString()) }
    }

    fun changeAvailability(resource: AdminResource, id: String, pause: Boolean) {
        val s = state.value
        if (s.destination != MobileDestination.PAUSES) return
        val items = when (resource) { AdminResource.BRANCH -> s.adminBranches; AdminResource.POINT -> s.adminPoints; else -> null }
        val item = items?.firstOrNull { it.id == id } ?: return
        startAdmin { api.admin.availabilityCommand(resource, item, pause, UUID.randomUUID().toString()) }
    }

    private fun startAdmin(command: () -> AdminCommand) {
        val s = state.value
        if (s.profile?.isAdministrator != true || s.loading || s.adminUncertain || s.adminConflict || s.branchUncertain) return
        try { pendingAdmin = command(); runAdminCommand() }
        catch (e: IllegalArgumentException) { mutableState.value = state.value.copy(error = "Actualizá el estado antes de elegir ese cambio.") }
    }

    fun retryAdminOperation() { if (!state.value.loading) runAdminCommand() }

    private fun runAdminCommand() {
        val command = pendingAdmin ?: return
        if (state.value.profile?.isAdministrator != true || state.value.loading) return
        mutableState.value = state.value.copy(loading = true, error = null, message = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { api.admin.execute(command) }
                pendingAdmin = null
                mutableState.value = state.value.copy(loading = false, adminUncertain = false,
                    message = "Cambio confirmado en ${command.name}.")
                refreshAdmin()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val uncertain = e !is ApiException || e.status >= 500 || e.status == 408
                if (!uncertain) pendingAdmin = null
                mutableState.value = state.value.copy(loading = false, adminUncertain = uncertain, adminConflict = !uncertain,
                    error = if (uncertain) "No pudimos confirmar la respuesta. Recuperá el mismo cambio antes de continuar."
                    else e.message ?: "Actualizá el estado antes de volver a operar.")
            }
        }
    }

    fun login(user: String, password: String) {
        if (user.isBlank() || password.isBlank()) {
            mutableState.value = mutableState.value.copy(error = "Ingresá tu usuario y contraseña.")
            return
        }
        request({ api.login(user, password) }, ::openProfile)
    }

    fun refreshPoint() {
        val s = state.value
        loadPointOrders(s.pointSection, s.pointQuery, s.pointPage.page)
    }

    fun loadPointOrders(section: PointSection, text: String, page: Int) {
        val s = state.value
        if (s.loading) return
        val changed = section != s.pointSection || text != s.pointQuery
        mutableState.value = s.copy(
            pointSection = section,
            pointQuery = text,
            pointPage = if (changed) PointPage(emptyList(), 0, 0) else s.pointPage,
        )
        request({
            val context = api.pointContext()
            val result = if (section == PointSection.FINANCE) PointPage(emptyList(), 0, 0)
                else api.pointOrders(section, text, page)
            context to if (result.orders.isEmpty() && result.page > 0) api.pointOrders(section, text, 0) else result
        }) { (context, result) ->
            mutableState.value = state.value.copy(point = context, pointPage = result)
        }
    }

    fun openPointOrder(order: PointOrder) = request({ api.pointOrderDetail(order.pedido) }) {
        mutableState.value = state.value.copy(pointDetail = it, message = null)
    }

    fun closePointOrder() {
        if (!state.value.loading) mutableState.value = state.value.copy(pointDetail = null, error = null)
    }

    fun receivePointOrders(ids: Set<String>) {
        if (state.value.loading || state.value.pointSection != PointSection.ARRIVING) return
        val selected = state.value.pointPage.orders.filter { it.puedeRecibir && it.pedido in ids }
            .map { OrderVersion(it.pedido, it.version) }
        if (selected.isEmpty()) return
        val operation = operationFor(selected)
        request({ api.receiveAtPoint(operation, selected) }) {
            closePointOrder()
            actionFinished(ActionResult(true, "Recepción confirmada. Ya figuran en Para retirar."), ::refreshPoint)
        }
    }

    fun deliverPointOrder(code: String, payment: PointPayment?) {
        val detail = state.value.pointDetail ?: return
        if (state.value.loading || !validCode(code)) return
        val operation = operationFor(listOf(detail.order.pedido, detail.order.version, detail.order.saldo, payment, code))
        request({ api.deliverAtPoint(operation, detail, code, payment) }) { result ->
            if (result.accepted) closePointOrder()
            actionFinished(result, ::refreshPoint)
        }
    }

    fun refreshRider() = request({ api.riderRoutes() }) {
        mutableState.value = state.value.copy(riderRoutes = it)
    }

    fun presentRiderOrders(routeId: String, ids: Set<String>) {
        if (state.value.loading) return
        val route = state.value.riderRoutes.firstOrNull { it.id == routeId } ?: return
        val items = route.orders.filter { it.canPresent && it.id in ids }
            .map { OrderVersion(it.id, it.version) }
        if (items.isEmpty()) return
        request({ api.presentAtPoint(routeId, items) }) { actionFinished(it, ::refreshRider) }
    }

    fun riderAction(order: RiderOrder, action: String, code: String) {
        if (state.value.loading || action == "ENTREGAR" && !validCode(code)) return
        val operation = operationFor(listOf(order.id, order.version, action, code))
        request({ api.riderAction(operation, order, action, code) }) { actionFinished(it, ::refreshRider) }
    }

    fun refreshBranch() {
        if (state.value.loading || state.value.branchUncertain || state.value.adminUncertain) return
        if (state.value.branchContext == null) {
            request({ api.branchDesk.context() }) { context ->
                if (!context.canDeliver || context.branches.isEmpty()) {
                    mutableState.value = state.value.copy(destination = if (state.value.profile?.isAdministrator == true) MobileDestination.BRANCH else MobileDestination.UNSUPPORTED,
                        branchContext = context,
                        error = "Tu cuenta necesita una sucursal habilitada y permiso de gestión de entregas para usar el mostrador móvil.")
                } else {
                    mutableState.value = state.value.copy(branchContext = context, branchId = state.value.branchId
                        ?.takeIf { id -> context.branches.any { it.id == id } } ?: context.cashBranch
                        ?.takeIf { id -> context.branches.any { it.id == id } } ?: context.branches.first().id)
                    loadBranchOrders(BranchSection.READY, "", 0)
                }
            }
        } else {
            val s = state.value
            loadBranchOrders(s.branchSection, s.branchQuery, s.branchCash?.page?.takeIf { s.branchSection == BranchSection.CASH } ?: s.branchPage.page)
        }
    }

    fun selectBranch(id: String) {
        val s = state.value
        if (s.loading || s.branchUncertain || s.branchContext?.branches?.none { it.id == id } != false) return
        mutableState.value = s.copy(branchId = id, branchPage = BranchPage(emptyList(), 0, 0), branchCash = null,
            branchDetail = null, branchQuery = "", message = null, error = null)
        loadBranchOrders(BranchSection.READY, "", 0)
    }

    fun loadBranchOrders(section: BranchSection, text: String, page: Int) {
        val s = state.value
        val branch = s.branchId ?: return
        val context = s.branchContext ?: return
        if (s.loading || s.branchUncertain || !context.canDeliver || context.branches.none { it.id == branch }) return
        if (section == BranchSection.CASH && !context.canSeeCash(branch, s.profile)) return
        mutableState.value = s.copy(branchSection = section, branchQuery = text.take(160),
            branchPage = BranchPage(emptyList(), 0, page), branchCash = null, branchDetail = null)
        if (section == BranchSection.CASH) request({ api.branchDesk.cash(branch, page) }) {
            mutableState.value = state.value.copy(branchCash = it)
        } else request({
            val result = api.branchDesk.orders(branch, section, text, page)
            if (result.orders.isEmpty() && result.page > 0) api.branchDesk.orders(branch, section, text, 0) else result
        }) { mutableState.value = state.value.copy(branchPage = it) }
    }

    fun openBranchOrder(order: BranchOrder) {
        if (state.value.loading || state.value.branchUncertain) return
        mutableState.value = state.value.copy(branchDetail = null, message = null)
        readBranchDetail(order)
    }

    private fun readBranchDetail(order: BranchOrder, notice: String? = null) {
        val s = state.value
        val branch = s.branchId ?: return
        val financial = s.branchContext?.permissions?.any { it in listOf("REGISTRAR_COBRO", "ACREDITAR_PAGO", "REGISTRAR_DEVOLUCION") } == true
        request({ api.branchDesk.detail(branch, order, financial) }) {
            mutableState.value = state.value.copy(branchDetail = it, error = notice)
        }
    }

    fun closeBranchOrder() {
        if (state.value.loading || state.value.branchUncertain) return
        mutableState.value = state.value.copy(branchDetail = null, error = null)
        refreshBranch()
    }

    fun branchAction(action: BranchAction, code: String, method: String, reference: String) {
        val s = state.value
        val detail = s.branchDetail ?: return
        val branch = s.branchId ?: return
        if (s.loading || s.branchUncertain || pendingBranch != null) return
        try {
            val command = api.branchDesk.command(branch, detail, action, UUID.randomUUID().toString(), code, method, reference)
            pendingBranch = PendingBranch(command, branch, detail.order)
            runBranchCommand()
        } catch (e: IllegalArgumentException) {
            mutableState.value = s.copy(error = e.message?.takeUnless { it == "Failed requirement." }
                ?: "Actualizá el pedido y revisá el código, el saldo y el medio recibido.")
        }
    }

    fun retryBranchOperation() { if (!state.value.loading) runBranchCommand() }

    private fun runBranchCommand() {
        val pending = pendingBranch ?: return
        if (state.value.loading || state.value.branchId != pending.branch) return
        mutableState.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { api.branchDesk.execute(pending.command) }
                pendingBranch = null
                mutableState.value = state.value.copy(loading = false, branchUncertain = false, branchDetail = null,
                    message = result.message.takeIf { result.accepted }, error = result.message.takeUnless { result.accepted })
                if (pending.command.action == BranchAction.RECEIPT || !result.accepted) {
                    readBranchDetail(pending.order, result.message.takeUnless { result.accepted })
                } else refreshBranch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val uncertain = e !is ApiException || e.status >= 500 || e.status == 408
                if (!uncertain) pendingBranch = null
                mutableState.value = state.value.copy(loading = false, branchUncertain = uncertain,
                    error = if (uncertain) "No pudimos confirmar la respuesta. Recuperá el mismo registro antes de continuar para evitar duplicarlo."
                    else e.message ?: "No pudimos registrar el movimiento.")
            }
        }
    }

    fun logout() {
        if (state.value.loading || state.value.branchUncertain || state.value.adminUncertain) return
        mutableState.value = state.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { api.logout() } }
            retry = null
            pendingBranch = null
            pendingAdmin = null
            mutableState.value = AppUiState()
        }
    }

    private fun <T> request(call: () -> T, onSuccess: (T) -> Unit) {
        if (state.value.loading) return
        mutableState.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { call() }
                mutableState.value = state.value.copy(loading = false)
                onSuccess(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.value = state.value.copy(loading = false, error = e.message ?: "No pudimos completar la consulta.")
            }
        }
    }

    private fun operationFor(key: Any): String {
        val pending = retry?.takeIf { it.first == key } ?: (key to UUID.randomUUID().toString())
        retry = pending
        return pending.second
    }

    private fun actionFinished(result: ActionResult, refresh: () -> Unit) {
        retry = null
        mutableState.value = state.value.copy(
            error = if (result.accepted) null else result.message,
            message = if (result.accepted) result.message else null,
        )
        if (result.accepted) refresh()
    }

    private fun validCode(code: String): Boolean {
        if (code.matches(Regex("[A-HJ-NP-Z2-9]{8}"))) return true
        mutableState.value = state.value.copy(error = "Ingresá los 8 caracteres del código de entrega.")
        return false
    }

    private fun openProfile(profile: Profile) {
        when (destinationFor(profile)) {
            MobileDestination.POINT -> {
                mutableState.value = AppUiState(destination = MobileDestination.POINT, profile = profile)
                refreshPoint()
            }
            MobileDestination.RIDER -> {
                mutableState.value = AppUiState(destination = MobileDestination.RIDER, profile = profile)
                refreshRider()
            }
            MobileDestination.BRANCH -> {
                mutableState.value = AppUiState(destination = MobileDestination.BRANCH, profile = profile)
                refreshBranch()
            }
            MobileDestination.UNSUPPORTED -> mutableState.value = AppUiState(
                destination = MobileDestination.UNSUPPORTED,
                profile = profile,
                error = if (profile.debeCambiarContrasena) "Cambiá la contraseña inicial desde la Web antes de usar Android." else null,
            )
            MobileDestination.LOGIN -> mutableState.value = AppUiState()
            MobileDestination.PRINTERS, MobileDestination.PAUSES -> error("El acceso administrativo se abre desde el menú.")
        }
    }
}
