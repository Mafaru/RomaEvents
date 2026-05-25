package com.romaevents.app.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.romaevents.app.R
import com.romaevents.app.data.repository.EventRepository
import com.romaevents.app.model.EventMapItem
import com.romaevents.app.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.romaevents.app.data.remote.RouteService

class MapFragment : Fragment() {

    private val repository = EventRepository()
    private val routeService = RouteService()

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomTitle: TextView
    private lateinit var bottomAddress: TextView
    private lateinit var bottomDetailButton: MaterialButton
    private lateinit var routeButton: MaterialButton

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    private var userMarker: Marker? = null
    private var routeLine: Polyline? = null
    private var lastUserLocation: GeoPoint? = null

    private var hasLoadedEvents = false
    private var focusEventId: Long? = null
    private var shouldShowRoute: Boolean = false
    private var selectedEventId: Long? = null
    private var userBearing: Float = 0f

    private val fallbackRomaLat = 41.9028
    private val fallbackRomaLon = 12.4964

    private val cartoPositronTileSource = XYTileSource(
        "CartoDB Positron", 0, 20, 512, ".png",
        arrayOf("https://a.basemaps.cartocdn.com/light_all/", "https://b.basemaps.cartocdn.com/light_all/", "https://c.basemaps.cartocdn.com/light_all/")
    )

