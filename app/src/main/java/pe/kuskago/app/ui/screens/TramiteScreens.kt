package pe.kuskago.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.Route
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.data.StoredDoc
import pe.kuskago.app.ui.components.ChoiceCard
import pe.kuskago.app.ui.components.Chip
import pe.kuskago.app.ui.components.FieldLabel
import pe.kuskago.app.ui.components.GhostButton
import pe.kuskago.app.ui.components.KCard
import pe.kuskago.app.ui.components.KProgress
import pe.kuskago.app.ui.components.KTextField
import pe.kuskago.app.ui.components.ListItemRow
import pe.kuskago.app.ui.components.OptionChip
import pe.kuskago.app.ui.components.PillTabs
import pe.kuskago.app.ui.components.PrimaryButton
import pe.kuskago.app.ui.components.Screen
import pe.kuskago.app.ui.components.SecondaryButton
import pe.kuskago.app.ui.components.SectionLabel
import pe.kuskago.app.ui.components.SuccessCheck
import pe.kuskago.app.ui.components.TopBar
import pe.kuskago.app.ui.components.buildDocHtml
import pe.kuskago.app.ui.components.openUrl
import pe.kuskago.app.ui.components.printHtml
import pe.kuskago.app.ui.components.rememberSpeaker
import pe.kuskago.app.ui.components.ScanDniCard
import pe.kuskago.app.ui.components.UseMyLocationButton
import pe.kuskago.app.ui.components.shareText
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.theme.KFont
import pe.kuskago.app.ui.components.IconLabel
import pe.kuskago.app.ui.components.KIcon
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WarnBg = K.WarnBg
private val WarnText = K.WarnText
private val WarnIcon = K.WarnText

private fun todayLabel(): String =
    SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE")).format(Date())

private fun shortDate(): String =
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE")).format(Date())

@Composable
private fun ScrollBody(content: @Composable () -> Unit) {
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
private fun Body(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 13.5.sp, color = K.Muted, lineHeight = 19.sp, modifier = modifier.padding(bottom = 14.dp))
}

