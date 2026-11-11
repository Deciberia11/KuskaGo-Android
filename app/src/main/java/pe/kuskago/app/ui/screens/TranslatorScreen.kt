package pe.kuskago.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import pe.kuskago.app.data.translator.BureaucracyTranslator
import pe.kuskago.app.data.translator.BureaucracyTranslator.Term
import pe.kuskago.app.data.translator.AiClient
import pe.kuskago.app.ui.components.ChoiceCard
import pe.kuskago.app.ui.components.ErrorCard
import pe.kuskago.app.ui.components.GhostButton
import pe.kuskago.app.ui.components.KCard
import pe.kuskago.app.ui.components.KTextField
import pe.kuskago.app.ui.components.OptionChip
import pe.kuskago.app.ui.components.PillTabs
import pe.kuskago.app.ui.components.PrimaryButton
import pe.kuskago.app.ui.components.Screen
import pe.kuskago.app.ui.components.SecondaryButton
import pe.kuskago.app.ui.components.SectionLabel
import pe.kuskago.app.ui.components.TopBar
import pe.kuskago.app.ui.components.Speaker
import pe.kuskago.app.ui.components.rememberSpeaker
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.components.IconLabel
import pe.kuskago.app.ui.components.KIcon

private val AiBg = K.OkBg
private val AiBorder = K.OkBorder

