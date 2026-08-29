package com.mockgps.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.px
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Double,
    onMapClick: (Double, Double) -> Unit,
    onCameraChange: (Double, Double, Double) -> Unit,
    markers: List<MapMarker> = emptyList(),
    showCurrentLocation: Boolean = true,
    followLocation: Boolean = false
) {
    val context = LocalContext.current
    val mapView = remember { MapViewWrapper(context) }
    
    // Initialize map configuration
    android.util.Log.d("OsmMapView", "Creating map view")
    
    androidx.compose.runtime.DisposableEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = "MockGPS/1.0"
        }
        mapView.initialize()
        onDispose { mapView.onDestroy() }
    }

    // Update camera position
    androidx.compose.runtime.LaunchedEffect(latitude, longitude, zoom, followLocation) {
        mapView.setCenter(latitude, longitude, zoom)
        if (followLocation) {
            mapView.setFollowLocation(true)
        }
    }

    // Update markers
    androidx.compose.runtime.LaunchedEffect(markers) {
        mapView.updateMarkers(markers)
    }

    ComposeCanvas(modifier
        .fillMaxSize()
        .detectTapGestures(onTap = { offset ->
            val (lat, lng) = mapView.getCoordinatesFromPixel(offset.x, offset.y)
            onMapClick(lat, lng)
        })
    ) {
        // This is a placeholder - actual map rendering happens in AndroidView
    }

    AndroidView(
        factory = { mapView.mapView },
        modifier = modifier.fillMaxSize(),
        update = { it } // MapView handles its own updates
    )
}

class MapViewWrapper(private val context: Context) {
    val mapView: MapView = MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        setBuiltInZoomControls(true)
        setClickable(true)
        setFocusable(true)
    }
    
    private val mapController: IMapController = mapView.controller.apply {
        setZoom(15.0)
    }
    
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var compassOverlay: CompassOverlay? = null
    private var scaleBarOverlay: ScaleBarOverlay? = null
    private var followLocation = false

    fun initialize() {
        // My location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
        mapView.overlays.add(myLocationOverlay!!)

        // Compass overlay
        compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
            enableCompass()
        }
        mapView.overlays.add(compassOverlay!!)

        // Scale bar
        scaleBarOverlay = ScaleBarOverlay(context).apply {
            setCentred(true)
            setScaleBarOffset(context.resources.getDimensionPixelSize(org.osmdroid.library.R.dimen.osm_scale_bar_offset))
        }
        mapView.overlays.add(scaleBarOverlay!!)
    }

    fun setCenter(lat: Double, lng: Double, zoom: Double) {
        mapController.animateTo(GeoPoint(lat, lng))
        mapController.setZoom(zoom)
    }

    fun setFollowLocation(enabled: Boolean) {
        followLocation = enabled
        myLocationOverlay?.let {
            if (enabled) it.enableFollowLocation() else it.disableFollowLocation()
        }
    }

    fun updateMarkers(markers: List<MapMarker>) {
        mapView.overlays.removeIf { it is Marker }
        markers.forEach { markerData ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(markerData.latitude, markerData.longitude)
                title = markerData.title
                snippet = markerData.snippet
                icon = createMarkerBitmap(markerData.color)
                anchor = Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM
            }
            marker.setOnMarkerClickListener { _, _ ->
                markerData.onClick?.invoke()
                true
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    private fun createMarkerBitmap(color: Int): Bitmap {
        val size = (48 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            color = color
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius * 0.8f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(radius, radius, radius * 0.4f, paint)
        return bitmap
    }

    fun getCoordinatesFromPixel(x: Float, y: Float): Pair<Double, Double> {
        val point = Point(x.toInt(), y.toInt())
        val geoPoint = mapView.projection.fromPixels(point.x, point.y)
        return geoPoint.latitude to geoPoint.longitude
    }

    fun onDestroy() {
        myLocationOverlay?.disableMyLocation()
        myLocationOverlay?.disableFollowLocation()
        compassOverlay?.disableCompass()
        mapView.onDetach()
    }
}

data class MapMarker(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val snippet: String = "",
    val color: Int = 0xFFFF0000,
    val onClick: (() -> Unit)? = null
)