    companion object {
        private const val ARG_EVENT_ID = "event_id"
        private const val ARG_SHOW_ROUTE = "show_route"
        private const val LOCATION_PERMISSION_REQUEST = 2001

        fun newInstance(eventId: Long, showRoute: Boolean = false): MapFragment {
            return MapFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_EVENT_ID, eventId)
                    putBoolean(ARG_SHOW_ROUTE, showRoute)
                }
            }
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            userBearing = (azimuthDegrees + 360f) % 360f
            userMarker?.icon = createUserLocationIcon(userBearing)
            mapView.invalidate()
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            updateUserMarker(location.latitude, location.longitude)
            if (!hasLoadedEvents && focusEventId == null) {
                hasLoadedEvents = true
                loadEventsOnMap(fallbackRomaLat, fallbackRomaLon)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = requireContext().packageName
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        focusEventId = arguments?.getLong(ARG_EVENT_ID)?.takeIf { it > 0 }
        shouldShowRoute = arguments?.getBoolean(ARG_SHOW_ROUTE) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mapView = MapView(requireContext()).apply {
            setTileSource(cartoPositronTileSource)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            setUseDataConnection(true)
            setBuiltInZoomControls(false)
            controller.setZoom(13.5)
            controller.setCenter(GeoPoint(fallbackRomaLat, fallbackRomaLon))
        }

        val root = FrameLayout(requireContext())
        root.addView(mapView, FrameLayout.LayoutParams(-1, -1))
        root.addView(createTopInfoBox())
        
        // Aggiunta Legenda Premium (Solo Attivo e Prossimo)
        root.addView(createLegendBox())

        bottomSheet = createBottomSheet()
        root.addView(bottomSheet)

        loadEventsOnMap(fallbackRomaLat, fallbackRomaLon)
        hasLoadedEvents = true
        return root
    }

    private fun createLegendBox(): View {
        val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)
        val inactiveGray = Color.parseColor("#333333") // Il "neretto" usato per i marker prossimi

        val card = MaterialCardView(requireContext()).apply {
            radius = 24f
            cardElevation = 8f
            setCardBackgroundColor(Color.parseColor("#E61E1E1E"))
            strokeWidth = 2
            setStrokeColor(ColorStateList.valueOf(0x22FFFFFF))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(32, 0, 0, 64)
            }
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
        }

        // Solo Attivo e Prossimo con i colori corretti (Arancio e Neretto)
        container.addView(createLegendItem("Attivo", orange))
        container.addView(createLegendItem("Prossimo", inactiveGray))

        card.addView(container)
        return card
    }

    private fun createLegendItem(label: String, color: Int): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
            
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(20, 20).apply { setMargins(0, 0, 12, 0) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    // Aggiungiamo bordo bianco al pallino grigio per visibilità
                    if (color == Color.parseColor("#333333")) setStroke(1, Color.WHITE)
                }
            }
            
            val text = TextView(requireContext()).apply {
                text = label
                textSize = 10f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.05f
            }
            
            addView(dot)
            addView(text)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        checkLocationPermission()
        rotationSensor?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopLocationUpdates()
        sensorManager.unregisterListener(sensorListener)
    }

    private fun createTopInfoBox(): View {
        val surfDark = ContextCompat.getColor(requireContext(), R.color.surface_dark)
        val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)
        val textSec = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = roundedBackground(surfDark, 32f).apply { setStroke(2, 0x33FFFFFF.toInt()) }
            elevation = 12f
            addView(TextView(requireContext()).apply {
                text = "MAPPA EVENTI"
                textSize = 18f
                setTextColor(orange)
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                letterSpacing = 0.05f
            })
            addView(TextView(requireContext()).apply {
                text = "Tocca un marker per i dettagli"
                textSize = 12f
                setTextColor(textSec)
                setPadding(0, 4, 0, 0)
            })
            layoutParams = FrameLayout.LayoutParams(-1, -2).apply {
                gravity = Gravity.TOP
                setMargins(24, 24, 24, 0)
            }
        }
    }

    private fun createBottomSheet(): LinearLayout {
        val surfDark = ContextCompat.getColor(requireContext(), R.color.surface_dark)
        val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)
        val white = ContextCompat.getColor(requireContext(), R.color.white)
        val textSec = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        val black = ContextCompat.getColor(requireContext(), R.color.black)

        val sheet = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(34, 32, 34, 36)
            background = roundedBackground(surfDark, 48f).apply { setStroke(3, 0x33FFFFFF.toInt()) }
            elevation = 20f
            visibility = View.GONE
        }

        bottomTitle = TextView(requireContext()).apply {
            textSize = 22f
            setTextColor(white)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }

        bottomAddress = TextView(requireContext()).apply {
            textSize = 15f
            setTextColor(textSec)
            setPadding(0, 0, 0, 32)
        }

        val btnContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        bottomDetailButton = MaterialButton(requireContext()).apply {
            text = "Dettagli"
            cornerRadius = 24
            backgroundTintList = ColorStateList.valueOf(0x1AFFFFFF)
            setTextColor(white)
            strokeWidth = 2
            strokeColor = ColorStateList.valueOf(0x33FFFFFF)
            setOnClickListener {
                selectedEventId?.let { id ->
                    hideBottomSheet()
                    (activity as? MainActivity)?.openEventDetail(id)
                }
            }
        }

        routeButton = MaterialButton(requireContext()).apply {
            text = "Percorso"
            cornerRadius = 24
            backgroundTintList = ColorStateList.valueOf(orange)
            setTextColor(black)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                selectedEventId?.let { id ->
                    val marker = mapView.overlays.filterIsInstance<Marker>().find { it.relatedObject == id }
                    marker?.let { m -> drawRouteToEvent(m.position.latitude, m.position.longitude) }
                }
            }
        }

        btnContainer.addView(bottomDetailButton, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0,0,12,0) })
        btnContainer.addView(routeButton, LinearLayout.LayoutParams(0, -2, 1f))

        sheet.addView(bottomTitle)
        sheet.addView(bottomAddress)
        sheet.addView(btnContainer)
        
        val closeText = TextView(requireContext()).apply {
            text = "CHIUDI"
            textSize = 12f
            setTextColor(orange)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
            setOnClickListener { hideBottomSheet() }
        }
        sheet.addView(closeText)

        sheet.layoutParams = FrameLayout.LayoutParams(-1, -2).apply {
            gravity = Gravity.BOTTOM
            setMargins(24, 0, 24, 32)
        }
        return sheet
    }

    private fun showBottomSheet(eventId: Long, title: String, address: String?) {
        selectedEventId = eventId
        bottomTitle.text = title
        bottomAddress.text = "📍 ${address ?: "Indirizzo non disponibile"}"
        bottomSheet.visibility = View.VISIBLE
        bottomSheet.alpha = 0f
        bottomSheet.translationY = 100f
        bottomSheet.animate().translationY(0f).alpha(1f).setDuration(250).start()
    }

    private fun hideBottomSheet() {
        selectedEventId = null
        bottomSheet.visibility = View.GONE
        routeLine?.let {
            mapView.overlays.remove(it)
            routeLine = null
            mapView.invalidate()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            @Suppress("DEPRECATION")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { updateUserMarker(it.latitude, it.longitude) }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateUserMarker(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)
        lastUserLocation = point
        if (userMarker == null) {
            userMarker = Marker(mapView).apply {
                position = point
                icon = createUserLocationIcon(userBearing)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ -> true }
            }
            mapView.overlays.add(userMarker)
        } else {
            userMarker?.position = point
            userMarker?.icon = createUserLocationIcon(userBearing)
        }
        mapView.invalidate()
    }

    private fun drawRouteToEvent(eventLat: Double, eventLon: Double) {
        val startPoint = lastUserLocation
        val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)
        if (startPoint == null) {
            Toast.makeText(requireContext(), "Posizione non disponibile. Attendi un istante...", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val routePoints = withContext(Dispatchers.IO) {
                    routeService.getWalkingRoute(startPoint.latitude, startPoint.longitude, eventLat, eventLon)
                }
                routeLine?.let { mapView.overlays.remove(it) }
                if (routePoints.isNotEmpty()) {
                    routeLine = Polyline().apply {
                        setPoints(routePoints)
                        width = 14f
                        color = orange
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                    }
                    mapView.overlays.add(routeLine)
                    try { mapView.zoomToBoundingBox(routeLine!!.bounds, true, 200) } catch (e: Exception) { mapView.controller.animateTo(routePoints[0]) }
                }
                mapView.invalidate()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Percorso: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadEventsOnMap(lat: Double, lon: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) { repository.getMapEvents(lat, lon, 10.0) }
                addEventMarkers(events)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Mappa: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addEventMarkers(events: List<EventMapItem>) {
        val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)
        val inactiveColor = Color.parseColor("#333333")
        
        events.forEach { event ->
            val isFocused = focusEventId == event.id
            
            // Logica colori: Attivo (Orange), Altro/Prossimo (Neretto/Gray)
            val markerColor = when {
                isFocused || event.status == "ACTIVE_NOW" || event.status == "IN_CORSO" -> orange
                else -> inactiveColor
            }

            val marker = Marker(mapView).apply {
                position = GeoPoint(event.latitude, event.longitude)
                title = event.title
                icon = createEventMarkerIcon(markerColor)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                relatedObject = event.id
                setOnMarkerClickListener { _, _ ->
                    mapView.controller.animateTo(this.position)
                    if (mapView.zoomLevelDouble < 16.0) mapView.controller.setZoom(16.0)
                    showBottomSheet(event.id, event.title, event.address)
                    true
                }
            }
            mapView.overlays.add(marker)
            if (isFocused) {
                mapView.controller.setZoom(16.5)
                mapView.controller.animateTo(marker.position)
                showBottomSheet(event.id, event.title, event.address)
                if (shouldShowRoute) drawRouteToEvent(event.latitude, event.longitude)
            }
        }
        mapView.invalidate()
    }

    private fun createEventMarkerIcon(color: Int): BitmapDrawable {
        val width = 86; val height = 110
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
        canvas.drawCircle(width / 2f, 36f, 28f, paint)
        val path = Path().apply { moveTo(width / 2f - 20f, 54f); lineTo(width / 2f + 20f, 54f); lineTo(width / 2f, height - 16f); close() }
        canvas.drawPath(path, paint)
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = if (color == Color.parseColor("#333333")) Color.WHITE else Color.BLACK; style = Paint.Style.FILL }
        canvas.drawCircle(width / 2f, 36f, 10f, innerPaint)
        return BitmapDrawable(resources, bitmap)
    }

    private fun createUserLocationIcon(rotationDegrees: Float = 0f): BitmapDrawable {
        val size = 90; val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val userBlue = Color.parseColor("#2196F3")
        canvas.rotate(rotationDegrees, size / 2f, size / 2f)
        val conePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = userBlue; style = Paint.Style.FILL }
        val conePath = Path().apply { moveTo(size / 2f, 6f); lineTo(size / 2f - 18f, size / 2f + 14f); lineTo(size / 2f, size / 2f + 6f); lineTo(size / 2f + 18f, size / 2f + 14f); close() }
        canvas.drawPath(conePath, conePaint)
        canvas.rotate(-rotationDegrees, size / 2f, size / 2f)
        canvas.drawCircle(size / 2f, size / 2f, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = userBlue; style = Paint.Style.FILL })
        canvas.drawCircle(size / 2f, size / 2f, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 6f })
        return BitmapDrawable(resources, bitmap)
    }

    private fun roundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply { setColor(color); cornerRadius = radius }
    }
}
