package ar.com.lamontana.mobile

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import ar.com.lamontana.mobile.ui.Navy
import ar.com.lamontana.mobile.ui.Ink
import ar.com.lamontana.mobile.ui.Green
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ar.com.lamontana.mobile.data.*
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private fun localMoment(raw:String)=runCatching {
    DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.of("America/Argentina/Buenos_Aires")).format(Instant.parse(raw))
}.getOrDefault(raw)

@Composable
internal fun PointFinanceScreen(state:AppUiState,load:(Boolean,String,Int)->Unit,send:(PointCommand)->Unit){
    val enabled=!state.loading&&!state.pointUncertain
    var form by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Movement?>(null) }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        listOf("Cobros","Rendiciones").forEachIndexed { i,label->
            FilterChip(selected=state.remittances==(i==1),enabled=enabled,onClick={form=false;load(i==1,state.financePeriod,0)},label={Text(label)})
        }
    }
    if(!state.remittances)Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
        listOf("HOY" to "Hoy","AYER" to "Ayer","SEMANA" to "7 días").forEach{(value,label)->
            FilterChip(selected=state.financePeriod==value,enabled=enabled,onClick={load(false,value,0)},label={Text(label)})
        }
    }
    val view=state.finance?:return
    if(state.remittances){
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Navy)){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("Pendiente de rendición",color=Color.White,fontSize=14.sp)
                Text(money(view.amounts.first().second),color=Color.White,fontSize=29.sp,fontWeight=FontWeight.Bold)
                Text("Fondos recibidos por el punto aún no confirmados por la imprenta",color=Color.White,fontSize=12.sp)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            view.amounts.drop(1).forEach{(label,value)->
                Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=Color.White)){
                    Column(Modifier.padding(12.dp)){
                        Text(label,color=Ink,fontSize=12.sp)
                        Text(money(value),color=Ink,fontWeight=FontWeight.Bold,fontSize=18.sp)
                    }
                }
            }
        }
        Text("Cuando entregues efectivo o hagas una transferencia a la imprenta, informá el importe y el destinatario. La sucursal confirma después que lo recibió.",fontSize=13.sp)
        if(view.accounts.isEmpty()) Text("Todavía no hay un destinatario habilitado para rendir. Administración debe acordar y habilitar una sucursal y el medio de entrega para este punto.",color=Color(0xFF76521B),fontSize=13.sp)
        Button(enabled=enabled&&view.accounts.isNotEmpty()&&view.available.toBigDecimalOrNull()?.signum()==1,onClick={form=true}){Text("Informar rendición")}
        if(view.settlements.isEmpty())Text("Todavía no hay rendiciones informadas.")
        view.settlements.forEach{r->ListItem(headlineContent={Text("${money(r.amount)} · ${r.state}")},
            supportingContent={Text("${localMoment(r.date)} · ${r.recipient} · ${r.method}\n${r.note}")})}
        if(form&&view.accounts.isNotEmpty())ReportRemittance(view.accounts,view.available,{form=false}){data->
            form=false
            send(PointCommand("/api/punto/rendiciones",data.toString(),"Rendición informada. Pendiente de confirmación."))
        }
    }else{
        val amounts=view.amounts.toMap()
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Navy)){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("Recibido por el punto · ${if(state.financePeriod=="HOY")"hoy" else if(state.financePeriod=="AYER")"ayer" else "últimos 7 días"}",color=Color.White,fontSize=14.sp)
                Text(money(amounts["Recibido por el punto"]?:"0"),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
                Text("Dinero efectivamente recibido en este período",color=Color.White,fontSize=12.sp)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            listOf("Efectivo","Transferencias al punto").forEach{label->
                Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=Color.White)){
                    Column(Modifier.padding(12.dp)){
                        Text(label,fontSize=12.sp,color=Ink)
                        Text(money(amounts[label]?:"0"),fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink)
                    }
                }
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=Color.White)){
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Transferencias directas a sucursal · ${money(amounts["Directo a sucursal"]?:"0")}")
                Text("Total gestionado · ${money(amounts["Total gestionado"]?:"0")}",fontWeight=FontWeight.Bold)
                Text("Pendiente de clientes para retirar · ${money(amounts["Pendiente de cobro al retirar"]?:"0")}",color=Green)
            }
        }
        Text("Movimientos del período",fontWeight=FontWeight.Bold,fontSize=18.sp)
        if(view.movements.isEmpty())Text("No hay movimientos en este período.")
        view.movements.forEach{m->OutlinedButton(enabled=enabled,onClick={detail=m},modifier=Modifier.fillMaxWidth()){
            Text("PED-${m.order} · ${money(m.amount)}\n${m.state} · ${m.recipient}")
        }}
        Row{if(state.financePage>0)TextButton(enabled=enabled,onClick={load(false,state.financePeriod,state.financePage-1)}){Text("Anteriores")}
            if((state.financePage+1)*25<view.total)TextButton(enabled=enabled,onClick={load(false,state.financePeriod,state.financePage+1)}){Text("Siguientes")}}
    }
    detail?.let{m->AlertDialog(onDismissRequest={detail=null},title={Text("PED-${m.order} · ${money(m.amount)}")},
        text={Column{
            Text("${localMoment(m.date)}\n${m.method} · ${m.recipient}\n${m.state}\n${m.reference}")
            var fileError by remember{mutableStateOf("")}
            if(m.state=="PENDIENTE")ProofPicker("Adjuntar comprobante PDF","application/pdf",{file->
                detail=null
                send(PointCommand("/api/punto/avisos/${m.id}/comprobante",JSONObject().put("operacion",UUID.randomUUID().toString()).put("pdf",file).toString(),"Comprobante guardado."))
            },{fileError=it})
            if(fileError.isNotEmpty())Text(fileError)
        }},
        confirmButton={if(m.canVerify)Button(onClick={detail=null;send(PointCommand("/api/punto/finanzas/verificar",
            JSONObject().put("operacion",UUID.randomUUID().toString()).put("aviso",m.id).put("cotizacion",m.quote).toString(),"Acreditación confirmada."))}){
            Text("Verifiqué la acreditación")}},dismissButton={TextButton(onClick={detail=null}){Text("Cerrar")}})}
}

