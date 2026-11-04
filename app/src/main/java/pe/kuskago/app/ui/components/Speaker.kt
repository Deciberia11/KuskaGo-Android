package pe.kuskago.app.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale
import java.util.UUID

/** Estado de la lectura y control explícito de detención. */
class Speaker(private val context: Context) {
    var isSpeaking by mutableStateOf(false)
        private set
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var closed = false
    private var session: String? = null
    private val engine = TextToSpeech(context.applicationContext) { status ->
        main.post { if (!closed) ready = status == TextToSpeech.SUCCESS }
    }

    init {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                main.post {
                    if (utteranceId == session + "_last") {
                        session = null
                        isSpeaking = false
                    }
                }
            }
            @Deprecated("Compatibilidad con motores antiguos")
            override fun onError(utteranceId: String?) { failed(utteranceId) }
            override fun onError(utteranceId: String?, errorCode: Int) { failed(utteranceId) }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                main.post { if (session != null && utteranceId?.startsWith(session + "_") == true) stop() }
            }
        })
    }

    private fun failed(id: String?) {
        main.post {
            if (session != null && id?.startsWith(session + "_") == true) {
                stop()
                context.toast("No se pudo reproducir la explicación. Inténtalo otra vez.")
            }
        }
    }

    operator fun invoke(text: String) {
        if (isSpeaking) { stop(); return }
        if (closed || !ready) {
            context.toast("La voz aún no está disponible. Inténtalo en unos segundos.")
            return
        }
        if (text.isBlank()) return
        var language = engine.setLanguage(Locale.forLanguageTag("es-PE"))
        if (language < 0) language = engine.setLanguage(Locale.forLanguageTag("es"))
        if (language < 0) {
            context.toast("No hay una voz en español disponible en este dispositivo.")
            return
        }
        val parts = mutableListOf<String>()
        var remaining = text.trim()
        val limit = TextToSpeech.getMaxSpeechInputLength()
        while (remaining.length > limit) {
            val end = remaining.lastIndexOf(' ', limit).takeIf { it > 0 } ?: limit
            parts += remaining.substring(0, end)
            remaining = remaining.substring(end).trimStart()
        }
        if (remaining.isNotEmpty()) parts += remaining
        val token = UUID.randomUUID().toString()
        session = token
        isSpeaking = true
        parts.forEachIndexed { index, part ->
            val id = token + if (index == parts.lastIndex) "_last" else "_$index"
            if (engine.speak(part, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id) == TextToSpeech.ERROR) {
                failed(id)
                return
            }
        }
    }

    fun stop() {
        session = null
        isSpeaking = false
        engine.stop()
    }

    fun close() {
        closed = true
        stop()
        engine.shutdown()
    }
}

@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember(context) { Speaker(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(speaker, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) speaker.stop()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); speaker.close() }
    }
    return speaker
}
