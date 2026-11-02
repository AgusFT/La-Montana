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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.com.lamontana.mobile.ui.Blue
import ar.com.lamontana.mobile.ui.Canvas
import ar.com.lamontana.mobile.ui.Green
import ar.com.lamontana.mobile.ui.LaMontanaTheme
import ar.com.lamontana.mobile.ui.Navy
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
)

@Composable
internal fun RiderRouteScreen(
    rider: String,
    route: String,
    stops: List<RiderStop>,
    onOpenStop: (RiderStop) -> Unit,
) {
    val completed = stops.count { it.completed }
    val orders = stops.sumOf { it.orders.size }
    Scaffold(
        containerColor = Canvas,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(Navy).padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Brand(dark = true, compact = true)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("REPARTO", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(rider, color = Color.White, fontSize = 12.sp)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(true, {}, { Icon(Icons.Default.LocalShipping, null) }, label = { Text("Recorrido", fontSize = 11.sp) })
                NavigationBarItem(false, {}, { Icon(Icons.Default.Warning, null) }, label = { Text("Incidencias", fontSize = 11.sp) })
                NavigationBarItem(false, {}, { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Finalizados", fontSize = 11.sp) })
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        ) {
            Text("Mi recorrido", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("Sólo ves los pedidos que Administración te asignó.", color = Color(0xFF5D7187), fontSize = 13.sp)
            RouteSummary(route, orders, completed, stops.size)
            Text("Próximas paradas", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
            stops.sortedBy { it.order }.forEach { stop -> RiderStopCard(stop, onOpenStop) }
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
private fun RiderStopCard(stop: RiderStop, onOpen: (RiderStop) -> Unit) {
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
                    Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
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

private fun stopIcon(type: StopType): ImageVector = when (type) {
    StopType.POINT -> Icons.Default.Storefront
    StopType.HOME -> Icons.Default.Home
    StopType.BRANCH -> Icons.Default.LocalShipping
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RiderRoutePreview() = LaMontanaTheme {
    RiderRouteScreen(
        rider = "Diego Fernández",
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