// =============================================================
// 9b. TRADUCTOR DE BUROCRACIA (algoritmo sin internet + IA opcional)
// =============================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val speak = rememberSpeaker()
    val scope = rememberCoroutineScope()
    val examples = remember { BureaucracyTranslator.GLOSSARY.take(8) }

    var docText by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("local") }   // "local" | "ia"
    var result by remember { mutableStateOf<BureaucracyTranslator.Result?>(null) }
    var aiText by rememberSaveable { mutableStateOf<String?>(null) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var exampleIdx by rememberSaveable { mutableIntStateOf(0) }
    var ai by remember { mutableStateOf(AiClient.load(context)) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var testMsg by remember { mutableStateOf<AiClient.Outcome?>(null) }
    var testing by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    docText = input.bufferedReader().readText().take(6000)
                    result = null; aiText = null; aiError = null
                }
            } catch (e: Exception) {
                docText = ""
            }
        }
    }

    fun runLocal() {
        speak.stop()
        result = BureaucracyTranslator.translate(docText)
    }

    fun runAi() {
        speak.stop()
        aiError = null
        aiText = null
        aiLoading = true
        val requestedSettings = ai
        val requestedText = docText
        scope.launch {
            try {
                val r = AiClient.explain(requestedSettings, requestedText)
                if (ai == requestedSettings && docText == requestedText) when (r) {
                    is AiClient.Outcome.Ok -> aiText = r.text
                    is AiClient.Outcome.Error -> aiError = r.message
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                aiError = "No se pudo obtener la explicación. Comprueba la conexión e inténtalo de nuevo."
            } finally {
                aiLoading = false
            }
        }
    }

    Screen {
        TopBar(onBack = onBack, title = "Traductor de burocracia")
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("1. Elige cómo explicar tu documento", fontWeight = FontWeight.Bold, color = K.Ink)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TranslatorChoice("Diccionario", mode == "local", Modifier.weight(1f)) { speak.stop(); mode = "local" }
                TranslatorChoice("Con inteligencia artificial", mode == "ia", Modifier.weight(1f)) { speak.stop(); mode = "ia" }
            }
            Text(if (mode == "local") "Seleccionado: diccionario sin Internet ni IA."
                else "Seleccionado: IA con Ollama o Gemini. Configura el servicio abajo.",
                fontSize = 12.sp, color = K.Muted)
        }
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Text(
                    if (mode == "local") "Pega o sube el texto de tu documento. Lo explicamos con palabras simples, sin usar internet."
                    else "La IA lee tu documento y te explica qué dice, qué tienes que hacer y el plazo.",
                    fontSize = 13.5.sp, color = K.Muted, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 12.dp),
                )
                Text("2. Agrega el texto", fontWeight = FontWeight.Bold, color = K.Ink, modifier = Modifier.padding(bottom = 8.dp))
                SecondaryButton(":attachfile: Seleccionar archivo .txt", { picker.launch(arrayOf("text/plain")) }, Modifier.padding(bottom = 10.dp))
                KTextField(
                    value = docText,
                    onValueChange = { docText = it.take(6000); result = null; aiText = null; aiError = null },
                    placeholder = "…o pega aquí el texto de tu documento",
                    singleLine = false,
                    minLines = 5,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                SecondaryButton("Usar un texto de ejemplo", {
                    docText = BureaucracyTranslator.SAMPLE
                    result = null; aiText = null; aiError = null
                }, Modifier.padding(bottom = 12.dp), minHeight = 48.dp, fontSize = 14.sp)
                if (docText.isBlank()) Text("Agrega texto o usa el ejemplo para activar el botón de explicación.",
                    fontSize = 12.sp, color = K.Muted, modifier = Modifier.padding(bottom = 10.dp))

                if (mode == "local") {
                    PrimaryButton("3. Explicar con el diccionario", { runLocal() }, enabled = docText.isNotBlank())
                    Spacer(Modifier.height(18.dp))
                    result?.let { r -> LocalResult(r, speak) }
                } else {
                    AiSettingsCard(
                        settings = ai,
                        expanded = showSettings,
                        onToggle = { showSettings = !showSettings },
                        onChange = { ai = it; AiClient.save(context, it); testMsg = null; aiText = null; aiError = null },
                        testing = testing,
                        testMsg = testMsg,
                        onTest = {
                            testing = true; testMsg = null
                            val requestedSettings = ai
                            scope.launch {
                                try {
                                    val response = AiClient.testConnection(requestedSettings)
                                    if (ai == requestedSettings) testMsg = response
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    testMsg = AiClient.Outcome.Error("No se pudo comprobar la conexión. Inténtalo nuevamente.")
                                } finally { testing = false }
                            }
                        },
                    )
                    PrimaryButton(
                        if (aiLoading) "Explicando…" else ":autoawesome: Explicar con IA",
                        { if (docText.isNotBlank() && !aiLoading) runAi() },
                        containerColor = K.SelvaText,
                        enabled = docText.isNotBlank() && !aiLoading && !testing,
                    )
                    IconLabel(
                        if (ai.provider == AiClient.Provider.OLLAMA)
                            ":lock: Con Ollama el texto se procesa en tu propia computadora. Igual ocultamos DNI, celular y correo."
                        else
                            ":lock: Antes de enviar a Gemini ocultamos tu DNI, celular y correo. Aun así, no pegues datos que no quieras compartir.",
                        fontSize = 11.5.sp, color = K.Muted2, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                    if (aiLoading) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(color = K.SelvaText, modifier = Modifier.size(32.dp))
                        }
                    }
                    aiError?.let { ErrorCard("No se pudo explicar", it, Modifier.padding(bottom = 14.dp)) }
                    aiText?.let { t ->
                        KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), background = AiBg, borderColor = AiBorder) {
                            IconLabel(":autoawesome: EXPLICACIÓN CON IA", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = K.SelvaText, modifier = Modifier.padding(bottom = 8.dp))
                            Text(t, fontSize = 14.sp, color = K.Ink, lineHeight = 21.sp)
                        }
                        SecondaryButton(if (speak.isSpeaking) ":stop: Detener explicación" else ":volumeup: Escuchar explicación", { speak(t) })
                        Text(
                            "La IA puede equivocarse. Si tienes dudas, confirma en la entidad o usa \"Pedir ayuda\".",
                            fontSize = 11.5.sp, color = K.Muted2, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("Diccionario rápido", Modifier.padding(bottom = 8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp),
                ) {
                    examples.forEachIndexed { i, t -> OptionChip(t.label, i == exampleIdx) { exampleIdx = i } }
                }
                TermCard(examples[exampleIdx], K.MustardBg, K.MustardBorder, big = true)
                Text(
                    "El diccionario tiene ${BureaucracyTranslator.GLOSSARY.size} términos de trámites del Perú.",
                    fontSize = 11.5.sp, color = K.Muted2,
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun LocalResult(r: BureaucracyTranslator.Result, speak: Speaker) {
    if (r.isEmpty) {
        KCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), background = K.OrangeBg, borderColor = K.OrangeBorder) {
            Text("No reconocimos términos difíciles conocidos", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.MustardText)
            Text("Prueba la pestaña \"Con IA\" o pide ayuda a un asesor.", fontSize = 12.5.sp, color = K.Muted, modifier = Modifier.padding(top = 4.dp))
        }
        return
    }

    KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
        Text("EN PALABRAS SIMPLES", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, modifier = Modifier.padding(bottom = 6.dp))
        Text(r.simplifiedText, fontSize = 14.sp, color = K.Ink, lineHeight = 21.sp)
    }

    if (r.actions.isNotEmpty() || r.deadlines.isNotEmpty() || r.documents.isNotEmpty()) {
        KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            if (r.actions.isNotEmpty()) {
                IconLabel(":checkcircle: Lo que tienes que hacer", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.Ink, modifier = Modifier.padding(bottom = 4.dp))
                r.actions.forEach { Text("•  $it", fontSize = 13.sp, color = K.Ink, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 4.dp)) }
            }
            if (r.deadlines.isNotEmpty()) {
                IconLabel(":alarm: Plazo", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.Ink, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Text(r.deadlines.joinToString(" · "), fontSize = 13.sp, color = K.ErrorTitle, fontWeight = FontWeight.Bold)
            }
            if (r.documents.isNotEmpty()) {
                IconLabel(":description: Documentos que mencionan", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.Ink, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Text(r.documents.joinToString(" · "), fontSize = 13.sp, color = K.Ink)
            }
        }
    }

    SecondaryButton(if (speak.isSpeaking) ":stop: Detener explicación" else ":volumeup: Escuchar explicación", {
        val parts = mutableListOf(r.simplifiedText)
        if (r.deadlines.isNotEmpty()) parts += "Plazo: " + r.deadlines.joinToString(", ")
        speak(parts.joinToString(". "))
    })
    Spacer(Modifier.height(18.dp))

    if (r.terms.isNotEmpty()) {
        SectionLabel("Encontramos ${r.terms.size} término(s) difícil(es)", Modifier.padding(bottom = 10.dp))
        r.terms.forEach { TermCard(it, K.White, K.Border) }
    }
}

