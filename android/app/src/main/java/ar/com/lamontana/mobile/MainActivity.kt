package ar.com.lamontana.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.com.lamontana.mobile.model.MobileDestination
import ar.com.lamontana.mobile.model.isAdministrator
import androidx.compose.runtime.CompositionLocalProvider
import ar.com.lamontana.mobile.model.PointContext
import ar.com.lamontana.mobile.model.PointOrder
import ar.com.lamontana.mobile.model.PointOrderDetail
import ar.com.lamontana.mobile.model.PointPayment
import ar.com.lamontana.mobile.model.PointPage
import ar.com.lamontana.mobile.model.PointSection
import ar.com.lamontana.mobile.ui.Blue
import ar.com.lamontana.mobile.ui.Canvas
import ar.com.lamontana.mobile.ui.Green
import ar.com.lamontana.mobile.ui.Ink
import ar.com.lamontana.mobile.ui.LaMontanaTheme
import ar.com.lamontana.mobile.ui.Navy
import ar.com.lamontana.mobile.ui.Orange

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LaMontanaTheme { LaMontanaApp() } }
    }
}

@Composable
fun LaMontanaApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val admin = state.profile?.takeIf { it.isAdministrator }?.let {
        AdminNavigation(it, state.destination, state.loading || state.branchUncertain || state.adminUncertain, vm::selectAdminSection)
    }
    CompositionLocalProvider(LocalAdminNavigation provides admin) {
    when (state.destination) {
        MobileDestination.LOGIN -> LoginScreen(state.loading, state.error, vm::login)
        MobileDestination.POINT -> PointDeskScreen(
            state, vm::refreshPoint, vm::loadPointOrders, vm::openPointOrder,
            vm::closePointOrder, vm::receivePointOrders, vm::deliverPointOrder, vm::logout,
        )
        MobileDestination.RIDER -> RiderDesk(state, vm::refreshRider, vm::presentRiderOrders, vm::riderAction, vm::logout)
        MobileDestination.BRANCH -> BranchDeskScreen(
            state = state, onRefresh = vm::refreshBranch, onSelectBranch = vm::selectBranch,
            onLoad = vm::loadBranchOrders, onOpen = vm::openBranchOrder, onClose = vm::closeBranchOrder,
            onAction = vm::branchAction, onRetry = vm::retryBranchOperation, onLogout = vm::logout,
        )
        MobileDestination.PRINTERS, MobileDestination.PAUSES -> AdminWorkspace(state, vm::refreshAdmin, vm::logout,
            vm::changePrinter, vm::changeAvailability, vm::retryAdminOperation)
        MobileDestination.UNSUPPORTED -> PendingRoleScreen(
            title = "Acceso no disponible",
            message = state.error ?: "Esta cuenta no tiene una experiencia Android habilitada.",
            onLogout = vm::logout,
        )
    }
    }
}

