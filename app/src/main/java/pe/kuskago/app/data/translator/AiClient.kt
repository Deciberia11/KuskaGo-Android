package pe.kuskago.app.data.translator

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pe.kuskago.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

/**
 * Explicación con IA para el Traductor de burocracia.
 *
 * Proveedores (por defecto se usa Gemini si hay clave; si no, Ollama):
 *  - OLLAMA: IA gratuita que corre en tu computadora (https://ollama.com).
 *    Sin clave, sin internet externo: el texto no sale de tu red.
 *      · Emulador de Android Studio → http://10.0.2.2:11434  (10.0.2.2 = "localhost" de tu PC)
 *      · Celular real en la misma Wi-Fi → http://IP_DE_TU_PC:11434
 *  - GEMINI (opcional): API de Google con clave gratuita en local.properties (GEMINI_API_KEY).
 *
 * Antes de enviar el texto se ocultan DNI, celulares y correos.
 */
object AiClient {

    enum class Provider { OLLAMA, GEMINI }

    data class Settings(
        // Si hay clave de Gemini en local.properties, se usa Gemini por defecto; si no, Ollama.
        val provider: Provider = if (BuildConfig.GEMINI_API_KEY.isNotBlank()) Provider.GEMINI else Provider.OLLAMA,
        val ollamaUrl: String = BuildConfig.OLLAMA_URL,
        val ollamaModel: String = BuildConfig.OLLAMA_MODEL,
    )

    sealed class Outcome {
        data class Ok(val text: String) : Outcome()
        data class Error(val message: String) : Outcome()
    }

    // ---------------- Configuración guardada en el teléfono ----------------

    private const val PREFS = "kuskago_ai"

    fun load(context: Context): Settings {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaults = Settings()
        return Settings(
            provider = runCatching { Provider.valueOf(sp.getString("provider", defaults.provider.name)!!) }.getOrDefault(defaults.provider),
            ollamaUrl = sp.getString("ollama_url", null)?.takeIf { it.isNotBlank() } ?: defaults.ollamaUrl,
            ollamaModel = sp.getString("ollama_model", null)?.takeIf { it.isNotBlank() } ?: defaults.ollamaModel,
        )
    }

    fun save(context: Context, s: Settings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("provider", s.provider.name)
            .putString("ollama_url", s.ollamaUrl.trim().trimEnd('/'))
            .putString("ollama_model", s.ollamaModel.trim())
            .apply()
    }

    val geminiConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    // ---------------- Instrucciones para la IA ----------------

    private val SYSTEM_PROMPT = """
        Eres el "Traductor de burocracia" de KuskaGo, una app que ayuda a personas de zonas rurales del Perú
        a entender trámites del Estado. Explica el texto oficial que te den en español MUY sencillo
        (nivel primaria), con frases cortas y tratando a la persona de "tú". No inventes datos que no estén en el texto.
        Responde SOLO con este formato, sin markdown ni asteriscos:

        Qué dice:
        (2 a 4 frases simples)

        Qué tienes que hacer:
        - (acción 1)
        - (acción 2)

        Plazo:
        (el plazo, o "No se menciona un plazo")

        Palabras difíciles:
        - palabra: significado simple
    """.trimIndent()

