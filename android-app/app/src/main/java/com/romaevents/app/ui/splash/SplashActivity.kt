package com.romaevents.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.romaevents.app.R
import com.romaevents.app.data.session.SessionManager
import com.romaevents.app.ui.auth.LoginActivity
import com.romaevents.app.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backgroundDark = ContextCompat.getColor(this, R.color.background_dark)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(backgroundDark)
        }

        // Logo GIGANTE - Solo il logo come richiesto
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo_roma_events)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f
            scaleX = 0.4f
            scaleY = 0.4f
        }

        // Layout per occupare quasi tutta la larghezza (margini minimi)
        root.addView(
            logo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 0, 20, 0)
            }
        )

        setContentView(root)

        // Animazione d'ingresso: Dissolvenza + Zoom fluido
        logo.animate()
            .alpha(1f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(1600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            val session = SessionManager(this)

            val intent = if (session.isLoggedIn()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            // Transizione cinematografica tra activity
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500)
    }
}