@Composable
private fun LoginScreen(loading: Boolean, error: String?, onLogin: (String, String) -> Unit) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().background(Canvas).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Brand(dark = false)
        Spacer(Modifier.height(34.dp))
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp)) {
                Text("Ingresá a tu espacio", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Usá la cuenta que te entregó la imprenta.", color = Color(0xFF52677E), modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Usuario o correo") },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar contraseña")
                        }
                    },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (error != null) Text(error, color = Color(0xFFB3261E), modifier = Modifier.padding(top = 12.dp))
                Button(
                    onClick = { onLogin(user, password) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Ingresar", fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Las cuentas se crean y administran desde la Web.", fontSize = 12.sp, color = Color(0xFF60758C), modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun PointDeskScreen(
    state: AppUiState,
    onRefresh: () -> Unit,
    onLoadOrders: (PointSection, String, Int) -> Unit,
    onOpenOrder: (PointOrder) -> Unit,
    onCloseOrder: () -> Unit,
    onReceive: (Set<String>) -> Unit,
    onDeliver: (String, PointPayment?) -> Unit,
    onLogout: () -> Unit,
) {
    val section = state.pointSection
    val point = state.point
    val loading = state.loading
    val page = state.pointPage
    Scaffold(
        topBar = { MobileHeader("RETIRO DE PEDIDOS", onRefresh, onLogout, loading) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                PointSection.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        enabled = !loading,
                        onClick = { if (item != section) onLoadOrders(item, "", 0) },
                        icon = { Icon(sectionIcon(item), null) },
                        label = { Text(item.title, fontSize = 10.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        ) {
            Text("Tu punto de entrega", color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(point?.nombre ?: "Consultando punto…", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Responsable: ${point?.responsable ?: "—"}", color = Color(0xFF5F7288))
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Counter("Por llegar", point?.porLlegar, Orange, Modifier.weight(1f))
                Counter("Para retirar", point?.disponibles, Green, Modifier.weight(1f))
                Counter("Entregados", point?.entregados, Blue, Modifier.weight(1f))
            }
            if (state.pointDetail == null) state.error?.let { Notice(it, Color(0xFFFFE9E7), Color(0xFF9B1C1C)) }
            state.message?.let { Notice(it, Color(0xFFE5F5EA), Green) }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
            if (section == PointSection.FINANCE) {
                EmptySection("Finanzas del punto", "Podés consultar los movimientos desde el mostrador web.")
            } else {
                OrdersSection(section, page, state.pointQuery, loading, onLoadOrders, onOpenOrder, onReceive)
            }
        }
    }
    state.pointDetail?.let {
        PointOrderDialog(it, point?.nombre.orEmpty(), loading, state.error, onCloseOrder, onDeliver, onReceive)
    }
}

@Composable
private fun OrdersSection(
    section: PointSection,
    page: PointPage,
    appliedQuery: String,
    loading: Boolean,
    onSearch: (PointSection, String, Int) -> Unit,
    onOpen: (PointOrder) -> Unit,
    onReceive: (Set<String>) -> Unit,
) {
    var query by remember(section, appliedQuery) { mutableStateOf(appliedQuery) }
    var selected by remember(section, page) { mutableStateOf(emptySet<String>()) }
    var confirming by remember(section, page) { mutableStateOf(false) }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        label = { Text("Buscar pedido, cliente o código") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = { IconButton(enabled = !loading, onClick = { onSearch(section, query, 0) }) { Icon(Icons.Default.Search, "Buscar") } },
        singleLine = true,
    )
    Text(section.title, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    if (page.orders.isEmpty() && !loading) EmptySection("Sin pedidos", "No hay pedidos para esta búsqueda.")
    if (section == PointSection.ARRIVING && page.orders.isNotEmpty()) {
        Text("Seleccioná los pedidos que el rider ya dejó en el punto.", modifier = Modifier.padding(bottom = 8.dp))
        Button(enabled = selected.isNotEmpty() && !loading, onClick = { confirming = true }) {
            Text("Confirmar recepción (${selected.size})")
        }
    }
    page.orders.forEach { order ->
        PointOrderCard(
            order = order,
            enabled = !loading,
            checked = order.pedido in selected,
            onSelect = if (section == PointSection.ARRIVING && order.puedeRecibir) {
                { checked -> selected = if (checked) selected + order.pedido else selected - order.pedido }
            } else null,
            onOpen = { onOpen(order) },
        )
    }
    if (page.total > 25) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(enabled = !loading && page.page > 0, onClick = { onSearch(section, appliedQuery, page.page - 1) }) { Text("Anterior") }
            TextButton(enabled = !loading && (page.page + 1) * 25 < page.total, onClick = { onSearch(section, appliedQuery, page.page + 1) }) { Text("Siguiente") }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Confirmar recepción") },
            text = { Text("Confirmá que recibiste físicamente los ${selected.size} pedidos seleccionados. Pasarán a Para retirar.") },
            confirmButton = { Button(onClick = { confirming = false; onReceive(selected) }) { Text("Recibí los pedidos") } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Volver") } },
        )
    }
}

@Composable
private fun PointOrderCard(
    order: PointOrder, enabled: Boolean, checked: Boolean, onSelect: ((Boolean) -> Unit)?, onOpen: () -> Unit,
) {
    val status = if (order.puedeRecibir) "Listo para recibir" else logisticsLabel(order.recorrido)
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("PED-${order.numero}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(order.cliente, color = Color(0xFF52677E))
                }
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE5F5EA)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(status, color = Color(0xFF087532), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    if (order.saldo.toBigDecimalOrNull()?.signum() == 0) {
                        Text("Sin deuda pendiente", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Saldo pendiente", fontSize = 11.sp, color = Color(0xFF6A7B8E))
                        Text(money(order.saldo), fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = onOpen, enabled = enabled) {
                    Text("Ver pedido", fontSize = 12.sp)
                }
            }
            if (onSelect != null) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, onSelect, enabled = enabled)
                Text("Incluir en la recepción")
            }
        }
    }
}

@Composable
private fun PointOrderDialog(
    detail: PointOrderDetail,
    pointName: String,
    loading: Boolean,
    error: String?,
    onClose: () -> Unit,
    onDeliver: (String, PointPayment?) -> Unit,
    onReceive: (Set<String>) -> Unit,
) {
    val order = detail.order
    val collect = order.recorrido == "DISPONIBLE_PUNTO" && detail.paymentMethods.isNotEmpty()
    val canDeliver = order.recorrido == "DISPONIBLE_PUNTO" && (detail.deliveryBlocks.isEmpty() || collect)
    val action = if (collect) "Confirmar cobro y entrega" else "Confirmar entrega"
    var code by remember(order.pedido) { mutableStateOf("") }
    var method by remember(order.pedido, detail.paymentMethods) {
        mutableStateOf(order.medio.takeIf { it in detail.paymentMethods } ?: detail.paymentMethods.firstOrNull().orEmpty())
    }
    var destination by remember(order.pedido) { mutableStateOf("SUCURSAL_ORIGEN") }
    var reference by remember(order.pedido) { mutableStateOf("") }
    val transfer = collect && method == "TRANSFERENCIA"
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("PED-${order.numero} · ${order.cliente}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (order.puedeRecibir) "Listo para confirmar recepción" else logisticsLabel(order.recorrido))
                if (order.franja.isNotBlank()) Text(order.franja)
                Text(if (order.saldo.toBigDecimalOrNull()?.signum() == 0) "Sin deuda pendiente" else "Saldo pendiente: ${money(order.saldo)}")
                if (order.recorrido == "EN_VIAJE") {
                    Text(if (order.puedeRecibir) {
                        "El rider ya presentó el pedido. Confirmá la recepción después de verificar que lo tenés en el punto."
                    } else {
                        "El rider debe presentar este pedido antes de que puedas confirmar la recepción."
                    })
                }
                if (collect) Text("Medio elegido al hacer el pedido: ${logisticsLabel(order.medio)}")
                detail.works.forEach { Text("${it.nombre} · ${it.paginas} pág. · ${it.copias} copias") }
                detail.receipts.forEach {
                    Text("${logisticsLabel(it.medio)} · ${money(it.importe)} · ${it.destino}")
                }
                if (order.recorrido == "DISPONIBLE_PUNTO" && !collect) {
                    detail.deliveryBlocks.forEach { Text(it) }
                }
                if (collect) {
                    Text("Medio de pago recibido", fontWeight = FontWeight.Bold)
                    Column(Modifier.selectableGroup()) {
                        detail.paymentMethods.forEach { option ->
                            PaymentOption(logisticsLabel(option), method == option, !loading) { method = option }
                        }
                    }
                    if (transfer) {
                        Text("Cuenta que recibió la transferencia", fontWeight = FontWeight.Bold)
                        Column(Modifier.selectableGroup()) {
                            PaymentOption("Sucursal de origen · ${detail.originBranch}", destination == "SUCURSAL_ORIGEN", !loading) {
                                destination = "SUCURSAL_ORIGEN"
                            }
                            PaymentOption("Punto de entrega · $pointName", destination == "PUNTO_ENTREGA", !loading) {
                                destination = "PUNTO_ENTREGA"
                            }
                        }
                        OutlinedTextField(
                            value = reference,
                            onValueChange = { reference = it.take(160) },
                            label = { Text("Número de operación") },
                            enabled = !loading,
                            singleLine = true,
                        )
                        Text(if (destination == "SUCURSAL_ORIGEN") {
                            "Confirmá cuando la sucursal verifique la acreditación."
                        } else {
                            "Verificá la acreditación en la cuenta del punto antes de confirmar."
                        })
                    } else {
                        Text("Confirmá después de recibir el efectivo en el punto.")
                    }
                }
                if (canDeliver) OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().trim().take(8) },
                    label = { Text("Código de entrega") },
                    enabled = !loading,
                    singleLine = true,
                )
                error?.let { Text(it, color = Color(0xFFB3261E)) }
            }
        },
        confirmButton = {
            if (order.puedeRecibir) Button(enabled = !loading, onClick = { onReceive(setOf(order.pedido)) }) {
                Text(if (loading) "Registrando…" else "Confirmar recepción")
            }
            if (canDeliver) Button(
                enabled = !loading && code.length == 8 && (!transfer || reference.isNotBlank()),
                onClick = {
                    val payment = if (!collect) null else PointPayment(
                        method,
                        if (transfer) destination else "PUNTO_ENTREGA",
                        reference.trim().takeIf { transfer },
                    )
                    onDeliver(code, payment)
                },
            ) {
                Text(if (loading) "Registrando…" else action)
            }
        },
        dismissButton = { TextButton(enabled = !loading, onClick = onClose) { Text("Cerrar") } },
    )
}

