package com.romaevents.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

    private var userMarker: Marker? = null
    private var hasLoadedEvents = false
    private var focusEventId: Long? = null

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
            background = roundedBackground(0xEEFFFFFF.toInt())
        }

        box.addView(TextView(requireContext()).apply {
            text = "Mappa eventi"
            textSize = 20f
            setTextColor(0xFF1B1B1B.toInt())
            typeface = Typeface.DEFAULT_BOLD
        })

        box.addView(TextView(requireContext()).apply {
            text = "Tocca un marker per vedere le informazioni"
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
                icon = createMarkerIcon(0xFF1565C0.toInt())
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
                    icon = createMarkerIcon(0xFFFF9800.toInt())
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    setOnMarkerClickListener { _, _ ->
                        showEventPopup(
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
                icon = createMarkerIcon(markerColor)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                setOnMarkerClickListener { _, _ ->
                    showEventPopup(
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

    private fun showEventPopup(eventId: Long, title: String, address: String?) {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 28)
            background = roundedBackground(Color.WHITE)
        }

        content.addView(TextView(requireContext()).apply {
            text = title
            textSize = 21f
            setTextColor(0xFF1B1B1B.toInt())
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 18)
        })

        content.addView(TextView(requireContext()).apply {
            text = "📍 ${address ?: "Indirizzo non disponibile"}"
            textSize = 15f
            setTextColor(0xFF555555.toInt())
            setPadding(0, 0, 0, 28)
        })

        val dialog = AlertDialog.Builder(requireContext())
            .setView(content)
            .setPositiveButton("Dettaglio evento") { _, _ ->
                (activity as? MainActivity)?.openEventDetail(eventId)
            }
            .setNegativeButton("Chiudi", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog.show()
    }

    private fun createMarkerIcon(color: Int): BitmapDrawable {
        val width = 72
        val height = 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        canvas.drawCircle(width / 2f, 34f, 24f, paint)

        val path = Path().apply {
            moveTo(width / 2f - 16f, 52f)
            lineTo(width / 2f + 16f, 52f)
            lineTo(width / 2f, height - 10f)
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

    private fun roundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 32f
        }
    }
}