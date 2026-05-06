package com.romaevents.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        val root = ScrollView(this).apply {
            setBackgroundColor(0xFFF8F3EF.toInt())
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(42, 60, 42, 42)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo_roma_events)
            adjustViewBounds = true
        }

        content.addView(
            logo,
            LinearLayout.LayoutParams(
                700,
                700
            ).apply {
                setMargins(0, 0, 0, 18)
            }
        )

        val title = TextView(this).apply {
            text = "Benvenuto"
            textSize = 30f
            setTextColor(0xFF8B3A22.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Accedi per scoprire gli eventi di Roma"
            textSize = 15f
            setTextColor(0xFF6F5A50.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 30)
        }

        val card = MaterialCardView(this).apply {
            radius = 30f
            cardElevation = 6f
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            setContentPadding(30, 30, 30, 30)
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val emailInput = EditText(this).apply {
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val passwordInput = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val loginButton = Button(this).apply {
            text = "Accedi"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF8B3A22.toInt())
        }

        val registerText = TextView(this).apply {
            text = "Non hai un account? Registrati"
            textSize = 15f
            setTextColor(0xFF8B3A22.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 0)
        }

        form.addView(emailInput)
        form.addView(passwordInput)
        form.addView(loginButton)
        form.addView(registerText)

        card.addView(form)

        content.addView(title)
        content.addView(subtitle)
        content.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(content)
        setContentView(root)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            lifecycleScope.launch {
                try {
                    val auth = withContext(Dispatchers.IO) {
                        ApiService.login(email, password)
                    }

                    sessionManager.saveUser(auth)

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login fallito",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}