    /** Oculta datos personales antes de enviarlos. */
    fun maskPersonalData(text: String): String = text
        .replace(Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+"), "[CORREO]")
        .replace(Regex("(?<!\\d)9\\d{2}[ -]?\\d{3}[ -]?\\d{3}(?!\\d)"), "[CELULAR]")
        .replace(Regex("(?<!\\d)\\d{8}(?!\\d)"), "[DNI]")

    private fun clean(s: String) = s
        .replace(Regex("(?s)<think>.*?</think>"), "")   // algunos modelos muestran su "razonamiento"
        .replace("**", "").replace("#", "").trim()

    // ---------------- Uso principal ----------------

    suspend fun explain(settings: Settings, text: String): Outcome = withContext(Dispatchers.IO) {
        val safe = "Texto oficial:\n" + maskPersonalData(text.take(6000))
        when (settings.provider) {
            Provider.OLLAMA -> ollamaChat(settings, safe)
            Provider.GEMINI -> geminiChat(safe)
        }
    }

    /** Comprueba que Ollama responde y que el modelo está descargado. */
    suspend fun testOllama(settings: Settings): Outcome = withContext(Dispatchers.IO) {
        try {
            val (code, raw) = http("GET", settings.ollamaUrl.trimEnd('/') + "/api/tags", null, emptyMap(), 8_000)
            if (code !in 200..299) return@withContext Outcome.Error("Ollama respondió con error ($code).")
            val models = JSONObject(raw).optJSONArray("models")
            val names = (0 until (models?.length() ?: 0)).map { models!!.getJSONObject(it).optString("name") }
            val wanted = settings.ollamaModel.trim()
            val found = names.any { it == wanted || (!wanted.contains(":") && it == "$wanted:latest") }
            when {
                names.isEmpty() -> Outcome.Error("Ollama funciona, pero no tiene modelos. En tu PC ejecuta: ollama pull ${settings.ollamaModel}")
                !found ->
                    Outcome.Error("Conectado ✓, pero falta el modelo \"${settings.ollamaModel}\". Modelos en tu PC: ${names.joinToString(", ")}. Ejecuta: ollama pull ${settings.ollamaModel}")
                else -> Outcome.Ok("Conectado ✓ · modelo \"${settings.ollamaModel}\" listo.")
            }
        } catch (e: Exception) {
            Outcome.Error(connectionHelp(settings, e))
        }
    }

    // ---------------- Ollama ----------------

    private fun ollamaChat(s: Settings, userText: String): Outcome {
        val body = JSONObject()
            .put("model", s.ollamaModel)
            .put("stream", false)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", userText)),
            )
            .put("options", JSONObject().put("temperature", 0.3))
        return try {
            // Los modelos locales en CPU pueden tardar: damos hasta 3 minutos.
            val (code, raw) = http("POST", s.ollamaUrl.trimEnd('/') + "/api/chat", body.toString(), emptyMap(), 180_000)
            if (code !in 200..299) {
                val err = runCatching { JSONObject(raw).optString("error") }.getOrDefault("")
                return if (err.contains("not found", ignoreCase = true))
                    Outcome.Error("Falta el modelo \"${s.ollamaModel}\" en tu PC. Ejecuta: ollama pull ${s.ollamaModel}")
                else Outcome.Error("Ollama devolvió un error ($code). $err")
            }
            val answer = clean(JSONObject(raw).optJSONObject("message")?.optString("content").orEmpty())
            if (answer.isBlank()) Outcome.Error("La IA no devolvió una respuesta. Inténtalo otra vez.") else Outcome.Ok(answer)
        } catch (e: java.net.SocketTimeoutException) {
            Outcome.Error("La IA tardó demasiado. Prueba un modelo más liviano (por ejemplo llama3.2:1b).")
        } catch (e: Exception) {
            Outcome.Error(connectionHelp(s, e))
        }
    }

    private fun connectionHelp(s: Settings, e: Exception): String {
        val emulatorHint = if (s.ollamaUrl.contains("10.0.2.2"))
            "Si usas un celular real (no el emulador), cambia la dirección por la IP de tu PC, por ejemplo http://192.168.1.50:11434."
        else
            "En tu PC: 1) abre Ollama, 2) configura OLLAMA_HOST=0.0.0.0 para que acepte conexiones de la red, 3) permite el puerto 11434 en el Firewall de Windows. El celular debe estar en la misma Wi-Fi."
        return "No se pudo conectar con Ollama en ${s.ollamaUrl}. ¿Está abierto en tu computadora? $emulatorHint"
    }

    // ---------------- Gemini (alternativa) ----------------

    private fun geminiChat(userText: String): Outcome {
        if (!geminiConfigured) return Outcome.Error("Gemini no está configurado: falta GEMINI_API_KEY en local.properties.")
        val body = JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", userText)))))
            .put("generationConfig", JSONObject().put("temperature", 0.3).put("maxOutputTokens", 2048))
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent"
            val (code, raw) = http("POST", url, body.toString(), mapOf("x-goog-api-key" to BuildConfig.GEMINI_API_KEY), 45_000)
            if (code !in 200..299) {
                return Outcome.Error(
                    when (code) {
                        400, 403 -> "La clave de Gemini no es válida. Revisa GEMINI_API_KEY en local.properties."
                        404 -> "No se encontró el modelo \"${BuildConfig.GEMINI_MODEL}\"."
                        429 -> "Se alcanzó el límite gratuito de Gemini. Espera un minuto."
                        else -> "Error de Gemini ($code)."
                    }
                )
            }
            val parts = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val answer = buildString {
                if (parts != null) for (i in 0 until parts.length()) {
                    val p = parts.optJSONObject(i) ?: continue
                    if (!p.optBoolean("thought", false)) append(p.optString("text"))
                }
            }
            val c = clean(answer)
            if (c.isBlank()) Outcome.Error("Gemini no devolvió una respuesta.") else Outcome.Ok(c)
        } catch (e: java.net.UnknownHostException) {
            Outcome.Error("Sin conexión a internet.")
        } catch (e: Exception) {
            Outcome.Error("No se pudo usar Gemini: ${e.message ?: "error desconocido"}")
        }
    }

    // ---------------- HTTP ----------------

    private fun http(method: String, url: String, body: String?, headers: Map<String, String>, readTimeoutMs: Int): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 8_000
            conn.readTimeout = readTimeoutMs
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return code to raw
        } finally {
            conn.disconnect()
        }
    }
}
