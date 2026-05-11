package com.github.rahmnathan.localmovies.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    val serverUrl: String = "https://movies.nathanrahm.com",
    val authServerUrl: String = "https://login.nathanrahm.com",
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessTokenExpiresAtEpochMillis: Long = 0,
    val refreshTokenExpiresAtEpochMillis: Long = 0,
    val legacyPassword: String = ""
) {
    fun hasSession(): Boolean = username.isNotBlank() && refreshToken.isNotBlank()

    fun hasLegacyPassword(): Boolean = username.isNotBlank() && legacyPassword.isNotBlank()

    fun isAccessTokenValid(
        nowEpochMillis: Long = System.currentTimeMillis(),
        refreshThresholdMillis: Long = 30_000
    ): Boolean {
        return accessToken.isNotBlank() && accessTokenExpiresAtEpochMillis > nowEpochMillis + refreshThresholdMillis
    }
}

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val USERNAME = stringPreferencesKey("username")
        val LEGACY_PASSWORD = stringPreferencesKey("password")
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_SERVER_URL = stringPreferencesKey("auth_server_url")
        val ACCESS_TOKEN_EXPIRES_AT = longPreferencesKey("access_token_expires_at")
        val REFRESH_TOKEN_EXPIRES_AT = longPreferencesKey("refresh_token_expires_at")
        val SUBTITLE_OFFSET = floatPreferencesKey("subtitle_offset")
        val DISMISSED_RECOMMENDATION_IDS = stringSetPreferencesKey("dismissed_recommendation_ids")
    }

    private companion object {
        private const val SECURE_PREFS_NAME = "secure_user_credentials"
        private const val SECURE_PASSWORD_KEY = "password"
        private const val SECURE_PASSWORD_IV_KEY = "password_iv"
        private const val SECURE_ACCESS_TOKEN_KEY = "access_token"
        private const val SECURE_ACCESS_TOKEN_IV_KEY = "access_token_iv"
        private const val SECURE_REFRESH_TOKEN_KEY = "refresh_token"
        private const val SECURE_REFRESH_TOKEN_IV_KEY = "refresh_token_iv"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "localmovies_password_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    private val securePreferences: SharedPreferences by lazy {
        context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    val userCredentialsFlow: Flow<UserCredentials> = context.dataStore.data.map { prefs ->
        val secureLegacyPassword = getSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY)
        val plaintextLegacyPassword = prefs[PreferencesKeys.LEGACY_PASSWORD] ?: ""
        val legacyPassword = secureLegacyPassword.ifBlank { plaintextLegacyPassword }

        if (secureLegacyPassword.isBlank() && plaintextLegacyPassword.isNotBlank()) {
            saveSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY, plaintextLegacyPassword)
        }

        UserCredentials(
            username = prefs[PreferencesKeys.USERNAME] ?: "",
            serverUrl = prefs[PreferencesKeys.SERVER_URL] ?: "https://movies.nathanrahm.com",
            authServerUrl = prefs[PreferencesKeys.AUTH_SERVER_URL] ?: "https://login.nathanrahm.com",
            accessToken = getSecureString(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY),
            refreshToken = getSecureString(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY),
            accessTokenExpiresAtEpochMillis = prefs[PreferencesKeys.ACCESS_TOKEN_EXPIRES_AT] ?: 0L,
            refreshTokenExpiresAtEpochMillis = prefs[PreferencesKeys.REFRESH_TOKEN_EXPIRES_AT] ?: 0L,
            legacyPassword = legacyPassword
        )
    }

    suspend fun saveCredentials(credentials: UserCredentials) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USERNAME] = credentials.username
            prefs[PreferencesKeys.SERVER_URL] = credentials.serverUrl
            prefs[PreferencesKeys.AUTH_SERVER_URL] = credentials.authServerUrl
            prefs[PreferencesKeys.ACCESS_TOKEN_EXPIRES_AT] = credentials.accessTokenExpiresAtEpochMillis
            prefs[PreferencesKeys.REFRESH_TOKEN_EXPIRES_AT] = credentials.refreshTokenExpiresAtEpochMillis
            prefs.remove(PreferencesKeys.LEGACY_PASSWORD)
        }
        saveSecureString(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY, credentials.accessToken)
        saveSecureString(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY, credentials.refreshToken)
        clearSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY)
    }

    suspend fun saveLegacyCredentials(credentials: UserCredentials) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USERNAME] = credentials.username
            prefs[PreferencesKeys.SERVER_URL] = credentials.serverUrl
            prefs[PreferencesKeys.AUTH_SERVER_URL] = credentials.authServerUrl
            prefs[PreferencesKeys.ACCESS_TOKEN_EXPIRES_AT] = 0L
            prefs[PreferencesKeys.REFRESH_TOKEN_EXPIRES_AT] = 0L
            prefs.remove(PreferencesKeys.LEGACY_PASSWORD)
        }
        clearSecureString(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY)
        clearSecureString(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY)
        saveSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY, credentials.legacyPassword)
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
        clearSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY)
        clearSecureString(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY)
        clearSecureString(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY)
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

    private fun getSecureString(valueKey: String, ivKey: String): String {
        val encrypted = securePreferences.getString(valueKey, null) ?: return ""
        val iv = securePreferences.getString(ivKey, null) ?: return ""

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.getDecoder().decode(iv))
            )
            String(cipher.doFinal(Base64.getDecoder().decode(encrypted)), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun saveSecureString(valueKey: String, ivKey: String, value: String) {
        if (value.isEmpty()) {
            clearSecureString(valueKey, ivKey)
            return
        }

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

            val encrypted = Base64.getEncoder()
                .encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)))
            val iv = Base64.getEncoder().encodeToString(cipher.iv)

            securePreferences.edit()
                .putString(valueKey, encrypted)
                .putString(ivKey, iv)
                .apply()
        } catch (_: Exception) {
            clearSecureString(valueKey, ivKey)
        }
    }

    private fun clearSecureString(valueKey: String, ivKey: String) {
        securePreferences.edit()
            .remove(valueKey)
            .remove(ivKey)
            .apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
