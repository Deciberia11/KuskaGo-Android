package pe.kuskago.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Iconografía de KuskaGo: Material Icons de Google, estilo Rounded (el estándar de las apps Android),
 * en lugar de emojis. En la barra inferior se sigue la convención de Material 3: ícono relleno en la
 * sección activa y de contorno en las demás.
 * Los emojis se siguen usando como "clave" en los datos (por ejemplo, los documentos guardados
 * en la base de datos tienen "📄"), pero en pantalla siempre se dibuja el ícono de Material:
 * se ve igual en todos los celulares, toma el color del tema y TalkBack no lee "emoji de…".
 */
private val ICONS: Map<String, ImageVector> = mapOf(
    "🏠" to Icons.Rounded.Home,
    "📋" to Icons.Rounded.Assignment,
    "🔔" to Icons.Rounded.Notifications,
    "👤" to Icons.Rounded.Person,
    "🧑" to Icons.Rounded.Person,
    "🔍" to Icons.Rounded.Search,
    "📄" to Icons.Rounded.Description,
    "🗂" to Icons.Rounded.FactCheck,
    "📁" to Icons.Rounded.Folder,
    "👨‍👩‍👧" to Icons.Rounded.FamilyRestroom,
    "🧭" to Icons.Rounded.Explore,
    "🗣" to Icons.Rounded.RecordVoiceOver,
    "🚌" to Icons.Rounded.DirectionsBus,
    "🙋" to Icons.Rounded.SupportAgent,
    "🆘" to Icons.Rounded.SupportAgent,
    "⌨" to Icons.Rounded.Keyboard,
    "🏞" to Icons.Rounded.Landscape,
    "📜" to Icons.Rounded.HistoryEdu,
    "🪪" to Icons.Rounded.Badge,
    "👵" to Icons.Rounded.Elderly,
    "👴" to Icons.Rounded.Elderly,
    "👦" to Icons.Rounded.Face,
    "👧" to Icons.Rounded.Face,
    "💑" to Icons.Rounded.Favorite,
    "🏛" to Icons.Rounded.AccountBalance,
    "🏦" to Icons.Rounded.Savings,
    "🏪" to Icons.Rounded.Storefront,
    "📍" to Icons.Rounded.LocationOn,
    "🗺" to Icons.Rounded.Map,
    "📷" to Icons.Rounded.PhotoCamera,
    "🖼" to Icons.Rounded.Image,
    "📝" to Icons.Rounded.EditNote,
    "✏" to Icons.Rounded.Edit,
    "📎" to Icons.Rounded.AttachFile,
    "❓" to Icons.Rounded.HelpOutline,
    "⚠" to Icons.Rounded.WarningAmber,
    "💬" to Icons.Rounded.Chat,
    "🎙" to Icons.Rounded.Mic,
    "🔊" to Icons.Rounded.VolumeUp,
    "⚙" to Icons.Rounded.Settings,
    "👍" to Icons.Rounded.ThumbUp,
    "🎉" to Icons.Rounded.Celebration,
    "➜" to Icons.Rounded.ArrowForward,
    "👉" to Icons.Rounded.ArrowForward,
    "←" to Icons.Rounded.ArrowBack,
    "🌐" to Icons.Rounded.Translate,
    "✨" to Icons.Rounded.AutoAwesome,
    "🔒" to Icons.Rounded.Lock,
    "✅" to Icons.Rounded.CheckCircle,
    "✓" to Icons.Rounded.Check,
    "⏰" to Icons.Rounded.Alarm,
    "💾" to Icons.Rounded.Save,
    "🖨" to Icons.Rounded.Print,
    "🧾" to Icons.Rounded.ReceiptLong,
    "📱" to Icons.Rounded.Smartphone,
    "✉" to Icons.Rounded.Email,
    "📞" to Icons.Rounded.Phone,
    "◑" to Icons.Rounded.Contrast,
    "👁" to Icons.Rounded.Visibility,
    "🙈" to Icons.Rounded.VisibilityOff,
)

/** Íconos de la barra inferior: relleno si está activo, contorno si no (convención de Material 3). */
fun navIcon(key: String, active: Boolean): ImageVector? = if (active) iconFor(key) else when (key.replace("\uFE0F", "").trim()) {
    "🏠" -> androidx.compose.material.icons.Icons.Outlined.Home
    "📋" -> androidx.compose.material.icons.Icons.Outlined.Assignment
    "🔔" -> androidx.compose.material.icons.Icons.Outlined.Notifications
    "👤" -> androidx.compose.material.icons.Icons.Outlined.Person
    else -> iconFor(key)
}

/** Quita el selector de variación (U+FE0F) que traen algunos emojis, p. ej. "🗂️" → "🗂". */
private fun clean(s: String) = s.replace("️", "").trim()

/** Ícono de Material para un emoji/clave, o null si no hay equivalente. */
fun iconFor(key: String?): ImageVector? = key?.let { ICONS[clean(it)] }

/**
 * Separa un ícono al inicio del texto: "📍 Usar mi ubicación" → (LocationOn, "Usar mi ubicación").
 * Si el texto no empieza con un ícono conocido, devuelve (null, texto).
 */
fun splitIcon(text: String): Pair<ImageVector?, String> {
    val t = text.trimStart()
    val sp = t.indexOf(' ')
    if (sp <= 0 || sp > 12) return null to text
    val icon = iconFor(t.substring(0, sp)) ?: return null to text
    return icon to t.substring(sp).trim()
}

/** Dibuja el ícono de Material; si la clave no tiene ícono (p. ej. un número), muestra el texto. */
@Composable
fun KIcon(
    key: String,
    size: Dp = 22.dp,
    tint: Color = Color.Unspecified,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val icon = iconFor(key)
    if (icon != null) {
        Icon(icon, contentDescription, modifier = modifier.size(size), tint = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint)
    } else {
        Text(key, fontSize = (size.value * 0.62f).sp, fontWeight = FontWeight.ExtraBold, color = tint, modifier = modifier)
    }
}

/**
 * Texto con ícono opcional al inicio. Acepta los mismos parámetros que Text, así que
 * Text("📍 Usar mi ubicación", …) se reemplaza por IconLabel("📍 Usar mi ubicación", …)
 * y en pantalla aparece el ícono de ubicación de Material seguido del texto.
 */
@Composable
fun IconLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    iconColor: Color = color,
) {
    val (icon, label) = splitIcon(text)
    if (icon == null) {
        Text(text, modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, fontFamily = fontFamily, textAlign = textAlign, lineHeight = lineHeight)
        return
    }
    val iconSize = with(LocalDensity.current) {
        val fs = if (fontSize == TextUnit.Unspecified) 15.sp else fontSize
        (fs.toPx() * 1.25f).toDp()
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start),
    ) {
        Icon(
            icon, null, modifier = Modifier.size(iconSize),
            tint = if (iconColor == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else iconColor,
        )
        Text(label, color = color, fontSize = fontSize, fontWeight = fontWeight, fontFamily = fontFamily, textAlign = textAlign, lineHeight = lineHeight)
    }
}
