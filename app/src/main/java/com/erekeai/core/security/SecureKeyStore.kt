package com.erekeai.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранит API-ключи пользователя (Gemini/Groq/OpenAI) в зашифрованном виде,
 * используя Android Keystore через Jetpack Security.
 */
@Singleton
class SecureKeyStore @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKey(providerId: String, apiKey: String) {
        prefs.edit().putString(keyName(providerId), apiKey).apply()
    }

    fun getKey(providerId: String): String? = prefs.getString(keyName(providerId), null)

    fun clearKey(providerId: String) {
        prefs.edit().remove(keyName(providerId)).apply()
    }

    private fun keyName(providerId: String) = "api_key_$providerId"
}
