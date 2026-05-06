package com.romaevents.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        showFragment(EventDetailFragment.newInstance(eventId))
    }

    fun openMapForEvent(eventId: Long) {
        bottomNavigation.menu.findItem(R.id.nav_map).isChecked = true
        showFragment(MapFragment.newInstance(eventId))
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