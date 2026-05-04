package com.romaevents.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
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

            val lat = location.latitude
            val lon = location.longitude

            updateUserMarker(lat, lon)

            if (!hasLoadedEvents) {
                hasLoadedEvents = true
                loadEventsOnMap(lat, lon)
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
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(fallbackRomaLat, fallbackRomaLon))
        }

        return FrameLayout(requireContext()).apply {
            addView(
                mapView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
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
        ) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )

        if (!hasLoadedEvents) {
            hasLoadedEvents = true
            loadEventsOnMap(fallbackRomaLat, fallbackRomaLon)
        }
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
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(userMarker)
        } else {
            userMarker?.position = point
        }

        // NON spostiamo automaticamente la mappa sulla posizione utente,
        // altrimenti l'emulatore può portarci in America.
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

    private fun addEventMarkers(events: List<EventMapItem>) {
        events.forEach { event ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(event.latitude, event.longitude)
                title = event.title
                snippet = event.address ?: "Indirizzo non disponibile"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                setOnMarkerClickListener { clickedMarker, _ ->
                    clickedMarker.showInfoWindow()
                    (activity as? MainActivity)?.openEventDetail(event.id)
                    true
                }
            }

            mapView.overlays.add(marker)

            if (focusEventId == event.id) {
                mapView.controller.setZoom(16.0)
                mapView.controller.animateTo(marker.position)
                marker.showInfoWindow()
            }
        }

        mapView.invalidate()
    }
}