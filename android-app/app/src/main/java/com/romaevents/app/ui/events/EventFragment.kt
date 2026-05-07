package com.romaevents.app.ui.events

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.romaevents.app.data.repository.EventRepository
import com.romaevents.app.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventsFragment : Fragment() {

    private val repository = EventRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF7F7F7.toInt())
        }

        val header = TextView(requireContext()).apply {
            text = "Eventi a Roma"
            textSize = 26f
            setTextColor(0xFF1B1B1B.toInt())
            setPadding(28, 28, 28, 12)
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(requireContext()).apply {
            text = "Scopri gli eventi disponibili in città"
            textSize = 15f
            setTextColor(0xFF666666.toInt())
            setPadding(28, 0, 28, 18)
        }

        val loading = TextView(requireContext()).apply {
            text = "Caricamento eventi..."
            textSize = 18f
            gravity = Gravity.CENTER
        }

        root.addView(header)
        root.addView(subtitle)
        root.addView(
            loading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        loadEvents(root, loading)

        return root
    }

    private fun loadEvents(root: LinearLayout, loading: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) {
                    repository.getEvents()
                }

                root.removeView(loading)

                val recyclerView = RecyclerView(requireContext()).apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = EventAdapter(events) { event ->
                        (activity as? MainActivity)?.openEventDetail(event.id)
                    }
                    clipToPadding = false
                    setPadding(0, 4, 0, 24)
                }

                root.addView(
                    recyclerView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                )

            } catch (e: Exception) {
                Log.e("EVENTS", "Errore caricamento eventi", e)

                loading.text = "Errore caricamento eventi:\n${e.message}"
                loading.setPadding(32, 32, 32, 32)
            }
        }
    }
}