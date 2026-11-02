package pe.kuskago.app.data.identity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import pe.kuskago.app.data.maps.PlaceFinder
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Lee el texto de la foto del DNI con ML Kit (Google), en el celular y sin internet.
 * La foto no se guarda: se borra apenas se lee.
 */
object DniScanner {
    suspend fun read(context: Context, uri: Uri): DniData = withContext(Dispatchers.Default) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, uri)
            val text = suspendCancellableCoroutine<String> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resume("") }
            }
            DniTextParser.parse(text)
        } catch (e: Exception) {
            DniData()
        } finally {
            recognizer.close()
        }
    }
}

/** Dirección sugerida a partir del GPS. */
data class AddressGuess(
    val address: String?,
    val district: String?,
    val province: String?,
    val department: String?,
) {
    /** Texto completo para un campo de "ubicación" (ej. terreno). */
    val full: String
        get() = listOfNotNull(address, district, province, department).distinct().joinToString(", ")
}

/**
 * Completa la dirección con la ubicación actual:
 *  1. Pide la ubicación al teléfono (GPS o red).
 *  2. Convierte las coordenadas en dirección con el Geocoder de Android
 *     y, si no está disponible, con Nominatim (OpenStreetMap).
 */
object LocationAutofill {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }
        val fresh = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cancel = android.os.CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    lm, provider, cancel, ContextCompat.getMainExecutor(context),
                ) { loc: Location? -> if (cont.isActive) cont.resume(loc) }
            }
        }
        return fresh
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun reverse(context: Context, lat: Double, lon: Double): AddressGuess? {
        androidGeocoder(context, lat, lon)?.let { return it }
        return PlaceFinder.reverse(context, lat, lon)?.let { AddressGuess(it.address, it.district, it.province, it.department) }
    }

    @Suppress("DEPRECATION")
    private suspend fun androidGeocoder(context: Context, lat: Double, lon: Double): AddressGuess? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.forLanguageTag("es-PE"))
        val list = try {
            if (Build.VERSION.SDK_INT >= 33) {
                withTimeoutOrNull(10_000) {
                    suspendCancellableCoroutine<List<android.location.Address>> { cont ->
                        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                if (cont.isActive) cont.resume(addresses.toList())
                            }
                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(emptyList())
                            }
                        })
                    }
                }
            } else {
                withContext(Dispatchers.IO) { geocoder.getFromLocation(lat, lon, 1)?.toList() }
            }
        } catch (e: Exception) {
            null
        }
        val a = list?.firstOrNull() ?: return null
        val street = listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(" ").ifBlank { null }
        return AddressGuess(
            address = listOfNotNull(street, a.subLocality).joinToString(", ").ifBlank { a.getAddressLine(0) },
            district = a.locality ?: a.subAdminArea,
            province = a.subAdminArea?.removePrefix("Provincia de ")?.trim(),
            department = a.adminArea?.removePrefix("Departamento de ")?.trim(),
        )
    }
}
