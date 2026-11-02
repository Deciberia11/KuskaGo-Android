package pe.kuskago.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import pe.kuskago.app.R
import pe.kuskago.app.data.AccessPrefs

/*
 * Paleta de KuskaGo (versión "viva"): se recuperan los colores originales del prototipo
 * (verde andino, menta, mostaza, terracota, celeste, crema) pero más saturados y luminosos,
 * cuidando que todo texto tenga contraste ≥ 4.5:1 (WCAG AA) para el público adulto.
 *
 *   Verde KuskaGo   #0B7A55  principal: botones, enlaces, selección      (blanco 5.3:1)
 *   Menta           #DFF5EA  fondos suaves verdes                        (verde #0A6E4F encima 5.5:1)
 *   Oro Inti        #F5B700  insignias y avisos, SIEMPRE con texto oscuro (tinta 7.8:1)
 *   Terracota       #C2410C  acento andino                               (blanco 5.2:1)
 *   Celeste         #1565C0  información                                 (blanco 5.8:1)
 *   Rojo alerta     #C62828  urgente / error                             (blanco 5.6:1)
 *   Morado          #6F35B5  familia                                     (blanco 7.4:1)
 *   Crema           #FFFAF0  fondo de pantallas
 *   Tinta           #14302A  texto                                       (sobre crema 13.6:1, AAA)
 */
object K {
    // ---- Paleta base ----
    val Verde = Color(0xFF0B7A55)
    val VerdeOscuro = Color(0xFF075C40)
    val VerdeBrillante = Color(0xFF22B573)     // solo decorativo (barras, franja), nunca texto
    val Menta = Color(0xFFDFF5EA)
    val Oro = Color(0xFFF5B700)
    val Terracota = Color(0xFFC2410C)
    val TerracotaText = Color(0xFF9A3412)    // texto terracota sobre su fondo suave (6:1)
    val Celeste = Color(0xFF1565C0)
    val Rojo = Color(0xFFC62828)
    val Morado = Color(0xFF6F35B5)
    val Crema = Color(0xFFFFFAF0)

    // ---- Principal ----
    val Green = Verde
    val GreenDark = VerdeOscuro
    val GreenLight = VerdeBrillante
    val GreenSoft = Menta
    val GreenSoftBorder = Color(0xFFB5E6CD)
    val Mint = Menta

    // ---- Éxito ----
    val SelvaText = Color(0xFF0A6E4F)          // 5.5:1 sobre menta
    val OkBg = Menta
    val OkBorder = GreenSoftBorder

    // ---- Avisos (oro) ----
    val WarnBg = Color(0xFFFFF3C4)
    val WarnBorder = Color(0xFFF5D46B)
    val WarnText = Color(0xFF7A5200)           // 6.2:1 sobre su fondo

    // ---- Neutros ----
    val Bg = Crema
    val Ink = Color(0xFF14302A)
    val Border = Color(0xFFE6DFD0)
    val RowBorder = Color(0xFFF0EADD)
    val Muted = Color(0xFF56625D)              // 6.1:1 sobre crema
    val Muted2 = Color(0xFF5C6863)             // 5.6:1
    val Label = Color(0xFF34443F)
    val Faint = Color(0xFFB8B2A4)
    val Track = Color(0xFFEDE7DA)
    val White = Color(0xFFFFFFFF)

    // ---- Alertas ----
    val ErrorBg = Color(0xFFFDE7E7)
    val ErrorBorder = Color(0xFFF5B5B5)
    val ErrorTitle = Color(0xFFB3261E)         // 5.5:1 sobre su fondo
    val ErrorBody = Color(0xFF7F1D1D)
    val Badge = Rojo

    val ContrastBg = Color(0xFF0F1A17)

