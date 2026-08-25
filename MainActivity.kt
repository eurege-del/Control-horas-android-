@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.controlhoras
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DiaTrabajo(
    val dia: Int,
    val nombreDia: String,
    val entrada: String = "",
    val salida: String = "",
    val descripcion: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Text("La aplicación funciona")
            }
        }
    }
}

@Composable
fun ControlHorasApp(context: Context) {
    val cal = remember { Calendar.getInstance() }
    var mes by rememberSaveable { mutableStateOf(cal.get(Calendar.MONTH)) }
    var ano by rememberSaveable { mutableStateOf(cal.get(Calendar.YEAR)) }
    var nombre by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences("ControlHoras", Context.MODE_PRIVATE)
                .getString("nombre", "") ?: ""
        )
    }
    var dias by remember {
        mutableStateOf(cargarDias(context, mes, ano, crearDiasDelMes(mes, ano)))
    }

    val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    var mensajePdf by remember { mutableStateOf<String?>(null) }

    val crearPdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                crearPdf(context, uri, nombre, meses[mes], ano, dias, mes)
                mensajePdf = "PDF guardado correctamente"
            } catch (_: Exception) {
                mensajePdf = "No se pudo crear el PDF"
            }
        }
    }

    LaunchedEffect(mes, ano) {
        dias = cargarDias(context, mes, ano, crearDiasDelMes(mes, ano))
    }

    LaunchedEffect(nombre) {
        context.getSharedPreferences("ControlHoras", Context.MODE_PRIVATE)
            .edit().putString("nombre", nombre).apply()
    }

    LaunchedEffect(dias, mes, ano) {
        guardarDias(context, mes, ano, dias)
    }

Box(
    modifier = Modifier.fillMaxSize()
) {

    Image(
        painter = painterResource(
            id = R.drawable.icono_tu_tiempo_trabajado
        ),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp)
            .alpha(0.08f),
        alignment = Alignment.Center
    )

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("CONTROL HORAS")
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectorMes(mes, { mes = it }, Modifier.weight(1f))
                SelectorAno(ano, { ano = it }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "TOTAL MES: ${formatoMinutos(dias.sumOf { calcularHoras(it.entrada, it.salida) })}",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val archivo = "Control_Horas_${meses[mes]}_$ano.pdf"
                    crearPdfLauncher.launch(archivo)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DESCARGAR COPIA EN PDF")
            }

            mensajePdf?.let {
                Spacer(Modifier.height(4.dp))
                Text(it)
            }

            Spacer(Modifier.height(12.dp))

            val semanas = remember(dias, mes, ano) {
                agruparPorSemanas(dias, mes, ano)
            }

            semanas.forEachIndexed { indiceSemana, semana ->
                Text(
                    "SEMANA ${indiceSemana + 1}",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(4.dp))

                semana.forEach { d ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("${d.nombreDia} ${d.dia}", modifier = Modifier.width(70.dp))

                        OutlinedTextField(
                            value = d.entrada,
                            onValueChange = { valor ->
                                dias = dias.map { actual ->
                                    if (actual.dia == d.dia) actual.copy(entrada = formatearHora(valor))
                                    else actual
                                }
                            },
                            label = { Text("Entrada") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .width(105.dp)
                                .onFocusChanged { estado ->
                                    if (estado.isFocused) {
                                        dias = dias.map { actual ->
                                            if (actual.dia == d.dia) actual.copy(entrada = horaActual())
                                            else actual
                                        }
                                    }
                                }
                        )

                        OutlinedTextField(
                            value = d.salida,
                            onValueChange = { valor ->
                                dias = dias.map { actual ->
                                    if (actual.dia == d.dia) actual.copy(salida = formatearHora(valor))
                                    else actual
                                }
                            },
                            label = { Text("Salida") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .width(105.dp)
                                .onFocusChanged { estado ->
                                    if (estado.isFocused) {
                                        dias = dias.map { actual ->
                                            if (actual.dia == d.dia) actual.copy(salida = horaActual())
                                            else actual
                                        }
                                    }
                                }
                        )

                        Text(
                            formatoMinutos(calcularHoras(d.entrada, d.salida)),
                            modifier = Modifier.width(85.dp)
                        )

                        OutlinedTextField(
                            value = d.descripcion,
                            onValueChange = { valor ->
                                dias = dias.map { actual ->
                                    if (actual.dia == d.dia) actual.copy(descripcion = valor)
                                    else actual
                                }
                            },
                            label = { Text("Qué hice") },
                            singleLine = true,
                            modifier = Modifier.width(180.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                val totalSemana = semana.sumOf {
                    calcularHoras(it.entrada, it.salida)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp)
                ) {
                    Text(
                        "TOTAL SEMANA ${indiceSemana + 1}: ${formatoMinutos(totalSemana)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SelectorMes(valor: Int, onCambio: (Int) -> Unit, modifier: Modifier = Modifier) {
    val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    var abierto by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = !abierto },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = meses[valor],
            onValueChange = {},
            readOnly = true,
            label = { Text("Mes") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            meses.forEachIndexed { i, m ->
                DropdownMenuItem(
                    text = { Text(m) },
                    onClick = { onCambio(i); abierto = false }
                )
            }
        }
    }
}

@Composable
fun SelectorAno(valor: Int, onCambio: (Int) -> Unit, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    val anos = (2024..2035).toList()

    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = !abierto },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = valor.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Año") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            anos.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.toString()) },
                    onClick = { onCambio(a); abierto = false }
                )
            }
        }
    }
}

