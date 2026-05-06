package com.romaevents.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val session = SessionManager(requireContext())

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(0xFFF7F7F7.toInt())
        }

        if (!session.isLoggedIn()) {
            root.addView(TextView(requireContext()).apply {
                text = "Area utente"
                textSize = 28f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 24)
            })

            root.addView(TextView(requireContext()).apply {
                text = "Accedi per usare le funzionalità utente."
                textSize = 16f
                setPadding(0, 0, 0, 24)
            })

            root.addView(Button(requireContext()).apply {
                text = "Accedi / Registrati"
                setOnClickListener {
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                }
            })

            return root
        }

        val card = MaterialCardView(requireContext()).apply {
            radius = 24f
            cardElevation = 4f
            setContentPadding(32, 32, 32, 32)
        }

        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        box.addView(TextView(requireContext()).apply {
            text = "Profilo"
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 24)
        })

        box.addView(TextView(requireContext()).apply {
            text = "👤 ${session.getUsername() ?: "Utente"}"
            textSize = 18f
            setPadding(0, 0, 0, 12)
        })

        box.addView(TextView(requireContext()).apply {
            text = "✉️ ${session.getEmail() ?: ""}"
            textSize = 16f
            setPadding(0, 0, 0, 28)
        })

        box.addView(Button(requireContext()).apply {
            text = "Logout"
            setOnClickListener {

                session.logout()

                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
            }
        })

        card.addView(box)

        root.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return root
    }

    override fun onResume() {
        super.onResume()
        view?.post {
            (activity as? MainActivity)?.refreshProfileIfVisible()
        }
    }
}