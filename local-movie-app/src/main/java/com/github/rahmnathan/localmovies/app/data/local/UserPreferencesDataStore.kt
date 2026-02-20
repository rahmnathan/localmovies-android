package com.github.rahmnathan.localmovies.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserCredentials(
    val username: String = "",
    val password: String = "",
    val serverUrl: String = "https://movies.nathanrahm.com",
    val authServerUrl: String = "https://login.nathanrahm.com"
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val USERNAME = stringPreferencesKey("username")
        val LEGACY_PASSWORD = stringPreferencesKey("password")
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_SERVER_URL = stringPreferencesKey("auth_server_url")
        val SUBTITLE_OFFSET = floatPreferencesKey("subtitle_offset")
        val DISMISSED_RECOMMENDATION_IDS = stringSetPreferencesKey("dismissed_recommendation_ids")
    }

    private companion object {
        private const val SECURE_PREFS_NAME = "secure_user_credentials"
        private const val SECURE_PASSWORD_KEY = "password"
        private const val SECURE_PASSWORD_IV_KEY = "password_iv"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "localmovies_password_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    private val securePreferences: SharedPreferences by lazy {
        context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    val userCredentialsFlow: Flow<UserCredentials> = context.dataStore.data.map { prefs ->
        val securePassword = getSecurePassword()
        val legacyPassword = prefs[PreferencesKeys.LEGACY_PASSWORD] ?: ""
        val password = securePassword.ifBlank { legacyPassword }

        // Best-effort one-time migration for existing users with plaintext DataStore password.
        if (securePassword.isBlank() && legacyPassword.isNotBlank()) {
            saveSecurePassword(legacyPassword)
        }

        UserCredentials(
            username = prefs[PreferencesKeys.USERNAME] ?: "",
            password = password,
            serverUrl = prefs[PreferencesKeys.SERVER_URL] ?: "https://movies.nathanrahm.com",
            authServerUrl = prefs[PreferencesKeys.AUTH_SERVER_URL] ?: "https://login.nathanrahm.com"
        )
    }

    suspend fun saveCredentials(credentials: UserCredentials) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USERNAME] = credentials.username
            prefs[PreferencesKeys.SERVER_URL] = credentials.serverUrl
            prefs[PreferencesKeys.AUTH_SERVER_URL] = credentials.authServerUrl
            prefs.remove(PreferencesKeys.LEGACY_PASSWORD)
        }
        saveSecurePassword(credentials.password)
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
        clearSecurePassword()
    }

    val subtitleOffsetFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SUBTITLE_OFFSET] ?: 0f
    }

    suspend fun saveSubtitleOffset(offset: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SUBTITLE_OFFSET] = offset
        }
    }

    val dismissedRecommendationIdsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DISMISSED_RECOMMENDATION_IDS] ?: emptySet()
    }

    suspend fun dismissRecommendation(mediaFileId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.DISMISSED_RECOMMENDATION_IDS] ?: emptySet()
            prefs[PreferencesKeys.DISMISSED_RECOMMENDATION_IDS] = current + mediaFileId
        }
    }

    private fun getSecurePassword(): String {
        val encrypted = securePreferences.getString(SECURE_PASSWORD_KEY, null) ?: return ""
        val iv = securePreferences.getString(SECURE_PASSWORD_IV_KEY, null) ?: return ""

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(
                GCM_TAG_LENGTH_BITS,
                Base64.getDecoder().decode(iv)
            ))
            String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun saveSecurePassword(password: String) {
        if (password.isEmpty()) {
            clearSecurePassword()
            return
        }

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

            val encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(password.toByteArray(Charsets.UTF_8)))
            val iv = Base64.getEncoder().encodeToString(cipher.iv)

            securePreferences.edit()
                .putString(SECURE_PASSWORD_KEY, encrypted)
                .putString(SECURE_PASSWORD_IV_KEY, iv)
                .apply()
        } catch (_: Exception) {
            clearSecurePassword()
        }
    }

    private fun clearSecurePassword() {
        securePreferences.edit()
            .remove(SECURE_PASSWORD_KEY)
            .remove(SECURE_PASSWORD_IV_KEY)
            .apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
