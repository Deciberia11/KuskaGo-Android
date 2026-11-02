package pe.kuskago.app.data.maps

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Busca lugares reales en el mapa usando Nominatim (buscador gratuito de OpenStreetMap).
 *
 * Reglas de uso de Nominatim que respetamos (https://operations.osmfoundation.org/policies/nominatim/):
 *  - Máximo 1 consulta por segundo  → esperamos 1,1 s entre consultas.
 *  - User-Agent que identifica la app.
 *  - Guardar resultados en caché     → cada búsqueda se hace UNA sola vez y queda en el teléfono.
 *  - Solo búsquedas que pide el usuario (al abrir el mapa), nunca en segundo plano.
 */
object PlaceFinder {

    data class Place(val lat: Double, val lon: Double, val label: String)

    private const val USER_AGENT = "KuskaGo/1.0 (prototipo academico; pe.kuskago.app)"
    private val mutex = Mutex()
    private var lastCall = 0L

    suspend fun find(context: Context, query: String): Place? = withContext(Dispatchers.IO) {
        val cache = context.getSharedPreferences("kuskago_geo", Context.MODE_PRIVATE)
        val key = query.trim().lowercase()
        cache.getString(key, null)?.let { return@withContext decode(it) }

        mutex.withLock {
            val wait = 1100 - (System.currentTimeMillis() - lastCall)
            if (wait > 0) delay(wait)
            lastCall = System.currentTimeMillis()

            try {
                val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=pe" +
                    "&accept-language=es&q=" + URLEncoder.encode(query, "UTF-8")
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                try {
                    if (conn.responseCode !in 200..299) return@withLock null
                    val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                    if (arr.length() == 0) return@withLock null
                    val o = arr.getJSONObject(0)
                    val place = Place(
                        lat = o.getString("lat").toDouble(),
                        lon = o.getString("lon").toDouble(),
                        label = shortLabel(o.optString("display_name")),
                    )
                    cache.edit().putString(key, encode(place)).apply()
                    place
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                null   // sin internet o servicio no disponible: la app sigue funcionando sin el punto
            }
        }
    }

    /** Dirección aproximada de unas coordenadas (geocodificación inversa con Nominatim). */
    data class ReverseResult(val address: String?, val district: String?, val province: String?, val department: String?)

    suspend fun reverse(context: Context, lat: Double, lon: Double): ReverseResult? = withContext(Dispatchers.IO) {
        val cache = context.getSharedPreferences("kuskago_geo", Context.MODE_PRIVATE)
        val key = "rev:%.4f,%.4f".format(java.util.Locale.US, lat, lon)   // ~10 m de precisión
        cache.getString(key, null)?.let { c ->
            val p = c.split("|", limit = 4)
            if (p.size == 4) return@withContext ReverseResult(p[0].ifBlank { null }, p[1].ifBlank { null }, p[2].ifBlank { null }, p[3].ifBlank { null })
        }
        mutex.withLock {
            val wait = 1100 - (System.currentTimeMillis() - lastCall)
            if (wait > 0) delay(wait)
            lastCall = System.currentTimeMillis()
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&accept-language=es&lat=$lat&lon=$lon"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                try {
                    if (conn.responseCode !in 200..299) return@withLock null
                    val a = org.json.JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).optJSONObject("address")
                        ?: return@withLock null
                    fun f(vararg keys: String) = keys.firstNotNullOfOrNull { k -> a.optString(k).takeIf { it.isNotBlank() } }
                    val street = listOfNotNull(f("road", "pedestrian", "footway"), f("house_number")).joinToString(" ").ifBlank { null }
                    val district = f("city_district", "suburb", "town", "village", "city")
                    val result = ReverseResult(
                        address = listOfNotNull(street, f("neighbourhood", "quarter", "hamlet")).joinToString(", ").ifBlank { null },
                        district = district,
                        province = f("county", "city"),
                        department = f("state", "region"),
                    )
                    cache.edit().putString(
                        key,
                        listOf(result.address, result.district, result.province, result.department).joinToString("|") { it.orEmpty().replace("|", " ") },
                    ).apply()
                    result
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /** "Jr. Deustua 458, Puno, Puno, Perú" en vez de la dirección larguísima de OSM. */
    private fun shortLabel(full: String): String =
        full.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3).joinToString(", ")

    private fun encode(p: Place) = "${p.lat}|${p.lon}|${p.label}"

    private fun decode(s: String): Place? = runCatching {
        val parts = s.split("|", limit = 3)
        Place(parts[0].toDouble(), parts[1].toDouble(), parts.getOrElse(2) { "" })
    }.getOrNull()
}
