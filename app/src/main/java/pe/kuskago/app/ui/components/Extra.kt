package pe.kuskago.app.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.speech.tts.TextToSpeech
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.Route
import pe.kuskago.app.ui.theme.K
import java.util.Locale

// =============================================================
// Navegación inferior común (Inicio · Trámites · Avisos · Perfil)
// =============================================================
@Composable
fun MainBottomNav(active: Route?, go: (Route) -> Unit) {
    BottomNav(
        listOf(
            NavEntry("🏠", "Inicio", active == Route.Home) { go(Route.Home) },
            NavEntry("📋", "Trámites", active == Route.Search) { go(Route.Search) },
            NavEntry("🔔", "Avisos", active == Route.Alerts) { go(Route.Alerts) },
            NavEntry("👤", "Perfil", active == Route.Profile) { go(Route.Profile) },
        )
    )
}

// =============================================================
// Fila de lista con icono, título, subtítulo y flecha
// =============================================================
@Composable
fun ListItemRow(
    icon: String?,
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = K.Muted,
    iconBg: Color = K.GreenSoft,
    iconColor: Color = K.Green,
    trailing: String? = "›",
    trailingColor: Color = K.Faint,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) { Text(icon, fontSize = 18.sp, color = iconColor, fontWeight = FontWeight.ExtraBold) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = K.Ink)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = subtitleColor, fontWeight = if (subtitleColor != K.Muted) FontWeight.Bold else FontWeight.Normal)
                }
            }
            if (trailing != null) {
                Text(trailing, color = trailingColor, fontSize = if (trailing == "›") 20.sp else 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(K.RowBorder)
        )
    }
}

// =============================================================
// Barra de progreso
// =============================================================
@Composable
fun KProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFEDEAE0)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF4FA684))
        )
    }
}

// =============================================================
// Pestañas tipo píldora (desplazables)
// =============================================================
@Composable
fun PillTabs(tabs: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { (id, label) ->
            val active = id == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) K.Green else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(id) }
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = if (active) K.White else K.Muted)
            }
        }
    }
}

/** Botón-píldora de opción (pill-chip). */
@Composable
fun OptionChip(label: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (active) K.Green else K.White)
            .border(1.5.dp, if (active) K.Green else K.Border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (active) K.White else K.Label)
    }
}

/** Tarjeta de opción grande (checkbox-card "on" del prototipo) para menús de elección. */
@Composable
fun ChoiceCard(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, fontSize: TextUnit = 15.sp) {
    CheckboxCard(checked = true, onClick = onClick, showBox = false, role = Role.Button, modifier = modifier) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = fontSize, color = K.Ink, modifier = Modifier.weight(1f))
    }
}

/** Círculo numerado de un paso (ruta). */
@Composable
fun StepDot(mark: String, state: Int, size: Dp = 30.dp) {
    // state: 2 = completo, 1 = actual, 0 = pendiente
    val bg = when (state) { 2 -> K.Green; 1 -> K.GreenSoft; else -> Color(0xFFF1EFE7) }
    val fg = when (state) { 2 -> K.White; 1 -> K.Green; else -> K.Muted2 }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .then(if (state == 1) Modifier.border(2.dp, K.Green, RoundedCornerShape(999.dp)) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Text(mark, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = fg) }
}

// =============================================================
// Voz (Texto a Voz en español)
// =============================================================
@Composable
fun rememberSpeaker(): (String) -> Unit {
    val context = LocalContext.current
    val tts = remember { TextToSpeech(context.applicationContext) { } }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    val speak: (String) -> Unit = remember {
        { text: String ->
            val r = tts.setLanguage(Locale.forLanguageTag("es-PE"))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.forLanguageTag("es"))
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kuskago")
            Unit
        }
    }
    return speak
}

// =============================================================
// Utilidades: avisos, enlaces, compartir, imprimir/guardar PDF
// =============================================================
fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        toast("No hay una app para abrir este enlace")
    }
}

fun Context.openUri(uri: String) {
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    } catch (e: Exception) {
        toast("No se pudo abrir el archivo")
    }
}

fun Context.shareText(title: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, "Compartir"))
}

fun Context.shareFile(uri: String) {
    try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = contentResolver.getType(Uri.parse(uri)) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Compartir documento"))
    } catch (e: Exception) {
        toast("No se pudo compartir el archivo")
    }
}

private var printWebView: WebView? = null

/**
 * Abre el diálogo de impresión de Android con el documento.
 * Desde ahí se puede imprimir o elegir "Guardar como PDF".
 */
fun Context.printHtml(jobName: String, html: String) {
    val activity = findActivity() ?: return
    val webView = WebView(activity)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val pm = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
            pm.print(jobName, view.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().build())
            printWebView = null
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    printWebView = webView
}

private fun esc(s: String) = s
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

/** HTML formal del documento (igual al del prototipo web). */
fun buildDocHtml(title: String, meta: String, intro: String, lines: List<Pair<String, String>>): String {
    val rows = lines.joinToString("") { "<tr><td>${esc(it.first)}</td><td>${esc(it.second)}</td></tr>" }
    return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\"><title>${esc(title)}</title><style>" +
        "body{font-family:Georgia,'Times New Roman',serif;color:#1A2E29;max-width:640px;margin:40px auto;padding:0 24px}" +
        "h1{font-size:18px;text-transform:uppercase;letter-spacing:0.4px;text-align:center;margin-bottom:4px}" +
        ".meta{text-align:center;color:#6B6558;font-size:12px;margin-bottom:24px}" +
        "p.intro{font-size:13.5px;line-height:1.7;margin-bottom:24px;white-space:pre-wrap}" +
        "table{width:100%;border-collapse:collapse}" +
        "td{padding:9px 0;font-size:12.5px;border-bottom:1px dashed #E4E1D8}" +
        "td:last-child{text-align:right;font-weight:800}" +
        "td:first-child{color:#6B6558;font-weight:700}" +
        ".sign{margin-top:60px;text-align:center;font-size:11.5px;color:#6B6558}" +
        ".sign .line{width:220px;border-top:1.5px solid #1A2E29;margin:0 auto 6px}" +
        "</style></head><body>" +
        "<h1>${esc(title)}</h1><div class=\"meta\">${esc(meta)}</div>" +
        "<p class=\"intro\">${esc(intro)}</p>" +
        (if (lines.isNotEmpty()) "<table>$rows</table>" else "") +
        "<div class=\"sign\"><div class=\"line\"></div>Firma del solicitante</div>" +
        "</body></html>"
}