@Composable
private fun PaymentOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label)
    }
}

@Composable
private fun PendingRoleScreen(title: String, message: String, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Brand(false)
        Card(Modifier.fillMaxWidth().padding(top = 28.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(24.dp)) {
                Icon(Icons.Default.LocalShipping, null, tint = Blue, modifier = Modifier.size(44.dp))
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                Text(message, color = Color(0xFF52677E), modifier = Modifier.padding(top = 8.dp))
                TextButton(onClick = onLogout, modifier = Modifier.padding(top = 18.dp)) { Text("Cerrar sesión") }
            }
        }
    }
}

@Composable
internal fun MobileHeader(section: String, onRefresh: () -> Unit, onLogout: () -> Unit, loading: Boolean) {
    Row(
        Modifier.fillMaxWidth().background(Navy).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Brand(true, compact = true)
        Spacer(Modifier.weight(1f))
        val admin = LocalAdminNavigation.current
        if (admin == null) Text(section, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        else AdminMenu(admin)
        IconButton(enabled = !loading, onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White) }
        IconButton(enabled = !loading, onClick = onLogout) { Icon(Icons.Default.Logout, "Cerrar sesión", tint = Color.White) }
    }
}

@Composable
internal fun Brand(dark: Boolean, compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MountainLogo(Modifier.size(if (compact) 38.dp else 58.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("La Montaña", color = if (dark) Color.White else Navy, fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 17.sp else 25.sp)
            Text("IMPRESIONES", color = Orange, fontWeight = FontWeight.Bold, fontSize = if (compact) 8.sp else 11.sp, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun MountainLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(Orange, radius = size.minDimension / 2)
        val mountain = Path().apply {
            moveTo(size.width * .08f, size.height * .78f)
            lineTo(size.width * .42f, size.height * .30f)
            lineTo(size.width * .57f, size.height * .52f)
            lineTo(size.width * .72f, size.height * .27f)
            lineTo(size.width * .94f, size.height * .78f)
            close()
        }
        drawPath(mountain, Navy)
        val snow = Path().apply {
            moveTo(size.width * .28f, size.height * .50f)
            lineTo(size.width * .42f, size.height * .30f)
            lineTo(size.width * .53f, size.height * .46f)
            lineTo(size.width * .61f, size.height * .39f)
            lineTo(size.width * .72f, size.height * .27f)
            lineTo(size.width * .80f, size.height * .46f)
            lineTo(size.width * .70f, size.height * .42f)
            lineTo(size.width * .59f, size.height * .59f)
            lineTo(size.width * .46f, size.height * .46f)
            lineTo(size.width * .36f, size.height * .58f)
            close()
        }
        drawPath(snow, Color.White)
    }
}

@Composable
private fun Counter(label: String, value: Int?, color: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text((value ?: 0).toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
            Text(label, fontSize = 11.sp, color = Color(0xFF5E7186))
        }
    }
}

@Composable
internal fun Notice(text: String, background: Color, foreground: Color) {
    Box(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(12.dp)).background(background).padding(14.dp)) {
        Text(text, color = foreground, fontSize = 13.sp)
    }
}

