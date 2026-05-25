package com.romaevents.app.ui.events

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.romaevents.app.R
import com.romaevents.app.data.repository.EventRepository
import com.romaevents.app.ui.main.MainActivity
import com.romaevents.app.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
        val backgroundDark = ContextCompat.getColor(requireContext(), R.color.background_dark)
        val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)

        val scrollView = ScrollView(requireContext()).apply {
            setBackgroundColor(backgroundDark)
            isFillViewport = true
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 48)
        }

        val loading = TextView(requireContext()).apply {
            text = "PREPARAZIONE DETTAGLI..."
            textSize = 14f
            setTextColor(orange)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            letterSpacing = 0.2f
            setPadding(0, 150, 0, 0)
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

                if (!isAdded) return@launch

                content.removeAllViews()

                val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)
                val surfaceDark = ContextCompat.getColor(requireContext(), R.color.surface_dark)
                val white = ContextCompat.getColor(requireContext(), R.color.white)
                val textSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                val black = ContextCompat.getColor(requireContext(), R.color.black)

                // TITOLO HERO
                content.addView(TextView(requireContext()).apply {
                    text = detail.title.uppercase(Locale.ROOT)
                    textSize = 34f
                    setTextColor(orange)
                    typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                    letterSpacing = 0.05f
                    setPadding(0, 0, 0, 32)
                })

                val card = MaterialCardView(requireContext()).apply {
                    radius = 48f
                    cardElevation = 20f
                    setCardBackgroundColor(surfaceDark)
                    strokeWidth = 2
                    setStrokeColor(ColorStateList.valueOf(0x1AFFFFFF))
                    setContentPadding(40, 56, 40, 56)
                }

                val box = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                }

                // CHIP CATEGORIA
                val categoryTag = TextView(requireContext()).apply {
                    text = (detail.category ?: "EVENTO").uppercase(Locale.ROOT)
                    textSize = 11f
                    setTextColor(black)
                    typeface = Typeface.DEFAULT_BOLD
                    letterSpacing = 0.1f
                    setPadding(28, 10, 28, 10)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(orange)
                        cornerRadius = 100f
                    }
                }
                box.addView(LinearLayout(requireContext()).apply { addView(categoryTag) })

                // INFO PRINCIPALI
                box.addView(TextView(requireContext()).apply {
                    text = "📅  ${DateUtils.formatDateRange(detail.nextOccurrenceStart, detail.nextOccurrenceEnd)}"
                    textSize = 17f
                    setTextColor(white)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 40, 0, 14)
                })

                box.addView(TextView(requireContext()).apply {
                    text = "📍  ${detail.address ?: "Roma, Italia"}"
                    textSize = 16f
                    setTextColor(textSecondary)
                    setPadding(0, 0, 0, 32)
                })

                // DIVISORE
                box.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(-1, 2).apply { setMargins(0, 8, 0, 40) }
                    setBackgroundColor(0x1AFFFFFF)
                })

                // DESCRIZIONE
                box.addView(TextView(requireContext()).apply {
                    text = detail.description ?: "Scopri l'incanto di questo evento nel cuore di Roma."
                    textSize = 16f
                    setTextColor(white)
                    setLineSpacing(10f, 1.2f)
                    setPadding(0, 0, 0, 56)
                })

                // BOX METEO PREMIUM
                if (detail.latitude != null && detail.longitude != null) {
                    try {
                        val weather = withContext(Dispatchers.IO) {
                            repository.getWeather(detail.latitude, detail.longitude)
                        }

                        val weatherCard = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(32, 32, 32, 32)
                            background = android.graphics.drawable.GradientDrawable().apply {
                                setColor(0x0DFFFFFF)
                                cornerRadius = 40f
                                setStroke(2, 0x1AFFFFFF)
                            }
                        }

                        weatherCard.addView(TextView(requireContext()).apply {
                            text = "METEO ATTUALE"
                            textSize = 11f
                            setTextColor(orange)
                            typeface = Typeface.DEFAULT_BOLD
                            letterSpacing = 0.2f
                            setPadding(0, 0, 0, 16)
                        })

                        weatherCard.addView(TextView(requireContext()).apply {
                            val desc = weather.description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                            text = "🌡️ ${weather.temperature}°C  |  $desc"
                            textSize = 19f
                            setTextColor(white)
                            typeface = Typeface.DEFAULT_BOLD
                        })

                        box.addView(weatherCard, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 56) })
                    } catch (e: Exception) {
                        Log.e("WEATHER", "Meteo non disponibile")
                    }
                }

                // CTA BUTTONS
                box.addView(MaterialButton(requireContext()).apply {
                    text = "MOSTRA IL PERCORSO"
                    textSize = 16f
                    cornerRadius = 40
                    setTextColor(black)
                    backgroundTintList = ColorStateList.valueOf(orange)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 36, 0, 36)
                    elevation = 12f
                    setOnClickListener {
                        (activity as? MainActivity)?.openMapForEvent(eventId, showRoute = true)
                    }
                })

                box.addView(TextView(requireContext()).apply {
                    text = "TORNA AGLI EVENTI"
                    textSize = 13f
                    setTextColor(textSecondary)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setPadding(0, 48, 0, 0)
                    letterSpacing = 0.15f
                    setOnClickListener {
                        (activity as? MainActivity)?.goBackToEvents()
                    }
                })

                card.addView(box)
                content.addView(card)

            } catch (e: Exception) {
                content.removeAllViews()
                content.addView(TextView(requireContext()).apply {
                    text = "Impossibile caricare i dettagli.\nRiprova più tardi."
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setPadding(40, 150, 40, 0)
                })
            }
        }
    }
}
