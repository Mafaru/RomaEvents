package com.romaevents.app.data.session

import android.content.Context
import com.romaevents.app.model.AuthResponse

//SessionManager is a class that manages the user session in the app, it uses SharedPreferences to store the user's authentication information such as user ID, username, email, and token. It provides methods to save the user information when they log in or register, check if the user is currently logged in, retrieve the username and email of the logged-in user, and log out by clearing the stored session data. This class helps to maintain the user's session state across app launches and provides a way to access user information when needed.
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