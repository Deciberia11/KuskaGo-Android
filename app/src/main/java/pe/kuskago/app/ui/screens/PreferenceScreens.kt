package pe.kuskago.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.data.AccessPrefs
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.components.CheckboxCard
import pe.kuskago.app.ui.components.GhostButton
import pe.kuskago.app.ui.components.KCard
import pe.kuskago.app.ui.components.PrimaryButton
import pe.kuskago.app.ui.components.Screen
import pe.kuskago.app.ui.components.TextSizeSelector
import pe.kuskago.app.ui.components.TopBar
import pe.kuskago.app.ui.components.rememberSpeaker
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.theme.buttonScale
import pe.kuskago.app.ui.theme.textScale

/** Las seis opciones de accesibilidad del prototipo. */
private enum class PrefKey(val icon: String, val title: String, val shortTitle: String, val desc: String) {
    BIG("Aa", "Texto grande", "Texto grande", "Letras más grandes"),
    LISTEN("🔊", "Prefiero escuchar", "Prefiero escuchar", "Audio en cada paso"),
    GUIDED("➜", "Necesito ayuda paso a paso", "Ayuda paso a paso", "Modo guiado y sencillo"),
    CONTRAST("◑", "Alto contraste", "Alto contraste", "Colores más visibles"),
    SIMPLE("💬", "Lenguaje sencillo", "Lenguaje sencillo", "Palabras fáciles de entender"),
    TRANSLATE("🌐", "Traducir automáticamente", "Traductor automático", "Explica términos difíciles"),
}

private fun AccessPrefs.get(k: PrefKey) = when (k) {
    PrefKey.BIG -> big
    PrefKey.LISTEN -> listen
    PrefKey.GUIDED -> guided
    PrefKey.CONTRAST -> contrast
    PrefKey.SIMPLE -> simple
    PrefKey.TRANSLATE -> translate
}

private fun AccessPrefs.toggle(k: PrefKey) = when (k) {
    PrefKey.BIG -> copy(big = !big)
    PrefKey.LISTEN -> copy(listen = !listen)
    PrefKey.GUIDED -> copy(guided = !guided)
    PrefKey.CONTRAST -> copy(contrast = !contrast)
    PrefKey.SIMPLE -> copy(simple = !simple)
    PrefKey.TRANSLATE -> copy(translate = !translate)
}

// =============================================================
// 5. PREFERENCIAS
// =============================================================
@Composable
fun PreferencesScreen(
    store: KuskaStore,
    onBack: () -> Unit,
    onContinue: (guided: Boolean) -> Unit,
    onSkip: () -> Unit,
) {
    val dni = remember { store.currentUser()?.dni.orEmpty() }
    var p by remember { mutableStateOf(store.getPrefs(dni) ?: AccessPrefs()) }
    fun save(newPrefs: AccessPrefs) {
        p = newPrefs
        store.savePrefs(dni, newPrefs)
    }

    val scale = p.textScale()
    val btnScale = p.buttonScale()
    fun px(base: Float): TextUnit = (base * scale).sp
    val speak = rememberSpeaker()

    val preview = if (p.simple) {
        "Sube los documentos que necesitas para continuar."
    } else {
        "Adjunte la documentación requerida para continuar con el procedimiento."
    }
    val previewSize = if (p.big) (mapOf(1 to 15, 2 to 17, 3 to 19, 4 to 22, 5 to 25)[p.textSize] ?: 19) else 15

    Screen {
        TopBar(onBack = onBack, title = "¿Cómo prefieres usar KuskaGo?", titleSize = px(17f))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 2.dp),
        ) {
            Text(
                "Elige una o varias opciones. Puedes cambiarlas cuando quieras.",
                fontSize = px(13.5f),
                color = K.Muted,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            PrefKey.entries.forEach { key ->
                CheckboxCard(
                    checked = p.get(key),
                    onClick = { save(p.toggle(key)) },
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${key.icon}  ${key.title}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = px(15f),
                            color = K.Ink,
                        )
                        Text(key.desc, fontSize = px(12.5f), color = K.Muted)
                    }
                }
                if (key == PrefKey.BIG && p.big) {
                    TextSizeSelector(p.textSize) { save(p.copy(textSize = it)) }
                }
            }

            Text(
                "Así se verá KuskaGo",
                fontSize = px(13f),
                fontWeight = FontWeight.ExtraBold,
                color = K.Label,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            KCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp),
                background = if (p.contrast) K.ContrastBg else K.White,
                borderColor = if (p.contrast) K.ContrastBg else K.Border,
            ) {
                Text(
                    preview,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = previewSize.sp,
                    lineHeight = (previewSize * 1.35f).sp,
                    color = if (p.contrast) K.White else K.Ink,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                if (p.listen) {
                    Button(
                        onClick = { speak(preview) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = K.Green, contentColor = K.White),
                    ) {
                        Text("🔊 Escuchar explicación", fontWeight = FontWeight.ExtraBold, fontSize = (14.5f * btnScale).sp)
                    }
                }
                if (p.translate) {
                    HorizontalDivider(color = K.Border, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
                    val labelColor = if (p.contrast) K.Faint else K.Muted2
                    val textColor = if (p.contrast) K.White else K.Ink
                    Text("TÉRMINO OFICIAL", fontSize = px(11f), fontWeight = FontWeight.ExtraBold, color = labelColor)
                    Text(
                        "Constancia de posesión debidamente suscrita",
                        fontSize = px(13f),
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Text("EN PALABRAS SIMPLES", fontSize = px(11f), fontWeight = FontWeight.ExtraBold, color = labelColor)
                    Text("Documento que confirma que usas o vives en ese terreno.", fontSize = px(13.5f), color = textColor)
                }
            }
        }
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PrimaryButton("Continuar", { onContinue(p.guided) }, fontSize = (16f * btnScale).sp)
            Text(
                "Puedes cambiar estas opciones después desde tu perfil.",
                fontSize = px(12f),
                color = K.Muted2,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            GhostButton("Configurar después", onSkip, fontSize = (13.5f * btnScale).sp)
        }
    }
}

// =============================================================
// 9. ACCESIBILIDAD (desde Perfil)
// =============================================================
@Composable
fun AccessibilityScreen(store: KuskaStore, onBack: () -> Unit) {
    val dni = remember { store.currentUser()?.dni.orEmpty() }
    // Si aún no hay preferencias guardadas, el prototipo propone "Texto grande" y "Lenguaje sencillo".
    var p by remember { mutableStateOf(store.getPrefs(dni) ?: AccessPrefs(big = true, simple = true)) }
    fun save(newPrefs: AccessPrefs) {
        p = newPrefs
        store.savePrefs(dni, newPrefs)
    }

    Screen {
        TopBar(onBack = onBack, title = "Accesibilidad")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 2.dp),
        ) {
            Text(
                "Estas son tus preferencias guardadas. Puedes cambiarlas cuando quieras.",
                fontSize = 13.sp,
                color = K.Muted,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            PrefKey.entries.forEach { key ->
                CheckboxCard(
                    checked = p.get(key),
                    onClick = { save(p.toggle(key)) },
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    Row(Modifier.weight(1f)) {
                        Text(
                            "${key.icon}  ${key.shortTitle}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = K.Ink,
                        )
                    }
                }
                if (key == PrefKey.BIG && p.big) {
                    TextSizeSelector(p.textSize) { save(p.copy(textSize = it)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
            PrimaryButton("Guardar cambios", {
                save(p)
                onBack()
            })
        }
    }
}