@Composable
private fun EmptySection(title: String, description: String) {
    Card(Modifier.fillMaxWidth().padding(top = 20.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(description, color = Color(0xFF52677E), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

private fun sectionIcon(section: PointSection): ImageVector = when (section) {
    PointSection.ARRIVING -> Icons.Default.LocalShipping
    PointSection.READY -> Icons.Default.Inventory2
    PointSection.DELIVERED -> Icons.Default.CheckCircle
    PointSection.FINANCE -> Icons.Default.AccountBalanceWallet
}

internal fun logisticsLabel(value: String): String = when (value) {
    "EN_VIAJE" -> "En viaje"
    "DISPONIBLE_PUNTO", "LISTO_RETIRO" -> "Disponible para retirar"
    "ENTREGADO" -> "Entregado"
    else -> value.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

internal fun money(value: String): String = value.toBigDecimalOrNull()?.let { amount ->
    val parts = String.format(java.util.Locale.US, "%.2f", amount).split('.')
    "$ " + parts[0].reversed().chunked(3).joinToString(".").reversed() + "," + parts[1]
} ?: "$ 0,00"

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPreview() = LaMontanaTheme { LoginScreen(false, null) { _, _ -> } }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PointPreview() = LaMontanaTheme {
    PointDeskScreen(
        state = AppUiState(
            point = PointContext("1", "Punto Caballito", "Responsable Caballito", true, 3, 5, 18),
            pointPage = PointPage(listOf(
                PointOrder("1", 176, "Martín Gómez", "LISTO_PARA_ENTREGA", "DISPONIBLE_PUNTO", "1137.00", "EFECTIVO", "2026-11-02T12:00:00Z"),
                PointOrder("2", 143, "Sofía Rossi", "LISTO_PARA_ENTREGA", "DISPONIBLE_PUNTO", "0.00", "TRANSFERENCIA", "2026-11-02T12:00:00Z"),
            ), 2, 0),
        ),
        onRefresh = {},
        onLoadOrders = { _, _, _ -> },
        onOpenOrder = { _ -> },
        onCloseOrder = {},
        onReceive = { _ -> },
        onDeliver = { _, _ -> },
        onLogout = {},
    )
}
