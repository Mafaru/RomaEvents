package com.romaevents.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri
import com.google.android.material.button.MaterialButton

class EventDetailFragment : Fragment() {

    private val repository = EventRepository()
    private var eventId: Long = -1L

    companion object {
        private const val ARG_EVENT_ID = "event_id"

        fun newInstance(eventId: Long): EventDetailFragment {
            return EventDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_EVENT_ID, eventId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = requireArguments().getLong(ARG_EVENT_ID)
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scrollView = ScrollView(requireContext()).apply {
            setBackgroundColor(0xFFF7F7F7.toInt())
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 32)
        }

        val loading = TextView(requireContext()).apply {
            text = "Caricamento dettaglio..."
            textSize = 18f
            setPadding(20, 40, 20, 40)
        }

        content.addView(loading)
        scrollView.addView(content)

        loadDetail(content)

        return scrollView
    }

    private fun loadDetail(content: LinearLayout) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    repository.getEventDetail(eventId)
                }

                content.removeAllViews()

                val card = MaterialCardView(requireContext()).apply {
                    radius = 24f
                    cardElevation = 5f
                    setContentPadding(28, 28, 28, 28)
                }

                val box = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                }

                box.addView(TextView(requireContext()).apply {
                    text = detail.title
                    textSize = 25f
                    setTextColor(0xFF1B1B1B.toInt())
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, 20)
                })

                box.addView(TextView(requireContext()).apply {
                    text = detail.category ?: "Categoria non disponibile"
                    textSize = 14f
                    setTextColor(0xFF1A73E8.toInt())
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, 18)
                })

                box.addView(TextView(requireContext()).apply {
                    text = "📅 ${DateUtils.formatDateRange(detail.nextOccurrenceStart, detail.nextOccurrenceEnd)}"
                    textSize = 15f
                    setTextColor(0xFF555555.toInt())
                    setPadding(0, 0, 0, 10)
                })

                box.addView(TextView(requireContext()).apply {
                    text = "📍 ${detail.address ?: "Indirizzo non disponibile"}"
                    textSize = 15f
                    setTextColor(0xFF555555.toInt())
                    setPadding(0, 0, 0, 24)
                })

                box.addView(TextView(requireContext()).apply {
                    text = when (detail.status) {
                        "IN_CORSO" -> "● Evento in corso"
                        "PROSSIMO" -> "● Evento prossimo"
                        "PASSATO" -> "● Evento passato"
                        else -> "● Stato non disponibile"
                    }

                    setTextColor(
                        when (detail.status) {
                            "IN_CORSO" -> 0xFF2E7D32.toInt()
                            "PROSSIMO" -> 0xFF1565C0.toInt()
                            else -> 0xFF757575.toInt()
                        }
                    )

                    textSize = 14f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, 60)
                })

                box.addView(TextView(requireContext()).apply {
                    text = detail.description ?: "Descrizione non disponibile"
                    textSize = 16f
                    setTextColor(0xFF333333.toInt())
                    setLineSpacing(4f, 1.1f)
                    setPadding(0, 0, 0, 30)
                })

                box.addView(MaterialButton(requireContext()).apply {
                    text = "Vedi sulla mappa"
                    setIconResource(android.R.drawable.ic_dialog_map)

                    setOnClickListener {
                        (activity as? MainActivity)?.openMapForEvent(eventId)
                    }
                })

                box.addView(MaterialButton(requireContext()).apply {
                    text = "Apri indicazioni"
                    setIconResource(android.R.drawable.ic_menu_directions)

                    setOnClickListener {
                        val lat = detail.latitude
                        val lon = detail.longitude

                        if (lat == null || lon == null) {
                            Toast.makeText(
                                requireContext(),
                                "Coordinate evento non disponibili",
                                Toast.LENGTH_LONG
                            ).show()
                            return@setOnClickListener
                        }

                        val uri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1" +
                                    "&destination=$lat,$lon" +
                                    "&travelmode=walking"
                        )

                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                })

                box.addView(MaterialButton(requireContext()).apply {
                    text = "Torna agli eventi"
                    setIconResource(android.R.drawable.ic_media_previous)

                    setOnClickListener {
                        (activity as? MainActivity)?.goBackToEvents()
                    }
                })





                card.addView(box)

                content.addView(
                    card,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

            } catch (e: Exception) {
                content.removeAllViews()
                content.addView(TextView(requireContext()).apply {
                    text = "Errore dettaglio:\n${e.message}"
                    textSize = 16f
                    setPadding(32, 32, 32, 32)
                })
            }
        }
    }
}