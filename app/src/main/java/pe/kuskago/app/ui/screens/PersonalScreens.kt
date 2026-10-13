package pe.kuskago.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.R
import pe.kuskago.app.Route
import pe.kuskago.app.data.FamilyMember
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.data.StoredDoc
import pe.kuskago.app.ui.components.ChoiceCard
import pe.kuskago.app.ui.components.Chip
import pe.kuskago.app.ui.components.ErrorCard
import pe.kuskago.app.ui.components.FieldLabel
import pe.kuskago.app.ui.components.GhostButton
import pe.kuskago.app.ui.components.KCard
import pe.kuskago.app.ui.components.KProgress
import pe.kuskago.app.ui.components.KTextField
import pe.kuskago.app.ui.components.ListItemRow
import pe.kuskago.app.ui.components.MainBottomNav
import pe.kuskago.app.ui.components.PillTabs
import pe.kuskago.app.ui.components.PrimaryButton
import pe.kuskago.app.ui.components.Screen
import pe.kuskago.app.ui.components.SecondaryButton
import pe.kuskago.app.ui.components.SectionLabel
import pe.kuskago.app.ui.components.StepDot
import pe.kuskago.app.ui.components.SuccessCheck
import pe.kuskago.app.ui.components.TopBar
import pe.kuskago.app.ui.components.buildDocHtml
import pe.kuskago.app.ui.components.openUri
import pe.kuskago.app.ui.components.openUrl
import pe.kuskago.app.ui.components.printHtml
import pe.kuskago.app.ui.components.rememberSpeaker
import pe.kuskago.app.ui.components.shareFile
import pe.kuskago.app.ui.components.toast
import pe.kuskago.app.ui.theme.K
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WarnBg = Color(0xFFFDF0DE)
private val WarnText = Color(0xFFB4530A)
private val UrgentBg = Color(0xFFFDE2E1)
private val UrgentText = Color(0xFFB42318)

private fun nowLabel(): String = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE")).format(Date())

@Composable
private fun Scroll(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        content()
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 13.5.sp, color = K.Muted, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 16.dp))
}

