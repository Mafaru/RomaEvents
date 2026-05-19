package com.romaevents.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.romaevents.app.ui.events.EventDetailFragment
import com.romaevents.app.ui.events.EventsFragment
import com.romaevents.app.ui.auth.LoginActivity
import com.romaevents.app.ui.map.MapFragment
import com.romaevents.app.ui.profile.ProfileFragment
import com.romaevents.app.R
import com.romaevents.app.ui.scanner.ScannerActivity
import com.romaevents.app.data.session.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_events -> {
                    showFragment(EventsFragment())
                    true
                }

                R.id.nav_map -> {
                    showFragment(MapFragment())
                    true
                }

                R.id.nav_scanner -> {
                    startActivity(Intent(this, ScannerActivity::class.java))
                    false
                }

                R.id.nav_profile -> {
                    showFragment(ProfileFragment())
                    true
                }

                else -> false
            }
        }

        val handled = handleIntent(intent)

        if (!handled && savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_events
            showFragment(EventsFragment())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent): Boolean {
        val eventId = intent.getLongExtra("open_event_id", -1L)

        return if (eventId != -1L) {
            openEventDetail(eventId)
            intent.removeExtra("open_event_id")
            true
        } else {
            false
        }
    }

    fun openEventDetail(eventId: Long) {
        bottomNavigation.menu.findItem(R.id.nav_events).isChecked = true
        showFragment(EventDetailFragment.Companion.newInstance(eventId))
    }

    fun openMapForEvent(eventId: Long) {
        bottomNavigation.menu.findItem(R.id.nav_map).isChecked = true
        showFragment(MapFragment.Companion.newInstance(eventId))
    }

    fun goBackToEvents() {
        bottomNavigation.menu.findItem(R.id.nav_events).isChecked = true
        showFragment(EventsFragment())
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, fragment)
            .commit()
    }
}