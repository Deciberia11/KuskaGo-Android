package pe.kuskago.app.data.translator

import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import pe.kuskago.app.BuildConfig

/** La clave solo se envía al extremo HTTPS de Google, nunca en la URL ni en errores. */
internal object GeminiClient {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models/"

    internal fun normalizedModel(value: String): String? = value.trim().removePrefix("models/")
        .takeIf { it.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]*")) }

    // Comprueba generación real, no solo la existencia del modelo.
    fun test(model: String): AiClient.Outcome = when (val response = explain(
        model, "Responde en español en una sola frase breve.",
        "Explica qué significa subsanar una solicitud."
    )) {
        is AiClient.Outcome.Ok -> AiClient.Outcome.Ok("Gemini generó una respuesta de prueba correctamente. Modelo ${normalizedModel(model)} listo.")
        is AiClient.Outcome.Error -> response
    }

    fun explain(model: String, instructions: String, text: String): AiClient.Outcome {
        if (!AiClient.geminiConfigured) return missingKey()
        val name = normalizedModel(model) ?: return invalidModel()
        val generation = JSONObject().put("temperature", 0.2)
            .put("maxOutputTokens", 4096).put("responseMimeType", "text/plain")
        // Los modelos 2.x no aceptan thinkingLevel.
        if (name.startsWith("gemini-3.") || name.startsWith("gemini-4-")) {
            generation.put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instructions))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", text)))))
            .put("generationConfig", generation)
        return safeRequest {
            val requestUrl = BASE + name + ":generateContent"
            val payload = body.toString()
            var response = AiClient.http("POST", requestUrl, payload, headers(), 90_000)
            // Solo un reintento por errores temporales confirmados; nunca por
            // cuota, permisos o timeout (podría duplicar una generación ya iniciada).
            if (response.first in setOf(502, 503, 504)) {
                Thread.sleep(1_000)
                response = AiClient.http("POST", requestUrl, payload, headers(), 90_000)
            }
            val (code, raw) = response
            if (code !in 200..299) errorResponse(code, raw) else parseAnswer(raw)
        }
    }

    private fun headers() = mapOf("x-goog-api-key" to BuildConfig.GEMINI_API_KEY.trim())
    private fun missingKey() = AiClient.Outcome.Error("Falta la clave de Gemini. Agrega GEMINI_API_KEY en local.properties y vuelve a compilar la app. No publiques esa clave en GitHub.")
    private fun invalidModel() = AiClient.Outcome.Error("Escribe un modelo de Gemini válido, por ejemplo gemini-3.8-flash.")

    internal fun errorResponse(code: Int, raw: String): AiClient.Outcome.Error {
        val details = runCatching { JSONObject(raw).optJSONObject("error")?.toString().orEmpty() }.getOrDefault("")
        val message = when {
            details.contains("API_KEY_INVALID") || details.contains("API_KEY_EXPIRED") ->
                "La clave de Gemini no es válida o ha caducado. Revisa la clave en Google AI Studio."
            code == 401 -> "Gemini no aceptó la clave configurada. Revisa GEMINI_API_KEY."
            code == 403 -> "La clave no tiene permiso para usar Gemini. Revisa el proyecto y las restricciones de la clave en Google AI Studio."
            code == 404 -> "El modelo no está disponible para esta clave o versión de la API. Cambia el modelo y pulsa Probar conexión."
            code == 429 -> "Se alcanzó la cuota o el límite de solicitudes de Gemini. Revisa los límites de tu proyecto en Google AI Studio; esperar no resuelve una cuota agotada."
            code == 400 -> "Gemini rechazó la solicitud. Comprueba el nombre del modelo y la configuración de la clave."
            code in 500..599 -> "Google no pudo generar la respuesta (HTTP $code). Si vuelve a ocurrir, prueba otro modelo de Gemini disponible para tu clave o usa Ollama. No es un error de Wi-Fi confirmado."
            else -> "No se pudo completar la solicitud de Gemini (HTTP $code)."
        }
        return AiClient.Outcome.Error(message)
    }

    internal fun parseAnswer(raw: String): AiClient.Outcome {
        val json = JSONObject(raw)
        val blocked = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        if (blocked.isNotBlank() && blocked != "BLOCK_REASON_UNSPECIFIED") {
            return AiClient.Outcome.Error("Gemini no pudo procesar ese texto por sus filtros. Prueba con un fragmento del documento sin información sensible.")
        }
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            ?: return AiClient.Outcome.Error("Gemini no devolvió una explicación. Prueba con un texto más corto.")
        when (candidate.optString("finishReason")) {
            "MAX_TOKENS" -> return AiClient.Outcome.Error("La explicación se cortó antes de terminar. Prueba con un fragmento más corto del documento.")
            "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII" ->
                return AiClient.Outcome.Error("Gemini bloqueó la explicación. Prueba con otro fragmento sin información sensible.")
        }
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        val answer = buildList {
            if (parts != null) for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (!part.optBoolean("thought", false)) part.optString("text").takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }.joinToString("\n").replace("**", "").replace(Regex("(?m)^#+\\s*"), "").trim()
        return if (answer.isBlank()) AiClient.Outcome.Error("Gemini devolvió una respuesta vacía. Inténtalo otra vez.")
            else AiClient.Outcome.Ok(answer)
    }

    private inline fun safeRequest(block: () -> AiClient.Outcome): AiClient.Outcome = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: java.net.UnknownHostException) {
        AiClient.Outcome.Error("No se pudo encontrar el servidor de Gemini (DNS). Aunque aparezca Wi-Fi, el emulador puede no tener acceso a ese servidor. Comprueba Internet en el navegador del emulador o reinícialo con Cold Boot. Ollama puede seguir funcionando en tu PC.")
    } catch (_: java.net.SocketTimeoutException) {
        AiClient.Outcome.Error("Se agotó el tiempo de espera al conectar o recibir la respuesta de Gemini. Comprueba el acceso a Internet del emulador y vuelve a probar; este error no confirma si la clave es válida.")
    } catch (_: javax.net.ssl.SSLException) {
        AiClient.Outcome.Error("No se pudo establecer una conexión segura con Gemini. Revisa la conexión y la fecha del dispositivo.")
    } catch (_: org.json.JSONException) {
        AiClient.Outcome.Error("La respuesta recibida no tiene el formato esperado de Gemini. Inténtalo nuevamente.")
    } catch (_: Exception) {
        AiClient.Outcome.Error("No se pudo conectar con Gemini. Comprueba tu conexión y pulsa Probar conexión.")
    }
}
