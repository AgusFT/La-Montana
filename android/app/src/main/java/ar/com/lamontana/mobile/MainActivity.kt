package ar.com.lamontana.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import ar.com.lamontana.mobile.model.PointContext
import ar.com.lamontana.mobile.model.PointOrder
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
    when (state.destination) {
        MobileDestination.LOGIN -> LoginScreen(state.loading, state.error, vm::login)
        MobileDestination.POINT -> PointDeskScreen(state.point, state.pointOrders, state.loading, state.error, vm::refreshPoint, vm::loadPointOrders, vm::logout)
        MobileDestination.RIDER_PENDING -> PendingRoleScreen(
            title = "Reparto mobile",
            message = "Tu usuario fue reconocido. La asignación individual de recorridos todavía no está habilitada en el backend.",
            onLogout = vm::logout,
        )
        MobileDestination.UNSUPPORTED -> PendingRoleScreen(
            title = "Acceso no disponible",
            message = state.error ?: "Esta cuenta no tiene una experiencia Android habilitada.",
            onLogout = vm::logout,
        )
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
    point: PointContext?,
    orders: List<PointOrder>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onLoadOrders: (PointSection, String) -> Unit,
    onLogout: () -> Unit,
) {
    var section by remember { mutableStateOf(PointSection.READY) }
    Scaffold(
        topBar = { MobileHeader("RETIRO DE PEDIDOS", onRefresh, onLogout) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                PointSection.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { section = item; onLoadOrders(item, "") },
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
            if (error != null) Notice(error, Color(0xFFFFE9E7), Color(0xFF9B1C1C))
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
            when (section) {
                PointSection.ARRIVING -> OrdersSection(section, orders, onLoadOrders)
                PointSection.READY -> OrdersSection(section, orders, onLoadOrders)
                PointSection.DELIVERED -> OrdersSection(section, orders, onLoadOrders)
                PointSection.FINANCE -> EmptySection("Finanzas del punto", "Movimientos, efectivo y transferencias de los últimos seis meses.")
            }
        }
    }
}

@Composable
private fun OrdersSection(section: PointSection, orders: List<PointOrder>, onSearch: (PointSection, String) -> Unit) {
    var query by remember { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        label = { Text("Buscar pedido o cliente") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = { IconButton(onClick = { onSearch(section, query) }) { Icon(Icons.Default.Search, "Buscar") } },
        singleLine = true,
    )
    Text(section.title, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    if (orders.isEmpty()) EmptySection("Sin pedidos", "No hay pedidos para esta búsqueda.")
    orders.forEach { order ->
        SampleOrder(
            number = "PED-${order.numero}",
            customer = order.cliente,
            status = logisticsLabel(order.recorrido),
            balance = money(order.saldo),
            cash = order.medio == "EFECTIVO" && order.saldo.toBigDecimalOrNull()?.signum() == 1,
        )
    }
}

@Composable
private fun SampleOrder(number: String, customer: String, status: String, balance: String, cash: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(number, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(customer, color = Color(0xFF52677E)) }
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE5F5EA)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(status, color = Color(0xFF087532), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Saldo pendiente", fontSize = 11.sp, color = Color(0xFF6A7B8E)); Text(balance, fontWeight = FontWeight.Bold) }
                Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(containerColor = if (cash) Orange else Green)) {
                    Text("Abrir pedido", fontSize = 12.sp)
                }
            }
        }
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
private fun MobileHeader(section: String, onRefresh: () -> Unit, onLogout: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Navy).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Brand(true, compact = true)
        Spacer(Modifier.weight(1f))
        Text(section, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White) }
        IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Cerrar sesión", tint = Color.White) }
    }
}

@Composable
private fun Brand(dark: Boolean, compact: Boolean = false) {
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
private fun Notice(text: String, background: Color, foreground: Color) {
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

private fun logisticsLabel(value: String): String = when (value) {
    "EN_VIAJE" -> "En viaje"
    "DISPONIBLE_PUNTO", "LISTO_RETIRO" -> "Disponible para retirar"
    "ENTREGADO" -> "Entregado"
    else -> value.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

private fun money(value: String): String = value.toBigDecimalOrNull()?.let { amount ->
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
        PointContext("1", "Punto Caballito", "Responsable Caballito", true, 3, 5, 18),
        listOf(
            PointOrder("1", 176, "Martín Gómez", "LISTO_PARA_ENTREGA", "DISPONIBLE_PUNTO", "1137.00", "EFECTIVO", "2026-11-02T12:00:00Z"),
            PointOrder("2", 143, "Sofía Rossi", "LISTO_PARA_ENTREGA", "DISPONIBLE_PUNTO", "0.00", "TRANSFERENCIA", "2026-11-02T12:00:00Z"),
        ),
        false,
        null,
        {},
        { _, _ -> },
        {},
    )
}
