package pe.kuskago.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.kuskago.app.Route
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.ui.components.*
import pe.kuskago.app.ui.theme.K

private data class GuideSection(val title: String, val steps: List<String>, val action: String, val route: Route)

private val guideSections = listOf(
    GuideSection("Preferencias y accesibilidad", listOf(
        "Elige modo normal para ver todas las herramientas o sencillo para tener menos opciones y botones grandes.",
        "En Accesibilidad puedes ajustar texto, voz, contraste, lenguaje y traductor. Usa Guía para elegir mis preferencias si necesitas una explicación.",
        "Revisa la vista previa antes de continuar. Puedes volver a cambiar las opciones desde Mi perfil."
    ), "Revisar accesibilidad", Route.Accessibility),
    GuideSection("Buscar y preparar un trámite", listOf(
        "Abre Buscar trámite y escribe lo que necesitas, por ejemplo DNI o terreno.",
        "Selecciona el trámite, completa los datos solicitados y revisa los requisitos.",
        "Revisa el documento antes de guardarlo o imprimirlo. Prepararlo en KuskaGo no lo presenta automáticamente ante la entidad."
    ), "Buscar trámite", Route.Search),
    GuideSection("Mis trámites", listOf(
        "Abre Mis trámites para encontrar los que guardaste.",
        "Selecciona uno para consultar su avance y continuar con el siguiente paso.",
        "Actualiza su estado cuando corresponda. Confirma las respuestas oficiales con la entidad."
    ), "Ver mis trámites", Route.MyTramites),
    GuideSection("Mis documentos", listOf(
        "Agrega un PDF o una foto del teléfono, o crea un documento de texto.",
        "Selecciona un archivo para verlo, compartirlo o reemplazarlo.",
        "Revisa el nombre antes de eliminarlo. Los documentos se conservan en este teléfono."
    ), "Ver mis documentos", Route.Documents),
    GuideSection("Mi familia", listOf(
        "Abre Mi familia y agrega los datos del familiar.",
        "Comprueba que el DNI tenga ocho dígitos y revisa los datos antes de guardar.",
        "Desde este apartado puedes iniciar el trámite familiar de herencia."
    ), "Ver mi familia", Route.Family),
    GuideSection("Ruta Ciudadana", listOf(
        "Consulta el orden de los pasos y abre cada uno para ver su explicación.",
        "Marca los pasos que ya completaste para conservar tu avance.",
        "Abre el mapa para ubicar oficinas. Si autorizas tu ubicación, podrás consultar distancias y cómo llegar. El mapa necesita internet."
    ), "Ver Ruta Ciudadana", Route.CitizenRoute),
    GuideSection("KuskaAlert y avisos", listOf(
        "Abre Avisos o KuskaAlert para revisar los recordatorios de tus trámites.",
        "Usa los filtros Todas, Importantes o Urgentes.",
        "Lee el aviso y abre la acción indicada. Los recordatorios se generan a partir de lo registrado en Mis trámites."
    ), "Ver avisos", Route.Alerts),
    GuideSection("Traductor de burocracia", listOf(
        "Pega un texto o carga un archivo de texto para entender palabras difíciles.",
        "El modo sin internet explica términos con el diccionario incluido. El modo con IA necesita conexión con el servicio configurado.",
        "Pulsa Escuchar explicación para oírla y Detener explicación para parar. Revisa los plazos y requisitos con la entidad."
    ), "Abrir traductor", Route.Translator),
    GuideSection("Mi perfil y tutoriales", listOf(
        "En Mi perfil puedes editar tus datos y cambiar la foto eligiendo una imagen del teléfono.",
        "Abre Accesibilidad para cambiar tus preferencias.",
        "Guía completa de KuskaGo abre esta ayuda. Volver a ver el tutorial repite el recorrido del modo que elegiste."
    ), "Ir a mi perfil", Route.Profile),
    GuideSection("Pedir ayuda y soporte humano", listOf(
        "Pedir ayuda ofrece orientación automática según tu problema; no es una conversación con una persona.",
        "Elige Hablar con una persona para escribir por WhatsApp o abrir el marcador con el 939 138 494.",
        "Puedes describir el problema antes de abrir WhatsApp. Tú revisas y envías el mensaje. La respuesta depende de la disponibilidad del soporte."
    ), "Pedir ayuda", Route.Help),
)

@Composable
fun CompleteGuideScreen(store: KuskaStore, onBack: () -> Unit, go: (Route) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    Screen {
        TopBar(onBack = onBack, title = "Guía completa de KuskaGo")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Elige un apartado para ver sus pasos. Esta guía siempre estará en Mi perfil.", color = K.Muted)
            guideSections.forEach { section ->
                val open = expanded == section.title
                Column {
                    SecondaryButton("${if (open) "−" else "+"} ${section.title}", {
                        expanded = if (open) null else section.title
                    })
                    if (open) KCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(section.title, color = K.Ink, fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() })
                        section.steps.forEachIndexed { i, text ->
                            Text("${i + 1}. $text", color = K.Ink, fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp))
                        }
                        PrimaryButton(section.action, { go(section.route) })
                    }
                }
            }
            SecondaryButton("Repetir tutorial de Inicio", {
                val simple = store.getPrefs(store.currentUser()?.dni.orEmpty())?.guided == true
                store.restartTutorial(simple)
                go(if (simple) Route.HomeSimple else Route.Home)
            })
            SecondaryButton("Hablar con una persona", { go(Route.Support) })
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Solo prepara el contacto: no envía mensajes ni inicia llamadas automáticamente. */
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var problem by rememberSaveable { mutableStateOf("") }
    fun openContact(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.toast("No se pudo abrir el contacto. Puedes llamar al 939 138 494 desde tu teléfono.")
        } catch (_: SecurityException) {
            context.toast("No se pudo abrir el contacto. El número de soporte es 939 138 494.")
        }
    }
    Screen {
        TopBar(onBack = onBack, title = "Hablar con una persona")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Soporte de KuskaGo", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = K.Ink)
            Text("939 138 494", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = K.GreenDark)
            Text("Elige cómo comunicarte. La atención depende de la disponibilidad del soporte.", color = K.Muted)
            FieldLabel("¿Qué problema tienes? (opcional)")
            KTextField(problem, { problem = it.take(1500) }, "Por ejemplo: no encuentro mi documento.",
                singleLine = false, minLines = 4)
            Text("Describe lo que ocurrió. No incluyas contraseñas ni documentos personales.", color = K.Muted, fontSize = 13.sp)
            PrimaryButton("Escribir por WhatsApp", {
                val text = "Hola, necesito ayuda con KuskaGo." + problem.trim().let { if (it.isEmpty()) "" else "\nMi problema: $it" }
                openContact(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/51939138494?text=${Uri.encode(text)}")))
            })
            SecondaryButton("Llamar al 939 138 494", {
                openContact(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+51939138494")))
            })
            Text("WhatsApp necesita internet y abre el mensaje para que lo revises y envíes. Llamar abre el marcador; tú decides cuándo iniciar la llamada.", color = K.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}
