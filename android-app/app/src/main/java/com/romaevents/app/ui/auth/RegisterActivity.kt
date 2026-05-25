package com.romaevents.app.ui.auth

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.romaevents.app.R
import com.romaevents.app.data.api.ApiService
import com.romaevents.app.data.session.SessionManager
import com.romaevents.app.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val orange = ContextCompat.getColor(this, R.color.roma_orange)
        val backgroundDark = ContextCompat.getColor(this, R.color.background_dark)
        val surfaceDark = ContextCompat.getColor(this, R.color.surface_dark)
        val white = ContextCompat.getColor(this, R.color.white)
        val textSecondary = ContextCompat.getColor(this, R.color.text_secondary)
        val black = ContextCompat.getColor(this, R.color.black)

        val root = ScrollView(this).apply {
            setBackgroundColor(backgroundDark)
            isFillViewport = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 60)
        }

        // Logo Gigante per coerenza Premium
        content.addView(ImageView(this).apply {
            setImageResource(R.drawable.logo_roma_events)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 700).apply {
            setMargins(0, -60, 0, -40)
        })

        content.addView(TextView(this).apply {
            text = "UNISCITI A NOI"
            textSize = 38f
            setTextColor(orange)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        })

        content.addView(TextView(this).apply {
            text = "CREA IL TUO PROFILO IMPERIALE"
            textSize = 12f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
            setPadding(0, 4, 0, 48)
        })

        // Card Premium con angoli a 48f
        val card = MaterialCardView(this).apply {
            radius = 48f
            cardElevation = 25f
            setCardBackgroundColor(surfaceDark)
            strokeWidth = 2
            setStrokeColor(ColorStateList.valueOf(0x1AFFFFFF))
            setContentPadding(48, 56, 48, 56)
        }

        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val nameLayout = createMaterialInput("Username", InputType.TYPE_CLASS_TEXT, orange, white, textSecondary)
        val emailLayout = createMaterialInput("Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, orange, white, textSecondary)
        val passwordLayout = createMaterialInput("Password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, orange, white, textSecondary)
        val confirmPasswordLayout = createMaterialInput("Conferma Password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, orange, white, textSecondary)

        nameLayout.setStartIconDrawable(android.R.drawable.ic_menu_edit)
        nameLayout.setStartIconTintList(ColorStateList.valueOf(orange))
        emailLayout.setStartIconDrawable(android.R.drawable.ic_dialog_email)
        emailLayout.setStartIconTintList(ColorStateList.valueOf(orange))
        passwordLayout.setStartIconDrawable(android.R.drawable.ic_lock_idle_lock)
        passwordLayout.setStartIconTintList(ColorStateList.valueOf(orange))
        passwordLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        confirmPasswordLayout.setStartIconDrawable(android.R.drawable.ic_lock_idle_lock)
        confirmPasswordLayout.setStartIconTintList(ColorStateList.valueOf(orange))
        confirmPasswordLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE

        val registerButton = MaterialButton(this).apply {
            text = "REGISTRATI"
            textSize = 17f
            cornerRadius = 40
            setTextColor(black)
            backgroundTintList = ColorStateList.valueOf(orange)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 36, 0, 36)
            elevation = 15f
        }

        val loginText = TextView(this).apply {
            text = "HAI GIÀ UN ACCOUNT? ACCEDI"
            textSize = 13f
            setTextColor(orange)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 0)
            letterSpacing = 0.05f
        }

        form.addView(nameLayout)
        form.addView(emailLayout, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 16, 0, 0) })
        form.addView(passwordLayout, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 16, 0, 0) })
        form.addView(confirmPasswordLayout, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 16, 0, 48) })
        form.addView(registerButton)
        form.addView(loginText)

        card.addView(form)
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(32, 0, 32, 0) })
        root.addView(content)
        setContentView(root)

        loginText.setOnClickListener { finish() }

        registerButton.setOnClickListener {
            val username = nameLayout.editText?.text.toString().trim()
            val email = emailLayout.editText?.text.toString().trim()
            val password = passwordLayout.editText?.text.toString()
            val confirm = confirmPasswordLayout.editText?.text.toString()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Le password non coincidono", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val auth = withContext(Dispatchers.IO) { 
                        ApiService.register(username, email, password) 
                    }
                    SessionManager(this@RegisterActivity).saveUser(auth)
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finishAffinity()
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createMaterialInput(hintText: String, type: Int, accent: Int, textCol: Int, hintCol: Int): TextInputLayout {
        val layout = TextInputLayout(this).apply {
            hint = hintText
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxStrokeColorStateList(ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(accent, hintCol)
            ))
            hintTextColor = ColorStateList.valueOf(accent)
            defaultHintTextColor = ColorStateList.valueOf(hintCol)
            setBoxCornerRadii(32f, 32f, 32f, 32f)
        }
        val editText = TextInputEditText(layout.context).apply {
            this.inputType = type
            setTextColor(textCol)
            textSize = 16f
        }
        layout.addView(editText)
        return layout
    }
}
