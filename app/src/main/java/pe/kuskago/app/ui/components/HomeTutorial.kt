package pe.kuskago.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.composed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.theme.K

private data class TourStep(val key: String, val title: String, val text: String)

/** No tiene temporizador: solo Entendido completa el recorrido de este modo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTutorial(store: KuskaStore, simple: Boolean, active: Boolean = true, content: @Composable ((String) -> Modifier) -> Unit) {
    val dni = store.currentUser()?.dni.orEmpty()
    val steps = remember(simple) {
        if (simple) listOf(
            TourStep("continuar", "Continuar mi trámite", "Recibe ayuda paso a paso para continuar tu trámite."),
            TourStep("avisos", "Ver avisos", "Consulta los avisos y recordatorios de tus trámites."),
            TourStep("ayuda", "Pedir ayuda", "Encuentra orientación si no sabes cómo continuar."),
            TourStep("familia", "Trámites de mi familia", "Consulta y organiza los datos de tus familiares."),
            TourStep("perfil", "Tu guía siempre disponible", "Abre Mi perfil y elige Guía completa de KuskaGo. También puedes pedir ayuda a una persona."),
            TourStep("perfil", "Más guías cuando las necesites", "En Mi perfil, Guía completa explica cada apartado. En Accesibilidad puedes repetir la guía de preferencias y cambiar tus opciones.")
        ) else listOf(
            TourStep("buscar", "Buscar trámite", "Busca un trámite, revisa sus requisitos y empieza a prepararlo."),
            TourStep("tramites", "Mis trámites", "Revisa tus trámites guardados, su avance y el siguiente paso."),
            TourStep("documentos", "Mis documentos", "Abre tus documentos guardados o agrega archivos del teléfono."),
            TourStep("familia", "Mi familia", "Organiza los datos de tus familiares para sus trámites."),
            TourStep("ruta", "Ruta Ciudadana", "Revisa los pasos de tu trámite y ubica las oficinas en el mapa."),
            TourStep("alertas", "KuskaAlert", "Abre tus avisos para conocer los recordatorios y qué hacer después."),
            TourStep("ayuda", "Pedir ayuda", "Encuentra orientación si no sabes cómo continuar."),
            TourStep("traductor", "Traductor de burocracia", "Pega o sube un texto para entenderlo en palabras sencillas y escucharlo."),
            TourStep("perfil", "Tu guía en Mi perfil", "En Perfil encontrarás la Guía completa de KuskaGo, tus ajustes y soporte. Puedes consultarla cuando quieras."),
            TourStep("perfil", "Más guías cuando las necesites", "En Mi perfil, Guía completa explica cada apartado. En Accesibilidad puedes repetir la guía de preferencias y cambiar tus opciones.")
        )
    }
    // El repositorio ya conserva cada paso. Leerlo al entrar evita restaurar el
    // estado compartido y completado que ocultaba el tutorial de la versión anterior.
    var finished by remember(dni, simple) { mutableStateOf(dni.isBlank() || store.tutorialCompleted(dni, simple)) }
    var index by remember(dni, simple) { mutableStateOf(store.tutorialStep(dni, simple).coerceIn(0, steps.lastIndex)) }
    val showTutorial = active && !finished
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var height by remember { mutableStateOf(0) }
    var panelHeight by remember { mutableStateOf(0) }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    val step = steps[index]
    BackHandler(enabled = showTutorial) { /* Evita cerrar la guía por accidente. */ }
    Box(Modifier.fillMaxSize().onGloballyPositioned {
        height = it.size.height
        overlayOrigin = it.positionInRoot()
    }) {
        Box(if (!showTutorial) Modifier else Modifier.clearAndSetSemantics { }) {
            content { key -> Modifier.composed {
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(index, showTutorial) {
                    if (showTutorial && key == step.key) {
                        withFrameNanos { }
                        requester.bringIntoView()
                    }
                }
                Modifier.bringIntoViewRequester(requester).onGloballyPositioned { bounds[key] = it.boundsInRoot() }
            } }
        }
        if (showTutorial) {
            val target = bounds[step.key]?.translate(-overlayOrigin.x, -overlayOrigin.y)
            val showAbove = target != null && target.top > height - target.bottom
            val safeMargin = with(LocalDensity.current) { 64.dp.toPx() }
            val availableSpace = if (target == null) height.toFloat() else maxOf(target.top, height - target.bottom)
            val panelAlignment = when {
                panelHeight > availableSpace - safeMargin -> Alignment.Center
                showAbove -> Alignment.TopCenter
                else -> Alignment.BottomCenter
            }
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
                    modifier = Modifier.align(panelAlignment)
                        .systemBarsPadding().padding(12.dp).widthIn(max = 520.dp).fillMaxWidth()
                        .onGloballyPositioned { panelHeight = it.size.height },
                    shape = RoundedCornerShape(20.dp), color = K.White,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${if (simple) "Modo sencillo" else "Modo normal"} · ${index + 1} de ${steps.size}", color = K.GreenDark, fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; heading() })
                        LinearProgressIndicator(progress = { (index + 1f) / steps.size }, modifier = Modifier.fillMaxWidth(), color = K.Green)
                        Text(step.title, fontSize = 18.sp, color = K.Ink, fontWeight = FontWeight.ExtraBold)
                        Text(step.text, fontSize = 15.sp, lineHeight = 20.sp, color = K.Ink)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (index > 0) TextButton(modifier = Modifier.weight(1f), onClick = { index--; store.saveTutorialStep(dni, simple, index) }) { Text("Anterior", color = K.GreenDark) }
                            Button(modifier = Modifier.weight(1f), onClick = {
                                if (index == steps.lastIndex) { store.completeTutorial(dni, simple); finished = true }
                                else { index++; store.saveTutorialStep(dni, simple, index) }
                            }, colors = ButtonDefaults.buttonColors(containerColor = K.Green)) {
                                Text(if (index == steps.lastIndex) "Entendido" else "Siguiente")
                            }
                        }
                    }
                }
            }
        }
    }
}
