package com.romaevents.app.ui.profile

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.romaevents.app.R
import com.romaevents.app.data.session.SessionManager
import com.romaevents.app.ui.auth.LoginActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val session = SessionManager(requireContext())
        
        val orange = ContextCompat.getColor(requireContext(), R.color.roma_orange)
        val bgDark = ContextCompat.getColor(requireContext(), R.color.background_dark)
        val surfDark = ContextCompat.getColor(requireContext(), R.color.surface_dark)
        val white = ContextCompat.getColor(requireContext(), R.color.white)
        val textSec = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        val black = ContextCompat.getColor(requireContext(), R.color.black)

        val scroll = ScrollView(requireContext()).apply {
            setBackgroundColor(bgDark)
            isFillViewport = true
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 0, 32, 40) 
        }

        val logo = ImageView(requireContext()).apply {
            setImageResource(R.drawable.logo_roma_events)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // Alzato drasticamente tutto il contenuto sotto il logo
        root.addView(
            logo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                580 
            ).apply {
                setMargins(0, -80, 0, -200) // Ridotta altezza logo e alzato il blocco sotto di 200px
            }
        )

        root.addView(TextView(requireContext()).apply {
            text = "IL TUO PROFILO"
            textSize = 28f
            setTextColor(orange)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
        })

        root.addView(TextView(requireContext()).apply {
            text = "Area utente Roma Events"
            textSize = 14f
            setTextColor(textSec)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 16) // Spazio ridotto tra sottotitolo e card
        })

        val card = MaterialCardView(requireContext()).apply {
            radius = 48f
            cardElevation = 15f
            setCardBackgroundColor(surfDark)
            strokeWidth = 2
            setStrokeColor(ColorStateList.valueOf(0x33FFFFFF))
            setContentPadding(48, 48, 48, 48)
        }

        val cardContent = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        cardContent.addView(TextView(requireContext()).apply {
            text = "👤 ${session.getUsername() ?: "Utente"}"
            textSize = 22f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 12)
        })

        cardContent.addView(TextView(requireContext()).apply {
            text = "✉️ ${session.getEmail() ?: "Email non disponibile"}"
            textSize = 16f
            setTextColor(textSec)
            setPadding(0, 0, 0, 32)
        })

        val logoutButton = MaterialButton(requireContext()).apply {
            text = "LOGOUT"
            textSize = 15f
            cornerRadius = 24
            setTextColor(black)
            backgroundTintList = ColorStateList.valueOf(orange)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 32, 0, 32)
            elevation = 8f
        }

        logoutButton.setOnClickListener {
            session.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        cardContent.addView(logoutButton)
        card.addView(cardContent)

        root.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        scroll.addView(root)

        return scroll
    }
}
