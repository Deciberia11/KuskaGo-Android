package pe.kuskago.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.Route
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.data.MyTramite
import pe.kuskago.app.ui.components.Chip
import pe.kuskago.app.ui.components.KCard
import pe.kuskago.app.ui.components.KProgress
import pe.kuskago.app.ui.components.MainBottomNav
import pe.kuskago.app.ui.components.PrimaryButton
import pe.kuskago.app.ui.components.Screen
import pe.kuskago.app.ui.components.SecondaryButton
import pe.kuskago.app.ui.components.SectionLabel
import pe.kuskago.app.ui.components.StepDot
import pe.kuskago.app.ui.components.TopBar
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.components.IconLabel
import pe.kuskago.app.ui.components.KIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class StatusStyle(val label: String, val bg: Color, val fg: Color)

private fun styleFor(status: String): StatusStyle = when (status) {
    KuskaStore.STATUS_DONE -> StatusStyle("Finalizado", K.OkBg, K.GreenDark)
    KuskaStore.STATUS_SUBMITTED -> StatusStyle("Presentado", K.SkyBg, K.SkyText)
    KuskaStore.STATUS_GENERATED -> StatusStyle("Documento listo", K.WarnBg, K.WarnText)
    else -> StatusStyle("En curso", K.WarnBg, K.WarnText)
}

private val ICONS = mapOf(
    "terreno" to "🏞️", "constancia" to "📜", "dni" to "🪪",
    "pension65" to "👵", "herencia" to "🏛️", "licencia" to "🏪",
)

private fun dateLabel(ms: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE")).format(Date(ms))

/** Qué debe hacer la persona ahora, según el estado. */
private fun nextStep(t: MyTramite): String = when (t.status) {
    KuskaStore.STATUS_GENERATED -> "Imprime tu documento, fírmalo y preséntalo en ${t.entity}."
    KuskaStore.STATUS_SUBMITTED -> "Espera la respuesta de ${t.entity}. Te avisaremos por KuskaAlert."
    KuskaStore.STATUS_DONE -> "¡Trámite terminado! Guarda tu constancia en Mis documentos."
    else -> "Te faltan requisitos. Revísalos antes de viajar."
}

// =============================================================
// MIS TRÁMITES — seguimiento real (tabla tramite)
// =============================================================
@Composable
fun MyTramitesScreen(
    store: KuskaStore,
    onBack: () -> Unit,
    go: (Route) -> Unit,
    onOpenWizard: (String) -> Unit,
) {
    var items by remember { mutableStateOf(store.getTramites()) }
    var openId by rememberSaveable { mutableLongStateOf(-1L) }
    var confirmDelete by remember { mutableStateOf(false) }
    val open = items.firstOrNull { it.id == openId }

    fun refresh() { items = store.getTramites() }
    BackHandler(enabled = open != null) { openId = -1L }

    Screen {
        TopBar(onBack = { if (open != null) openId = -1L else onBack() }, title = if (open == null) "Mis trámites" else open.title)
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                if (open == null) {
                    val active = items.count { it.status != KuskaStore.STATUS_DONE }
                    val done = items.size - active
                    if (items.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            KIcon("🗂️", size = 46.dp, tint = K.Green)
                            Text("Aún no tienes trámites", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = K.Ink)
                            Text(
                                "Cuando completes un formulario, tu trámite aparecerá aquí para que sigas su avance.",
                                fontSize = 13.sp, color = K.Muted, textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Text(
                            "$active en curso · $done ${if (done == 1) "finalizado" else "finalizados"}",
                            fontSize = 13.5.sp, color = K.Muted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp),
                        )
                        items.forEach { t ->
                            val st = styleFor(t.status)
                            KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), onClick = { openId = t.id }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    androidx.compose.foundation.layout.Box(
                                        Modifier.size(44.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(K.Menta),
                                        contentAlignment = Alignment.Center,
                                    ) { KIcon(ICONS[t.type] ?: "📄", size = 26.dp, tint = K.Verde) }
                                    Column(Modifier.weight(1f)) {
                                        Text(t.title, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, color = K.Ink)
                                        Text("${t.entity} · ${dateLabel(t.updated)}", fontSize = 12.sp, color = K.Muted)
                                    }
                                    Chip(st.label, st.bg, st.fg)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                                    KProgress(t.progress / 100f, Modifier.weight(1f))
                                    Text("${t.progress}%", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = K.Ink, modifier = Modifier.padding(start = 10.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("＋ Iniciar un trámite", { go(Route.Search) })
                } else {
                    val st = styleFor(open.status)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                        Chip(open.entity, K.SkyBg, K.SkyText)
                        Chip(st.label, st.bg, st.fg)
                    }
                    KProgress(open.progress / 100f, Modifier.padding(bottom = 6.dp))
                    Text("Avance: ${open.progress}%", fontSize = 12.sp, color = K.Muted2, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                    KCard(Modifier.fillMaxWidth().padding(bottom = 16.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                        IconLabel("👉 Lo que sigue", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.Ink, modifier = Modifier.padding(bottom = 4.dp))
                        Text(nextStep(open), fontSize = 13.5.sp, color = K.Ink, lineHeight = 20.sp)
                    }

                    SectionLabel("Etapas", Modifier.padding(bottom = 6.dp))
                    val stages = listOf(
                        "Documento generado" to (open.status != KuskaStore.STATUS_IN_PROGRESS),
                        "Presentado en ${open.entity}" to (open.status == KuskaStore.STATUS_SUBMITTED || open.status == KuskaStore.STATUS_DONE),
                        "Trámite finalizado" to (open.status == KuskaStore.STATUS_DONE),
                    )
                    val currentIdx = stages.indexOfFirst { !it.second }
                    stages.forEachIndexed { i, (label, ok) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StepDot(if (ok) "✓" else "${i + 1}", if (ok) 2 else if (i == currentIdx) 1 else 0)
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (ok) K.Ink else K.Muted)
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (open.status) {
                            KuskaStore.STATUS_IN_PROGRESS ->
                                PrimaryButton("Continuar trámite", { go(Route.TramiteDetail) })
                            KuskaStore.STATUS_GENERATED ->
                                PrimaryButton("✓ Ya lo presenté", { store.setTramiteStatus(open.id, KuskaStore.STATUS_SUBMITTED); refresh() })
                            KuskaStore.STATUS_SUBMITTED ->
                                PrimaryButton("✓ Ya recibí la respuesta", { store.setTramiteStatus(open.id, KuskaStore.STATUS_DONE); refresh() })
                            else -> {}
                        }
                        if (open.documentName.isNotBlank()) {
                            SecondaryButton("📄 Ver mi documento", { go(Route.Documents) })
                        }
                        if (open.type == "terreno" || open.type == "constancia") {
                            SecondaryButton("🧭 Ver Ruta Ciudadana", { go(Route.CitizenRoute) })
                        }
                        if (open.status != KuskaStore.STATUS_IN_PROGRESS) {
                            SecondaryButton("📝 Llenar otra solicitud igual", { onOpenWizard(open.type) })
                        }
                        SecondaryButton("Eliminar de mi lista", { confirmDelete = true }, contentColor = K.ErrorTitle, borderColor = K.ErrorBorder)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        MainBottomNav(null, go)
    }

    if (confirmDelete && open != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Quitar este trámite?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Se quitará \"${open.title}\" de Mis trámites. Tu documento seguirá en Mis documentos.") },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteTramite(open.id)
                    confirmDelete = false
                    openId = -1L
                    refresh()
                }) { Text("Quitar", color = K.ErrorTitle, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = K.Green) } },
            containerColor = K.White,
        )
    }
}
