package com.github.rahmnathan.localmovies.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
    val offlineToken: String = "",
    val accessTokenExpiresAtEpochMillis: Long = 0,
    val offlineTokenExpiresAtEpochMillis: Long = 0,
    val legacyPassword: String = ""
) {
    fun hasSession(): Boolean = username.isNotBlank() && offlineToken.isNotBlank()

    fun hasLegacyPassword(): Boolean = username.isNotBlank() && legacyPassword.isNotBlank()

    fun isAccessTokenValid(
        nowEpochMillis: Long = System.currentTimeMillis(),
        refreshThresholdMillis: Long = 30_000
    ): Boolean {
        return accessToken.isNotBlank() && accessTokenExpiresAtEpochMillis > nowEpochMillis + refreshThresholdMillis
    }
}

enum class AuthState {
    SignedOut,
    SignedIn
}

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val USERNAME = stringPreferencesKey("username")
        val LEGACY_PASSWORD = stringPreferencesKey("password")
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_SERVER_URL = stringPreferencesKey("auth_server_url")
        val AUTH_MESSAGE = stringPreferencesKey("auth_message")
        val ACCESS_TOKEN_EXPIRES_AT = longPreferencesKey("access_token_expires_at")
        val REFRESH_TOKEN_EXPIRES_AT = longPreferencesKey("refresh_token_expires_at")
        val DISMISSED_RECOMMENDATION_IDS = stringSetPreferencesKey("dismissed_recommendation_ids")
    }

    private companion object {
        private const val DIRECT_SECURE_PREFS_NAME = "secure_user_credentials_v3"
        private const val V1_SECURE_PREFS_NAME = "secure_user_credentials"

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
        context.getSharedPreferences(DIRECT_SECURE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val v1SecurePreferences: SharedPreferences by lazy {
        context.getSharedPreferences(V1_SECURE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    val userCredentialsFlow: Flow<UserCredentials> = context.dataStore.data.map { prefs ->
        migrateSecureValuesIfNeeded()

        val legacyPassword = readSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY)
            .ifBlank { prefs[PreferencesKeys.LEGACY_PASSWORD] ?: "" }

        UserCredentials(
            username = prefs[PreferencesKeys.USERNAME] ?: "",
            serverUrl = prefs[PreferencesKeys.SERVER_URL] ?: "https://movies.nathanrahm.com",
            authServerUrl = prefs[PreferencesKeys.AUTH_SERVER_URL] ?: "https://login.nathanrahm.com",
            accessToken = readSecureString(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY),
            offlineToken = readSecureString(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY),
            accessTokenExpiresAtEpochMillis = prefs[PreferencesKeys.ACCESS_TOKEN_EXPIRES_AT] ?: 0L,
            offlineTokenExpiresAtEpochMillis = prefs[PreferencesKeys.REFRESH_TOKEN_EXPIRES_AT] ?: 0L,
            legacyPassword = legacyPassword
        )
    }

    val authStateFlow: Flow<AuthState> = userCredentialsFlow.map { credentials ->
        if (credentials.hasSession() || credentials.hasLegacyPassword()) {
            AuthState.SignedIn
        } else {
            AuthState.SignedOut
        }
    }

    val authMessageFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTH_MESSAGE]
    }

    suspend fun saveCredentials(credentials: UserCredentials) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USERNAME] = credentials.username
            prefs[PreferencesKeys.SERVER_URL] = credentials.serverUrl
            prefs[PreferencesKeys.AUTH_SERVER_URL] = credentials.authServerUrl
            prefs[PreferencesKeys.ACCESS_TOKEN_EXPIRES_AT] = credentials.accessTokenExpiresAtEpochMillis
            prefs[PreferencesKeys.REFRESH_TOKEN_EXPIRES_AT] = credentials.offlineTokenExpiresAtEpochMillis
            prefs.remove(PreferencesKeys.AUTH_MESSAGE)
            prefs.remove(PreferencesKeys.LEGACY_PASSWORD)
        }
        writeSecureString(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY, credentials.accessToken)
        writeSecureString(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY, credentials.offlineToken)
        clearSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY)
    }

    suspend fun saveLegacyCredentials(credentials: UserCredentials) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USERNAME] = credentials.username
            prefs[PreferencesKeys.SERVER_URL] = credentials.serverUrl
            prefs[PreferencesKeys.AUTH_SERVER_URL] = credentials.authServerUrl
            prefs[PreferencesKeys.ACCESS_TOKEN_EXPIRES_AT] = 0L
            prefs[PreferencesKeys.REFRESH_TOKEN_EXPIRES_AT] = 0L
            prefs.remove(PreferencesKeys.AUTH_MESSAGE)
            prefs.remove(PreferencesKeys.LEGACY_PASSWORD)
        }
        clearSecureString(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY)
        clearSecureString(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY)
        writeSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY, credentials.legacyPassword)
    }

    suspend fun clearSession(message: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ACCESS_TOKEN_EXPIRES_AT] = 0L
            prefs[PreferencesKeys.REFRESH_TOKEN_EXPIRES_AT] = 0L
            prefs.remove(PreferencesKeys.LEGACY_PASSWORD)
            if (message.isNullOrBlank()) {
                prefs.remove(PreferencesKeys.AUTH_MESSAGE)
            } else {
                prefs[PreferencesKeys.AUTH_MESSAGE] = message
            }
        }
        clearSecureString(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY)
        clearSecureString(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY)
        clearSecureString(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY)
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
        securePreferences.edit().clear().apply()
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

    private fun migrateSecureValuesIfNeeded() {
        migrateValueFromAnyLegacyStore(SECURE_PASSWORD_KEY, SECURE_PASSWORD_IV_KEY)
        migrateValueFromAnyLegacyStore(SECURE_ACCESS_TOKEN_KEY, SECURE_ACCESS_TOKEN_IV_KEY)
        migrateValueFromAnyLegacyStore(SECURE_REFRESH_TOKEN_KEY, SECURE_REFRESH_TOKEN_IV_KEY)
    }

    private fun migrateValueFromAnyLegacyStore(valueKey: String, ivKey: String) {
        if (securePreferences.contains(valueKey) && securePreferences.contains(ivKey)) {
            clearV1SecureValue(valueKey, ivKey)
            return
        }

        val v1SecureValue = readV1EncryptedString(valueKey, ivKey)
        if (v1SecureValue.isNotBlank()) {
            writeSecureString(valueKey, ivKey, v1SecureValue)
        }
        clearV1SecureValue(valueKey, ivKey)
    }

    private fun readSecureString(valueKey: String, ivKey: String): String {
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

    private fun writeSecureString(valueKey: String, ivKey: String, value: String) {
        if (value.isBlank()) {
            clearSecureString(valueKey, ivKey)
            return
        }

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

            securePreferences.edit()
                .putString(valueKey, Base64.getEncoder().encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8))))
                .putString(ivKey, Base64.getEncoder().encodeToString(cipher.iv))
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

    private fun clearV1SecureValue(valueKey: String, ivKey: String) {
        v1SecurePreferences.edit()
            .remove(valueKey)
            .remove(ivKey)
            .apply()
    }

    private fun readV1EncryptedString(valueKey: String, ivKey: String): String {
        val encrypted = v1SecurePreferences.getString(valueKey, null) ?: return ""
        val iv = v1SecurePreferences.getString(ivKey, null) ?: return ""

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
