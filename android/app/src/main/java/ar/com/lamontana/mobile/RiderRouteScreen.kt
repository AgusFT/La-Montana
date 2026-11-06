package ar.com.lamontana.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.com.lamontana.mobile.model.RiderOrder
import ar.com.lamontana.mobile.ui.Blue
import ar.com.lamontana.mobile.ui.Canvas
import ar.com.lamontana.mobile.ui.Green
import ar.com.lamontana.mobile.ui.LaMontanaTheme
import ar.com.lamontana.mobile.ui.Orange

internal enum class StopType { POINT, HOME, BRANCH }

internal data class RiderStop(
    val order: Int,
    val type: StopType,
    val name: String,
    val address: String,
    val orders: List<Int>,
    val status: String,
    val completed: Boolean = false,
    val routeId: String = "",
    val items: List<RiderOrder> = emptyList(),
) {
    val key: String get() = "$routeId:${items.firstOrNull()?.id ?: order}"
}

@Composable
internal fun RiderDesk(
    state: AppUiState,
    onRefresh: () -> Unit,
    onPresent: (String, Set<String>) -> Unit,
    onAction: (RiderOrder, String, String) -> Unit,
    onLogout: () -> Unit,
) {
    var stopKey by remember { mutableStateOf<String?>(null) }
    val stops = state.riderRoutes.flatMap { route ->
        route.orders.groupBy { order ->
            if (order.mode == "RETIRO_PUNTO_ENTREGA") order.destinationId ?: order.destination else order.id
        }.values.map { items ->
            val first = items.first()
            val type = when (first.mode) {
                "RETIRO_PUNTO_ENTREGA" -> StopType.POINT
                "ENVIO_DOMICILIO" -> StopType.HOME
                else -> StopType.BRANCH
            }
            val finished = items.none { it.status == "EN_VIAJE" || it.status == "CLIENTE_AUSENTE" }
            val status = when {
                finished -> "Completada"
                items.any { it.status == "CLIENTE_AUSENTE" } -> "Sin receptor"
                type == StopType.POINT && items.none { it.canPresent } -> "Pendiente de recepción"
                else -> "Pendiente"
            }
            val name = if (type == StopType.HOME) first.recipient else first.destination.substringBefore(" · ")
            val address = if (type == StopType.HOME) first.destination else first.destination.substringAfter(" · ", "")
            RiderStop(0, type, name, address, items.map { it.number }, status, finished, route.id, items)
        }
    }.mapIndexed { index, stop -> stop.copy(order = index + 1) }
    val open = stops.firstOrNull { it.key == stopKey }
    RiderRouteScreen(
        route = "${state.riderRoutes.size} recorridos asignados",
        stops = stops,
        onOpenStop = { stopKey = it.key },
        onRefresh = onRefresh,
        onLogout = onLogout,
        loading = state.loading,
        error = if (open == null) state.error else null,
        message = state.message,
    )
    open?.let { stop ->
        RiderStopDialog(
            stop, state.loading, state.error, state.message,
            onClose = { if (!state.loading) stopKey = null },
            onPresent = { onPresent(stop.routeId, it) },
            onAction = onAction,
        )
    }
}

@Composable
internal fun RiderRouteScreen(
    route: String,
    stops: List<RiderStop>,
    onOpenStop: (RiderStop) -> Unit,
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {},
    loading: Boolean = false,
    error: String? = null,
    message: String? = null,
) {
    val completed = stops.count { it.completed }
    val orders = stops.sumOf { it.orders.size }
    Scaffold(
        containerColor = Canvas,
        topBar = { MobileHeader("REPARTO", onRefresh, onLogout, loading) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        ) {
            Text("Mis recorridos", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("Sólo ves los pedidos que Administración te asignó.", color = Color(0xFF5D7187), fontSize = 13.sp)
            error?.let { Notice(it, Color(0xFFFFE9E7), Color(0xFF9B1C1C)) }
            message?.let { Notice(it, Color(0xFFE5F5EA), Green) }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            if (stops.isNotEmpty()) {
                RouteSummary(route, orders, completed, stops.size)
                Text("Paradas", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
                stops.forEach { stop -> RiderStopCard(stop, onOpenStop, loading) }
            } else if (!loading && error == null) {
                Text("Todavía no tenés recorridos asignados.", modifier = Modifier.padding(top = 24.dp))
            }
        }
    }
}

@Composable
private fun RouteSummary(route: String, orders: Int, completed: Int, total: Int) {
    Card(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Blue),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("RECORRIDO ACTIVO", color = Color(0xFFCFE0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(route, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF)).padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Text("En reparto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryValue(orders.toString(), "pedidos")
                SummaryValue(total.toString(), "paradas")
                SummaryValue("$completed/$total", "completadas")
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else completed.toFloat() / total },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(7.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color(0x44FFFFFF),
            )
        }
    }
}

@Composable
private fun SummaryValue(value: String, label: String) {
    Column {
        Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFCFE0FF), fontSize = 10.sp)
    }
}

