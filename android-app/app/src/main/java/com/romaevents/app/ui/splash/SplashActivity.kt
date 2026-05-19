
package com.romaevents.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.romaevents.app.R
import com.romaevents.app.data.session.SessionManager
import com.romaevents.app.ui.auth.LoginActivity
import com.romaevents.app.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF7FAFF.toInt())
            setPadding(48, 48, 48, 48)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo_roma_events)
            adjustViewBounds = true
        }

        val title = TextView(this).apply {
            text = "Roma Events"
            textSize = 30f
            setTextColor(0xFF1565C0.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Scopri gli eventi della città"
            textSize = 15f
            setTextColor(0xFF666666.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        root.addView(
            logo,
            LinearLayout.LayoutParams(360, 360)
        )

        root.addView(title)
        root.addView(subtitle)

        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            val session = SessionManager(this)

            val intent = if (session.isLoggedIn()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            finish()
        }, 1200)
    }
}