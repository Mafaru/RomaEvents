package com.romaevents.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private val repository = EventRepository()

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomTitle: TextView
    private lateinit var bottomAddress: TextView
    private lateinit var bottomDetailButton: Button

    private var userMarker: Marker? = null
    private var hasLoadedEvents = false
    private var focusEventId: Long? = null
    private var selectedEventId: Long? = null

    private val fallbackRomaLat = 41.9028
    private val fallbackRomaLon = 12.4964

    companion object {
        private const val ARG_EVENT_ID = "event_id"
        private const val LOCATION_PERMISSION_REQUEST = 2001

        fun newInstance(eventId: Long): MapFragment {
            return MapFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_EVENT_ID, eventId)
                }
            }
        }
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
        focusEventId = arguments?.getLong(ARG_EVENT_ID)?.takeIf { it > 0 }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mapView = MapView(requireContext()).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.5)
            controller.setCenter(GeoPoint(fallbackRomaLat, fallbackRomaLon))
        }

        val root = FrameLayout(requireContext())

        root.addView(
            mapView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(createTopInfoBox())

        bottomSheet = createBottomSheet()
        root.addView(bottomSheet)

        if (focusEventId != null) {
            loadSingleEventOnMap(focusEventId!!)
        } else {
            loadEventsOnMap(fallbackRomaLat, fallbackRomaLon)
            hasLoadedEvents = true
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        checkLocationPermission()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopLocationUpdates()
    }

    private fun createTopInfoBox(): View {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 20, 28, 20)
            background = roundedBackground(0xEEFFFFFF.toInt(), 32f)
            elevation = 8f
        }

        box.addView(TextView(requireContext()).apply {
            text = "Mappa eventi"
            textSize = 20f
            setTextColor(0xFF1B1B1B.toInt())
            typeface = Typeface.DEFAULT_BOLD
        })

        box.addView(TextView(requireContext()).apply {
            text = "Tocca un marker per vedere i dettagli"
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 0)
        })

        return box.apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
                setMargins(24, 24, 24, 0)
            }
        }
    }

    private fun createBottomSheet(): LinearLayout {
        val sheet = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(34, 28, 34, 30)
            background = roundedBackground(Color.WHITE, 42f)
            elevation = 16f
            visibility = View.GONE
        }

        bottomTitle = TextView(requireContext()).apply {
            textSize = 21f
            setTextColor(0xFF1B1B1B.toInt())
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 12)
        }

        bottomAddress = TextView(requireContext()).apply {
            textSize = 15f
            setTextColor(0xFF555555.toInt())
            setPadding(0, 0, 0, 22)
        }

        bottomDetailButton = Button(requireContext()).apply {
            text = "Dettaglio evento"
            setOnClickListener {
                selectedEventId?.let { id ->
                    hideBottomSheet()
                    (activity as? MainActivity)?.openEventDetail(id)
                }
            }
        }

        val closeText = TextView(requireContext()).apply {
            text = "Chiudi"
            textSize = 14f
            setTextColor(0xFF777777.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 0)
            setOnClickListener {
                hideBottomSheet()
            }
        }

        sheet.addView(bottomTitle)
        sheet.addView(bottomAddress)
        sheet.addView(bottomDetailButton)
        sheet.addView(closeText)

        sheet.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            setMargins(24, 0, 24, 24)
        }

        return sheet
    }

    private fun showBottomSheet(eventId: Long, title: String, address: String?) {
        selectedEventId = eventId
        bottomTitle.text = title
        bottomAddress.text = "📍 ${address ?: "Indirizzo non disponibile"}"
        bottomSheet.visibility = View.VISIBLE
        bottomSheet.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(180)
            .start()
    }

    private fun hideBottomSheet() {
        selectedEventId = null
        bottomSheet.visibility = View.GONE
    }

    private fun checkLocationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startLocationUpdates()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateUserMarker(lat: Double, lon: Double) {
        val point = GeoPoint(lat, lon)

        if (userMarker == null) {
            userMarker = Marker(mapView).apply {
                title = "Tu sei qui"
                position = point
                icon = createUserLocationIcon()
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }

            mapView.overlays.add(userMarker)
        } else {
            userMarker?.position = point
        }

        mapView.invalidate()
    }

    private fun loadEventsOnMap(lat: Double, lon: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) {
                    repository.getMapEvents(lat, lon, 10.0)
                }

                addEventMarkers(events)

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Errore caricamento mappa: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadSingleEventOnMap(eventId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    repository.getEventDetail(eventId)
                }

                val lat = detail.latitude
                val lon = detail.longitude

                if (lat == null || lon == null) {
                    Toast.makeText(
                        requireContext(),
                        "Coordinate evento non disponibili",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val point = GeoPoint(lat, lon)

                val marker = Marker(mapView).apply {
                    position = point
                    title = detail.title
                    snippet = detail.address ?: "Indirizzo non disponibile"
                    icon = createEventMarkerIcon(0xFFFF9800.toInt())
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    setOnMarkerClickListener { _, _ ->
                        showBottomSheet(
                            eventId = detail.id,
                            title = detail.title,
                            address = detail.address
                        )
                        true
                    }
                }

                mapView.overlays.add(marker)
                mapView.controller.setZoom(17.0)
                mapView.controller.animateTo(point)
                showBottomSheet(
                    eventId = detail.id,
                    title = detail.title,
                    address = detail.address
                )
                mapView.invalidate()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Errore apertura evento sulla mappa: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addEventMarkers(events: List<EventMapItem>) {
        events.forEach { event ->
            val markerColor = when (event.status) {
                "ACTIVE_NOW", "IN_CORSO" -> 0xFF2E7D32.toInt()
                else -> 0xFFE53935.toInt()
            }

            val marker = Marker(mapView).apply {
                position = GeoPoint(event.latitude, event.longitude)
                title = event.title
                snippet = event.address ?: "Indirizzo non disponibile"
                icon = createEventMarkerIcon(markerColor)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                setOnMarkerClickListener { _, _ ->
                    mapView.controller.animateTo(GeoPoint(event.latitude, event.longitude))
                    showBottomSheet(
                        eventId = event.id,
                        title = event.title,
                        address = event.address
                    )
                    true
                }
            }

            mapView.overlays.add(marker)
        }

        mapView.invalidate()
    }

    private fun createEventMarkerIcon(color: Int): BitmapDrawable {
        val width = 82
        val height = 104
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0x44000000
            style = Paint.Style.FILL
        }

        canvas.drawOval(
            width / 2f - 18f,
            height - 18f,
            width / 2f + 18f,
            height - 8f,
            shadowPaint
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        canvas.drawCircle(width / 2f, 34f, 25f, paint)

        val path = Path().apply {
            moveTo(width / 2f - 17f, 52f)
            lineTo(width / 2f + 17f, 52f)
            lineTo(width / 2f, height - 14f)
            close()
        }

        canvas.drawPath(path, paint)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.FILL
        }

        canvas.drawCircle(width / 2f, 34f, 9f, innerPaint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun createUserLocationIcon(): BitmapDrawable {
        val size = 72
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x331565C0
            style = Paint.Style.FILL
        }

        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1565C0.toInt()
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        canvas.drawCircle(size / 2f, size / 2f, 30f, outerPaint)
        canvas.drawCircle(size / 2f, size / 2f, 14f, mainPaint)
        canvas.drawCircle(size / 2f, size / 2f, 14f, strokePaint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun roundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }
}