// =============================================================
// 12. MIS DOCUMENTOS
// =============================================================
@Composable
fun DocumentsScreen(store: KuskaStore, onExit: () -> Unit, go: (Route) -> Unit) {
    val context = LocalContext.current
    val docs = remember { mutableStateListOf<StoredDoc>().apply { addAll(store.getDocs()) } }
    var step by rememberSaveable { mutableStateOf("list") }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var replaceIndex by rememberSaveable { mutableIntStateOf(-1) }
    var pendingUri by rememberSaveable { mutableStateOf("") }
    var pendingFile by rememberSaveable { mutableStateOf("") }
    var docName by rememberSaveable { mutableStateOf("") }
    var docType by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var viewing by remember { mutableStateOf<StoredDoc?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun persist() = store.saveDocs(docs.toList())

    fun insertOrReplace(doc: StoredDoc) {
        if (replaceIndex in docs.indices) docs[replaceIndex] = doc else docs.add(0, doc)
        replaceIndex = -1
        persist()
        step = "list"
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { /* algunos proveedores no lo permiten */ }
            var name = "documento"
            val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0) ?: name
                }
                cursor.close()
            }
            pendingUri = uri.toString()
            pendingFile = name
            docName = name.substringBeforeLast('.')
            docType = ""
            step = "form"
        }
    }

    val titles = mapOf(
        "list" to "Mis documentos", "detail" to "Documento", "choose" to "Agregar documento",
        "form" to "Datos del documento", "create" to "Crear documento",
    )
    BackHandler(enabled = step != "list") { step = "list"; replaceIndex = -1 }

    Screen {
        TopBar(onBack = { if (step != "list") { step = "list"; replaceIndex = -1 } else onExit() }, title = titles[step])
        Box(Modifier.weight(1f)) {
            Scroll {
                when (step) {
                    "list" -> {
                        if (docs.isEmpty()) Hint("Todavía no tienes documentos guardados.")
                        docs.forEachIndexed { i, d ->
                            ListItemRow(d.icon, d.name, d.meta) { selected = i; step = "detail" }
                        }
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton("＋ Agregar documento", { replaceIndex = -1; step = "choose" })
                    }

                    "detail" -> {
                        val d = docs.getOrNull(selected)
                        if (d == null) {
                            Hint("Este documento ya no existe.")
                        } else {
                            KCard(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(K.GreenSoft),
                                        contentAlignment = Alignment.Center,
                                    ) { Text(d.icon, fontSize = 22.sp) }
                                    Column {
                                        Text(d.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = K.Ink)
                                        Text(d.meta, fontSize = 12.sp, color = K.Muted)
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SecondaryButton("Ver", {
                                    if (d.uri.isNotBlank()) context.openUri(d.uri) else viewing = d
                                })
                                SecondaryButton("Usar en trámite", { go(Route.TramiteDetail) })
                                SecondaryButton("Descargar", {
                                    when {
                                        d.uri.isNotBlank() -> context.shareFile(d.uri)
                                        d.content.isNotBlank() -> context.printHtml(d.name, buildDocHtml(d.name.substringBeforeLast('.'), d.meta, d.content, emptyList()))
                                        else -> context.toast("Este es un documento de ejemplo del prototipo")
                                    }
                                })
                                SecondaryButton("Reemplazar", { replaceIndex = selected; step = "choose" })
                                SecondaryButton("Eliminar", { confirmDelete = true }, contentColor = K.ErrorTitle, borderColor = K.ErrorBorder)
                            }
                        }
                    }

                    "choose" -> {
                        Hint("¿Cómo quieres agregar tu documento?")
                        ChoiceCard("📄 Subir PDF o foto desde tu equipo", {
                            picker.launch(arrayOf("application/pdf", "image/*"))
                        }, Modifier.padding(bottom = 10.dp))
                        ChoiceCard("✏️ Crear documento nuevo", {
                            docName = ""; content = ""; step = "create"
                        })
                    }

                    "form" -> {
                        KCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                            Text("✓  Archivo cargado: $pendingFile", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.GreenDark)
                        }
                        FieldLabel("Nombre del documento")
                        KTextField(docName, { docName = it }, "Ej. Constancia de posesión", Modifier.padding(bottom = 14.dp))
                        FieldLabel("Tipo de documento")
                        KTextField(docType, { docType = it }, "Ej. DNI, croquis, constancia", Modifier.padding(bottom = 22.dp))
                        PrimaryButton(
                            "Guardar documento",
                            {
                                if (docName.isNotBlank()) {
                                    val ext = pendingFile.substringAfterLast('.', "pdf")
                                    val icon = if (ext.lowercase() in listOf("jpg", "jpeg", "png", "webp", "heic")) "🖼️" else "📄"
                                    insertOrReplace(
                                        StoredDoc("${docName.trim()}.$ext", "${docType.ifBlank { "Documento" }} · agregado ${nowLabel()}", icon, uri = pendingUri)
                                    )
                                }
                            },
                            containerColor = if (docName.isNotBlank()) K.Green else K.Green.copy(alpha = 0.45f),
                        )
                    }

                    else -> { // create
                        FieldLabel("Título del documento")
                        KTextField(docName, { docName = it }, "Ej. Declaración jurada", Modifier.padding(bottom = 14.dp))
                        FieldLabel("Contenido")
                        KTextField(
                            content, { content = it }, "Escribe aquí el contenido de tu documento...",
                            Modifier.padding(bottom = 22.dp), singleLine = false, minLines = 7,
                        )
                        PrimaryButton(
                            "Guardar documento",
                            {
                                if (docName.isNotBlank()) {
                                    insertOrReplace(StoredDoc("${docName.trim()}.txt", "Creado en KuskaGo · ${nowLabel()}", "📝", content = content))
                                }
                            },
                            containerColor = if (docName.isNotBlank()) K.Green else K.Green.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }
    }

    viewing?.let { d ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("Cerrar", color = K.Green) } },
            title = { Text(d.name, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    d.content.ifBlank { "Documento de ejemplo del prototipo. Sube tus propios archivos con \"Agregar documento\"." },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            containerColor = K.White,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar documento?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Se quitará \"${docs.getOrNull(selected)?.name.orEmpty()}\" de Mis documentos.") },
            confirmButton = {
                TextButton(onClick = {
                    if (selected in docs.indices) docs.removeAt(selected)
                    persist()
                    confirmDelete = false
                    step = "list"
                }) { Text("Eliminar", color = K.ErrorTitle, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = K.Green) } },
            containerColor = K.White,
        )
    }
}