fun crearDiasDelMes(mes: Int, ano: Int): List<DiaTrabajo> {
    val c = Calendar.getInstance()
    c.set(ano, mes, 1)
    val n = c.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (1..n).map { d ->
        c.set(ano, mes, d)
        DiaTrabajo(
            d,
            c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale("es", "ES")) ?: ""
        )
    }
}

fun agruparPorSemanas(dias: List<DiaTrabajo>, mes: Int, ano: Int): List<List<DiaTrabajo>> {
    val calendario = Calendar.getInstance()
    calendario.firstDayOfWeek = Calendar.MONDAY
    return dias.groupBy { dia ->
        calendario.set(ano, mes, dia.dia)
        calendario.get(Calendar.WEEK_OF_MONTH)
    }.toSortedMap().values.toList()
}

fun horaActual(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

$marker
    val digitos = texto.filter { it.isDigit() }.take(4)
    return when (digitos.length) {
        0 -> ""
        1, 2, 3 -> digitos
        else -> "${digitos.substring(0, 2)}:${digitos.substring(2, 4)}"
    }
}

fun calcularHoras(entrada: String, salida: String): Int = try {
    if (!entrada.contains(":") || !salida.contains(":")) 0
    else {
        val e = entrada.split(":")
        val s = salida.split(":")
        val inicio = e[0].toInt() * 60 + e[1].toInt()
        var fin = s[0].toInt() * 60 + s[1].toInt()
        if (fin < inicio) fin += 1440
        fin - inicio
    }
} catch (_: Exception) { 0 }

fun formatoMinutos(m: Int) =
    String.format(Locale.getDefault(), "%02d h %02d min", m / 60, m % 60)

