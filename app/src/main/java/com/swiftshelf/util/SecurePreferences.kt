package com.swiftshelf.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveHostUrl(hostUrl: String) {
        sharedPreferences.edit().putString(KEY_HOST_URL, hostUrl).apply()
    }

    fun getHostUrl(): String? {
        return sharedPreferences.getString(KEY_HOST_URL, null)
    }

    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API_KEY, null)
    }

    fun saveSelectedLibraries(libraryIds: Set<String>) {
        sharedPreferences.edit().putStringSet(KEY_SELECTED_LIBRARIES, libraryIds).apply()
    }

    fun getSelectedLibraries(): Set<String> {
        return sharedPreferences.getStringSet(KEY_SELECTED_LIBRARIES, emptySet()) ?: emptySet()
    }

    fun saveItemLimit(limit: Int) {
        sharedPreferences.edit().putInt(KEY_ITEM_LIMIT, limit).apply()
    }

    fun getItemLimit(): Int {
        return sharedPreferences.getInt(KEY_ITEM_LIMIT, DEFAULT_ITEM_LIMIT)
    }

    fun saveProgressBarColor(color: String) {
        sharedPreferences.edit().putString(KEY_PROGRESS_COLOR, color).apply()
    }

    fun getProgressBarColor(): String {
        return sharedPreferences.getString(KEY_PROGRESS_COLOR, DEFAULT_PROGRESS_COLOR) ?: DEFAULT_PROGRESS_COLOR
    }

    fun savePreferredPlaybackSpeed(speed: Float) {
        sharedPreferences.edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()
    }

    fun getPreferredPlaybackSpeed(): Float {
        return sharedPreferences.getFloat(KEY_PLAYBACK_SPEED, DEFAULT_PLAYBACK_SPEED)
    }

    fun saveCurrentLibraryId(libraryId: String?) {
        sharedPreferences.edit().apply {
            if (libraryId.isNullOrEmpty()) {
                remove(KEY_CURRENT_LIBRARY_ID)
            } else {
                putString(KEY_CURRENT_LIBRARY_ID, libraryId)
            }
        }.apply()
    }

    fun getCurrentLibraryId(): String? {
        return sharedPreferences.getString(KEY_CURRENT_LIBRARY_ID, null)
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return !getHostUrl().isNullOrEmpty() && !getApiKey().isNullOrEmpty()
    }

    companion object {
        private const val PREFS_FILENAME = "swiftshelf_secure_prefs"
        private const val KEY_HOST_URL = "host_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SELECTED_LIBRARIES = "selected_libraries"
        private const val KEY_ITEM_LIMIT = "item_limit"
        private const val KEY_PROGRESS_COLOR = "progress_bar_color"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_CURRENT_LIBRARY_ID = "current_library_id"

        private const val DEFAULT_ITEM_LIMIT = 10
        private const val DEFAULT_PROGRESS_COLOR = "Yellow"
        private const val DEFAULT_PLAYBACK_SPEED = 1.0f
    }
}
