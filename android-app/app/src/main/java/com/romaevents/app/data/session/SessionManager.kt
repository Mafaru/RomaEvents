package com.romaevents.app.data.session

import android.content.Context
import com.romaevents.app.model.AuthResponse

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUser(auth: AuthResponse) {
        prefs.edit()
            .putLong("user_id", auth.userId)
            .putString("username", auth.username)
            .putString("email", auth.email)
            .putString("token", auth.token)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.contains("token")

    fun getUsername(): String? = prefs.getString("username", null)

    fun getEmail(): String? = prefs.getString("email", null)

    fun logout() {
        prefs.edit().clear().apply()
    }
}