package pe.kuskago.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.theme.KFont

fun profileInitials(name: String): String = name.trim().split(Regex("\\s+"))
    .mapNotNull { word -> word.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() }
    .take(2).joinToString("").ifEmpty { "K" }

@Composable
fun ProfileAvatar(path: String?, modifier: Modifier = Modifier, name: String = "") {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = null
        value = withContext(Dispatchers.IO) {
            path?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
        }
    }
    val image = if (path.isNullOrBlank()) null else bitmap
    if (image != null) {
        Image(image, "Foto de perfil", modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        BoxWithConstraints(
            modifier.clip(CircleShape)
                .background(Brush.linearGradient(listOf(K.GreenSoft, K.Menta)))
                .border(1.dp, K.GreenSoftBorder, CircleShape)
                .clearAndSetSemantics { contentDescription = if (name.isBlank()) "Perfil de KuskaGo" else "Perfil de $name" },
            contentAlignment = Alignment.Center,
        ) {
            // Las iniciales son un emblema: mantienen su proporción aun con texto grande.
            val size = with(LocalDensity.current) { (maxWidth * 0.36f).toSp() }
            Text(profileInitials(name), color = K.GreenDark, fontWeight = FontWeight.ExtraBold,
                fontFamily = KFont.Title, fontSize = size, maxLines = 1)
        }
    }
}
