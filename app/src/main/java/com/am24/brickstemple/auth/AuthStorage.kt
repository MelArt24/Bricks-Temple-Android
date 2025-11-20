package com.am24.brickstemple.auth

import android.content.Context

object AuthStorage {

    private const val PREF_NAME = "auth_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"

    fun save(context: Context, token: String?, email: String?, username: String?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        AuthSession.updateToken(prefs.getString(KEY_TOKEN, null))
        AuthSession.updateEmail(prefs.getString(KEY_EMAIL, null))
        AuthSession.updateUsername(prefs.getString(KEY_USERNAME, null))
        AuthSession.markLoaded()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
