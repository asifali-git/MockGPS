package com.mockgps.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
    val mapViewWrapper = remember { MapViewWrapper(context) }

    DisposableEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = "MockGPS/1.0"
        }
        mapViewWrapper.initialize()
        onDispose { mapViewWrapper.onDestroy() }
    }

    LaunchedEffect(latitude, longitude, zoom) {
        mapViewWrapper.setCenter(latitude, longitude, zoom)
    }

    LaunchedEffect(followLocation) {
        mapViewWrapper.setFollowLocation(followLocation)
    }

    LaunchedEffect(markers) {
        mapViewWrapper.updateMarkers(markers)
    }

    AndroidView(
        factory = { mapViewWrapper.mapView },
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val (lat, lng) = mapViewWrapper.getCoordinatesFromPixel(offset.x, offset.y)
                    onMapClick(lat, lng)
                }
            },
        update = { /* Map handles its own updates */ }
    )
}

class MapViewWrapper(context: Context) {
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
    private var followLocationEnabled = false

    fun initialize() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(mapView.context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
        mapView.overlays.add(myLocationOverlay!!)

        compassOverlay = CompassOverlay(mapView.context, InternalCompassOrientationProvider(mapView.context), mapView).apply {
            enableCompass()
        }
        mapView.overlays.add(compassOverlay!!)

        scaleBarOverlay = ScaleBarOverlay(mapView.context).apply {
            setCentred(true)
        }
        mapView.overlays.add(scaleBarOverlay!!)
    }

    fun setCenter(lat: Double, lng: Double, zoom: Double) {
        mapController.animateTo(GeoPoint(lat, lng))
        mapController.setZoom(zoom)
    }

    fun setFollowLocation(enabled: Boolean) {
        followLocationEnabled = enabled
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
                setIcon(createMarkerBitmap(markerData.color))
                anchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
        val size = (48 * mapView.context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            this.color = color
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius * 0.8f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(radius, radius, radius * 0.4f, paint)
        return bitmap
    }

    fun getCoordinatesFromPixel(x: Float, y: Float): Pair<Double, Double> {
        val point = Point(x.toInt(), y.toInt())
        val geoPoint = mapView.projection.fromPixels(point.x, point.y) as GeoPoint
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
    val color: Int = -65536,
    val onClick: (() -> Unit)? = null
)