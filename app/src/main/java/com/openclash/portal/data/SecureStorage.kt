package com.openclash.portal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(
    context: Context,
) {
    private val preferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "openclash_secure_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun savePassword(password: String) {
        preferences.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun loadPassword(): String? = preferences.getString(KEY_PASSWORD, null)

    fun saveCookies(serializedCookies: String) {
        preferences.edit().putString(KEY_COOKIES, serializedCookies).apply()
    }

    fun loadCookies(): String? = preferences.getString(KEY_COOKIES, null)

    fun clearSession() {
        preferences.edit().remove(KEY_COOKIES).apply()
    }

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_PASSWORD = "password"
        const val KEY_COOKIES = "cookies"
    }
}

