package com.pranavakshit.gpscamportal.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.pranavakshit.gpscamportal.data.remote.OfflineBundleDto

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAuthData(token: String, username: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun saveOfflineBundle(bundle: OfflineBundleDto) {
        val jsonString = gson.toJson(bundle)
        prefs.edit().putString(KEY_OFFLINE_BUNDLE, jsonString).apply()
    }

    fun getOfflineBundle(): OfflineBundleDto? {
        val jsonString = prefs.getString(KEY_OFFLINE_BUNDLE, null) ?: return null
        return try {
            gson.fromJson(jsonString, OfflineBundleDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_OFFLINE_BUNDLE = "offline_bundle"
    }
}
