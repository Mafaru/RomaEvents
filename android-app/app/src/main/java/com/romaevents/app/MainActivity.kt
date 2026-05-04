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

        if (savedInstanceState == null) {
            showFragment(EventsFragment())
        }

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
                    true
                }

                else -> false
            }
        }
    }

    fun openEventDetail(eventId: Long) {
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