@Composable
private fun TermCard(t: Term, bg: Color, border: Color, big: Boolean = false) {
    KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), background = bg, borderColor = border) {
        Text("TÉRMINO OFICIAL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = K.Muted2)
        Text(
            "\"${t.official}\"", fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
            fontSize = if (big) 15.sp else 14.sp, color = K.Ink, modifier = Modifier.padding(bottom = 8.dp),
        )
        Text("EN PALABRAS SIMPLES", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = K.Muted2)
        Text(t.simple, fontSize = if (big) 14.sp else 13.5.sp, color = K.Ink, lineHeight = 20.sp)
    }
}

@Composable
private fun AiSettingsCard(
    settings: AiClient.Settings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (AiClient.Settings) -> Unit,
    testing: Boolean,
    testMsg: AiClient.Outcome?,
    onTest: () -> Unit,
) {
    val providerLabel = if (settings.provider == AiClient.Provider.OLLAMA) "Ollama · ${settings.ollamaModel}" else "Gemini · ${settings.geminiModel}"
    KCard(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                IconLabel(":settings: IA: $providerLabel", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.Ink)

            }

        }
        Spacer(Modifier.height(10.dp))
        SecondaryButton(if (expanded) "Ocultar configuración de IA" else ":settings: Configurar IA", onToggle, minHeight = 48.dp, fontSize = 14.sp)
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                TranslatorChoice("Ollama local", settings.provider == AiClient.Provider.OLLAMA, Modifier.weight(1f)) {
                    onChange(settings.copy(provider = AiClient.Provider.OLLAMA))
                }
                TranslatorChoice("Gemini", settings.provider == AiClient.Provider.GEMINI, Modifier.weight(1f)) {
                    onChange(settings.copy(provider = AiClient.Provider.GEMINI))
                }
            }
            Text(if (settings.provider == AiClient.Provider.OLLAMA) "Ollama usa el modelo instalado en tu computadora."
                else "Gemini necesita acceso a Internet y una clave válida de Google.",
                fontSize = 12.sp, color = K.Muted, modifier = Modifier.padding(bottom = 10.dp))
            if (settings.provider == AiClient.Provider.OLLAMA) {
                Text("Dirección de Ollama", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Label, modifier = Modifier.padding(bottom = 4.dp))
                KTextField(settings.ollamaUrl, { onChange(settings.copy(ollamaUrl = it)) }, "http://10.0.2.2:11434", Modifier.padding(bottom = 4.dp))
                Text(
                    "Emulador: http://10.0.2.2:11434 · Celular real: http://IP_DE_TU_PC:11434",
                    fontSize = 11.sp, color = K.Muted2, modifier = Modifier.padding(bottom = 10.dp),
                )
                Text("Modelo", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Label, modifier = Modifier.padding(bottom = 4.dp))
                KTextField(settings.ollamaModel, { onChange(settings.copy(ollamaModel = it)) }, "llama3.2:3b", Modifier.padding(bottom = 10.dp))
            } else {
                Text("Modelo de Gemini", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Label,
                    modifier = Modifier.padding(bottom = 4.dp))
                KTextField(settings.geminiModel, { onChange(settings.copy(geminiModel = it)) },
                    "gemini-3.8-flash", Modifier.padding(bottom = 10.dp))
                Text(
                    if (AiClient.geminiConfigured) "Gemini está configurado (clave en local.properties)."
                    else "Para usar Gemini agrega GEMINI_API_KEY en local.properties y vuelve a compilar.",
                    fontSize = 12.5.sp, color = if (AiClient.geminiConfigured) K.Green else K.Muted,
                )
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton(if (testing) "Probando…" else "Probar conexión", { if (!testing) onTest() }, minHeight = 48.dp, fontSize = 14.sp)
            when (testMsg) {
                is AiClient.Outcome.Ok -> Text(testMsg.text, fontSize = 12.5.sp, color = K.Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                is AiClient.Outcome.Error -> Text(testMsg.message, fontSize = 12.5.sp, color = K.ErrorTitle, modifier = Modifier.padding(top = 8.dp))
                null -> {}
            }
        }
    }
}

@Composable
private fun GhostButtonSmall(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text, color = K.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun TranslatorChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(if (selected) "✓ $label" else label, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
        modifier = modifier.heightIn(min = 52.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) K.Green else K.Muted),
        colors = FilterChipDefaults.filterChipColors(containerColor = K.White,
            labelColor = K.Ink, selectedContainerColor = K.Green, selectedLabelColor = K.White),
    )
}
