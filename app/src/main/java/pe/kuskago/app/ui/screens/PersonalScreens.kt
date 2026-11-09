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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import pe.kuskago.app.ui.components.ProfileAvatar
import androidx.compose.runtime.LaunchedEffect
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
import pe.kuskago.app.ui.components.ScanDniCard
import pe.kuskago.app.ui.components.UseMyLocationButton
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.theme.KFont
import pe.kuskago.app.ui.components.IconLabel
import pe.kuskago.app.ui.components.KIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WarnBg = K.WarnBg
private val WarnText = K.WarnText
private val UrgentBg = K.ErrorBg
private val UrgentText = K.ErrorTitle

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
                                        Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(K.Menta),
                                        contentAlignment = Alignment.Center,
                                    ) { KIcon(d.icon, size = 28.dp, tint = K.Verde) }
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
                        ChoiceCard(":description: Subir PDF o foto desde tu equipo", {
                            picker.launch(arrayOf("application/pdf", "image/*"))
                        }, Modifier.padding(bottom = 10.dp))
                        ChoiceCard(":edit: Crear documento nuevo", {
                            docName = ""; content = ""; step = "create"
                        })
                    }

                    "form" -> {
                        KCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                            IconLabel(":check:  Archivo cargado: $pendingFile", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.GreenDark)
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
                                    val icon = if (ext.lowercase() in listOf("jpg", "jpeg", "png", "webp", "heic")) ":image:" else ":description:"
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
                                    insertOrReplace(StoredDoc("${docName.trim()}.txt", "Creado en KuskaGo · ${nowLabel()}", ":editnote:", content = content))
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
            "madre" in r || "abuela" in r -> ":elderly:"
            "padre" in r || "abuelo" in r -> ":elderly:"
            "hijo" in r -> ":face:"
            "hija" in r -> ":face:"
            "esposa" in r || "cónyuge" in r || "conyuge" in r || "esposo" in r -> ":favorite:"
            else -> ":person:"
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
                                KIcon(":accountbalance:", size = 28.dp, tint = K.Green)
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
    "other" to HelpResp("Habla con soporte", "Puedes explicar tu problema a una persona por WhatsApp o llamar al 939 138 494. Esta orientación automática no envía tu consulta.", "Hablar con una persona", Route.Support),
)

