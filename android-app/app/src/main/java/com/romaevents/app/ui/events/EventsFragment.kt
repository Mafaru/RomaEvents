package com.romaevents.app.ui.events

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

/**
 * Fragment per la lista eventi. Versione stabile e performante.
 */
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

        // Header Premium
        val header = TextView(requireContext()).apply {
            text = "EVENTI A ROMA"
            textSize = 28f
            setTextColor(orange)
            setPadding(32, 48, 32, 8)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.05f
        }

        val subtitle = TextView(requireContext()).apply {
            text = "Scopri cosa succede nell'Eterna Città"
            textSize = 14f
            setTextColor(textSecondary)
            setPadding(32, 0, 32, 24)
        }

        root.addView(header)
        root.addView(subtitle)

        // Box di caricamento
        val loadingBox = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 120, 32, 32)
            addView(ProgressBar(requireContext()).apply {
                indeterminateTintList = ColorStateList.valueOf(orange)
            })
            addView(TextView(requireContext()).apply {
                text = "CARICAMENTO..."
                setTextColor(orange)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 24, 0, 0)
            })
        }
        root.addView(loadingBox)

        loadEvents(root, loadingBox)

        return root
    }

    private fun loadEvents(root: LinearLayout, loadingBox: View) {
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
                    setPadding(0, 8, 0, 60)
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
                Log.e("EVENTS", "Errore caricamento", e)

                if (isAdded) {
                    root.removeView(loadingBox)
                    val errorText = TextView(requireContext()).apply {
                        text = "Connessione assente o nessun evento trovato."
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.roma_orange))
                        gravity = Gravity.CENTER
                        setPadding(40, 100, 40, 0)
                    }
                    root.addView(errorText)
                }
            }
        }
    }
}
