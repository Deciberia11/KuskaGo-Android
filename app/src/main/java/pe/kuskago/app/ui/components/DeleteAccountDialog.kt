package pe.kuskago.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.theme.K

@Composable
fun DeleteAccountDialog(store: KuskaStore, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val dni = remember { store.currentUser()?.dni.orEmpty() }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("¿Eliminar tu cuenta?") },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Se eliminarán permanentemente tu cuenta, foto de perfil, preferencias, trámites y datos guardados de esta cuenta en este teléfono.")
                Text("Los archivos originales de tu galería o carpeta de descargas se conservarán.")
                FieldLabel("Escribe tu DNI para confirmar")
                KTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(8); error = null },
                    "Tu DNI de 8 dígitos", keyboardType = KeyboardType.Number, enabled = !deleting)
                error?.let { Text(it, color = K.ErrorTitle) }
            }
        },
        dismissButton = {
            TextButton(enabled = !deleting, onClick = onDismiss) { Text("Cancelar") }
        },
        confirmButton = {
            TextButton(enabled = !deleting && dni.length == 8 && confirmation == dni, onClick = {
                deleting = true
                scope.launch {
                    try {
                        if (store.deleteCurrentAccount()) {
                            context.toast("Tu cuenta se eliminó de este teléfono")
                            onDeleted()
                        } else {
                            error = "No se encontró la cuenta. Vuelve a iniciar sesión."
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        error = "No se pudo eliminar la cuenta. Inténtalo nuevamente."
                    } finally {
                        deleting = false
                    }
                }
            }) { Text(if (deleting) "Eliminando…" else "Eliminar cuenta", color = K.ErrorTitle) }
        },
        containerColor = K.White,
    )
}