fun crearPdf(
    context: Context,
    uri: Uri,
    nombre: String,
    nombreMes: String,
    ano: Int,
    dias: List<DiaTrabajo>,
    mes: Int
) {
    val documento = PdfDocument()
    val ancho = 595
    val alto = 842
    val margen = 36
    val pintura = Paint().apply { textSize = 11f }
    val titulo = Paint().apply { textSize = 18f; isFakeBoldText = true }
    val subtitulo = Paint().apply { textSize = 13f; isFakeBoldText = true }

    var numeroPagina = 1
    var pagina = documento.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numeroPagina).create())
    var canvas = pagina.canvas
    var y = 45

    fun nuevaPagina() {
        documento.finishPage(pagina)
        numeroPagina++
        pagina = documento.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numeroPagina).create())
        canvas = pagina.canvas
        y = 45
    }

    canvas.drawText("CONTROL DE HORAS", margen.toFloat(), y.toFloat(), titulo)
    y += 28
    canvas.drawText("Nombre: ${if (nombre.isBlank()) "-" else nombre}", margen.toFloat(), y.toFloat(), pintura)
    y += 20
    canvas.drawText("Mes: $nombreMes $ano", margen.toFloat(), y.toFloat(), pintura)
    y += 28

    val semanas = agruparPorSemanas(dias, mes, ano)

    semanas.forEachIndexed { indice, semana ->
        if (y > alto - 100) nuevaPagina()

        canvas.drawText("SEMANA ${indice + 1}", margen.toFloat(), y.toFloat(), subtitulo)
        y += 20

        semana.forEach { d ->
            if (y > alto - 70) nuevaPagina()

            val linea = "${d.nombreDia} ${d.dia}    Entrada: ${d.entrada.ifBlank { "-" }}    Salida: ${d.salida.ifBlank { "-" }}    Total: ${formatoMinutos(calcularHoras(d.entrada, d.salida))}"
            canvas.drawText(linea, margen.toFloat(), y.toFloat(), pintura)
            y += 16

            if (d.descripcion.isNotBlank()) {
                canvas.drawText("   Qué hice: ${d.descripcion}", margen.toFloat(), y.toFloat(), pintura)
                y += 16
            }
        }

        val totalSemana = semana.sumOf { calcularHoras(it.entrada, it.salida) }
        canvas.drawText(
            "TOTAL SEMANA ${indice + 1}: ${formatoMinutos(totalSemana)}",
            margen.toFloat(),
            y.toFloat(),
            subtitulo
        )
        y += 28
    }

    if (y > alto - 50) nuevaPagina()
    val totalMes = dias.sumOf { calcularHoras(it.entrada, it.salida) }
    canvas.drawText("TOTAL MES: ${formatoMinutos(totalMes)}", margen.toFloat(), y.toFloat(), titulo)

    documento.finishPage(pagina)
    context.contentResolver.openOutputStream(uri)?.use { salida ->
        documento.writeTo(salida)
    }
    documento.close()
}

fun clave(m: Int, a: Int) = "dias_${a}_${m}"

fun guardarDias(c: Context, m: Int, a: Int, dias: List<DiaTrabajo>) {
    val ar = JSONArray()
    dias.forEach { d ->
        ar.put(
            JSONObject()
                .put("dia", d.dia)
                .put("nombreDia", d.nombreDia)
                .put("entrada", d.entrada)
                .put("salida", d.salida)
                .put("descripcion", d.descripcion)
        )
    }
    c.getSharedPreferences("ControlHoras", Context.MODE_PRIVATE)
        .edit().putString(clave(m, a), ar.toString()).apply()
}

fun cargarDias(c: Context, m: Int, a: Int, base: List<DiaTrabajo>): List<DiaTrabajo> {
    val t = c.getSharedPreferences("ControlHoras", Context.MODE_PRIVATE)
        .getString(clave(m, a), null) ?: return base
    return try {
        val ar = JSONArray(t)
        val guardados = mutableMapOf<Int, DiaTrabajo>()
        for (i in 0 until ar.length()) {
            val o = ar.getJSONObject(i)
            val d = o.getInt("dia")
            guardados[d] = DiaTrabajo(
                d,
                o.optString("nombreDia", ""),
                o.optString("entrada", ""),
                o.optString("salida", ""),
                o.optString("descripcion", "")
            )
        }
        base.map { guardados[it.dia] ?: it }
    } catch (_: Exception) { base }
}
