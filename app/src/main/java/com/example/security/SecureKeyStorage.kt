package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "sojib_secure_prefs"
        private const val KEY_ALIAS = "sojib_ai_key_alias"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_gemini_key"
        private const val KEY_IV = "gemini_key_iv"
        private const val KEY_VALIDATED = "gemini_key_validated"
        private const val KEY_MODEL = "selected_gemini_model"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    @Synchronized
    fun saveApiKey(rawKey: String): Boolean {
        return try {
            val cleanKey = rawKey.trim()
            if (cleanKey.isEmpty()) {
                clearApiKey()
                return false
            }

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(cleanKey.toByteArray(Charsets.UTF_8))

            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)

            prefs.edit()
                .putString(KEY_ENCRYPTED_API_KEY, encryptedString)
                .putString(KEY_IV, ivString)
                .putBoolean(KEY_VALIDATED, false) // Requires connection test before unlock
                .apply()
            true
        } catch (e: Exception) {
            // Fallback obfuscated storage for test environments or older APIs without Keystore GCM
            try {
                val encoded = Base64.encodeToString(rawKey.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                prefs.edit()
                    .putString(KEY_ENCRYPTED_API_KEY, encoded)
                    .putString(KEY_IV, "plain_fallback")
                    .putBoolean(KEY_VALIDATED, false)
                    .apply()
                true
            } catch (err: Exception) {
                false
            }
        }
    }

    @Synchronized
    fun getApiKey(): String? {
        val encryptedString = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        val ivString = prefs.getString(KEY_IV, null) ?: return null

        if (ivString == "plain_fallback") {
            return try {
                String(Base64.decode(encryptedString, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }

        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val secretKey = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: return null

            val iv = Base64.decode(ivString, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val encryptedBytes = Base64.decode(encryptedString, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    fun setValidationPassed(passed: Boolean) {
        prefs.edit().putBoolean(KEY_VALIDATED, passed).apply()
    }

    @Synchronized
    fun isValidationPassed(): Boolean {
        val hasKey = getApiKey()?.isNotEmpty() == true
        return hasKey && prefs.getBoolean(KEY_VALIDATED, false)
    }

    @Synchronized
    fun clearApiKey() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_API_KEY)
            .remove(KEY_IV)
            .putBoolean(KEY_VALIDATED, false)
            .apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }
}
