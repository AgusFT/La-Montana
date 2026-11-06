package ar.com.lamontana.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.lamontana.mobile.data.LaMontanaApi
import ar.com.lamontana.mobile.model.ActionResult
import ar.com.lamontana.mobile.model.MobileDestination
import ar.com.lamontana.mobile.model.OrderVersion
import ar.com.lamontana.mobile.model.PointContext
import ar.com.lamontana.mobile.model.PointOrder
import ar.com.lamontana.mobile.model.PointOrderDetail
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
    val message: String? = null,
    val error: String? = null,
)

class AppViewModel(private val api: LaMontanaApi = LaMontanaApi()) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()
    private var retry: Pair<Any, String>? = null

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
            actionFinished(ActionResult(true, "Recepción confirmada. Ya figuran en Para retirar."), ::refreshPoint)
        }
    }

    fun deliverPointOrder(code: String) {
        val detail = state.value.pointDetail ?: return
        if (state.value.loading || !validCode(code)) return
        val operation = operationFor(listOf(detail.order.pedido, detail.order.version, detail.cashAmount, code))
        request({ api.deliverAtPoint(operation, detail, code) }) { result ->
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

    fun logout() {
        if (state.value.loading) return
        mutableState.value = state.value.copy(loading = true)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { api.logout() } }
            retry = null
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
            MobileDestination.UNSUPPORTED -> mutableState.value = AppUiState(
                destination = MobileDestination.UNSUPPORTED,
                profile = profile,
                error = if (profile.debeCambiarContrasena) "Cambiá la contraseña inicial desde la Web antes de usar Android." else null,
            )
            MobileDestination.LOGIN -> mutableState.value = AppUiState()
        }
    }
}

internal fun destinationFor(profile: Profile): MobileDestination = when {
    profile.debeCambiarContrasena -> MobileDestination.UNSUPPORTED
    profile.puntoRetiro -> MobileDestination.POINT
    profile.repartidor -> MobileDestination.RIDER
    else -> MobileDestination.UNSUPPORTED
}
