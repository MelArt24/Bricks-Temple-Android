package com.am24.brickstemple.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AuthStorage {

    private const val PREF_NAME = "auth_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"

    internal var tokenCipher: TokenCipher = AndroidKeyStoreTokenCipher

    fun save(context: Context, token: String?, email: String?, username: String?) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, token?.let(tokenCipher::encrypt))
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val storedToken = prefs.getString(KEY_TOKEN, null)
        val token = when {
            storedToken == null -> null
            tokenCipher.isEncrypted(storedToken) -> tokenCipher.decrypt(storedToken)
            else -> storedToken
        }

        AuthSession.updateToken(token)
        AuthSession.updateEmail(prefs.getString(KEY_EMAIL, null))
        AuthSession.updateUsername(prefs.getString(KEY_USERNAME, null))
        AuthSession.markLoaded()

        if (!token.isNullOrBlank() && storedToken != null && !tokenCipher.isEncrypted(storedToken)) {
            save(
                context = context,
                token = token,
                email = prefs.getString(KEY_EMAIL, null),
                username = prefs.getString(KEY_USERNAME, null)
            )
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

internal interface TokenCipher {
    fun isEncrypted(value: String): Boolean
    fun encrypt(value: String): String
    fun decrypt(value: String): String?
}

private object AndroidKeyStoreTokenCipher : TokenCipher {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "bricks_temple_auth_storage_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "enc:"
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    override fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    override fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted

        return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    override fun decrypt(value: String): String? {
        if (!isEncrypted(value)) return value

        return runCatching {
            val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
            val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }

        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
