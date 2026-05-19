package com.romaevents.app.ui.events

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.romaevents.app.data.repository.EventRepository
import com.romaevents.app.ui.main.MainActivity
import kotlinx.coroutines.CancellationException
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
            setBackgroundColor(0xFFF7FAFF.toInt())
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

        val loadingBox = createLoadingBox()

        root.addView(header)
        root.addView(subtitle)
        root.addView(
            loadingBox,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        loadEvents(root, loadingBox)

        return root
    }

    private fun createLoadingBox(): LinearLayout {
        val loadingBox = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 48)
        }

        val progressBar = ProgressBar(requireContext())

        val loadingText = TextView(requireContext()).apply {
            text = "Caricamento eventi..."
            textSize = 17f
            setTextColor(0xFF1565C0.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 6)
        }

        val loadingSubtitle = TextView(requireContext()).apply {
            text = "Sto recuperando gli eventi da Roma Events"
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            gravity = Gravity.CENTER
        }

        loadingBox.addView(progressBar)
        loadingBox.addView(loadingText)
        loadingBox.addView(loadingSubtitle)

        return loadingBox
    }

    private fun loadEvents(root: LinearLayout, loadingBox: LinearLayout) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val events = withContext(Dispatchers.IO) {
                    repository.getEvents()
                }

                if (!isAdded) {
                    return@launch
                }

                root.removeView(loadingBox)

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
                if (e is CancellationException) {
                    throw e
                }

                Log.e("EVENTS", "Errore caricamento eventi", e)

                if (!isAdded) {
                    return@launch
                }

                root.removeView(loadingBox)

                val errorBox = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(40, 60, 40, 60)
                }

                errorBox.addView(TextView(requireContext()).apply {
                    text = "Impossibile caricare gli eventi"
                    textSize = 20f
                    setTextColor(0xFFB00020.toInt())
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 12)
                })

                errorBox.addView(TextView(requireContext()).apply {
                    text = "Se il server era inattivo, attendi qualche secondo e riapri la schermata."
                    textSize = 15f
                    setTextColor(0xFF666666.toInt())
                    gravity = Gravity.CENTER
                })

                root.addView(
                    errorBox,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                )
            }
        }
    }
}