// =============================================================
// 9. DETALLE DE TRÁMITE (Registro de terreno)
// =============================================================
@Composable
fun TramiteDetailScreen(
    store: KuskaStore,
    initialTab: String,
    onBack: () -> Unit,
    go: (Route) -> Unit,
) {
    val context = LocalContext.current
    var tab by remember(initialTab) { mutableStateOf(initialTab) }
    val docs = remember { store.getDocs() }

    Screen {
        TopBar(onBack = onBack, title = "Registro de terreno")
        PillTabs(
            tabs = listOf(
                "resumen" to "Resumen", "requisitos" to "Requisitos", "documentos" to "Documentos",
                "plantillas" to "Plantillas", "ruta" to "Ruta", "entidad" to "Entidad",
            ),
            selected = tab,
            onSelect = { tab = it },
        )
        Box(Modifier.weight(1f)) {
            ScrollBody {
                when (tab) {
                    "resumen" -> {
                        KCard(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Text("Simulamos tu trámite", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = K.Ink)
                            Text(
                                "Veamos qué tan preparado estás para iniciar tu solicitud",
                                fontSize = 12.5.sp, color = K.Muted, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                KProgress(0.6f, Modifier.weight(1f))
                                Text("60%", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = K.Ink, modifier = Modifier.padding(start = 10.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CheckLine(true, "Tienes tu DNI")
                                CheckLine(true, "Tienes el plano o croquis")
                                CheckLine(false, "Te falta la constancia de posesión")
                                CheckLine(false, "Te falta el pago de tasa")
                            }
                        }
                        KCard(
                            Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            background = K.SkyBg, borderColor = K.SkyBorder,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                KIcon(":directionsbus:", size = 24.dp, tint = K.SkyText)
                                Text(
                                    "No viajes todavía. Aún te faltan 2 requisitos. Puedes completarlos sin desplazarte.",
                                    fontSize = 12.5.sp, color = K.SkyText, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        PrimaryButton("Ver pasos detallados", { tab = "requisitos" })
                        Spacer(Modifier.height(12.dp))
                        SecondaryButton("Llenar solicitud paso a paso", { go(Route.TramiteWizard) })
                    }

                    "requisitos" -> {
                        ListItemRow(":check:", "DNI vigente", "Completo", K.Green, K.OkBg, K.Green) { go(Route.RequisitoDetail) }
                        ListItemRow(":check:", "Plano o croquis", "Completo", K.Green, K.OkBg, K.Green) { go(Route.RequisitoDetail) }
                        ListItemRow("!", "Constancia de posesión", "Pendiente", WarnText, WarnBg, WarnText) { go(Route.RequisitoDetail) }
                        ListItemRow("!", "Pago de tasa", "Pendiente", WarnText, WarnBg, WarnText) { go(Route.RequisitoDetail) }
                    }

                    "documentos" -> {
                        Body("Documentos que ya subiste para este trámite.")
                        docs.take(3).forEach { d ->
                            ListItemRow(d.icon, d.name, d.meta) { go(Route.Documents) }
                        }
                        Spacer(Modifier.height(14.dp))
                        SecondaryButton("Agregar documento", { go(Route.Documents) }, minHeight = 42.dp, fontSize = 14.sp)
                    }

                    "plantillas" -> {
                        Body("Plantillas listas para este trámite.")
                        PrimaryButton("Ver plantillas", { go(Route.Templates) })
                    }

                    "ruta" -> {
                        Body("Sigue tu ruta ciudadana paso a paso.")
                        PrimaryButton("Ver Ruta Ciudadana", { go(Route.CitizenRoute) })
                    }

                    else -> {
                        KCard(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                            Text("COFOPRI", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = K.Ink)
                            Text(
                                "Organismo de Formalización de la Propiedad Informal",
                                fontSize = 13.sp, color = K.Muted, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                            )
                            IconLabel(":locationon: Jr. Los Álamos 123, tu distrito", fontSize = 13.sp, color = K.Label)
                            IconLabel(":phone: (01) 615-6969", fontSize = 13.sp, color = K.Label)
                        }
                        SecondaryButton("Ir al sitio oficial", { context.openUrl("https://www.gob.pe/cofopri") })
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckLine(ok: Boolean, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        KIcon(if (ok) ":check:" else ":warningamber:", size = 20.dp, tint = if (ok) K.SelvaText else WarnIcon)
        Text(text, fontSize = 13.sp, color = K.Ink)
    }
}

// =============================================================
// 10. DETALLE DE REQUISITO
// =============================================================
@Composable
fun RequisitoDetailScreen(
    onBack: () -> Unit,
    go: (Route) -> Unit,
    onEntity: () -> Unit,
) {
    Screen {
        TopBar(onBack = onBack, title = "Constancia de posesión")
        Box(Modifier.weight(1f)) {
            ScrollBody {
                Box(Modifier.padding(bottom = 16.dp)) { Chip("Pendiente", WarnBg, WarnText) }
                Section("¿Qué es?", "Documento que confirma que usas o vives en ese terreno, firmado por la autoridad de tu comunidad o municipalidad.")
                Section("¿Dónde lo consigo?", "En la municipalidad de tu distrito o con el presidente de tu comunidad campesina.")
                Section("¿Cómo obtenerlo?", "Solicita la constancia con tu DNI. La entidad la firma y sella en el momento o en unos días.")
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Subir documento", { go(Route.Documents) })
                    SecondaryButton("Usar documento guardado", { go(Route.Documents) })
                    SecondaryButton(":recordvoiceover: Traducir este documento", { go(Route.Translator) })
                    SecondaryButton("Usar plantilla de solicitud", { go(Route.Templates) })
                    SecondaryButton("Ver entidad", onEntity)
                    GhostButton("Volver", onBack)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, color = K.Ink, modifier = Modifier.padding(bottom = 6.dp))
    Text(body, fontSize = 13.5.sp, color = K.Label, lineHeight = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
}

// =============================================================
// 11. PLANTILLAS
// =============================================================
private data class TemplateDef(val title: String, val intro: (String, String, String) -> String)

private val TEMPLATES = listOf(
    TemplateDef("Solicitud de constancia de posesión") { n, d, t ->
        "Yo, $n, identificado con DNI, solicito se me otorgue constancia de posesión del terreno ubicado en $d, del cual soy poseedor desde hace $t."
    },
    TemplateDef("Declaración jurada de posesión") { n, d, t ->
        "Yo, $n, identificado con DNI, declaro bajo juramento que ejerzo posesión pacífica, continua y pública del terreno ubicado en $d desde hace $t."
    },
)

@Composable
fun TemplatesScreen(
    store: KuskaStore,
    onExit: () -> Unit,
    go: (Route) -> Unit,
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf("list") }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val known = remember { store.getPersonalData() }
    var name by rememberSaveable { mutableStateOf(known["nombre"] ?: store.getProfile().name) }
    var address by rememberSaveable { mutableStateOf(known["predio_direccion"] ?: known["predio_ubicacion"] ?: "") }
    var time by rememberSaveable { mutableStateOf(known["posesion_tiempo"] ?: "") }
    var showErrors by remember { mutableStateOf(false) }

    val order = listOf("list", "fill", "preview", "saved")
    fun back() {
        val i = order.indexOf(step)
        if (i in 1..2) step = order[i - 1] else onExit()
    }
    BackHandler(enabled = step == "fill" || step == "preview") { back() }

    val tpl = TEMPLATES[selected]
    val bodyText = tpl.intro(name.trim(), address.trim(), time.trim())
    fun html() = buildDocHtml(tpl.title, todayLabel(), bodyText, emptyList())
    fun saveDoc() {
        store.addDoc(StoredDoc(tpl.title + ".pdf", "Plantilla · ${shortDate()}", ":editnote:", content = bodyText))
        store.savePersonalData(mapOf("nombre" to name, "predio_direccion" to address, "posesion_tiempo" to time))
    }

    Screen {
        TopBar(onBack = ::back, title = "Plantillas")
        Box(Modifier.weight(1f)) {
            ScrollBody {
                when (step) {
                    "list" -> {
                        Body("Elige la plantilla que necesitas para tu trámite.")
                        TEMPLATES.forEachIndexed { i, t ->
                            ListItemRow(":editnote:", t.title, "Editable · PDF") { selected = i; step = "fill" }
                        }
                    }

                    "fill" -> {
                        Text(tpl.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = K.Ink, modifier = Modifier.padding(bottom = 14.dp))
                        if (showErrors) {
                            Text("Completa los tres campos para continuar.", color = K.ErrorTitle, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                        }
                        FieldLabel("Nombre completo")
                        KTextField(name, { name = it }, "Juan Quispe Mamani", Modifier.padding(bottom = 14.dp))
                        FieldLabel("Dirección del terreno")
                        KTextField(address, { address = it }, "Sector, distrito, provincia", Modifier.padding(bottom = 14.dp))
                        FieldLabel("Tiempo de posesión")
                        KTextField(time, { time = it }, "Ej. 8 años", Modifier.padding(bottom = 22.dp))
                        PrimaryButton("Vista previa", {
                            if (name.isBlank() || address.isBlank() || time.isBlank()) showErrors = true
                            else { showErrors = false; step = "preview" }
                        })
                    }

                    "preview" -> {
                        KCard(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Text(
                                tpl.title.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.Ink,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            )
                            Text(bodyText, fontSize = 12.5.sp, lineHeight = 21.sp, color = K.Ink)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                            SecondaryButton("Editar", { step = "fill" }, Modifier.weight(1f), minHeight = 42.dp, fontSize = 14.sp)
                            PrimaryButton("Guardar", { saveDoc(); step = "saved" }, Modifier.weight(1f), fontSize = 14.sp, minHeight = 42.dp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Descargar", { context.printHtml(tpl.title, html()) }, Modifier.weight(1f), minHeight = 42.dp, fontSize = 13.sp)
                            SecondaryButton("Imprimir", { context.printHtml(tpl.title, html()) }, Modifier.weight(1f), minHeight = 42.dp, fontSize = 13.sp)
                            SecondaryButton("Compartir", { context.shareText(tpl.title, tpl.title.uppercase() + "\n\n" + bodyText) }, Modifier.weight(1f), minHeight = 42.dp, fontSize = 13.sp)
                        }
                        Text(
                            "\"Descargar\" abre el diálogo de impresión: elige \"Guardar como PDF\".",
                            fontSize = 11.5.sp, color = K.Muted2, modifier = Modifier.padding(top = 10.dp),
                        )
                    }

                    else -> {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SuccessCheck(76.dp)
                            Text("Documento guardado", fontFamily = KFont.Title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                            Text("Lo encontrarás en Mis documentos.", fontSize = 13.5.sp, color = K.Muted)
                        }
                        PrimaryButton("Volver al trámite", { go(Route.TramiteDetail) })
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("Ir a Mis documentos", { go(Route.Documents) })
                    }
                }
            }
        }
    }
}

// =============================================================
// 9c. FORMULARIO DE TRÁMITE DINÁMICO
// =============================================================
private data class Q(
    val id: String,
    val label: String,
    val text: String,
    val placeholder: String = "",
    val options: List<String>? = null,
    val numeric: Boolean = false,
    /** Clave del dato del usuario que responde esta pregunta (null = depende de cada trámite). */
    val key: String? = null,
)

/** Preguntas que se pueden completar con la ubicación del teléfono. */
private val LOCATION_KEYS = setOf(
    "domicilio", "distrito", "predio_ubicacion", "predio_direccion", "predio_distrito", "negocio_direccion",
)

private data class WizardDef(
    val title: String,
    val entity: String,
    val docTitle: String,
    val docIntro: String,
    val questions: List<Q>,
)

private val WIZARDS: Map<String, WizardDef> = mapOf(
    "terreno" to WizardDef(
        "Registro de terreno", "COFOPRI", "Declaración Jurada de Posesión de Terreno",
        "El (la) solicitante declara bajo juramento poseer el terreno descrito a continuación, para efectos de su registro ante COFOPRI.",
        listOf(
            Q("nombre", "Nombre completo", "¿Cuál es tu nombre completo?", "Ej. Juan Quispe Mamani", key = "nombre"),
            Q("dni", "DNI", "¿Cuál es tu número de DNI?", "8 dígitos", numeric = true, key = "dni"),
            Q("ubicacion", "Ubicación del terreno", "¿Dónde está ubicado el terreno?", "Distrito, provincia, departamento", key = "predio_ubicacion"),
            Q("area", "Área aproximada", "¿Cuál es el área aproximada del terreno?", "Ej. 500 m²", key = "predio_area"),
            Q("tiempo", "Tiempo de posesión", "¿Hace cuántos años posees el terreno?", "Ej. 8 años", key = "posesion_tiempo"),
            Q("construccion", "¿Tiene construcción?", "¿El terreno tiene alguna construcción?", options = listOf("Sí", "No")),
        ),
    ),
    "constancia" to WizardDef(
        "Constancia de posesión", "Municipalidad", "Solicitud de Constancia de Posesión",
        "El (la) solicitante pide a la municipalidad la emisión de una constancia de posesión sobre el predio descrito a continuación.",
        listOf(
            Q("nombre", "Nombre completo", "¿Cuál es tu nombre completo?", "Ej. Juan Quispe Mamani", key = "nombre"),
            Q("dni", "DNI", "¿Cuál es tu número de DNI?", "8 dígitos", numeric = true, key = "dni"),
            Q("direccion", "Dirección del predio", "¿Cuál es la dirección del predio?", "Jr./Av., número, referencia", key = "predio_direccion"),
            Q("distrito", "Distrito", "¿En qué distrito se encuentra?", "Ej. tu distrito", key = "predio_distrito"),
            Q("tiempo", "Tiempo de posesión", "¿Hace cuánto tiempo vives u ocupas el predio?", "Ej. 5 años", key = "posesion_tiempo"),
        ),
    ),
    "dni" to WizardDef(
        "DNI — Duplicado", "RENIEC", "Solicitud de Duplicado de DNI",
        "El (la) solicitante pide a RENIEC la emisión de un duplicado de su Documento Nacional de Identidad por el motivo señalado.",
        listOf(
            Q("nombre", "Nombre completo", "¿Cuál es tu nombre completo?", "Ej. Juan Quispe Mamani", key = "nombre"),
            Q("dniAnterior", "DNI anterior", "¿Recuerdas tu número de DNI anterior?", "8 dígitos (si lo recuerdas)", numeric = true, key = "dni"),
            Q("motivo", "Motivo del duplicado", "¿Cuál es el motivo del duplicado?", options = listOf("Pérdida", "Robo", "Deterioro")),
            Q("direccion", "Dirección actual", "¿Cuál es tu dirección actual?", "Jr./Av., número, distrito", key = "domicilio"),
        ),
    ),
    "pension65" to WizardDef(
        "Pensión 65", "MIDIS", "Ficha de Inscripción — Pensión 65",
        "El (la) solicitante pide su inscripción al programa Pensión 65 con los datos declarados a continuación.",
        listOf(
            Q("nombre", "Nombre completo", "¿Cuál es tu nombre completo?", "Ej. Juan Quispe Mamani", key = "nombre"),
            Q("dni", "DNI", "¿Cuál es tu número de DNI?", "8 dígitos", numeric = true, key = "dni"),
            Q("edad", "Edad", "¿Cuál es tu edad?", "Ej. 68", numeric = true, key = "edad"),
            Q("distrito", "Distrito de residencia", "¿En qué distrito resides?", "Ej. tu distrito", key = "distrito"),
            Q("otraPension", "¿Recibe otra pensión?", "¿Recibes actualmente otra pensión?", options = listOf("Sí", "No")),
        ),
    ),
    "herencia" to WizardDef(
        "Herencia / Sucesión", "SUNARP · Notaría", "Solicitud de Sucesión Intestada",
        "El (la) solicitante, en calidad de heredero(a), pide iniciar el trámite de sucesión intestada con los datos declarados a continuación.",
        listOf(
            Q("fallecido", "Persona fallecida", "¿Cuál es el nombre completo de la persona fallecida?", "Nombre completo"),
            Q("fecha", "Fecha de fallecimiento", "¿Cuál fue la fecha de fallecimiento?", "DD/MM/AAAA"),
            Q("nombre", "Tu nombre completo", "¿Cuál es tu nombre completo?", "Ej. Juan Quispe Mamani", key = "nombre"),
            Q("dni", "Tu DNI", "¿Cuál es tu número de DNI?", "8 dígitos", numeric = true, key = "dni"),
            Q("relacion", "Relación con el fallecido", "¿Cuál es tu relación con la persona fallecida?", options = listOf("Hijo/a", "Cónyuge", "Padre/Madre", "Hermano/a", "Otro")),
            Q("herederos", "N° de herederos", "¿Cuántos herederos hay en total?", "Ej. 3", numeric = true),
        ),
    ),
    "licencia" to WizardDef(
        "Licencia de funcionamiento", "Municipalidad", "Solicitud de Licencia de Funcionamiento",
        "El (la) solicitante pide a la municipalidad el otorgamiento de una licencia de funcionamiento para el negocio descrito a continuación.",
        listOf(
            Q("negocio", "Nombre del negocio", "¿Cuál es el nombre de tu negocio?", "Nombre comercial", key = "negocio_nombre"),
            Q("nombre", "Nombre del propietario", "¿Cuál es tu nombre completo como propietario(a)?", "Ej. Juan Quispe Mamani", key = "nombre"),
            Q("dni", "DNI del propietario", "¿Cuál es tu número de DNI?", "8 dígitos", numeric = true, key = "dni"),
            Q("direccion", "Dirección del local", "¿Cuál es la dirección del local?", "Jr./Av., número, distrito", key = "negocio_direccion"),
            Q("giro", "Giro del negocio", "¿Cuál es el giro o actividad del negocio?", "Ej. bodega, restaurante, taller", key = "negocio_giro"),
        ),
    ),
)

@Composable
fun TramiteWizardScreen(
    type: String,
    store: KuskaStore,
    onExit: () -> Unit,
    go: (Route) -> Unit,
) {
    val context = LocalContext.current
    val def = WIZARDS[type] ?: WIZARDS.getValue("terreno")
    val total = def.questions.size

    // 1) Lo que ya sabemos del usuario (cuenta + trámites anteriores) rellena el formulario.
    val known = remember(type) { store.getPersonalData() }
    val answers = remember(type) {
        mutableStateListOf(*Array(total) { i -> def.questions[i].key?.let { known[it] }.orEmpty() })
    }
    val knownIdx = remember(type) { (0 until total).filter { answers[it].isNotBlank() } }
    val missingIdx = remember(type) { (0 until total).filter { it !in knownIdx } }

    // 2) Fases: "review" (confirmar lo conocido) → "ask" (solo lo que falta) → "done"
    var phase by rememberSaveable(type) { mutableStateOf(if (knownIdx.isEmpty()) "ask" else "review") }
    var askAll by rememberSaveable(type) { mutableStateOf(false) }   // true = el usuario quiere revisar todo
    var step by rememberSaveable(type) { mutableIntStateOf(0) }
    var saved by rememberSaveable(type) { mutableStateOf(false) }
    var newlySaved by rememberSaveable(type) { mutableIntStateOf(0) }

    val askList = if (askAll) (0 until total).toList() else missingIdx
    val isDone = phase == "done"

    fun back() {
        when {
            phase == "ask" && step > 0 -> step--
            phase == "ask" && knownIdx.isNotEmpty() -> { phase = "review"; step = 0 }
            else -> onExit()
        }
    }
    BackHandler(enabled = phase == "ask" && (step > 0 || knownIdx.isNotEmpty())) { back() }

    val lines = def.questions.mapIndexed { i, q -> q.label to answers[i].ifBlank { "—" } }
    val today = todayLabel()

    // 3) Al terminar: guardamos el documento y recordamos los datos para la próxima vez.
    LaunchedEffect(isDone) {
        if (isDone && !saved) {
            val text = def.docIntro + "\n\n" + lines.joinToString("\n") { "${it.first}: ${it.second}" }
            store.addDoc(StoredDoc(def.docTitle + ".pdf", "${def.entity} · ${shortDate()}", ":receiptlong:", content = text))
            store.addTramite(type, def.title, def.entity, def.docTitle + ".pdf")
            val toRemember = def.questions.mapIndexedNotNull { i, q ->
                q.key?.takeIf { it != "dni" }?.let { k -> k to answers[i] }
            }.toMap()
            newlySaved = toRemember.count { (k, v) -> v.isNotBlank() && known[k] != v.trim() }
            store.savePersonalData(toRemember)
            saved = true
        }
    }

    Screen {
        TopBar(
            onBack = ::back,
            title = if (isDone) "Documento listo" else def.title,
        )
        Box(Modifier.weight(1f)) {
            ScrollBody {
                when (phase) {
                    "review" -> {
                        Box(Modifier.padding(bottom = 14.dp)) { Chip(def.entity, K.SkyBg, K.SkyText) }
                        Text(
                            "Ya tenemos estos datos tuyos",
                            fontFamily = KFont.Title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(bottom = 6.dp),
                        )
                        Text(
                            if (missingIdx.isEmpty()) "No necesitas escribir nada más. Revisa que estén bien."
                            else "Solo te preguntaremos ${missingIdx.size} ${if (missingIdx.size == 1) "dato que falta" else "datos que faltan"}.",
                            fontSize = 13.5.sp, color = K.Muted, modifier = Modifier.padding(bottom = 14.dp),
                        )
                        KCard(Modifier.fillMaxWidth().padding(bottom = 18.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                            knownIdx.forEach { i ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    KIcon(":check:", size = 20.dp, tint = K.SelvaText)
                                    Text(def.questions[i].label, fontSize = 13.sp, color = K.Muted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(answers[i], fontSize = 13.sp, color = K.Ink, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        PrimaryButton(
                            if (missingIdx.isEmpty()) ":check: Sí, generar documento" else ":check: Sí, usar mis datos",
                            {
                                askAll = false; step = 0
                                phase = if (missingIdx.isEmpty()) "done" else "ask"
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("Cambiar algún dato", { askAll = true; step = 0; phase = "ask" })
                    }

                    "ask" -> {
                        val qi = askList[step.coerceIn(0, askList.lastIndex)]
                        val q = def.questions[qi]
                        val value = answers[qi]
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                            Chip(def.entity, K.SkyBg, K.SkyText)
                            if (!askAll && knownIdx.isNotEmpty()) Chip(":check: ${knownIdx.size} datos ya completados", K.OkBg, K.GreenDark)
                        }
                        KProgress(step.toFloat() / askList.size, Modifier.padding(bottom = 8.dp))
                        Text(
                            "Pregunta ${step + 1} de ${askList.size}", fontSize = 12.sp, color = K.Muted2, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 22.dp),
                        )
                        Text(q.text, fontFamily = KFont.Title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, lineHeight = 25.sp, modifier = Modifier.padding(bottom = 18.dp))
                        if (q.options != null) {
                            OptionsFlow(q.options, value) { answers[qi] = it }
                        } else {
                            KTextField(
                                value = value,
                                onValueChange = { answers[qi] = if (q.numeric) it.filter { c -> c.isDigit() }.take(8) else it },
                                placeholder = q.placeholder,
                                keyboardType = if (q.numeric) KeyboardType.Number else KeyboardType.Text,
                                enabled = q.key != "dni" || value.isBlank(),   // el DNI de la cuenta es de solo lectura
                                modifier = Modifier.padding(bottom = 22.dp),
                            )
                        }
                        val lk = q.key
                        if (lk != null && lk in LOCATION_KEYS) {
                            UseMyLocationButton(
                                label = if (lk.endsWith("distrito")) ":locationon: Usar el distrito donde estoy" else ":locationon: Usar mi ubicación actual",
                                onResult = { g ->
                                    answers[qi] = when {
                                        lk.endsWith("distrito") -> g.district ?: g.full
                                        lk == "predio_ubicacion" -> g.full
                                        else -> listOfNotNull(g.address, g.district).joinToString(", ").ifBlank { g.full }
                                    }
                                },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                        if (q.key == "domicilio") {
                            ScanDniCard(
                                onResult = { d -> listOfNotNull(d.address, d.district).joinToString(", ").takeIf { it.isNotBlank() }?.let { answers[qi] = it } },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                        if (q.key != null) {
                            IconLabel(
                                ":save: Lo recordaremos para tus próximos trámites.",
                                fontSize = 11.5.sp, color = K.Muted2, modifier = Modifier.padding(top = 0.dp, bottom = 12.dp),
                            )
                        }
                        PrimaryButton(
                            if (step >= askList.size - 1) "Generar documento" else "Siguiente",
                            {
                                if (value.isNotBlank()) {
                                    if (step >= askList.size - 1) phase = "done" else step++
                                }
                            },
                            containerColor = if (value.isNotBlank()) K.Green else K.Green.copy(alpha = 0.45f),
                        )
                    }

                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                            Chip(":check: Documento generado", K.OkBg, K.GreenDark)
                        }
                        if (newlySaved > 0) {
                            KCard(Modifier.fillMaxWidth().padding(bottom = 14.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                                IconLabel(
                                    ":save: Guardamos $newlySaved ${if (newlySaved == 1) "dato" else "datos"} para que no los vuelvas a escribir. Puedes verlos o borrarlos en Perfil → Editar datos.",
                                    fontSize = 12.5.sp, color = K.GreenDark, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        KCard(
                            Modifier.fillMaxWidth().padding(bottom = 18.dp),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 26.dp),
                        ) {
                            Text(
                                def.docTitle.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "${def.entity} · $today", fontSize = 12.sp, color = K.Muted, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp),
                            )
                            Text(def.docIntro, fontSize = 13.sp, lineHeight = 22.sp, color = K.Ink, modifier = Modifier.padding(bottom = 18.dp))
                            lines.forEach { (label, v) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(label, fontSize = 12.5.sp, color = K.Muted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(v, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                }
                                HorizontalDivider(color = K.Border)
                            }
                            Column(Modifier.fillMaxWidth().padding(top = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                HorizontalDivider(color = K.Ink, thickness = 1.5.dp, modifier = Modifier.width(180.dp))
                                Text("Firma del solicitante", fontSize = 11.5.sp, color = K.Muted, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                        PrimaryButton(":print: Imprimir / Guardar como PDF", {
                            context.printHtml(def.docTitle, buildDocHtml(def.docTitle, "${def.entity} · $today", def.docIntro, lines))
                        })
                        Spacer(Modifier.height(12.dp))
                        SecondaryButton("Ver en Mis trámites", { go(Route.MyTramites) })
                        Text(
                            "También lo guardamos en Mis documentos y en Mis trámites, para que sigas su avance.", fontSize = 12.sp, color = K.Muted2,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsFlow(options: List<String>, selected: String, onPick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 22.dp),
    ) {
        options.forEach { o -> OptionChip(o, selected == o) { onPick(o) } }
    }
}
