package ar.com.lamontana.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.com.lamontana.mobile.model.*
import ar.com.lamontana.mobile.ui.*

internal data class AdminNavigation(val profile: Profile, val destination: MobileDestination,
    val locked: Boolean, val select: (AdminSection) -> Unit)
internal val LocalAdminNavigation = staticCompositionLocalOf<AdminNavigation?> { null }

@Composable
internal fun AdminMenu(navigation: AdminNavigation) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(enabled = !navigation.locked, onClick = { expanded = true }) {
            Icon(Icons.Default.Menu, "Cambiar vista de administración", tint = Color.White)
        }
        DropdownMenu(expanded, { expanded = false }) {
            Text("Administración", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
            AdminSection.entries.forEach { option ->
                DropdownMenuItem(text = { Text(option.title, color = if (option.destination == navigation.destination) Blue else Ink) },
                    enabled = navigation.profile.canOpen(option), onClick = { expanded = false; navigation.select(option) })
            }
            if (!navigation.profile.repartidor) Text("Habilitá tu usuario como rider en la web para usar Mis repartos.",
                Modifier.widthIn(max = 270.dp).padding(16.dp), fontSize = 12.sp)
        }
    }
}

@Composable
internal fun AdminWorkspace(state: AppUiState, onRefresh: () -> Unit, onLogout: () -> Unit,
    onPrinter: (String, Boolean) -> Unit, onAvailability: (AdminResource, String, Boolean) -> Unit, onRetry: () -> Unit) {
    val printers = state.destination == MobileDestination.PRINTERS
    val locked = state.loading || state.adminUncertain
    val cannotChange = locked || state.adminConflict
    var points by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(state.destination, points) { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf<String?>(null) }
    var branches by remember { mutableStateOf(false) }
    BackHandler(enabled = state.adminUncertain) { }
    Scaffold(containerColor = Canvas, topBar = { MobileHeader(if (printers) "IMPRESORAS" else "PAUSAS", onRefresh, onLogout, locked) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(if (printers) "Impresoras" else "Pausas de atención", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (printers) "Estado operativo de todas las sucursales." else "La pausa impide pedidos nuevos. Los pedidos confirmados siguen su curso.",
                    fontSize = 13.sp, color = Color(0xFF5D7187))
                state.error?.let { Notice(it, Color(0xFFFFE9E7), Color(0xFF9B1C1C)) }
                state.message?.let { Notice(it, Color(0xFFE5F5EA), Green) }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
                if (state.adminUncertain) Button(enabled = !state.loading, onClick = onRetry) { Text("Recuperar el mismo cambio") }
                if (state.adminConflict) OutlinedButton(enabled = !locked, onClick = onRefresh) { Text("Actualizar estado para continuar") }
                if (!printers) Row {
                    TextButton(enabled = !locked, onClick = { points = false }) { Text("Sucursales", fontWeight = if (!points) FontWeight.Bold else FontWeight.Normal) }
                    TextButton(enabled = !locked, onClick = { points = true }) { Text("Puntos de entrega", fontWeight = if (points) FontWeight.Bold else FontWeight.Normal) }
                }
                OutlinedTextField(value = query, onValueChange = { query = it.take(160) }, enabled = !locked,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true,
                    label = { Text(if (printers) "Buscar impresora o sucursal" else "Buscar nombre o dirección") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                if (printers) Box {
                    val options = state.adminPrinters?.printers.orEmpty().map { it.branch to it.location }.distinct().sortedBy { it.second }
                    TextButton(enabled = !locked, onClick = { branches = true }) {
                        Text(options.firstOrNull { it.first == branch }?.second?.takeIf { branch != null } ?: "Todas las sucursales")
                        Icon(Icons.Default.ArrowDropDown, "Filtrar por sucursal")
                    }
                    DropdownMenu(branches, { branches = false }) {
                        DropdownMenuItem(text = { Text("Todas las sucursales") }, onClick = { branch = null; branches = false })
                        options.filter { it.first != null }.forEach { option -> DropdownMenuItem(text = { Text(option.second) }, onClick = { branch = option.first; branches = false }) }
                    }
                }
            }
            if (printers) {
                val panel = state.adminPrinters
                val rows = panel?.printers.orEmpty().filter { (branch == null || branch == it.branch) && "${it.name} ${it.location}".contains(query.trim(), true) }
                items(rows, key = { it.id }) { printer ->
                    AdminCard {
                        Text(printer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(printer.location, fontSize = 13.sp)
                        Text(when { printer.retired -> "Retirada"; printer.configuredState != "OPERATIVA" -> "No operativa · ${printer.configuredState}"; printer.enabled -> "Habilitada"; else -> "Deshabilitada" },
                            color = if (printer.available) Green else Color(0xFFA84300), fontWeight = FontWeight.Bold)
                        Text("Papel: ${printer.sheets?.let { "$it hojas" } ?: "sin conteo registrado"} · Capacidad ${printer.capacity}", fontSize = 12.sp)
                        AuditLine(printer.actor, printer.updatedAt)
                        if (panel?.canManage == true && !printer.retired) {
                            Button(enabled = !cannotChange && (printer.enabled || printer.canEnable),
                                onClick = { onPrinter(printer.id, !printer.enabled) }) { Text(if (printer.enabled) "Deshabilitar impresora" else "Habilitar impresora") }
                        }
                    }
                }
                if (panel != null && rows.isEmpty()) item { Text("No hay impresoras para esta búsqueda.") }
            } else {
                val data = if (points) state.adminPoints else state.adminBranches
                val rows = data.orEmpty().filter { "${it.name} ${it.address}".contains(query.trim(), true) }
                items(rows, key = { it.id }) { item ->
                    val paused = item.status in listOf("PAUSADA", "DESHABILITADO")
                    AdminCard {
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(item.address, fontSize = 13.sp)
                        Text(if (item.status == "SIN_DEFINIR") "Disponibilidad sin definir" else if (item.outOfService) "Momentáneamente fuera de servicio" else "Recibe pedidos nuevos",
                            color = if (item.outOfService || item.status == "SIN_DEFINIR") Color(0xFFA84300) else Green, fontWeight = FontWeight.Bold)
                        if (points && item.origins.isNotEmpty()) Text("Sucursales: " + item.origins.joinToString { it.name + if (it.available) " · disponible" else " · fuera de servicio" }, fontSize = 12.sp)
                        if (points && item.status == "HABILITADO" && item.outOfService) Text("Para recibir pedidos, reanudá una sucursal de origen o configurá otra ruta habilitada.", fontSize = 12.sp)
                        AuditLine(item.actor, item.updatedAt)
                        val resource = if (points) AdminResource.POINT else AdminResource.BRANCH
                        Button(enabled = !cannotChange, onClick = { onAvailability(resource, item.id, if (item.status == "SIN_DEFINIR") false else !paused) }) {
                            Text(if (points && paused) "Quitar pausa propia" else if (paused) "Reanudar atención" else if (item.status == "SIN_DEFINIR") "Habilitar atención" else if (points && item.outOfService) "Pausar también el punto" else "Pausar pedidos nuevos")
                        }
                    }
                }
                if (data != null && rows.isEmpty()) item { Text("No hay coincidencias.") }
            }
        }
    }
}

@Composable private fun AdminCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable private fun AuditLine(actor: String?, date: String?) {
    if (actor != null && date != null) Text("Último cambio: $actor · ${runCatching { java.time.Instant.parse(date).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")) }.getOrDefault(date)}",
        fontSize = 11.sp, color = Color(0xFF5D7187))
}