@Composable
private fun ReportRemittance(accounts:List<FinanceAccount>,available:String,close:()->Unit,send:(JSONObject)->Unit){
    var account by remember { mutableStateOf(accounts.first()) }
    var amount by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=close,title={Text("Informar rendición")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text("Disponible para informar ${money(available)}")
            OutlinedTextField(amount,{amount=it},label={Text("Importe a rendir")},singleLine=true)
            accounts.forEach{a->FilterChip(selected=account.id==a.id,onClick={account=a;reference=""},label={Text("${a.name} · ${a.method}")})}
            Text("Destinatario: ${account.name} · ${account.method}",fontWeight=FontWeight.Bold)
            Text(account.instructions)
            OutlinedTextField(reference,{reference=it.take(140)},label={Text(if(account.method=="EFECTIVO")"Persona receptora" else "Referencia")})
            OutlinedTextField(note,{note=it.take(300)},label={Text("Observación opcional")})
            ProofPicker(if(attachment==null)"Adjuntar comprobante" else "Cambiar comprobante","*/*",{attachment=it;error=""},{error=it})
            if(error.isNotEmpty())Text(error)
        }},confirmButton={Button(enabled=amount.replace(',','.').toBigDecimalOrNull()?.let { it.signum()==1 && it <= available.toBigDecimal() }==true&&reference.isNotBlank(),onClick={
            send(JSONObject().put("operacion",UUID.randomUUID().toString()).put("cuenta",account.id).put("importe",amount.replace(',','.'))
                .put(if(account.method=="EFECTIVO")"receptor" else "referencia",reference).put("observacion",note).put("comprobante",attachment))
        }){Text("Informar")}},dismissButton={TextButton(onClick=close){Text("Volver")}})
}

@Composable
private fun ProofPicker(label:String,type:String,selected:(String)->Unit,failed:(String)->Unit){
    val resolver=LocalContext.current.contentResolver
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null)runCatching {
            val bytes=requireNotNull(resolver.openInputStream(uri)).use { stream->
                val output=java.io.ByteArrayOutputStream()
                val buf=ByteArray(8192)
                var n=stream.read(buf)
                while(n!=-1){require(output.size()+n<=2097152);output.write(buf,0,n);n=stream.read(buf)}
                output.toByteArray()
            }
            require(bytes.isNotEmpty())
            selected(Base64.encodeToString(bytes,Base64.NO_WRAP))
        }.onFailure{failed("Elegí un comprobante de hasta 2 MiB.")}
    }
    TextButton(onClick={picker.launch(type)}){Text(label)}
}
