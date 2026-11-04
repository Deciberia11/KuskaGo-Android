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
import androidx.compose.material.icons.rounded.Stop
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

/** Iconos Material identificados por claves descriptivas. */
private val ICONS: Map<String, ImageVector> = mapOf(
    ":stop:" to Icons.Rounded.Stop,
    ":home:" to Icons.Rounded.Home,
    ":assignment:" to Icons.Rounded.Assignment,
    ":notifications:" to Icons.Rounded.Notifications,
    ":person:" to Icons.Rounded.Person,
    ":search:" to Icons.Rounded.Search,
    ":description:" to Icons.Rounded.Description,
    ":factcheck:" to Icons.Rounded.FactCheck,
    ":folder:" to Icons.Rounded.Folder,
    ":familyrestroom:" to Icons.Rounded.FamilyRestroom,
    ":explore:" to Icons.Rounded.Explore,
    ":recordvoiceover:" to Icons.Rounded.RecordVoiceOver,
    ":directionsbus:" to Icons.Rounded.DirectionsBus,
    ":supportagent:" to Icons.Rounded.SupportAgent,
    ":keyboard:" to Icons.Rounded.Keyboard,
    ":landscape:" to Icons.Rounded.Landscape,
    ":historyedu:" to Icons.Rounded.HistoryEdu,
    ":badge:" to Icons.Rounded.Badge,
    ":elderly:" to Icons.Rounded.Elderly,
    ":face:" to Icons.Rounded.Face,
    ":favorite:" to Icons.Rounded.Favorite,
    ":accountbalance:" to Icons.Rounded.AccountBalance,
    ":savings:" to Icons.Rounded.Savings,
    ":storefront:" to Icons.Rounded.Storefront,
    ":locationon:" to Icons.Rounded.LocationOn,
    ":map:" to Icons.Rounded.Map,
    ":photocamera:" to Icons.Rounded.PhotoCamera,
    ":image:" to Icons.Rounded.Image,
    ":editnote:" to Icons.Rounded.EditNote,
    ":edit:" to Icons.Rounded.Edit,
    ":attachfile:" to Icons.Rounded.AttachFile,
    ":helpoutline:" to Icons.Rounded.HelpOutline,
    ":warningamber:" to Icons.Rounded.WarningAmber,
    ":chat:" to Icons.Rounded.Chat,
    ":mic:" to Icons.Rounded.Mic,
    ":volumeup:" to Icons.Rounded.VolumeUp,
    ":settings:" to Icons.Rounded.Settings,
    ":thumbup:" to Icons.Rounded.ThumbUp,
    ":celebration:" to Icons.Rounded.Celebration,
    ":arrowforward:" to Icons.Rounded.ArrowForward,
    ":arrowback:" to Icons.Rounded.ArrowBack,
    ":translate:" to Icons.Rounded.Translate,
    ":autoawesome:" to Icons.Rounded.AutoAwesome,
    ":lock:" to Icons.Rounded.Lock,
    ":checkcircle:" to Icons.Rounded.CheckCircle,
    ":check:" to Icons.Rounded.Check,
    ":alarm:" to Icons.Rounded.Alarm,
    ":save:" to Icons.Rounded.Save,
    ":print:" to Icons.Rounded.Print,
    ":receiptlong:" to Icons.Rounded.ReceiptLong,
    ":smartphone:" to Icons.Rounded.Smartphone,
    ":email:" to Icons.Rounded.Email,
    ":phone:" to Icons.Rounded.Phone,
    ":contrast:" to Icons.Rounded.Contrast,
    ":visibility:" to Icons.Rounded.Visibility,
    ":visibilityoff:" to Icons.Rounded.VisibilityOff,
)

