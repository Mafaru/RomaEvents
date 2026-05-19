package com.romaevents.app.ui.events

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.romaevents.app.data.repository.EventRepository
import com.romaevents.app.ui.main.MainActivity
import com.romaevents.app.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        inflater: LayoutInflater,
        container: ViewGroup?,
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
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, 20)
                })

                box.addView(TextView(requireContext()).apply {
                    text = detail.category ?: "Categoria non disponibile"
                    textSize = 14f
                    setTextColor(0xFF1A73E8.toInt())
                    typeface = Typeface.DEFAULT_BOLD
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
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, 60)
                })

                box.addView(TextView(requireContext()).apply {
                    text = detail.description ?: "Descrizione non disponibile"
                    textSize = 16f
                    setTextColor(0xFF333333.toInt())
                    setLineSpacing(4f, 1.1f)
                    setPadding(0, 0, 0, 30)
                })

                if (detail.latitude != null && detail.longitude != null) {
                    try {
                        val weather = withContext(Dispatchers.IO) {
                            repository.getWeather(detail.latitude, detail.longitude)
                        }

                        val weatherCard = MaterialCardView(requireContext()).apply {
                            radius = 22f
                            cardElevation = 3f
                            setCardBackgroundColor(0xFFEAF4FF.toInt())
                            setContentPadding(24, 22, 24, 22)
                        }

                        val weatherBox = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                        }

                        weatherBox.addView(TextView(requireContext()).apply {
                            text = "Meteo vicino all'evento"
                            textSize = 17f
                            setTextColor(0xFF1B1B1B.toInt())
                            typeface = Typeface.DEFAULT_BOLD
                            setPadding(0, 0, 0, 10)
                        })

                        weatherBox.addView(TextView(requireContext()).apply {
                            text = "🌡️ ${weather.temperature}°C  •  ${weather.description}"
                            textSize = 16f
                            setTextColor(0xFF333333.toInt())
                            setPadding(0, 0, 0, 8)
                        })

                        weatherBox.addView(TextView(requireContext()).apply {
                            text = "💧 Umidità ${weather.humidity}%   💨 Vento ${weather.windSpeed} m/s"
                            textSize = 14f
                            setTextColor(0xFF555555.toInt())
                        })

                        weatherCard.addView(weatherBox)

                        box.addView(
                            weatherCard,
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 4, 0, 26)
                            }
                        )
                    } catch (e: Exception) {
                        // Ignoriamo errori meteo
                    }
                }

                box.addView(MaterialButton(requireContext()).apply {
                    text = "Vedi percorso sulla mappa"
                    // Usiamo icone standard senza android.R se possibile o specifichiamo bene
                    setIconResource(android.R.drawable.ic_dialog_map)

                    setOnClickListener {
                        (activity as? MainActivity)?.openMapForEvent(eventId, showRoute = true)
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