@Composable
private fun RiderStopCard(stop: RiderStop, onOpen: (RiderStop) -> Unit, loading: Boolean) {
    val icon = stopIcon(stop.type)
    val accent = if (stop.completed) Green else if (stop.order == 1) Orange else Blue
    Card(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Parada ${stop.order}", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(stop.status, color = if (stop.completed) Green else Color(0xFF5D7187), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text(stop.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 3.dp))
                    if (stop.address.isNotBlank()) Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFF667A90), modifier = Modifier.size(15.dp))
                        Text(stop.address, color = Color(0xFF667A90), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            Text(
                "${stop.orders.size} ${if (stop.orders.size == 1) "pedido" else "pedidos"}: ${stop.orders.joinToString { "PED-$it" }}",
                fontSize = 12.sp,
                color = Color(0xFF425A72),
                modifier = Modifier.padding(top = 13.dp),
            )
            if (!stop.completed) {
                Button(
                    onClick = { onOpen(stop) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().padding(top = 13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (stop.order == 1) Orange else Blue),
                    shape = RoundedCornerShape(11.dp),
                ) {
                    Text(if (stop.order == 1) "Abrir próxima parada" else "Ver parada", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RiderStopDialog(
    stop: RiderStop,
    loading: Boolean,
    error: String?,
    message: String?,
    onClose: () -> Unit,
    onPresent: (Set<String>) -> Unit,
    onAction: (RiderOrder, String, String) -> Unit,
) {
    val items = stop.items
    val first = items.first()
    val eligible = items.filter { it.canPresent }
    var selected by remember(stop.key, items) { mutableStateOf(emptySet<String>()) }
    var code by remember(stop.key) { mutableStateOf("") }
    var absent by remember(stop.key) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stop.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stop.address.isNotBlank()) Text(stop.address)
                if (stop.type == StopType.POINT) {
                    Text("Marcá los pedidos que dejaste. El punto debe confirmar la recepción antes de habilitar el retiro.")
                    if (eligible.size > 1) TextButton(enabled = !loading, onClick = {
                        selected = if (selected.size == eligible.size) emptySet() else eligible.map { it.id }.toSet()
                    }) { Text(if (selected.size == eligible.size) "Quitar selección" else "Seleccionar todos") }
                }
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.canPresent) Checkbox(
                            checked = item.id in selected,
                            onCheckedChange = { selected = if (it) selected + item.id else selected - item.id },
                            enabled = !loading,
                        )
                        val status = if (item.presented && item.status == "EN_VIAJE") "Pendiente de recepción"
                            else logisticsLabel(item.status)
                        Text("PED-${item.number} · $status")
                    }
                }
                if (stop.type == StopType.HOME && first.status == "EN_VIAJE") {
                    if (absent) Text("Confirmá que no encontraste a una persona para recibir el pedido.")
                    else {
                        OutlinedTextField(
                            value = code, onValueChange = { code = it.trim().uppercase().take(8) },
                            label = { Text("Código de entrega") }, enabled = !loading, singleLine = true,
                        )
                        TextButton(enabled = !loading, onClick = { absent = true }) { Text("Cliente ausente") }
                    }
                }
                error?.let { Text(it, color = Color(0xFFB3261E)) }
                if (error == null) message?.let { Text(it, color = Green) }
            }
        },
        confirmButton = {
            when {
                stop.type == StopType.POINT && eligible.isNotEmpty() ->
                    Button(enabled = !loading && selected.isNotEmpty(), onClick = { onPresent(selected) }) {
                        Text("Confirmar que los dejé (${selected.size})")
                    }
                stop.type == StopType.HOME && first.status == "EN_VIAJE" ->
                    Button(enabled = !loading && (absent || code.length == 8), onClick = {
                        onAction(first, if (absent) "CLIENTE_AUSENTE" else "ENTREGAR", if (absent) "" else code)
                    }) { Text(if (absent) "Confirmar ausencia" else "Confirmar entrega") }
                stop.type == StopType.HOME && first.status == "CLIENTE_AUSENTE" ->
                    Button(enabled = !loading, onClick = { absent = false; onAction(first, "REINTENTAR_ENTREGA", "") }) {
                        Text("Iniciar nuevo intento")
                    }
                stop.type == StopType.BRANCH && first.status == "EN_VIAJE" ->
                    Button(enabled = !loading, onClick = { onAction(first, "LLEGAR_PUNTO", "") }) { Text("Confirmar llegada") }
                else -> Unit
            }
        },
        dismissButton = { TextButton(enabled = !loading, onClick = onClose) { Text("Cerrar") } },
    )
}

private fun stopIcon(type: StopType): ImageVector = when (type) {
    StopType.POINT -> Icons.Default.Storefront
    StopType.HOME -> Icons.Default.Home
    StopType.BRANCH -> Icons.Default.LocalShipping
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RiderRoutePreview() = LaMontanaTheme {
    RiderRouteScreen(
        route = "REC-024",
        stops = listOf(
            RiderStop(1, StopType.POINT, "Punto Caballito", "Av. Acoyte 550, Caballito", listOf(176, 143, 137), "Próxima"),
            RiderStop(2, StopType.HOME, "Martín Gómez", "Av. Rivadavia 8350, Floresta", listOf(138), "Pendiente"),
            RiderStop(3, StopType.POINT, "Punto Palermo", "Av. Santa Fe 3200, Palermo", listOf(136, 135), "Pendiente"),
            RiderStop(4, StopType.HOME, "Sofía Rossi", "Paraguay 2155, Recoleta", listOf(133), "Completada", true),
        ),
        onOpenStop = {},
    )
}
