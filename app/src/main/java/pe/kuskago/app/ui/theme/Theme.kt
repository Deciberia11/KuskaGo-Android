package pe.kuskago.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import pe.kuskago.app.data.AccessPrefs

// Paleta tomada de theme.css del prototipo
object K {
    val Green = Color(0xFF176B57)
    val GreenDark = Color(0xFF0F4A3D)
    val GreenLight = Color(0xFF5FBE93)
    val GreenSoft = Color(0xFFEDF6F2)
    val GreenSoftBorder = Color(0xFFCDE7DB)
    val Mint = Color(0xFFDCEFE6)
    val OkBg = Color(0xFFE1F3EC)

    val Bg = Color(0xFFFAF7F1)
    val Ink = Color(0xFF1A2E29)
    val Border = Color(0xFFE4E1D8)
    val RowBorder = Color(0xFFEEEBE2)
    val Muted = Color(0xFF6B6558)
    val Muted2 = Color(0xFF9C9788)
    val Label = Color(0xFF4B5563)
    val Faint = Color(0xFFC9C4B6)
    val White = Color(0xFFFFFFFF)

    val ErrorBg = Color(0xFFFDEDEC)
    val ErrorBorder = Color(0xFFF3B7B0)
    val ErrorTitle = Color(0xFFB42318)
    val ErrorBody = Color(0xFF7A2A20)
    val Badge = Color(0xFFDC2626)

    val ContrastBg = Color(0xFF111814)

    val SkyBg = Color(0xFFE4EEF9); val SkyBorder = Color(0xFFC6DBF0); val SkyText = Color(0xFF1D5A96)
    val MustardBg = Color(0xFFFDF3C7); val MustardBorder = Color(0xFFF0DE9B); val MustardText = Color(0xFF8A6D00)
    val PurpleBg = Color(0xFFF1E7F9); val PurpleBorder = Color(0xFFDCC3F0); val PurpleText = Color(0xFF7C3AAF)
    val OrangeBg = Color(0xFFFDF0DE); val OrangeBorder = Color(0xFFF3D8AF)

    // Franja andina
    val Terracota = Color(0xFFC1552C)
    val Mustard = Color(0xFFE3A008)
    val Sky = Color(0xFF2F80A5)
}

private val SCALE = mapOf(1 to 1f, 2 to 1.08f, 3 to 1.16f, 4 to 1.3f, 5 to 1.45f)
private val BTN_SCALE = mapOf(1 to 1f, 2 to 1.04f, 3 to 1.08f, 4 to 1.14f, 5 to 1.2f)
val SIZE_LABEL = mapOf(1 to "Normal", 2 to "Un poco grande", 3 to "Grande", 4 to "Muy grande", 5 to "Máximo")

/** Factor de escala del texto según la preferencia "Texto grande". */
fun AccessPrefs?.textScale(): Float =
    if (this?.big == true) SCALE[textSize] ?: 1.16f else 1f

fun AccessPrefs?.buttonScale(): Float =
    if (this?.big == true) BTN_SCALE[textSize] ?: 1.08f else 1f

@Composable
fun KuskaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = K.Green,
            onPrimary = K.White,
            background = K.Bg,
            onBackground = K.Ink,
            surface = K.White,
            onSurface = K.Ink,
        ),
        content = content,
    )
}
