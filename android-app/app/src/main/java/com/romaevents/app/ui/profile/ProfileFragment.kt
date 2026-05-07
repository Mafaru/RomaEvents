package com.romaevents.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.romaevents.app.R
import com.romaevents.app.data.session.SessionManager
import com.romaevents.app.ui.auth.LoginActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val session = SessionManager(requireContext())

        val scroll = ScrollView(requireContext()).apply {
            setBackgroundColor(0xFFF7FAFF.toInt())
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 42, 32, 42)
        }

        val logo = ImageView(requireContext()).apply {
            setImageResource(R.drawable.logo_roma_events)
            adjustViewBounds = true
        }

        root.addView(
            logo,
            LinearLayout.LayoutParams(400, 400).apply {
                setMargins(0, 0, 0, 8)
            }
        )

        root.addView(TextView(requireContext()).apply {
            text = "Profilo"
            textSize = 30f
            setTextColor(0xFF1565C0.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        root.addView(TextView(requireContext()).apply {
            text = "Area utente Roma Events"
            textSize = 15f
            setTextColor(0xFF666666.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        })

        val card = MaterialCardView(requireContext()).apply {
            radius = 28f
            cardElevation = 6f
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            setContentPadding(32, 32, 32, 32)
        }

        val cardContent = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        cardContent.addView(TextView(requireContext()).apply {
            text = "👤 ${session.getUsername() ?: "Utente"}"
            textSize = 20f
            setTextColor(0xFF1B1B1B.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 14)
        })

        cardContent.addView(TextView(requireContext()).apply {
            text = "✉️ ${session.getEmail() ?: ""}"
            textSize = 16f
            setTextColor(0xFF555555.toInt())
            setPadding(0, 0, 0, 24)
        })

        cardContent.addView(TextView(requireContext()).apply {
            text = "✅ Accesso effettuato"
            textSize = 15f
            setTextColor(0xFF2E7D32.toInt())
            setPadding(0, 0, 0, 30)
        })

        val logoutButton = Button(requireContext()).apply {
            text = "Logout"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1565C0.toInt())
        }

        logoutButton.setOnClickListener {

            session.logout()

            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
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