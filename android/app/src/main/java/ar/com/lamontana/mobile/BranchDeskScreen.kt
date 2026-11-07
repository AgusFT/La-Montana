package ar.com.lamontana.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.com.lamontana.mobile.model.*
import ar.com.lamontana.mobile.ui.*

@Composable
internal fun BranchDeskScreen(
    state: AppUiState, onRefresh: () -> Unit, onSelectBranch: (String) -> Unit,
    onLoad: (BranchSection, String, Int) -> Unit, onOpen: (BranchOrder) -> Unit,
    onClose: () -> Unit, onAction: (BranchAction, String, String, String) -> Unit,
    onRetry: () -> Unit, onLogout: () -> Unit,
) {
    val context = state.branchContext
    val branch = context?.branches?.firstOrNull { it.id == state.branchId }
    val locked = state.loading || state.branchUncertain
    val canSeeCash = state.branchId?.let { context?.canSeeCash(it, state.profile) } == true
    val sections = BranchSection.entries.filter { it != BranchSection.CASH || canSeeCash }
    var menu by remember { mutableStateOf(false) }
    var query by remember(state.branchId, state.branchSection, state.branchQuery) { mutableStateOf(state.branchQuery) }
    BackHandler(enabled = state.branchUncertain) { /* Recuperar el registro antes de salir. */ }
    Scaffold(
        containerColor = Canvas,
        topBar = { MobileHeader("MOSTRADOR", onRefresh, onLogout, locked) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                sections.forEach { section ->
                    NavigationBarItem(selected = section == state.branchSection, enabled = !locked,
                        onClick = { if (section != state.branchSection) onLoad(section, "", 0) },
                        icon = { Icon(when (section) {
                            BranchSection.READY -> Icons.Default.Inventory2
                            BranchSection.ARRIVING -> Icons.Default.LocalShipping
                            BranchSection.DELIVERED -> Icons.Default.CheckCircle
                            BranchSection.CASH -> Icons.Default.AccountBalanceWallet
                        }, null) }, label = { Text(section.title, fontSize = 11.sp) })
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 18.dp)) {
            Text("Mostrador de sucursal", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp))
            Box {
                if ((context?.branches?.size ?: 0) > 1) {
                    TextButton(enabled = !locked, contentPadding = PaddingValues(0.dp), onClick = { menu = true }) {
                        Text(branch?.name.orEmpty(), fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Icon(Icons.Default.ArrowDropDown, "Cambiar sucursal")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        context?.branches?.forEach { option -> DropdownMenuItem(text = { Text(option.name) },
                            onClick = { menu = false; onSelectBranch(option.id) }) }
                    }
                } else Text(branch?.name ?: "Consultando sucursal…", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("Atiende ${state.profile?.nombre.orEmpty()} ${state.profile?.apellido.orEmpty()}", fontSize = 12.sp, color = Color(0xFF5F7288))
            if (state.branchDetail == null) state.error?.let { Notice(it, Color(0xFFFFE9E7), Color(0xFF9B1C1C)) }
            if (state.branchDetail == null) state.message?.let { Notice(it, Color(0xFFE5F5EA), Green) }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            if (state.branchSection != BranchSection.CASH) {
                OutlinedTextField(value = query, onValueChange = { query = it.take(160) },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp), enabled = !locked,
                    label = { Text("Nombre del cliente o PED-156") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { IconButton(enabled = !locked && branch != null,
                        onClick = { onLoad(state.branchSection, query, 0) }) { Icon(Icons.Default.Search, "Buscar pedido") } })
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(state.branchSection.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${state.branchPage.total} pedidos", fontSize = 13.sp)
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (!state.loading && state.branchPage.orders.isEmpty()) item {
                        Text(if (state.branchQuery.isBlank()) "No hay pedidos en esta bandeja." else "No encontramos pedidos con esa búsqueda.",
                            modifier = Modifier.padding(vertical = 16.dp))
                    }
                    items(state.branchPage.orders, key = { it.id }) { order ->
                        BranchOrderCard(order, state.branchSection, !locked) { onOpen(order) }
                    }
                    if (state.branchPage.total > 25) item {
                        BranchPagination(state.branchPage.page, state.branchPage.total, !locked) {
                            onLoad(state.branchSection, state.branchQuery, it)
                        }
                    }
                }
            } else state.branchCash?.let { cash ->
                BranchCashView(cash, !locked) { onLoad(BranchSection.CASH, "", it) }
            }
        }
    }
    state.branchDetail?.let { detail -> BranchOrderDialog(detail, branch?.name.orEmpty(), state.loading,
        state.branchUncertain, state.error, state.message, onClose, onAction, onRetry,
        onRefresh = { onOpen(detail.order) }) }
}

@Composable
private fun BranchOrderCard(order: BranchOrder, section: BranchSection, enabled: Boolean, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PED-${order.number}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(if (section == BranchSection.ARRIVING) "En camino" else if (section == BranchSection.READY) "Para retirar" else "Entregado",
                    color = if (section == BranchSection.ARRIVING) Blue else Green, fontSize = 12.sp)
            }
            Text(order.customer)
            if (order.guest) Text("Invitado sin cuenta", fontSize = 11.sp, color = Color(0xFF5F7288))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    if (order.balance.toBigDecimalOrNull()?.signum() == 0) Text("Sin deuda", color = Green, fontWeight = FontWeight.Bold)
                    else { Text("Saldo pendiente", fontSize = 11.sp); Text(money(order.balance), fontWeight = FontWeight.Bold) }
                }
                Button(enabled = enabled, onClick = onOpen) {
                    Text(when (section) {
                        BranchSection.READY -> "Entregar / cobrar"
                        BranchSection.ARRIVING -> "Ver llegada"
                        else -> "Ver entrega"
                    }, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun BranchOrderDialog(
    detail: BranchOrderDetail, branchName: String, loading: Boolean, uncertain: Boolean,
    error: String?, message: String?, onClose: () -> Unit,
    onAction: (BranchAction, String, String, String) -> Unit, onRetry: () -> Unit, onRefresh: () -> Unit,
) {
    val locked = loading || uncertain
    val delivery = detail.delivery
    val methods = detail.payment?.methods.orEmpty()
    var method by remember(detail.order.id, delivery.balance, methods) {
        mutableStateOf(detail.order.preferredMethod.takeIf { it in methods } ?: methods.firstOrNull().orEmpty())
    }
    var code by remember(detail.order.id) { mutableStateOf("") }
    var reference by remember(detail.order.id) { mutableStateOf("") }
    var verified by remember(detail.order.id, delivery.balance, method) { mutableStateOf(false) }
    var showReceipts by remember(detail.order.id) { mutableStateOf(false) }
    val collect = detail.canCollect(method)
    val atomicCash = detail.canCollectAndDeliver(method)
    val action = when {
        delivery.canReceive -> BranchAction.RECEIVE
        detail.canDeliver -> BranchAction.DELIVER
        atomicCash -> BranchAction.CASH_DELIVERY
        collect -> BranchAction.RECEIPT
        else -> null
    }
    val enabled = !locked && verified && action != null &&
        (action !in listOf(BranchAction.DELIVER, BranchAction.CASH_DELIVERY) || detail.validCode(code)) &&
        (action != BranchAction.RECEIPT || method != "TRANSFERENCIA" || reference.isNotBlank())
    AlertDialog(onDismissRequest = { if (!locked) onClose() },
        title = { Text("PED-${detail.order.number} · ${detail.order.customer}", fontSize = 21.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(branchName, fontWeight = FontWeight.Bold)
                Text(logisticsLabel(delivery.logistics))
                Text(if (detail.paid) "Cobro registrado · Sin deuda" else "Saldo pendiente ${money(delivery.balance)}",
                    color = if (detail.paid) Green else Ink, fontWeight = FontWeight.Bold)
                delivery.deliveredAt?.let { Text("Entrega registrada ${displayDate(it)}\nAtendió ${delivery.deliveredBy.orEmpty()}") }
                if (delivery.canReceive) Text("Confirmá después de verificar que el pedido llegó físicamente a esta sucursal.")
                if (detail.ready && !detail.paid) {
                    if (methods.isNotEmpty()) {
                        Text("Cómo se paga este saldo", fontWeight = FontWeight.Bold)
                        Column(Modifier.selectableGroup()) {
                            methods.forEach { option ->
                                Row(Modifier.fillMaxWidth().selectable(selected = method == option, enabled = !locked,
                                    role = Role.RadioButton, onClick = { method = option }).padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = method == option, onClick = null, enabled = !locked)
                                    Text(if (option == "EFECTIVO") "Efectivo" else "Transferencia")
                                }
                            }
                        }
                        if (method == "TRANSFERENCIA") {
                            Text("Cuenta de la sucursal de origen · ${detail.payment?.originBranch.orEmpty()}")
                            Text("Registrá sólo dinero cuya acreditación verificaste en esa cuenta.")
                            val transfers = detail.payment?.transfers.orEmpty().filter {
                                it.amount.toBigDecimalOrNull()?.compareTo(delivery.balance.toBigDecimal()) == 0
                            }
                            transfers.forEach { transfer -> TextButton(enabled = !locked, onClick = { reference = transfer.reference }) {
                                Text("Usar aviso ${transfer.reference}")
                            } }
                            OutlinedTextField(value = reference, onValueChange = { reference = it.take(160) },
                                modifier = Modifier.fillMaxWidth(), enabled = !locked,
                                label = { Text("Número de operación") }, singleLine = true)
                        }
                    } else Text("Tu cuenta no tiene habilitado registrar este cobro. La entrega se habilita cuando el saldo esté acreditado.")
                    if (detail.payment?.unappliedMoney == true) Text("Ya hay dinero recibido sin aplicar. Revisá los cobros desde la web antes de volver a cobrar.", color = Orange)
                    if (!collect) delivery.blocks.forEach { Text(it) }
                }
                if (detail.canDeliver || atomicCash) {
                    if (detail.needsCode) OutlinedTextField(value = code,
                        onValueChange = { code = it.uppercase().filterNot(Char::isWhitespace).take(8) },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Código de entrega") }, enabled = !locked, singleLine = true)
                    else Text("Verificá el nombre del invitado y el número de pedido antes de entregarlo.")
                }
                if (action != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = verified, enabled = !locked, onCheckedChange = { verified = it })
                    Text(when {
                        action == BranchAction.RECEIVE -> "Recibí físicamente el pedido."
                        collect && method == "EFECTIVO" -> "Recibí ${money(delivery.balance)} en efectivo."
                        collect -> "Verifiqué la acreditación en la cuenta de origen."
                        else -> "Verifiqué el pedido y la persona que retira."
                    }, fontSize = 13.sp)
                }
                if (detail.payment?.receipts?.isNotEmpty() == true) {
                    TextButton(onClick = { showReceipts = !showReceipts }) { Text(if (showReceipts) "Ocultar cobros" else "Ver cobros registrados") }
                    if (showReceipts) detail.payment.receipts.forEach { Text("${logisticsLabel(it.method)} · ${money(it.amount)}\n${it.reference} · ${displayDate(it.receivedAt)}", fontSize = 12.sp) }
                }
                message?.let { Notice(it, Color(0xFFE5F5EA), Green) }
                error?.let { Text(it, color = Color(0xFF9B1C1C)) }
                if (!locked) TextButton(onClick = onRefresh) { Text("Actualizar pedido") }
            }
        },
        confirmButton = {
            if (uncertain) Button(enabled = !loading, onClick = onRetry) { Text(if (loading) "Recuperando…" else "Recuperar registro") }
            else if (action != null) Button(enabled = enabled, onClick = { onAction(action, code, method, reference) },
                colors = ButtonDefaults.buttonColors(containerColor = if (collect) Orange else Green)) {
                Text(when (action) {
                    BranchAction.RECEIVE -> "Confirmar llegada"
                    BranchAction.CASH_DELIVERY -> "Cobrar y entregar"
                    BranchAction.RECEIPT -> "Registrar cobro"
                    BranchAction.DELIVER -> "Confirmar entrega"
                })
            }
        },
        dismissButton = { TextButton(enabled = !locked, onClick = onClose) { Text("Cerrar") } },
    )
}

@Composable
private fun BranchCashView(cash: BranchCash, enabled: Boolean, onPage: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("Efectivo acumulado en caja", color = Color.White)
                    Text(money(cash.current), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Movimientos del ${cash.today}", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        item { Text("Entradas ${money(cash.incoming)} · Salidas ${money(cash.outgoing)}", fontSize = 14.sp) }
        item { Text("Transferencias recibidas ${money(cash.transfers)}", fontSize = 14.sp) }
        item { Text("Movimientos de hoy", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (cash.movements.isEmpty()) item { Text("No hay movimientos registrados hoy.") }
        items(cash.movements) { movement ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${logisticsLabel(movement.type)} · ${money(movement.amount)}", fontWeight = FontWeight.Bold)
                    if (movement.concept.isNotBlank()) Text(movement.concept)
                    Text("${logisticsLabel(movement.method)} · ${displayDate(movement.date)}", fontSize = 12.sp)
                    Text(movement.actor, fontSize = 12.sp)
                }
            }
        }
        if (cash.total > 25) item { BranchPagination(cash.page, cash.total, enabled, onPage) }
    }
}

@Composable
private fun BranchPagination(page: Int, total: Int, enabled: Boolean, onPage: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(enabled = enabled && page > 0, onClick = { onPage(page - 1) }) { Text("Anterior") }
        Text("${page + 1}")
        TextButton(enabled = enabled && (page + 1) * 25 < total, onClick = { onPage(page + 1) }) { Text("Siguiente") }
    }
}

private fun displayDate(date: String): String = runCatching {
    java.time.Instant.parse(date).atZone(java.time.ZoneId.of("America/Argentina/Buenos_Aires"))
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM · HH:mm"))
}.getOrDefault(date)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun BranchDeskPreview() = LaMontanaTheme {
    BranchDeskScreen(
        state = AppUiState(destination = MobileDestination.BRANCH,
            profile = Profile("Lucía", "Mostrador", "sucursal.la.plata", "EMPLEADO", false, false, false),
            branchId = "sucursal", branchContext = BranchContext(listOf(Branch("sucursal", "Sucursal La Plata", "Calle 7 1280")), setOf("GESTIONAR_ENTREGAS"), "sucursal"),
            branchPage = BranchPage(listOf(
                BranchOrder("pedido1", 156, "Raúl", true, "LISTO_PARA_ENTREGA", "1529.00", "TRANSFERENCIA"),
                BranchOrder("pedido2", 154, "Martín Gómez", false, "LISTO_PARA_ENTREGA", "0.00", "EFECTIVO")), 2, 0)),
        onRefresh = {}, onSelectBranch = { _ -> }, onLoad = { _, _, _ -> }, onOpen = { _ -> },
        onClose = {}, onAction = { _, _, _, _ -> }, onRetry = {}, onLogout = {},
    )
}