    // ---- Categorías (fondo suave + texto/ícono fuerte) ----
    val SkyBg = Color(0xFFE3F0FB); val SkyBorder = Color(0xFFB6D4F2); val SkyText = Celeste       // 5.0:1
    val MustardBg = WarnBg; val MustardBorder = WarnBorder; val MustardText = WarnText
    val PurpleBg = Color(0xFFF1E8FB); val PurpleBorder = Color(0xFFD9C3F2); val PurpleText = Morado // 6.2:1
    val OrangeBg = Color(0xFFFDE8DC); val OrangeBorder = Color(0xFFF6C3A6)

    // ---- Nombres usados en la versión anterior (se mantienen para no romper pantallas) ----
    val Mar = Verde
    val Selva = Verde
    val Sierra = Oro
    val Costa = Crema
    val Bandera = Rojo
    val Pizarra = Ink

    // ---- Franja andina ----
    val Mustard = Oro
    val Sky = Celeste
}

/*
 * Tipografías (Google Fonts, licencia OFL, incluidas en res/font para que funcionen sin internet):
 *   Open Sans  → texto, formularios y títulos (en Bold/ExtraBold); formas abiertas, muy legible
 *   Montserrat → botones de acción ("Iniciar trámite", "Traducir texto")
 */
object KFont {
    val Body = FontFamily(
        Font(R.font.open_sans_regular, FontWeight.Normal),
        Font(R.font.open_sans_semibold, FontWeight.SemiBold),
        Font(R.font.open_sans_bold, FontWeight.Bold),
        Font(R.font.open_sans_extrabold, FontWeight.ExtraBold),
    )
    /** Títulos: Open Sans en negrita (se quitó Fira Sans). */
    val Title = Body
    val Button = FontFamily(
        Font(R.font.montserrat_bold, FontWeight.Bold),
        Font(R.font.montserrat_extrabold, FontWeight.ExtraBold),
    )
}

private val SCALE = mapOf(1 to 1f, 2 to 1.08f, 3 to 1.16f, 4 to 1.3f, 5 to 1.45f)
private val BTN_SCALE = mapOf(1 to 1f, 2 to 1.04f, 3 to 1.08f, 4 to 1.14f, 5 to 1.2f)
val SIZE_LABEL = mapOf(1 to "Normal", 2 to "Un poco grande", 3 to "Grande", 4 to "Muy grande", 5 to "Máximo")

/** Factor de escala del texto según la preferencia "Texto grande". */
fun AccessPrefs?.textScale(): Float =
    if (this?.big == true) SCALE[textSize] ?: 1.16f else 1f

fun AccessPrefs?.buttonScale(): Float =
    if (this?.big == true) BTN_SCALE[textSize] ?: 1.08f else 1f

private fun kuskaTypography(): Typography {
    val base = Typography()
    fun TextStyle.body() = copy(fontFamily = KFont.Body)
    fun TextStyle.title() = copy(fontFamily = KFont.Title)
    return Typography(
        displayLarge = base.displayLarge.title(),
        displayMedium = base.displayMedium.title(),
        displaySmall = base.displaySmall.title(),
        headlineLarge = base.headlineLarge.title(),
        headlineMedium = base.headlineMedium.title(),
        headlineSmall = base.headlineSmall.title(),
        titleLarge = base.titleLarge.title(),
        titleMedium = base.titleMedium.body(),
        titleSmall = base.titleSmall.body(),
        bodyLarge = base.bodyLarge.body(),
        bodyMedium = base.bodyMedium.body(),
        bodySmall = base.bodySmall.body(),
        labelLarge = base.labelLarge.copy(fontFamily = KFont.Button),
        labelMedium = base.labelMedium.body(),
        labelSmall = base.labelSmall.body(),
    )
}

@Composable
fun KuskaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = K.Green,
            onPrimary = K.White,
            secondary = K.Celeste,
            onSecondary = K.White,
            tertiary = K.Oro,
            onTertiary = K.Ink,
            error = K.Rojo,
            onError = K.White,
            background = K.Bg,
            onBackground = K.Ink,
            surface = K.White,
            onSurface = K.Ink,
        ),
        typography = kuskaTypography(),
        content = content,
    )
}