// =============================================================
// 13. MI FAMILIA
// =============================================================
@Composable
fun FamilyScreen(store: KuskaStore, onExit: () -> Unit, go: (Route) -> Unit, onFamilyWizard: () -> Unit) {
    val family = remember { mutableStateListOf<FamilyMember>().apply { addAll(store.getFamily()) } }
    var step by rememberSaveable { mutableStateOf("list") }
    var chosen by rememberSaveable { mutableIntStateOf(0) }
    var fName by rememberSaveable { mutableStateOf("") }
    var fRel by rememberSaveable { mutableStateOf("") }
    var fDni by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val titles = mapOf("list" to "Mi familia", "add" to "Agregar familiar", "select" to "Crear trámite familiar", "herencia" to "Trámite familiar")
    BackHandler(enabled = step != "list") { step = "list" }

    fun iconFor(rel: String): String {
        val r = rel.lowercase()
        return when {
            "madre" in r || "abuela" in r -> "👵"
            "padre" in r || "abuelo" in r -> "👴"
            "hijo" in r -> "👦"
            "hija" in r -> "👧"
            "esposa" in r || "cónyuge" in r || "conyuge" in r || "esposo" in r -> "💑"
            else -> "🧑"
        }
    }

    Screen {
        TopBar(onBack = { if (step != "list") step = "list" else onExit() }, title = titles[step])
        Box(Modifier.weight(1f)) {
            Scroll {
                when (step) {
                    "list" -> {
                        family.forEach { m ->
                            ListItemRow(m.icon, m.name, "${m.relation} · DNI ${m.dni}", trailing = null)
                        }
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Agregar familiar", { fName = ""; fRel = ""; fDni = ""; error = null; step = "add" })
                            PrimaryButton("Crear trámite familiar", { if (family.isNotEmpty()) step = "select" })
                        }
                    }

                    "add" -> {
                        error?.let { ErrorCard("Revisa los datos", it, Modifier.padding(bottom = 14.dp)) }
                        FieldLabel("Nombre completo")
                        KTextField(fName, { fName = it; error = null }, "Nombre del familiar", Modifier.padding(bottom = 14.dp))
                        FieldLabel("Parentesco")
                        KTextField(fRel, { fRel = it; error = null }, "Ej. Madre, hijo, cónyuge", Modifier.padding(bottom = 14.dp))
                        FieldLabel("DNI")
                        KTextField(
                            fDni, { fDni = it.filter { c -> c.isDigit() }.take(8); error = null }, "DNI del familiar",
                            Modifier.padding(bottom = 22.dp), keyboardType = KeyboardType.Number,
                        )
                        PrimaryButton("Guardar familiar", {
                            error = when {
                                fName.isBlank() -> "Escribe el nombre completo del familiar."
                                fRel.isBlank() -> "Indica el parentesco (madre, hijo, cónyuge…)."
                                fDni.length != 8 -> "El DNI debe tener exactamente 8 números."
                                else -> null
                            }
                            if (error == null) {
                                family.add(FamilyMember(fName.trim(), fRel.trim().replaceFirstChar { it.uppercase() }, fDni, iconFor(fRel)))
                                store.saveFamily(family.toList())
                                step = "list"
                            }
                        })
                    }

                    "select" -> {
                        Hint("¿Para quién es este trámite?")
                        family.forEachIndexed { i, m ->
                            ChoiceCard("${m.icon}  ${m.name}", { chosen = i; step = "herencia" }, Modifier.padding(bottom = 10.dp))
                        }
                        SectionLabel("Trámite sugerido", Modifier.padding(top = 8.dp, bottom = 8.dp))
                        KCard(Modifier.fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🏛️", fontSize = 22.sp)
                                Column {
                                    Text("Herencia / Sucesión", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = K.Ink)
                                    Text("Registro de sucesión intestada", fontSize = 12.sp, color = K.Muted)
                                }
                            }
                        }
                    }

                    else -> { // herencia
                        val titular = family.getOrNull(chosen)?.name ?: ""
                        val others = family.filterIndexed { i, _ -> i != chosen }.joinToString(", ") { "${it.name} (heredero)" }
                        Text("Herencia / Sucesión", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = K.Ink, modifier = Modifier.padding(bottom = 14.dp))
                        InfoCard("Personas involucradas", "$titular (titular)" + if (others.isNotBlank()) " · $others" else "")
                        InfoCard("Documentos", "Partida de defunción, DNI de herederos, declaratoria de herederos")
                        InfoCard("Ruta orientativa", "4 pasos · Notaría o Juzgado de Paz Letrado")
                        InfoCard("Entidad", "SUNARP — Registro de sucesión")
                        InfoCard("Seguimiento", "Podrás ver el avance en Mis trámites.")
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton("Crear Ruta Ciudadana familiar", { go(Route.CitizenRoute) })
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("Llenar solicitud de sucesión", onFamilyWizard)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.Ink, modifier = Modifier.padding(bottom = 6.dp))
        Text(body, fontSize = 12.5.sp, color = K.Muted)
    }
}

// =============================================================
// 14. PEDIR AYUDA
// =============================================================
private data class HelpResp(val title: String, val body: String, val action: String, val route: Route)

private val HELP = mapOf(
    "doc" to HelpResp("Te ayudamos a conseguirlo", "Notamos que te falta un documento. Te mostramos qué es y cómo obtenerlo.", "Ver cómo obtenerlo", Route.RequisitoDetail),
    "term" to HelpResp("Aquí tienes la explicación", "Traducimos el término a lenguaje sencillo, con un ejemplo.", "Ver traductor de burocracia", Route.Translator),
    "observed" to HelpResp("Vamos a revisar la observación", "La entidad encontró algo que debes corregir. Te mostramos qué hacer.", "Ver observación", Route.TramiteDetail),
    "other" to HelpResp("Un asesor te contactará", "Registramos tu consulta. Un asesor de KuskaGo te escribirá pronto.", "Ir al inicio", Route.Home),
)

@Composable
fun HelpScreen(onExit: () -> Unit, go: (Route) -> Unit) {
    var step by rememberSaveable { mutableStateOf("problem") }
    var topic by rememberSaveable { mutableStateOf("doc") }
    var message by rememberSaveable { mutableStateOf("") }

    fun back() {
        when (step) {
            "response" -> step = "channel"
            "write" -> step = "channel"
            "channel" -> step = "problem"
            else -> onExit()
        }
    }
    BackHandler(enabled = step != "problem") { back() }

    Screen {
        TopBar(onBack = ::back, title = "Pedir ayuda")
        Box(Modifier.weight(1f)) {
            Scroll {
                when (step) {
                    "problem" -> {
                        Hint("¿En qué necesitas ayuda?")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ChoiceCard("📎 Me falta un documento", { topic = "doc"; step = "channel" })
                            ChoiceCard("❓ No entiendo un término", { topic = "term"; step = "channel" })
                            ChoiceCard("⚠️ Mi trámite está observado", { topic = "observed"; step = "channel" })
                            ChoiceCard("💬 Otro", { topic = "other"; step = "channel" })
                        }
                    }

                    "channel" -> {
                        Hint("¿Cómo prefieres contarnos más?")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ChoiceCard("⌨️ Escribir", { step = "write" })
                            ChoiceCard("🎙️ Hablar", { step = "response" })
                            ChoiceCard("📷 Enviar foto", { step = "response" })
                        }
                    }

                    "write" -> {
                        FieldLabel("Cuéntanos tu problema")
                        KTextField(
                            message, { message = it }, "Escribe aquí lo que necesitas…",
                            Modifier.padding(bottom = 22.dp), singleLine = false, minLines = 5,
                        )
                        PrimaryButton("Enviar", { step = "response" })
                    }

                    else -> {
                        val r = HELP.getValue(topic)
                        KCard(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                            Text(r.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = K.Ink, modifier = Modifier.padding(bottom = 6.dp))
                            Text(r.body, fontSize = 13.5.sp, color = K.Label, lineHeight = 20.sp)
                        }
                        PrimaryButton(r.action, { go(r.route) })
                    }
                }
            }
        }
    }
}

