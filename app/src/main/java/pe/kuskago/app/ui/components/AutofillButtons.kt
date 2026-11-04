package pe.kuskago.app.ui.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import pe.kuskago.app.data.identity.AddressGuess
import pe.kuskago.app.data.identity.DniData
import pe.kuskago.app.data.identity.DniScanner
import pe.kuskago.app.data.identity.LocationAutofill
import pe.kuskago.app.ui.theme.K
import java.io.File

/**
 * Tarjeta "Completar con mi DNI": toma una foto (o elige una de la galería) y lee
 * nombre, número y dirección con ML Kit, en el teléfono y sin internet.
 * Se pueden leer las dos caras: lo de la segunda foto completa lo que faltó en la primera.
 */
@Composable
fun ScanDniCard(onResult: (DniData) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    var accumulated by remember { mutableStateOf(DniData()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }

    fun process(uri: Uri, tempFile: File?) {
        reading = true
        scope.launch {
            val data = DniScanner.read(context, uri)
            tempFile?.delete()                       // no guardamos la foto del DNI
            reading = false
            if (data.isEmpty) {
                context.toast("No pudimos leer el DNI. Toma la foto con buena luz, de frente y sin reflejos.")
            } else {
                accumulated = accumulated.merge(data)
                onResult(accumulated)
                context.toast("Listo. Revisa los datos antes de guardar.")
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) process(uri, photoFile) else photoFile?.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) process(uri, null)
    }

    KCard(modifier.fillMaxWidth(), background = K.SkyBg, borderColor = K.SkyBorder) {
        IconLabel(":badge: Completar con mi DNI", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = K.Ink)
        Text(
            if (reading) "Leyendo tu DNI…"
            else "Toma una foto de tu DNI (las dos caras) y llenamos tus datos. La foto no se guarda.",
            fontSize = 12.5.sp, color = K.SkyText, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(":photocamera: Foto del DNI", {
                if (!reading) {
                    val dir = File(context.cacheDir, "dni").apply { mkdirs() }
                    val f = File(dir, "dni_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                    photoFile = f
                    photoUri = uri
                    camera.launch(uri)
                }
            }, Modifier.weight(1f), minHeight = 44.dp, fontSize = 13.sp)
            SecondaryButton(":image: Elegir foto", {
                if (!reading) gallery.launch("image/*")
            }, Modifier.weight(1f), minHeight = 44.dp, fontSize = 13.sp)
        }
    }
}

/** Botón ":locationon: Usar mi ubicación": completa dirección/distrito con el GPS. */
@Composable
fun UseMyLocationButton(
    onResult: (AddressGuess) -> Unit,
    label: String = ":locationon: Usar mi ubicación actual",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }

    fun locate() {
        working = true
        scope.launch {
            val loc = LocationAutofill.currentLocation(context)
            if (loc == null) {
                working = false
                context.toast("No pudimos obtener tu ubicación. Activa la ubicación (GPS) e inténtalo otra vez.")
                return@launch
            }
            val guess = LocationAutofill.reverse(context, loc.latitude, loc.longitude)
            working = false
            if (guess == null) {
                context.toast("Encontramos tu ubicación, pero no su dirección. Escríbela a mano, por favor.")
            } else {
                onResult(guess)
                context.toast("Dirección aproximada. Revísala y corrígela si hace falta.")
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locate()
        else context.toast("Sin permiso de ubicación no podemos completar la dirección.")
    }

    Column(modifier) {
        SecondaryButton(
            if (working) "Buscando tu ubicación…" else label,
            {
                if (!working) {
                    if (LocationAutofill.hasPermission(context)) locate()
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            },
            minHeight = 44.dp, fontSize = 13.5.sp,
        )
    }
}
