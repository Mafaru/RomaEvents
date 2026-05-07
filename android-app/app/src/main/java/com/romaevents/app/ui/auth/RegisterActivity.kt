package com.romaevents.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.romaevents.app.R
import com.romaevents.app.data.api.ApiService
import com.romaevents.app.data.session.SessionManager
import com.romaevents.app.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        val root = ScrollView(this).apply {
            setBackgroundColor(0xFFF7FAFF.toInt())
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(42, 48, 42, 42)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo_roma_events)
            adjustViewBounds = true
        }

        content.addView(
            logo,
            LinearLayout.LayoutParams(700, 700).apply {
                setMargins(0, 0, 0, 6)
            }
        )

        content.addView(TextView(this).apply {
            text = "Crea account"
            textSize = 30f
            setTextColor(0xFF1565C0.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        content.addView(TextView(this).apply {
            text = "Registrati per accedere a Roma Events"
            textSize = 15f
            setTextColor(0xFF666666.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        })

        val card = MaterialCardView(this).apply {
            radius = 30f
            cardElevation = 6f
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            setContentPadding(30, 30, 30, 30)
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val usernameInput = EditText(this).apply {
            hint = "Username"
            setSingleLine(true)
        }

        val emailInput = EditText(this).apply {
            hint = "Email"
            setSingleLine(true)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val passwordInput = EditText(this).apply {
            hint = "Password"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val registerButton = Button(this).apply {
            text = "Registrati"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1565C0.toInt())
        }

        val loginText = TextView(this).apply {
            text = "Hai già un account? Accedi"
            textSize = 15f
            setTextColor(0xFF1565C0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 0)
        }

        form.addView(usernameInput)
        form.addView(emailInput)
        form.addView(passwordInput)
        form.addView(registerButton)
        form.addView(loginText)

        card.addView(form)

        content.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(content)
        setContentView(root)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            lifecycleScope.launch {
                try {
                    val auth = withContext(Dispatchers.IO) {
                        ApiService.register(username, email, password)
                    }

                    sessionManager.saveUser(auth)

                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registrazione fallita: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        loginText.setOnClickListener {
            finish()
        }
    }
}