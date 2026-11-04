package pe.kuskago.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.R
import pe.kuskago.app.Route
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.components.Chip
import pe.kuskago.app.ui.components.IconBox
import pe.kuskago.app.ui.components.KCard
import pe.kuskago.app.ui.components.KTextField
import pe.kuskago.app.ui.components.ListItemRow
import pe.kuskago.app.ui.components.MainBottomNav
import pe.kuskago.app.ui.components.Screen
import pe.kuskago.app.ui.components.SecondaryButton
import pe.kuskago.app.ui.components.SectionLabel
import pe.kuskago.app.ui.components.TopBar
import pe.kuskago.app.ui.components.toast
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.theme.KFont
import pe.kuskago.app.ui.components.IconLabel
import pe.kuskago.app.ui.components.ProfileAvatar
import pe.kuskago.app.ui.components.KIcon
import pe.kuskago.app.ui.theme.textScale
import java.text.Normalizer

@Composable
private fun BrandHeader(onColor: Boolean = false, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Image(painterResource(R.drawable.ic_logo), null, Modifier.size(36.dp))
            Text("KuskaGo", fontFamily = KFont.Title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (onColor) K.White else K.GreenDark, letterSpacing = (-0.3).sp)
        }
        trailing()
    }
}

// =============================================================
// 6. INICIO — MODO NORMAL
// =============================================================
@Composable
fun HomeScreen(store: KuskaStore, go: (Route) -> Unit, target: (String) -> Modifier = { Modifier }) {
    val profile = remember { store.getProfile() }
    val prefs = remember { store.getPrefs(store.currentUser()?.dni.orEmpty()) }
    val scale = 1f   // el "Texto grande" ya se aplica a toda la app desde MainActivity
    val contrast = prefs?.contrast == true
    val firstName = profile.name.trim().split(" ").firstOrNull().orEmpty().ifEmpty { "vecino" }
    val textColor = if (contrast) K.White else K.Ink

    Screen(background = if (contrast) K.ContrastBg else K.Bg) {
        BrandHeader(onColor = contrast) {
            Row(modifier = target("perfil"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val pending = remember { buildAlerts(store).count { it.kind != "info" } }
                IconBox(":notifications:", "Avisos", { go(Route.Alerts) }, badge = if (pending > 0) "$pending" else null, labelColor = textColor)
                IconBox(":person:", "Perfil", { go(Route.Profile) }, labelColor = textColor)
            }
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 18.dp)) {
            Row(
                modifier = Modifier.padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(store.profilePhoto(), Modifier.size(52.dp))
                Column(Modifier.weight(1f)) {
                    Text("¡Hola, $firstName!", fontSize = (20 * scale).sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                    Text("¿En qué podemos ayudarte hoy?", fontSize = 13.5.sp, color = if (contrast) K.Faint else K.Muted, fontWeight = FontWeight.SemiBold)
                }
            }

            val barShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(barShape)
                    .background(K.White)
                    .border(1.5.dp, K.GreenSoftBorder, barShape)
                    .clickable(role = Role.Button) { go(Route.Search) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconLabel(":search:  Busca un trámite, documento o servicio", color = K.Muted2, iconColor = K.Verde, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp),
        ) {

            val cardFont = (15f * scale).sp
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val tileTextStyle = LocalTextStyle.current.merge(TextStyle(fontSize = cardFont, fontWeight = FontWeight.ExtraBold))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val labelWidth = with(density) { ((maxWidth - 12.dp) / 2 - 32.dp).roundToPx().coerceAtLeast(1) }
                val labelHeight = listOf("Buscar trámite", "Mis trámites", "Mis documentos", "Mi familia").maxOf { label ->
                    textMeasurer.measure(label, style = tileTextStyle, constraints = Constraints(maxWidth = labelWidth)).size.height
                }
                val tileHeight = with(density) { labelHeight.toDp() + 74.dp }.coerceAtLeast(96.dp)
                Column {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                HomeTile(":description:", "Buscar trámite", K.Menta, K.GreenSoftBorder, K.VerdeOscuro, cardFont, Modifier.weight(1f).height(tileHeight).then(target("buscar"))) { go(Route.Search) }
                HomeTile(":factcheck:", "Mis trámites", K.SkyBg, K.SkyBorder, K.SkyText, cardFont, Modifier.weight(1f).height(tileHeight).then(target("tramites"))) { go(Route.MyTramites) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                HomeTile(":folder:", "Mis documentos", K.MustardBg, K.MustardBorder, K.MustardText, cardFont, Modifier.weight(1f).height(tileHeight).then(target("documentos"))) { go(Route.Documents) }
                HomeTile(":familyrestroom:", "Mi familia", K.PurpleBg, K.PurpleBorder, K.PurpleText, cardFont, Modifier.weight(1f).height(tileHeight).then(target("familia"))) { go(Route.Family) }
            }

                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                QuickAction(":explore:", "Ruta Ciudadana", K.Verde, Modifier.weight(1f).then(target("ruta"))) { go(Route.CitizenRoute) }
                QuickAction(":notifications:", "KuskaAlert", K.Rojo, Modifier.weight(1f).then(target("alertas"))) { go(Route.Alerts) }
                QuickAction(":supportagent:", "Pedir ayuda", K.Terracota, Modifier.weight(1f).then(target("ayuda"))) { go(Route.Help) }
                QuickAction(":recordvoiceover:", "Traductor", K.Celeste, Modifier.weight(1f).then(target("traductor"))) { go(Route.Translator) }
            }

            KCard(
                Modifier.fillMaxWidth(),
                background = K.GreenSoft,
                borderColor = K.GreenSoftBorder,
                onClick = { go(Route.TramiteDetail) },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    KIcon(":directionsbus:", size = 28.dp, tint = K.Green)
                    Column(Modifier.weight(1f)) {
                        Text("No viajes todavía", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = K.Ink)
                        Text("Te ayudamos a verificar que tengas todo antes de desplazarte", fontSize = 12.sp, color = K.Muted)
                    }
                    Text("›", color = K.Green, fontWeight = FontWeight.ExtraBold, fontFamily = KFont.Title, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        MainBottomNav(Route.Home, go)
    }
}

@Composable
private fun HomeTile(
    icon: String,
    label: String,
    bg: Color,
    border: Color,
    textColor: Color,
    fontSize: TextUnit,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    KCard(
        modifier = modifier.heightIn(min = 96.dp),
        background = bg,
        borderColor = border,
        onClick = onClick,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KIcon(icon, size = 34.dp, tint = textColor)
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, color = textColor)
    }
}

@Composable
private fun QuickAction(icon: String, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    KCard(
        modifier = modifier.heightIn(min = 72.dp),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KIcon(icon, size = 28.dp, tint = color)
        Text(label, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = K.Ink, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

// =============================================================
// 7. INICIO — MODO SENCILLO
// =============================================================
@Composable
fun HomeSimpleScreen(go: (Route) -> Unit, onNormalMode: () -> Unit, target: (String) -> Modifier = { Modifier }) {
    Screen {
        BrandHeader { Chip("Modo sencillo", K.MustardBg, K.MustardText) }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text("¿Qué necesitas?", fontFamily = KFont.Title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(bottom = 22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                SimpleTile(":description:", "Continuar mi trámite", K.Menta, K.GreenSoftBorder, Modifier.weight(1f).then(target("continuar"))) { go(Route.SimpleFlow) }
                SimpleTile(":notifications:", "Ver avisos", K.SkyBg, K.SkyBorder, Modifier.weight(1f).then(target("avisos"))) { go(Route.Alerts) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SimpleTile(":supportagent:", "Pedir ayuda", K.OrangeBg, K.OrangeBorder, Modifier.weight(1f).then(target("ayuda"))) { go(Route.Help) }
                SimpleTile(":familyrestroom:", "Trámites de mi familia", K.PurpleBg, K.PurpleBorder, Modifier.weight(1f).then(target("familia"))) { go(Route.Family) }
            }
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
            SecondaryButton("Volver al modo normal", onNormalMode)
        }
    }
}

@Composable
private fun SimpleTile(icon: String, label: String, bg: Color, border: Color, modifier: Modifier, onClick: () -> Unit) {
    KCard(
        modifier = modifier.heightIn(min = 140.dp),
        background = bg,
        borderColor = border,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        val fg = when (bg) { K.SkyBg -> K.SkyText; K.OrangeBg -> K.TerracotaText; K.PurpleBg -> K.PurpleText; else -> K.VerdeOscuro }
        KIcon(icon, size = 46.dp, tint = fg)
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = fg, textAlign = TextAlign.Center)
    }
}

// =============================================================
// 8. BUSCAR TRÁMITE
// =============================================================
private data class Procedure(val type: String, val name: String, val sub: String, val icon: String, val keys: String)

private val PROCEDURES = listOf(
    Procedure("terreno", "Registro de terreno", "Propiedad rural · COFOPRI", ":landscape:", "registro terreno propiedad rural cofopri"),
    Procedure("constancia", "Constancia de posesión", "Municipalidad de tu distrito", ":historyedu:", "constancia posesion municipalidad"),
    Procedure("dni", "DNI — Duplicado", "RENIEC", ":badge:", "dni duplicado reniec documento identidad"),
    Procedure("pension65", "Pensión 65", "MIDIS", ":elderly:", "pension 65 adulto mayor midis"),
    Procedure("herencia", "Herencia / Sucesión", "SUNARP · Notaría", ":accountbalance:", "herencia sucesion familia"),
    Procedure("licencia", "Licencia de funcionamiento", "Municipalidad", ":storefront:", "licencia negocio funcionamiento municipalidad"),
)

/** Minúsculas y sin tildes, para que "pension" encuentre "Pensión". */
private fun normalize(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

@Composable
fun SearchScreen(onBack: () -> Unit, go: (Route) -> Unit, onPick: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = normalize(query.trim())
    val results = if (q.isEmpty()) PROCEDURES else PROCEDURES.filter {
        normalize(it.keys).contains(q) || normalize(it.name).contains(q)
    }

    Screen {
        TopBar(onBack = onBack, title = "Buscar trámite")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 2.dp),
        ) {
            KTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Ej. terreno, DNI, pensión...",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.padding(bottom = 18.dp),
            )
            if (query.isNotEmpty()) {
                SecondaryButton("Limpiar búsqueda", { query = "" }, modifier = Modifier.padding(bottom = 12.dp))
            }
            SectionLabel(
                if (q.isNotEmpty()) "Resultados (${results.size})" else "Trámites frecuentes (${results.size})",
                Modifier.padding(bottom = 4.dp),
            )
            results.forEach { r -> ListItemRow(r.icon, r.name, r.sub) { onPick(r.type) } }
            if (results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KIcon(":search:", size = 40.dp, tint = K.Muted)
                    Text("No encontramos \"$query\"", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = K.Muted, textAlign = TextAlign.Center)
                    Text("Prueba con otra palabra, como \"terreno\" o \"DNI\".", fontSize = 12.5.sp, color = K.Muted, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        MainBottomNav(Route.Search, go)
    }
}

// =============================================================
// 17. PERFIL
// =============================================================
@Composable
fun ProfileScreen(
    store: KuskaStore,
    showUpdated: Boolean,
    onBack: () -> Unit,
    go: (Route) -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val p = remember { store.getProfile() }

    Screen {
        TopBar(onBack = onBack, title = "Mi perfil")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            if (showUpdated) {
                KCard(Modifier.fillMaxWidth().padding(bottom = 14.dp), background = K.GreenSoft, borderColor = K.GreenSoftBorder) {
                    IconLabel(":check:  Tus datos se actualizaron correctamente.", fontSize = 12.5.sp, color = K.GreenDark, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier.padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileAvatar(store.profilePhoto(), Modifier.size(60.dp))
                Column {
                    Text(p.name, fontWeight = FontWeight.ExtraBold, fontFamily = KFont.Title, fontSize = 17.sp, color = K.Ink)
                    Text("DNI ${p.dni} · ${p.place} · ${p.phone}", fontSize = 12.5.sp, color = K.Muted)
                }
            }
            SectionLabel("Cuenta", Modifier.padding(top = 14.dp))
            ListItemRow(null, "Editar datos") { go(Route.EditProfile) }
            ListItemRow(null, "Cambiar contraseña") { go(Route.ForgotPassword) }
            SectionLabel("Preferencias", Modifier.padding(top = 18.dp))
            ListItemRow(null, "Accesibilidad") { go(Route.Accessibility) }
            ListItemRow(null, "Idioma", trailing = "Español", trailingColor = K.Green) {
                context.toast("Quechua y Aimara: disponibles próximamente")
            }
            SectionLabel("Mis datos", Modifier.padding(top = 18.dp))
            ListItemRow(null, "Mis trámites") { go(Route.MyTramites) }
            ListItemRow(null, "Mis documentos") { go(Route.Documents) }
            ListItemRow(null, "Familia") { go(Route.Family) }
            SectionLabel("Otros", Modifier.padding(top = 18.dp))
            ListItemRow(":supportagent:", "Volver a ver el tutorial", "Repetir desde el inicio la guía del modo seleccionado") {
                val simple = store.getPrefs(store.currentUser()?.dni.orEmpty())?.guided == true
                store.restartTutorial(simple = simple)
                go(if (simple) Route.HomeSimple else Route.Home)
            }
            ListItemRow(null, "Contenido sin conexión", trailing = "Activado", trailingColor = K.Green)
            ListItemRow(null, "Notificaciones", trailing = "Activado", trailingColor = K.Green)
            ListItemRow(null, "Privacidad") {
                context.toast("Tus datos se guardan solo en este teléfono")
            }
            Spacer(Modifier.height(24.dp))
            SecondaryButton("Cerrar sesión", onLogout, contentColor = K.ErrorTitle, borderColor = K.ErrorBorder)
            Spacer(Modifier.height(20.dp))
        }
        MainBottomNav(Route.Profile, go)
    }
}

