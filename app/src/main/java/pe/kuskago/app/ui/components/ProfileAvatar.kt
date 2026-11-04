package pe.kuskago.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.kuskago.app.R

@Composable
fun ProfileAvatar(path: String?, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            path?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
        }
    }
    val image = bitmap
    if (image != null) {
        Image(image, "Foto de perfil", modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        Image(painterResource(R.drawable.img_avatar), "Foto de perfil", modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    }
}
