package pe.kuskago.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import pe.kuskago.app.data.KuskaStore
import pe.kuskago.app.data.maps.PlaceFinder
import pe.kuskago.app.ui.components.Chip
import pe.kuskago.app.ui.components.KCard
import pe.kuskago.app.ui.components.SecondaryButton
import pe.kuskago.app.ui.components.openUrl
import pe.kuskago.app.ui.theme.K
import pe.kuskago.app.ui.components.KIcon
import java.io.File

/** Centro por defecto si no se puede ubicar el distrito: Plaza de Armas de Puno. */
private val DEFAULT_CENTER = GeoPoint(-15.8402, -70.0219)

private data class MapTarget(val name: String, val icon: String, val query: String, val why: String)

/**
 * Mapa real (OpenStreetMap) con los puntos de atención de la Ruta Ciudadana.
 * Los lugares se buscan en el distrito del usuario (Perfil → Editar datos).
 */
@Composable
fun RouteMapSection(store: KuskaStore) {
    val context = LocalContext.current
    val district = remember { store.getProfile().place.ifBlank { "Puno" } }
    val targets = remember(district) {
        listOf(
            MapTarget("Municipalidad", "🏛️", "Municipalidad $district", "Constancia de posesión"),
            MapTarget("Banco de la Nación", "🏦", "Banco de la Nación $district", "Pago de tasas"),
            MapTarget("COFOPRI", "📋", "COFOPRI $district", "Registrar tu terreno"),
            MapTarget("RENIEC", "🪪", "RENIEC $district", "DNI y actas"),
        )
    }

    var center by remember { mutableStateOf<GeoPoint?>(null) }
    val found = remember { mutableStateMapOf<String, PlaceFinder.Place>() }
    val notFound = remember { mutableStateMapOf<String, Boolean>() }
    var searching by remember { mutableStateOf(true) }
    var focus by remember { mutableStateOf<GeoPoint?>(null) }
    var myLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showMyLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        showMyLocation = result.values.any { it }
    }

    LaunchedEffect(district) {
        val c = PlaceFinder.find(context, "$district, Perú")
        center = c?.let { GeoPoint(it.lat, it.lon) } ?: DEFAULT_CENTER
        for (t in targets) {
            val p = PlaceFinder.find(context, t.query)
            if (p != null) found[t.name] = p else notFound[t.name] = true
        }
        searching = false
    }

    val markers = targets.mapNotNull { t -> found[t.name]?.let { t.name to GeoPoint(it.lat, it.lon) } }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, K.Border, RoundedCornerShape(16.dp)),
        ) {
            OsmMap(
                modifier = Modifier.fillMaxSize(),
                center = center,
                markers = markers,
                focus = focus,
                showMyLocation = showMyLocation,
                onMyLocation = { myLocation = it },
            )
        }
        Text(
            if (searching) "Buscando puntos de atención en $district…" else "Mapa: © colaboradores de OpenStreetMap",
            fontSize = 11.sp, color = K.Muted2, modifier = Modifier.padding(start = 22.dp, top = 4.dp, bottom = 8.dp),
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            if (!showMyLocation) {
                SecondaryButton("📍 Usar mi ubicación", {
                    permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, minHeight = 44.dp, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
            } else if (myLocation != null) {
                SecondaryButton("📍 Ir a mi ubicación", { focus = myLocation }, minHeight = 44.dp, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
            }

            targets.forEach { t ->
                val p = found[t.name]
                KCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KIcon(t.icon, size = 26.dp, tint = K.Green)
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = K.Ink)
                            Text(t.why, fontSize = 12.sp, color = K.Green, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    p != null -> p.label
                                    notFound[t.name] == true -> "No lo encontramos en el mapa. Usa \"Cómo llegar\" para buscarlo."
                                    else -> "Buscando…"
                                },
                                fontSize = 12.sp, color = K.Muted,
                            )
                        }
                        val me = myLocation
                        if (p != null && me != null) {
                            Chip(distanceLabel(me, p), K.SkyBg, K.SkyText)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (p != null) {
                            SecondaryButton("Ver en el mapa", { focus = GeoPoint(p.lat, p.lon) }, Modifier.weight(1f), minHeight = 42.dp, fontSize = 13.sp)
                        }
                        SecondaryButton("Cómo llegar", {
                            val url = if (p != null)
                                "https://www.google.com/maps/dir/?api=1&destination=${p.lat},${p.lon}"
                            else
                                "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(t.query)
                            context.openUrl(url)
                        }, Modifier.weight(1f), minHeight = 42.dp, fontSize = 13.sp)
                    }
                }
            }
            Text(
                "Los puntos se buscan en tu distrito ($district). Puedes cambiarlo en Perfil → Editar datos.",
                fontSize = 11.5.sp, color = K.Muted2, modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }
}

private fun distanceLabel(from: GeoPoint, to: PlaceFinder.Place): String {
    val out = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, to.lat, to.lon, out)
    val m = out[0]
    return if (m < 1000) "${m.toInt()} m" else String.format("%.1f km", m / 1000f)
}

/** MapView de osmdroid dentro de Compose. */
@Composable
private fun OsmMap(
    modifier: Modifier,
    center: GeoPoint?,
    markers: List<Pair<String, GeoPoint>>,
    focus: GeoPoint?,
    showMyLocation: Boolean,
    onMyLocation: (GeoPoint) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(15.0)
            controller.setCenter(DEFAULT_CENTER)
            overlays.add(CopyrightOverlay(context))
        }
    }
    val locationOverlay = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            locationOverlay.value?.disableMyLocation()
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(center) {
        center?.let { mapView.controller.setCenter(it) }
    }
    LaunchedEffect(focus) {
        focus?.let {
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(it)
        }
    }
    LaunchedEffect(showMyLocation) {
        if (showMyLocation && locationOverlay.value == null) {
            val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
            overlay.enableMyLocation()
            overlay.runOnFirstFix {
                val p = overlay.myLocation ?: return@runOnFirstFix
                mapView.post { onMyLocation(GeoPoint(p.latitude, p.longitude)) }
            }
            mapView.overlays.add(overlay)
            locationOverlay.value = overlay
            mapView.invalidate()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            mv.overlays.removeAll { it is Marker }
            markers.forEach { (title, point) ->
                mv.overlays.add(
                    Marker(mv).apply {
                        position = point
                        this.title = title
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                )
            }
            mv.invalidate()
        },
    )
}