// =============================================================
// 15. RUTA CIUDADANA
// =============================================================
private val ROUTE_NAMES = mapOf(
    1 to "Obtener constancia de posesión", 2 to "Pago de tasa", 3 to "Registrar solicitud",
    4 to "Seguimiento", 5 to "Resultado",
)
private val ROUTE_SUBS = mapOf(
    1 to "Municipalidad de tu distrito", 2 to "Banco de la Nación", 3 to "COFOPRI",
    4 to "En línea", 5 to "Te avisamos con KuskaAlert",
)
private val ROUTE_EXPLAIN = mapOf(
    1 to "Ve a la municipalidad de tu distrito con tu DNI y pide la constancia de posesión de tu terreno.",
    2 to "Paga la tasa del trámite en el Banco de la Nación y guarda el voucher; lo necesitarás después.",
    3 to "Presenta tu solicitud en COFOPRI con tu DNI, el croquis, la constancia y el voucher de pago.",
    4 to "Revisa el estado de tu trámite en línea. KuskaGo te avisará si hay observaciones.",
    5 to "Cuando la entidad responda, te avisaremos con KuskaAlert para que recojas tu resultado.",
)

private data class Point(val name: String, val addr: String, val dist: String, val query: String)

private val POINTS = listOf(
    Point("Municipalidad de tu distrito", "Jr. Principal 100, tu distrito", "1.2 km", "Municipalidad distrital"),
    Point("COFOPRI — Oficina zonal", "Jr. Los Álamos 123", "3.4 km", "COFOPRI oficina zonal"),
    Point("Banco de la Nación", "Av. Comercio 456", "2.0 km", "Banco de la Nación"),
)

