package ar.com.lamontana.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.lamontana.mobile.data.LaMontanaApi
import ar.com.lamontana.mobile.model.MobileDestination
import ar.com.lamontana.mobile.model.PointContext
import ar.com.lamontana.mobile.model.PointOrder
import ar.com.lamontana.mobile.model.PointSection
import ar.com.lamontana.mobile.model.Profile
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
    val pointOrders: List<PointOrder> = emptyList(),
    val error: String? = null,
)

class AppViewModel(private val api: LaMontanaApi = LaMontanaApi()) : ViewModel() {
    private val mutableState = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()

    fun login(user: String, password: String) {
        if (user.isBlank() || password.isBlank()) {
            mutableState.value = mutableState.value.copy(error = "Ingresá tu usuario y contraseña.")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            runCatching { withContext(Dispatchers.IO) { api.login(user, password) } }
                .onSuccess(::openProfile)
                .onFailure { mutableState.value = AppUiState(error = it.message ?: "No pudimos iniciar sesión.") }
        }
    }

    fun refreshPoint() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            runCatching { withContext(Dispatchers.IO) { api.pointContext() to api.pointOrders(PointSection.READY) } }
                .onSuccess { (context, orders) -> mutableState.value = mutableState.value.copy(loading = false, point = context, pointOrders = orders) }
                .onFailure { mutableState.value = mutableState.value.copy(loading = false, error = it.message) }
        }
    }

    fun loadPointOrders(section: PointSection, text: String = "") {
        if (section == PointSection.FINANCE) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            runCatching { withContext(Dispatchers.IO) { api.pointOrders(section, text) } }
                .onSuccess { mutableState.value = mutableState.value.copy(loading = false, pointOrders = it) }
                .onFailure { mutableState.value = mutableState.value.copy(loading = false, error = it.message) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { api.logout() } }
            mutableState.value = AppUiState()
        }
    }

    private fun openProfile(profile: Profile) {
        when (destinationFor(profile)) {
            MobileDestination.POINT -> {
                mutableState.value = AppUiState(destination = MobileDestination.POINT, profile = profile, loading = true)
                refreshPoint()
            }
            MobileDestination.RIDER_PENDING -> mutableState.value = AppUiState(
                destination = MobileDestination.RIDER_PENDING,
                profile = profile,
            )
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
    profile.repartidor -> MobileDestination.RIDER_PENDING
    else -> MobileDestination.UNSUPPORTED
}
