package com.droidexplorer.websim.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for TorBox API key using EncryptedSharedPreferences.
 * 
 * NO logging, NO network calls, NO side effects.
 * Application context only.
 */
class TorBoxStore(context: Context) {
    
    private val masterKey by lazy {
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "torbox_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val KEY_API_KEY = "api_key"
    }
    
    /**
     * Retrieves the stored API key.
     * @return The API key or null if not set
     */
    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API_KEY, null)
    }
    
    /**
     * Saves the API key securely.
     * @param key The API key to store
     */
    fun saveApiKey(key: String) {
        sharedPreferences.edit()
            .putString(KEY_API_KEY, key)
            .apply()
    }
    
    /**
     * Clears the stored API key.
     */
    fun clear() {
        sharedPreferences.edit()
            .remove(KEY_API_KEY)
            .apply()
    }
}
