package ar.com.lamontana.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
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

@Composable
internal fun RiderDesk(
    state: AppUiState,
    onRefresh: () -> Unit,
    onPresent: (String, Set<String>) -> Unit,
    onAction: (RiderOrder, String, String) -> Unit,
    onLogout: () -> Unit,
) {
    var stopKey by remember { mutableStateOf<String?>(null) }
    var section by rememberSaveable { mutableStateOf(RiderSection.CURRENT) }
    val board = remember(state.riderRoutes) { riderBoard(state.riderRoutes) }
    val open = (board.pending + board.delivered).firstOrNull { it.key == stopKey }
    RiderRouteScreen(
        board = board,
        section = section,
        onSectionChange = { section = it },
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
    board: RiderBoard,
    onOpenStop: (RiderStop) -> Unit,
    section: RiderSection = RiderSection.CURRENT,
    onSectionChange: (RiderSection) -> Unit = {},
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {},
    loading: Boolean = false,
    error: String? = null,
    message: String? = null,
) {
    var query by rememberSaveable(section) { mutableStateOf("") }
    val stops = board.stops(section, query)
    Scaffold(
        containerColor = Canvas,
        topBar = { MobileHeader("REPARTO", onRefresh, onLogout, loading) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                RiderSection.entries.forEach { item -> NavigationBarItem(
                    selected = section == item, enabled = !loading,
                    onClick = { if (item != section) onSectionChange(item) },
                    icon = { Icon(when (item) {
                        RiderSection.CURRENT -> Icons.Default.LocalShipping
                        RiderSection.POINTS -> Icons.Default.Storefront
                        RiderSection.HOME -> Icons.Default.Home
                        RiderSection.DELIVERED -> Icons.Default.CheckCircle
                    }, null) }, label = { Text(item.title, fontSize = 10.sp) },
                ) }
            }
        },
    ) { padding ->
        key(section) {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(18.dp)) {
                item {
                    Text("Mis repartos", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Sólo tus pedidos asignados.", color = Color(0xFF5D7187), fontSize = 13.sp)
                    error?.let { Notice(it, Color(0xFFFFE9E7), Color(0xFF9B1C1C)) }
                    message?.let { Notice(it, Color(0xFFE5F5EA), Green) }
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
                    RouteSummary(board)
                    OutlinedTextField(value = query, onValueChange = { query = it.take(160) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp), leadingIcon = { Icon(Icons.Default.Search, null) },
                        label = { Text("Cliente, destino o PED-156") })
                    Text(if (section == RiderSection.POINTS) "Puntos y sucursales" else if (section == RiderSection.DELIVERED) "Entregados hoy" else section.title,
                        fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                    Text(if (section == RiderSection.DELIVERED) "Recepciones y entregas confirmadas hoy." else "Pendientes, incluidos los de días anteriores.",
                        fontSize = 12.sp, color = Color(0xFF5D7187), modifier = Modifier.padding(top = 4.dp))
                    Text("${stops.size} paradas · ${stops.sumOf { it.orders.size }} pedidos", fontSize = 12.sp,
                        modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
                }
                items(stops, key = { it.key }) { stop -> RiderStopCard(stop, onOpenStop, loading) }
                if (stops.isEmpty() && !loading && error == null) item {
                    Text(if (query.isNotBlank()) "No encontramos paradas con esa búsqueda." else when (section) {
                        RiderSection.CURRENT -> "No tenés paradas pendientes."
                        RiderSection.POINTS -> "No tenés puntos ni sucursales pendientes."
                        RiderSection.HOME -> "No tenés entregas a domicilio pendientes."
                        RiderSection.DELIVERED -> "Todavía no hay entregas confirmadas hoy."
                    }, modifier = Modifier.padding(vertical = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun RouteSummary(board: RiderBoard) {
    Card(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Blue),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("JORNADA", color = Color(0xFFCFE0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(if (board.pendingOrders == 0) "Sin paradas pendientes" else "${board.pendingOrders} pedidos por completar",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryValue(board.pendingOrders.toString(), "pendientes")
                SummaryValue(board.homeOrders.toString(), "a domicilio")
                SummaryValue(board.deliveredOrders.toString(), "entregados hoy")
            }
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
                "${stop.orders.size} ${if (stop.orders.size == 1) "pedido" else "pedidos"}: ${stop.orders.take(4).joinToString { "PED-$it" }}" +
                    if (stop.orders.size > 4) " y ${stop.orders.size - 4} más" else "",
                fontSize = 12.sp,
                color = Color(0xFF425A72),
                modifier = Modifier.padding(top = 13.dp),
            )
            Button(
                onClick = { onOpen(stop) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().padding(top = 13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (stop.completed) Green else if (stop.order == 1) Orange else Blue),
                shape = RoundedCornerShape(11.dp),
            ) {
                Text(if (stop.completed) "Ver detalle" else if (stop.order == 1) "Abrir próxima parada" else "Ver parada", fontWeight = FontWeight.Bold)
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
                    Text(if (stop.completed) "El punto ya confirmó la recepción de estos pedidos." else if (eligible.isEmpty())
                        "Esperando que el punto confirme la recepción de los pedidos presentados." else
                        "Marcá los pedidos que dejaste. El punto debe confirmar la recepción antes de habilitar el retiro.")
                    if (eligible.size > 1) TextButton(enabled = !loading, onClick = {
                        selected = if (selected.isNotEmpty()) emptySet() else eligible.take(25).map { it.id }.toSet()
                    }) { Text(if (selected.isNotEmpty()) "Quitar selección" else if (eligible.size > 25) "Seleccionar 25" else "Seleccionar todos") }
                    if (eligible.size > 25) Text("Podés confirmar hasta 25 pedidos por vez.")
                }
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.canPresent) Checkbox(
                            checked = item.id in selected,
                            onCheckedChange = { selected = if (!it) selected - item.id else if (selected.size < 25) selected + item.id else selected },
                            enabled = !loading && (item.id in selected || selected.size < 25),
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
        board = riderBoard(listOf(ar.com.lamontana.mobile.model.RiderRoute("ruta", listOf(
            RiderOrder("1", 176, 1, "RETIRO_PUNTO_ENTREGA", "Punto Caballito · Av. Acoyte 550, Caballito", "caballito", "Lucía", "EN_VIAJE", false),
            RiderOrder("2", 138, 1, "ENVIO_DOMICILIO", "Av. Rivadavia 8350, Floresta", null, "Martín Gómez", "EN_VIAJE", false),
            RiderOrder("3", 136, 1, "RETIRO_PUNTO_ENTREGA", "Punto Palermo · Av. Santa Fe 3200, Palermo", "palermo", "Matías", "EN_VIAJE", true),
            RiderOrder("4", 133, 1, "ENVIO_DOMICILIO", "Paraguay 2155, Recoleta", null, "Sofía Rossi", "ENTREGADO", false, finishedAt = "2026-11-07T15:00:00Z"),
        )))),
        onOpenStop = {},
    )
}