private val LEGACY_ICON_KEYS = mapOf(
    "\uD83C\uDFE0" to ":home:",
    "\uD83D\uDCCB" to ":assignment:",
    "\uD83D\uDD14" to ":notifications:",
    "\uD83D\uDC64" to ":person:",
    "\uD83E\uDDD1" to ":person:",
    "\uD83D\uDD0D" to ":search:",
    "\uD83D\uDCC4" to ":description:",
    "\uD83D\uDDC2" to ":factcheck:",
    "\uD83D\uDCC1" to ":folder:",
    "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67" to ":familyrestroom:",
    "\uD83E\uDDED" to ":explore:",
    "\uD83D\uDDE3" to ":recordvoiceover:",
    "\uD83D\uDE8C" to ":directionsbus:",
    "\uD83D\uDE4B" to ":supportagent:",
    "\uD83C\uDD98" to ":supportagent:",
    "\u2328" to ":keyboard:",
    "\uD83C\uDFDE" to ":landscape:",
    "\uD83D\uDCDC" to ":historyedu:",
    "\uD83E\uDEAA" to ":badge:",
    "\uD83D\uDC75" to ":elderly:",
    "\uD83D\uDC74" to ":elderly:",
    "\uD83D\uDC66" to ":face:",
    "\uD83D\uDC67" to ":face:",
    "\uD83D\uDC91" to ":favorite:",
    "\uD83C\uDFDB" to ":accountbalance:",
    "\uD83C\uDFE6" to ":savings:",
    "\uD83C\uDFEA" to ":storefront:",
    "\uD83D\uDCCD" to ":locationon:",
    "\uD83D\uDDFA" to ":map:",
    "\uD83D\uDCF7" to ":photocamera:",
    "\uD83D\uDDBC" to ":image:",
    "\uD83D\uDCDD" to ":editnote:",
    "\u270F" to ":edit:",
    "\uD83D\uDCCE" to ":attachfile:",
    "\u2753" to ":helpoutline:",
    "\u26A0" to ":warningamber:",
    "\uD83D\uDCAC" to ":chat:",
    "\uD83C\uDF99" to ":mic:",
    "\uD83D\uDD0A" to ":volumeup:",
    "\u2699" to ":settings:",
    "\uD83D\uDC4D" to ":thumbup:",
    "\uD83C\uDF89" to ":celebration:",
    "\u279C" to ":arrowforward:",
    "\uD83D\uDC49" to ":arrowforward:",
    "\u2190" to ":arrowback:",
    "\uD83C\uDF10" to ":translate:",
    "\u2728" to ":autoawesome:",
    "\uD83D\uDD12" to ":lock:",
    "\u2705" to ":checkcircle:",
    "\u2713" to ":check:",
    "\u23F0" to ":alarm:",
    "\uD83D\uDCBE" to ":save:",
    "\uD83D\uDDA8" to ":print:",
    "\uD83E\uDDFE" to ":receiptlong:",
    "\uD83D\uDCF1" to ":smartphone:",
    "\u2709" to ":email:",
    "\uD83D\uDCDE" to ":phone:",
    "\u25D1" to ":contrast:",
    "\uD83D\uDC41" to ":visibility:",
    "\uD83D\uDE48" to ":visibilityoff:",
)

/** Íconos de la barra inferior: relleno si está activo, contorno si no (convención de Material 3). */
fun navIcon(key: String, active: Boolean): ImageVector? = if (active) iconFor(key) else when (clean(key)) {
    ":home:" -> androidx.compose.material.icons.Icons.Outlined.Home
    ":assignment:" -> androidx.compose.material.icons.Icons.Outlined.Assignment
    ":notifications:" -> androidx.compose.material.icons.Icons.Outlined.Notifications
    ":person:" -> androidx.compose.material.icons.Icons.Outlined.Person
    else -> iconFor(key)
}

/** Normaliza las claves almacenadas por versiones anteriores. */
private fun clean(s: String): String {
    val key = s.replace("\uFE0F", "").trim()
    return LEGACY_ICON_KEYS[key] ?: key
}

/** Ícono de Material para una clave descriptiva, o null si no hay equivalente. */
fun iconFor(key: String?): ImageVector? = key?.let { ICONS[clean(it)] }

/**
 * Separa un ícono al inicio del texto: ":locationon: Usar mi ubicación" → (LocationOn, "Usar mi ubicación").
 * Si el texto no empieza con un ícono conocido, devuelve (null, texto).
 */
fun splitIcon(text: String): Pair<ImageVector?, String> {
    val t = text.trimStart()
    val sp = t.indexOf(' ')
    if (sp <= 0 || sp > 64) return null to text
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
 * Text(":locationon: Usar mi ubicación", …) se reemplaza por IconLabel(":locationon: Usar mi ubicación", …)
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
