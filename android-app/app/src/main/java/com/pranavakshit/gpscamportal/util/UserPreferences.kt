package com.pranavakshit.gpscamportal.util

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUploaderName(name: String) {
        prefs.edit().putString(KEY_UPLOADER_NAME, name).apply()
    }

    fun getUploaderName(): String? {
        return prefs.getString(KEY_UPLOADER_NAME, null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_UPLOADER_NAME = "uploader_name"
    }
}
