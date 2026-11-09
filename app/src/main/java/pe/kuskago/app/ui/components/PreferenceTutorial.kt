package pe.kuskago.app.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.theme.K

private data class PreferenceStep(val key: String, val title: String, val text: String)

/** Resalta controles reales sin modificar las preferencias durante la explicación. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreferenceTutorial(
    store: KuskaStore,
    autoShow: Boolean = false,
    hasPreview: Boolean = false,
    content: @Composable ((String) -> Modifier, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val dni = store.currentUser()?.dni.orEmpty()
    val settings = remember(context) { context.getSharedPreferences("preference_tutorial", Context.MODE_PRIVATE) }
    val steps = remember(hasPreview) {
        listOf(
            PreferenceStep("normal_mode", "Modo normal", "Muestra todas las herramientas de KuskaGo. Elige este modo si prefieres ver todas las opciones."),
            PreferenceStep("simple_mode", "Modo sencillo", "Ofrece botones grandes y menos opciones. Elige este modo si necesitas avanzar paso a paso."),
            PreferenceStep("BIG", "Texto grande", "Activa esta opción para ampliar las letras. Al activarla aparece un selector del 1 al 5 para elegir el tamaño."),
            PreferenceStep("LISTEN", "Prefiero escuchar", "Activa la ayuda por voz en los pasos que la ofrecen. Escuchar explicación inicia la lectura y Detener explicación la detiene."),
            PreferenceStep("CONTRAST", "Alto contraste", "Ofrece colores más visibles en las secciones compatibles para ayudarte a distinguir el contenido."),
            PreferenceStep("SIMPLE", "Lenguaje sencillo", "Usa palabras más fáciles de entender en las secciones que ofrecen esta adaptación."),
            PreferenceStep("TRANSLATE", "Traducir automáticamente", "Muestra explicaciones de términos difíciles donde está disponible esta opción. También tienes el Traductor en Inicio.")
        ) + (if (hasPreview) listOf(
            PreferenceStep("preview", "Así se verá KuskaGo", "Revisa aquí cómo cambian el texto, los colores y las explicaciones según las opciones que elijas.")
        ) else emptyList()) + listOf(
            PreferenceStep("finish", if (hasPreview) "Continuar con tus preferencias" else "Guardar cambios",
                if (hasPreview) "Después de esta guía, elige tus opciones y pulsa Continuar. Configurar después permite seguir en modo normal. Esta pantalla de bienvenida solo se presenta en el primer ingreso."
                else "Después de esta guía, ajusta las opciones que necesites y pulsa Guardar cambios para volver a tu perfil."),
            PreferenceStep("guides", "Puedes volver a consultar la ayuda", "En Mi perfil tienes la Guía completa de KuskaGo. Para cambiar tus opciones, entra en Accesibilidad; allí puedes repetir esta guía con este botón.")
        )
    }
    var visible by remember(dni, autoShow) {
        mutableStateOf(autoShow && dni.isNotBlank() && !settings.getBoolean("spotlight_done_$dni", false))
    }
    var index by remember(dni, hasPreview) {
        mutableIntStateOf(settings.getInt("spotlight_step_$dni", 0).coerceIn(0, steps.lastIndex))
    }
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var height by remember { mutableIntStateOf(0) }
    var panelHeight by remember { mutableIntStateOf(0) }
    val currentStep = steps[index]
    val replay: () -> Unit = {
        index = 0
        settings.edit().putInt("spotlight_step_$dni", 0).apply()
        visible = true
    }
    BackHandler(enabled = visible) { /* Se avanza con Anterior, Siguiente o Entendido. */ }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned {
        origin = it.positionInRoot()
        height = it.size.height
    }) {
        val maxPanelHeight = (maxHeight - 24.dp).coerceAtLeast(1.dp)
        Box(if (visible) Modifier.clearAndSetSemantics { } else Modifier) {
            content({ key -> Modifier.composed {
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(index, visible) {
                    if (visible && key == currentStep.key) {
                        withFrameNanos { }
                        requester.bringIntoView()
                    }
                }
                Modifier.bringIntoViewRequester(requester)
                    .onGloballyPositioned { bounds[key] = it.boundsInRoot() }
            } }, replay)
        }
        if (visible) {
            val target = bounds[currentStep.key]?.translate(-origin.x, -origin.y)
            val topSpace = target?.top ?: 0f
            val bottomSpace = target?.let { height - it.bottom } ?: height.toFloat()
            val safeMargin = with(LocalDensity.current) { 40.dp.toPx() }
            val alignment = if (panelHeight > maxOf(topSpace, bottomSpace) - safeMargin) Alignment.Center
                else if (topSpace > bottomSpace) Alignment.TopCenter else Alignment.BottomCenter
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {
                Canvas(Modifier.fillMaxSize()) {
                    val hole = target?.let { RoundRect(it.inflate(4.dp.toPx()), CornerRadius(16.dp.toPx())) }
                    val shade = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        if (hole != null) addRoundRect(hole)
                    }
                    drawPath(shade, Color.Black.copy(alpha = 0.68f))
                    if (hole != null) drawPath(Path().apply { addRoundRect(hole) }, Color(0xFFFFD54F), style = Stroke(3.dp.toPx()))
                }
                Surface(
                    modifier = Modifier.align(alignment).systemBarsPadding().padding(12.dp)
                        .widthIn(max = 520.dp).fillMaxWidth().heightIn(max = maxPanelHeight)
                        .onGloballyPositioned { panelHeight = it.size.height },
                    shape = RoundedCornerShape(20.dp), color = K.White,
                ) {
                    key(index) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Guía de preferencias · ${index + 1} de ${steps.size}", color = K.GreenDark,
                                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; heading() })
                            LinearProgressIndicator(progress = { (index + 1f) / steps.size }, modifier = Modifier.fillMaxWidth(), color = K.Green)
                            Text(currentStep.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                            Text(currentStep.text, fontSize = 15.sp, lineHeight = 20.sp, color = K.Ink)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (index > 0) TextButton(modifier = Modifier.weight(1f), onClick = {
                                    index--
                                    settings.edit().putInt("spotlight_step_$dni", index).apply()
                                }) { Text("Anterior", color = K.GreenDark) }
                                Button(modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = K.Green), onClick = {
                                    if (index == steps.lastIndex) {
                                        settings.edit().putBoolean("spotlight_done_$dni", true).remove("spotlight_step_$dni").apply()
                                        visible = false
                                    } else {
                                        index++
                                        settings.edit().putInt("spotlight_step_$dni", index).apply()
                                    }
                                }) { Text(if (index == steps.lastIndex) "Entendido" else "Siguiente") }
                            }
                        }
                    }
                }
            }
        }
    }
}
