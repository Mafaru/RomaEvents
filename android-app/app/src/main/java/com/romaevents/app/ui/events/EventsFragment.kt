package com.romaevents.app.ui.events

import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.romaevents.app.R
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
        val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)
        val backgroundDark = ContextCompat.getColor(requireContext(), R.color.background_dark)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundDark)
        }

        val header = TextView(requireContext()).apply {
            text = "EVENTI A ROMA"
            textSize = 28f
            setTextColor(orange)
            setPadding(32, 40, 32, 8)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.05f
        }

        val subtitle = TextView(requireContext()).apply {
            text = "Scopri cosa succede nell'Eterna Città"
            textSize = 15f
            setTextColor(textSecondary)
            setPadding(32, 0, 32, 24)
        }

        val loadingBox = createLoadingBox(orange, textSecondary)

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

    private fun createLoadingBox(accentColor: Int, textColor: Int): LinearLayout {
        val loadingBox = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 48)
        }

        val progressBar = ProgressBar(requireContext()).apply {
            indeterminateTintList = ColorStateList.valueOf(accentColor)
        }

        val loadingText = TextView(requireContext()).apply {
            text = "Caricamento eventi..."
            textSize = 17f
            setTextColor(accentColor)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 6)
        }

        val loadingSubtitle = TextView(requireContext()).apply {
            text = "Sto interrogando i server imperiali"
            textSize = 14f
            setTextColor(textColor)
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

                if (!isAdded) return@launch

                root.removeView(loadingBox)

                val recyclerView = RecyclerView(requireContext()).apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = EventAdapter(events) { event ->
                        (activity as? MainActivity)?.openEventDetail(event.id)
                    }
                    clipToPadding = false
                    setPadding(0, 8, 0, 48)
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
                if (e is CancellationException) throw e

                Log.e("EVENTS", "Errore caricamento eventi", e)

                if (!isAdded) return@launch

                root.removeView(loadingBox)

                val errorBox = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(40, 60, 40, 60)
                }

                errorBox.addView(TextView(requireContext()).apply {
                    text = "Impossibile caricare gli eventi"
                    textSize = 20f
                    setTextColor(ContextCompat.getColor(context, R.color.roma_orange))
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, 12)
                })

                errorBox.addView(TextView(requireContext()).apply {
                    text = "Verifica la tua connessione o riprova tra pochi secondi."
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
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