@Composable
fun CitizenRouteScreen(store: KuskaStore, onExit: () -> Unit, go: (Route) -> Unit, onEntity: () -> Unit) {
    val context = LocalContext.current
    var view by rememberSaveable { mutableStateOf("list") }
    var selected by rememberSaveable { mutableIntStateOf(1) }
    var done by remember { mutableStateOf(store.getRouteDone()) }
    var explain by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = view != "list") { view = "list" }
    val current = (1..5).firstOrNull { it !in done }

    Screen {
        TopBar(
            onBack = { if (view != "list") view = "list" else onExit() },
            title = when (view) { "list" -> "Tu ruta ciudadana"; "map" -> "Puntos de atención"; else -> "Paso de tu ruta" },
        )
        Box(Modifier.weight(1f)) {
            Scroll {
                when (view) {
                    "list" -> {
                        Hint("Organizamos los pasos según tu necesidad, aunque involucren distintas entidades.")
                        KProgress(done.size / 5f, Modifier.padding(bottom = 6.dp))
                        Text("${done.size} de 5 pasos completados", fontSize = 12.sp, color = K.Muted2, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        (1..5).forEach { n ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { selected = n; explain = false; view = "detail" }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StepDot(if (n in done) "✓" else "$n", if (n in done) 2 else if (n == current) 1 else 0)
                                Column(Modifier.weight(1f)) {
                                    Text(ROUTE_NAMES.getValue(n), fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, color = K.Ink)
                                    Text(ROUTE_SUBS.getValue(n), fontSize = 12.sp, color = K.Muted)
                                }
                                Text("›", color = K.Faint, fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        SecondaryButton("📍 Ver mapa y puntos de atención", { view = "map" }, minHeight = 46.dp, fontSize = 14.sp)
                    }

                    "map" -> {
                        MapPlaceholder()
                        POINTS.forEach { p ->
                            KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = K.Ink)
                                        Text(p.addr, fontSize = 12.sp, color = K.Muted)
                                    }
                                    Chip(p.dist, K.SkyBg, K.SkyText)
                                }
                                SecondaryButton("Cómo llegar", {
                                    context.openUrl("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(p.query))
                                }, minHeight = 42.dp, fontSize = 14.sp)
                            }
                        }
                    }

                    else -> {
                        val isDone = selected in done
                        Text(ROUTE_NAMES.getValue(selected), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = K.Ink, modifier = Modifier.padding(bottom = 6.dp))
                        Box(Modifier.padding(bottom = 18.dp)) {
                            if (isDone) Chip("Completado", K.OkBg, K.GreenDark) else Chip("Pendiente", WarnBg, WarnText)
                        }
                        if (explain) {
                            KCard(Modifier.fillMaxWidth().padding(bottom = 14.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                                Text(ROUTE_EXPLAIN.getValue(selected), fontSize = 13.5.sp, color = K.Ink, lineHeight = 20.sp)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton(if (explain) "Ocultar explicación" else "Ver explicación", { explain = !explain })
                            SecondaryButton("Subir documento", { go(Route.RequisitoDetail) })
                            SecondaryButton("Usar plantilla", { go(Route.Templates) })
                            SecondaryButton("Consultar entidad", onEntity)
                            if (isDone) {
                                GhostButton("Marcar como pendiente", {
                                    done = done - selected; store.saveRouteDone(done); view = "list"
                                })
                            } else {
                                PrimaryButton("Marcar como completado", {
                                    done = done + selected; store.saveRouteDone(done); view = "list"
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPlaceholder() {
    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(bottom = 0.dp)
            .clip(shape)
            .border(1.5.dp, K.Border, shape)
            .background(Brush.linearGradient(listOf(K.Mint, K.GreenSoft))),
    ) {
        val w = maxWidth
        val h = maxHeight
        listOf(0.28f to 0.40f, 0.55f to 0.60f, 0.72f to 0.28f).forEach { (x, y) ->
            Text("📍", fontSize = 26.sp, modifier = Modifier.offset(x = w * x, y = h * y))
        }
    }
    Spacer(Modifier.height(16.dp))
}

// =============================================================
// 16. KUSKAALERT (avisos)
// =============================================================
private data class Alert(
    val id: String, val kind: String, val chip: String, val when_: String,
    val title: String, val summary: String, val body: String, val action: String, val route: Route,
)

private val ALERTS = listOf(
    Alert(
        "urgent", "urgent", "URGENTE", "Hace 1 hora", "Tienes 2 días para responder una observación",
        "La entidad encontró un documento que debes corregir.",
        "La entidad revisó tus documentos y encontró algo que debes corregir antes de continuar.",
        "Adjuntar ahora", Route.Documents,
    ),
    Alert(
        "important", "important", "IMPORTANTE", "Ayer", "Tu trámite cambió de estado",
        "Antes: En revisión → Ahora: Observado",
        "Tu Registro de terreno pasó de \"En revisión\" a \"Observado\". Revisa qué debes corregir.",
        "Ver trámite", Route.TramiteDetail,
    ),
    Alert(
        "info", "info", "INFORMATIVA", "Hace 3 días", "Tienes un trámite pendiente",
        "Registro de terreno · Progreso 60% · Te falta completar 2 requisitos",
        "Registro de terreno lleva 60% de avance. Te faltan 2 requisitos por completar.",
        "Continuar trámite", Route.TramiteDetail,
    ),
)

private fun alertColors(kind: String): Pair<Color, Color> = when (kind) {
    "urgent" -> UrgentBg to UrgentText
    "important" -> WarnBg to WarnText
    else -> K.SkyBg to K.SkyText
}

@Composable
fun AlertsScreen(onExit: () -> Unit, go: (Route) -> Unit) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf("all") }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = openId != null) { openId = null }
    val open = ALERTS.firstOrNull { it.id == openId }

    Screen {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                TopBar(onBack = { if (open != null) openId = null else onExit() }, title = if (open == null) "KuskaAlert" else "Notificación")
            }
            Box(
                Modifier
                    .padding(end = 20.dp, top = 8.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(K.White)
                    .border(1.dp, K.Border, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { go(Route.Profile) },
                contentAlignment = Alignment.Center,
            ) { Text("⚙️", fontSize = 16.sp) }
        }
        if (open == null) {
            val counts = mapOf(
                "all" to ALERTS.size,
                "important" to ALERTS.count { it.kind == "important" },
                "urgent" to ALERTS.count { it.kind == "urgent" },
            )
            PillTabs(
                tabs = listOf(
                    "all" to "Todas (${counts["all"]})",
                    "important" to "Importantes (${counts["important"]})",
                    "urgent" to "Urgentes (${counts["urgent"]})",
                ),
                selected = filter,
                onSelect = { filter = it },
            )
        }
        Box(Modifier.weight(1f)) {
            Scroll {
                if (open == null) {
                    ALERTS.filter { filter == "all" || it.kind == filter }.forEach { a ->
                        val (bg, fg) = alertColors(a.kind)
                        KCard(
                            Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            background = if (a.kind == "urgent") K.ErrorBg else K.White,
                            borderColor = if (a.kind == "urgent") K.ErrorBorder else K.Border,
                            onClick = { openId = a.id },
                        ) {
                            Box(Modifier.padding(bottom = 8.dp)) { Chip("${a.chip} · ${a.when_}", bg, fg) }
                            Text(a.title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = K.Ink, modifier = Modifier.padding(bottom = 4.dp))
                            Text(a.summary, fontSize = 12.5.sp, color = K.Muted)
                        }
                    }
                } else {
                    val (bg, fg) = alertColors(open.kind)
                    Box(Modifier.padding(bottom = 16.dp)) { Chip(open.chip, bg, fg) }
                    Text(open.title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, lineHeight = 25.sp, modifier = Modifier.padding(bottom = 10.dp))
                    Text(open.body, fontSize = 13.5.sp, color = K.Label, lineHeight = 21.sp, modifier = Modifier.padding(bottom = 24.dp))
                    PrimaryButton(open.action, { go(open.route) })
                    SectionLabel("Recordarme", Modifier.padding(top = 18.dp, bottom = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Mañana", "En una semana", "No recordarme").forEach { label ->
                            TextButton(onClick = {
                                context.toast(if (label == "No recordarme") "Listo, no te lo recordaremos" else "Te lo recordaremos: ${label.lowercase()}")
                                openId = null
                            }) { Text(label, color = K.Green, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                        }
                    }
                }
            }
        }
        MainBottomNav(Route.Alerts, go)
    }
}

// =============================================================
// 18b. EDITAR DATOS
// =============================================================
@Composable
fun EditProfileScreen(store: KuskaStore, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val p = remember { store.getProfile() }
    var name by rememberSaveable { mutableStateOf(p.name) }
    var phone by rememberSaveable { mutableStateOf(p.phone.filter { it.isDigit() }) }
    var place by rememberSaveable { mutableStateOf(p.place) }
    var error by remember { mutableStateOf<String?>(null) }

    Screen {
        TopBar(onBack = onBack, title = "Editar datos")
        Box(Modifier.weight(1f)) {
            Scroll {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(R.drawable.img_avatar), null,
                        Modifier.size(72.dp).clip(CircleShape),
                    )
                }
                GhostButton("Cambiar foto", { context.toast("Cambiar foto: disponible próximamente") }, fontSize = 13.sp)
                error?.let { ErrorCard("Revisa tus datos", it, Modifier.padding(bottom = 14.dp)) }
                FieldLabel("Nombres completos")
                KTextField(name, { name = it; error = null }, "Ej. Juan Quispe", Modifier.padding(bottom = 16.dp))
                FieldLabel("DNI", extra = "(no se puede cambiar)")
                KTextField(p.dni, {}, "", Modifier.padding(bottom = 16.dp), enabled = false)
                FieldLabel("Celular")
                KTextField(
                    phone, { phone = it.filter { c -> c.isDigit() }.take(9); error = null }, "9xx xxx xxx",
                    Modifier.padding(bottom = 16.dp), keyboardType = KeyboardType.Phone,
                )
                FieldLabel("Comunidad / distrito")
                KTextField(place, { place = it; error = null }, "Ej. Puno", Modifier.padding(bottom = 26.dp))
                PrimaryButton("Guardar cambios", {
                    error = when {
                        name.isBlank() -> "Escribe tus nombres completos."
                        phone.isNotEmpty() && !Regex("^9\\d{8}$").matches(phone) -> "El celular debe tener 9 números y empezar con 9."
                        place.isBlank() -> "Escribe tu comunidad o distrito."
                        else -> null
                    }
                    if (error == null) {
                        store.saveProfile(name.trim(), phone, place.trim())
                        onSaved()
                    }
                })
            }
        }
    }
}

// =============================================================
// 19. FLUJO COMPLETO — MODO SENCILLO
// =============================================================
@Composable
fun SimpleFlowScreen(onExit: () -> Unit, go: (Route) -> Unit) {
    val speak = rememberSpeaker()
    val order = listOf("search", "detail", "req", "route", "track")
    var step by rememberSaveable { mutableStateOf("search") }
    val idx = order.indexOf(step)
    val titles = mapOf("search" to "Buscar trámite", "detail" to "Tu trámite", "req" to "Requisitos", "route" to "Tu ruta", "track" to "Seguimiento")
    val texts = mapOf(
        "search" to "Toca tu trámite: Registro de terreno.",
        "detail" to "Vas 60 por ciento listo. Toca ver qué me falta.",
        "req" to "Te falta la constancia de posesión.",
        "route" to "Ve a tu municipalidad, pide la constancia, y súbela a KuskaGo.",
        "track" to "Listo. Te avisamos por KuskaAlert.",
    )
    fun back() { if (idx > 0) step = order[idx - 1] else onExit() }
    BackHandler(enabled = idx > 0) { back() }

    val say: () -> Unit = { speak(texts.getValue(step)) }

    Screen {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { TopBar(onBack = ::back, title = titles.getValue(step), titleSize = 19.sp) }
            Box(Modifier.padding(end = 20.dp, top = 8.dp)) { Chip("Sencillo", K.MustardBg, K.MustardText) }
        }
        Box(Modifier.weight(1f)) {
            Scroll {
                when (step) {
                    "search" -> {
                        Text("Toca tu trámite:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Label, modifier = Modifier.padding(bottom = 16.dp))
                        KCard(
                            Modifier.fillMaxWidth().padding(bottom = 14.dp),
                            onClick = { step = "detail" },
                            contentPadding = PaddingValues(20.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🏞️", fontSize = 30.sp)
                                Text("Registro de terreno", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = K.Ink)
                            }
                        }
                        ListenButton("🔊 Escuchar opciones", say)
                    }

                    "detail" -> {
                        Text("Registro de terreno", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(bottom = 14.dp))
                        KCard(Modifier.fillMaxWidth().padding(bottom = 18.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                            Text("Vas 60% listo 👍", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink, modifier = Modifier.padding(bottom = 10.dp))
                            KProgress(0.6f)
                        }
                        BigButton("Ver qué me falta") { step = "req" }
                        ListenButton("🔊 Escuchar", say)
                    }

                    "req" -> {
                        Text("Te falta esto:", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(bottom = 16.dp))
                        KCard(Modifier.fillMaxWidth().padding(bottom = 14.dp), contentPadding = PaddingValues(18.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📜", fontSize = 26.sp)
                                Text("Constancia de posesión", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = K.Ink)
                            }
                        }
                        BigButton("Ver cómo conseguirlo") { step = "route" }
                        ListenButton("🔊 Escuchar", say)
                    }

                    "route" -> {
                        Text("Sigue estos pasos:", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(bottom = 16.dp))
                        listOf(
                            Triple("✓", 2, "Ve a tu municipalidad"),
                            Triple("2", 1, "Pide la constancia"),
                            Triple("3", 0, "Súbela a KuskaGo"),
                        ).forEach { (mark, state, text) ->
                            KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StepDot(mark, state, 32.dp)
                                    Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = K.Ink)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        BigButton("Ya lo hice") { step = "track" }
                        ListenButton("🔊 Escuchar", say)
                    }

                    else -> {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                Modifier.size(80.dp).clip(CircleShape).background(K.OkBg),
                                contentAlignment = Alignment.Center,
                            ) { Text("👍", fontSize = 36.sp) }
                            Text("¡Listo! Te avisamos", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                            Text(
                                "Cuando tengas todo, te avisamos por KuskaAlert para el siguiente paso.",
                                fontSize = 15.sp, color = K.Label, textAlign = TextAlign.Center, lineHeight = 22.sp,
                            )
                        }
                        SecondaryButton("Volver al inicio", { go(Route.HomeSimple) }, minHeight = 60.dp)
                        ListenButton("🔊 Escuchar", say)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListenButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(top = 12.dp)) {
        Text(label, color = K.Green, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    PrimaryButton(text, onClick, fontSize = 18.sp, minHeight = 64.dp)
}