@Composable
fun HelpScreen(onExit: () -> Unit, go: (Route) -> Unit) {
    var step by rememberSaveable { mutableStateOf("problem") }
    var topic by rememberSaveable { mutableStateOf("doc") }

    fun back() {
        when (step) {
            "response" -> step = "channel"
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
                        PrimaryButton("Hablar con una persona", { go(Route.Support) }, modifier = Modifier.padding(bottom = 12.dp))
                        SecondaryButton("Ver guía completa de KuskaGo", { go(Route.CompleteGuide) }, modifier = Modifier.padding(bottom = 18.dp))
                        Text("Ayuda automática: elige un tema para consultar orientación. No estás conversando con un asesor.", color = K.Muted, modifier = Modifier.padding(bottom = 12.dp))
                        Hint("¿En qué necesitas ayuda?")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ChoiceCard(":attachfile: Me falta un documento", { topic = "doc"; step = "channel" })
                            ChoiceCard(":helpoutline: No entiendo un término", { topic = "term"; step = "channel" })
                            ChoiceCard(":warningamber: Mi trámite está observado", { topic = "observed"; step = "channel" })
                            ChoiceCard(":chat: Otro", { topic = "other"; step = "channel" })
                        }
                    }

                    "channel" -> {
                        Hint("¿Cómo prefieres recibir ayuda?")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ChoiceCard(":helpoutline: Ver orientación automática", { step = "response" })
                            ChoiceCard(":supportagent: Hablar con una persona", { go(Route.Support) })
                        }
                    }

                    else -> {
                        val r = HELP.getValue(topic)
                        KCard(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                            Text(r.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = K.Ink, modifier = Modifier.padding(bottom = 6.dp))
                            Text(r.body, fontSize = 13.5.sp, color = K.Label, lineHeight = 20.sp)
                        }
                        PrimaryButton(r.action, { go(r.route) })
                        SecondaryButton("Hablar con una persona", { go(Route.Support) }, modifier = Modifier.padding(top = 12.dp))
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
        if (view == "map") {
            // Mapa real (OpenStreetMap) con los puntos de atención del distrito del usuario
            Box(Modifier.weight(1f)) { RouteMapSection(store) }
        } else Box(Modifier.weight(1f)) {
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
                                StepDot(if (n in done) ":check:" else "$n", if (n in done) 2 else if (n == current) 1 else 0)
                                Column(Modifier.weight(1f)) {
                                    Text(ROUTE_NAMES.getValue(n), fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, color = K.Ink)
                                    Text(ROUTE_SUBS.getValue(n), fontSize = 12.sp, color = K.Muted)
                                }
                                Text("›", color = K.Faint, fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        SecondaryButton(":locationon: Ver mapa y puntos de atención", { view = "map" }, minHeight = 46.dp, fontSize = 14.sp)
                    }

                    "map" -> {}

                    else -> {
                        val isDone = selected in done
                        Text(ROUTE_NAMES.getValue(selected), fontWeight = FontWeight.ExtraBold, fontFamily = KFont.Title, fontSize = 17.sp, color = K.Ink, modifier = Modifier.padding(bottom = 6.dp))
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

// =============================================================
// 16. KUSKAALERT (avisos)
// =============================================================
internal data class Alert(
    val id: String, val kind: String, val chip: String, val when_: String,
    val title: String, val summary: String, val body: String, val action: String, val route: Route,
)

/** Avisos de ejemplo del prototipo (sobre el trámite "Registro de terreno" en curso). */
private val DEMO_ALERTS = listOf(
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

private fun relativeDay(ms: Long): String {
    val days = ((System.currentTimeMillis() - ms) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "Hoy"
        days == 1 -> "Ayer"
        else -> "Hace $days días"
    }
}

/**
 * Avisos reales a partir de "Mis trámites" + los avisos de ejemplo del prototipo.
 *  - Documento listo hace 3 días o más sin presentar → URGENTE
 *  - Documento listo                                → IMPORTANTE
 *  - Presentado, esperando respuesta                → INFORMATIVA
 */
internal fun buildAlerts(store: KuskaStore): List<Alert> {
    val real = store.getTramites().mapNotNull { t ->
        val days = ((System.currentTimeMillis() - t.updated) / 86_400_000L).toInt()
        when (t.status) {
            KuskaStore.STATUS_GENERATED -> if (days >= 3) Alert(
                "t${t.id}", "urgent", "URGENTE", relativeDay(t.updated),
                "No olvides presentar: ${t.title}",
                "Tu documento está listo hace $days días y aún no lo presentas.",
                "Tu solicitud de ${t.title} ya está lista. Imprímela, fírmala y preséntala en ${t.entity}. " +
                    "Cuando lo hagas, márcala como presentada en Mis trámites.",
                "Ver en Mis trámites", Route.MyTramites,
            ) else Alert(
                "t${t.id}", "important", "IMPORTANTE", relativeDay(t.updated),
                "Tu documento está listo: ${t.title}",
                "Imprímelo, fírmalo y preséntalo en ${t.entity}.",
                "Generaste tu solicitud de ${t.title}. El siguiente paso es presentarla en ${t.entity}. " +
                    "Antes de viajar, revisa que tengas todos los requisitos.",
                "Ver en Mis trámites", Route.MyTramites,
            )
            KuskaStore.STATUS_SUBMITTED -> Alert(
                "t${t.id}", "info", "INFORMATIVA", relativeDay(t.updated),
                "Esperando respuesta: ${t.title}",
                "${t.entity} está revisando tu solicitud.",
                "Presentaste tu solicitud de ${t.title} en ${t.entity}. Cuando recibas la respuesta, márcala en Mis trámites.",
                "Ver en Mis trámites", Route.MyTramites,
            )
            else -> null
        }
    }
    val hasDemo = store.getTramites().any { it.status == KuskaStore.STATUS_IN_PROGRESS }
    return real + if (hasDemo) DEMO_ALERTS else emptyList()
}

private fun alertColors(kind: String): Pair<Color, Color> = when (kind) {
    "urgent" -> UrgentBg to UrgentText
    "important" -> WarnBg to WarnText
    else -> K.SkyBg to K.SkyText
}

@Composable
fun AlertsScreen(store: KuskaStore, onExit: () -> Unit, go: (Route) -> Unit) {
    val alerts = remember { buildAlerts(store) }
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf("all") }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = openId != null) { openId = null }
    val open = alerts.firstOrNull { it.id == openId }

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
            ) { KIcon(":settings:", size = 22.dp, tint = K.Ink) }
        }
        if (open == null) {
            val counts = mapOf(
                "all" to alerts.size,
                "important" to alerts.count { it.kind == "important" },
                "urgent" to alerts.count { it.kind == "urgent" },
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
                    if (alerts.isEmpty()) {
                        IconLabel(":celebration: No tienes avisos por ahora.", fontSize = 14.sp, color = K.Muted, modifier = Modifier.padding(vertical = 30.dp))
                    }
                    alerts.filter { filter == "all" || it.kind == filter }.forEach { a ->
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
                    Text(open.title, fontFamily = KFont.Title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, lineHeight = 25.sp, modifier = Modifier.padding(bottom = 10.dp))
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
    var home by rememberSaveable { mutableStateOf(store.getPersonalData()["domicilio"].orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val savedData = remember { mutableStateListOf<Pair<String, String>>().apply { addAll(store.getSavedTramiteData()) } }
    var photo by rememberSaveable { mutableStateOf(store.profilePhoto()) }
    var photoBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            photoBusy = true
            try {
                val saved = store.saveProfilePhoto(uri)
                if (saved != null) {
                    photo = saved
                    context.toast("Foto de perfil actualizada")
                } else {
                    context.toast("No se pudo abrir la imagen. Elige otra foto.")
                }
            } finally {
                photoBusy = false
            }
        }
    }


    Screen {
        TopBar(onBack = onBack, title = "Editar datos")
        Box(Modifier.weight(1f)) {
            Scroll {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ProfileAvatar(photo, Modifier.size(88.dp), name = name)
                }
                SecondaryButton(
                    if (photoBusy) "Guardando foto…" else "Cambiar foto de perfil",
                    { if (!photoBusy) photoPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                Text("La foto se guarda al elegirla.", fontSize = 12.sp, color = K.Muted,
                    modifier = Modifier.padding(bottom = 12.dp))
                error?.let { ErrorCard("Revisa tus datos", it, Modifier.padding(bottom = 14.dp)) }
                ScanDniCard(
                    onResult = { d ->
                        d.fullName?.let { name = it }
                        d.address?.let { home = it }
                        d.district?.let { place = it }
                        error = null
                    },
                    modifier = Modifier.padding(bottom = 18.dp),
                )
                FieldLabel("Nombres completos")
                KTextField(name, { name = it; error = null }, "Ej. Juan Quispe", Modifier.padding(bottom = 16.dp))
                FieldLabel("DNI", extra = "(no se puede cambiar)")
                KTextField(p.dni, {}, "", Modifier.padding(bottom = 16.dp), enabled = false)
                FieldLabel("Celular")
                KTextField(
                    phone, { phone = it.filter { c -> c.isDigit() }.take(9); error = null }, "9xx xxx xxx",
                    Modifier.padding(bottom = 16.dp), keyboardType = KeyboardType.Phone,
                )
                FieldLabel("Dirección de tu casa")
                KTextField(home, { home = it }, "Jr./Av., número, barrio", Modifier.padding(bottom = 8.dp))
                UseMyLocationButton(
                    onResult = { g ->
                        g.address?.let { home = it }
                        g.district?.let { place = it }
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
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
                        if (home.isNotBlank()) store.savePersonalData(mapOf("domicilio" to home))
                        onSaved()
                    }
                })

                // ---- Datos que la app recuerda para los trámites ----
                Spacer(Modifier.height(28.dp))
                SectionLabel("Datos guardados para tus trámites", Modifier.padding(bottom = 4.dp))
                Text(
                    "Los usamos para no volver a preguntarte lo mismo. Puedes borrar cualquiera.",
                    fontSize = 12.5.sp, color = K.Muted, modifier = Modifier.padding(bottom = 6.dp),
                )
                if (savedData.isEmpty()) {
                    Text(
                        "Todavía no hay datos guardados. Se guardan solos cuando completas un trámite.",
                        fontSize = 12.5.sp, color = K.Muted2, modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                savedData.forEach { (key, value) ->
                    ListItemRow(
                        icon = null,
                        title = KuskaStore.PERSONAL_LABELS[key] ?: key,
                        subtitle = value,
                        trailing = "Borrar",
                        trailingColor = K.ErrorTitle,
                    ) {
                        store.deletePersonalData(key)
                        savedData.removeAll { it.first == key }
                        context.toast("Dato borrado")
                    }
                }
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
    LaunchedEffect(step) { speak.stop() }
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
                                KIcon(":landscape:", size = 38.dp, tint = K.Green)
                                Text("Registro de terreno", fontWeight = FontWeight.ExtraBold, fontFamily = KFont.Title, fontSize = 17.sp, color = K.Ink)
                            }
                        }
                        ListenButton(if (speak.isSpeaking) ":stop: Detener explicación" else ":volumeup: Escuchar opciones", say)
                    }

                    "detail" -> {
                        Text("Registro de terreno", fontFamily = KFont.Title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(bottom = 14.dp))
                        KCard(Modifier.fillMaxWidth().padding(bottom = 18.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                            IconLabel(":thumbup: Vas 60% listo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink, modifier = Modifier.padding(bottom = 10.dp))
                            KProgress(0.6f)
                        }
                        BigButton("Ver qué me falta") { step = "req" }
                        ListenButton(if (speak.isSpeaking) ":stop: Detener explicación" else ":volumeup: Escuchar", say)
                    }

                    "req" -> {
                        Text("Te falta esto:", fontFamily = KFont.Title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(bottom = 16.dp))
                        KCard(Modifier.fillMaxWidth().padding(bottom = 14.dp), contentPadding = PaddingValues(18.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                KIcon(":historyedu:", size = 34.dp, tint = K.Green)
                                Text("Constancia de posesión", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = K.Ink)
                            }
                        }
                        BigButton("Ver cómo conseguirlo") { step = "route" }
                        ListenButton(if (speak.isSpeaking) ":stop: Detener explicación" else ":volumeup: Escuchar", say)
                    }

                    "route" -> {
                        Text("Sigue estos pasos:", fontFamily = KFont.Title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(bottom = 16.dp))
                        listOf(
                            Triple(":check:", 2, "Ve a tu municipalidad"),
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
                        ListenButton(if (speak.isSpeaking) ":stop: Detener explicación" else ":volumeup: Escuchar", say)
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
                            ) { KIcon(":thumbup:", size = 42.dp, tint = K.SelvaText) }
                            Text("¡Listo! Te avisamos", fontFamily = KFont.Title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                            Text(
                                "Cuando tengas todo, te avisamos por KuskaAlert para el siguiente paso.",
                                fontSize = 15.sp, color = K.Label, textAlign = TextAlign.Center, lineHeight = 22.sp,
                            )
                        }
                        SecondaryButton("Volver al inicio", { go(Route.HomeSimple) }, minHeight = 60.dp)
                        ListenButton(if (speak.isSpeaking) ":stop: Detener explicación" else ":volumeup: Escuchar", say)
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
