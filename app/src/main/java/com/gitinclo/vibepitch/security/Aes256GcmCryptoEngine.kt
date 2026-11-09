package com.gitinclo.vibepitch.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Production AES-256-GCM (Galois/Counter Mode) Authenticated Encryption with Associated Data (AEAD) engine.
 *
 * Implements the three critical safeguards:
 * 1. Fresh 96-bit (12-byte) cryptographically random nonce per encryption via [SecureRandom] (never reuses nonces).
 * 2. Context binding with Associated Authenticated Data (AAD) so ciphertext cannot be moved across users or records.
 * 3. 256-bit (32-byte) master key backed by Android Keystore TEE/StrongBox with automatic JVM/Robolectric fallback
 *    and optional PBKDF2-HMAC-SHA256 passphrase key derivation.
 */
object Aes256GcmCryptoEngine {

    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val KEY_ALGORITHM = "AES"
    const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    const val KEYSTORE_ALIAS = "vibe_pitch_aes256_gcm_master_key_v1"

    const val KEY_SIZE_BITS = 256
    const val KEY_SIZE_BYTES = 32
    const val NONCE_LENGTH_BYTES = 12 // 96-bit standard GCM nonce
    const val AUTH_TAG_LENGTH_BITS = 128 // 16-byte authentication tag
    const val AUTH_TAG_LENGTH_BYTES = 16
    const val ENVELOPE_PREFIX = "ENCv1:"

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 16_384

    private val secureRandom = SecureRandom()

    @Volatile
    private var cachedMasterKey: SecretKey? = null

    @Volatile
    private var isHardwareKeystoreBacked: Boolean = false

    private val jvmFallbackKeyBytes: ByteArray by lazy {
        val bytes = ByteArray(KEY_SIZE_BYTES)
        secureRandom.nextBytes(bytes)
        bytes
    }

    /**
     * Generates a standalone random 256-bit (32-byte) AES key using [SecureRandom].
     */
    fun generateRandomKey(): SecretKey {
        val keyBytes = ByteArray(KEY_SIZE_BYTES)
        secureRandom.nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    /**
     * Derives a 256-bit (32-byte) AES key from a user password and salt using PBKDF2-HMAC-SHA256.
     */
    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        require(salt.size >= 8) { "Salt must be at least 8 bytes" }
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_SIZE_BITS
        )
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val encoded = factory.generateSecret(spec).encoded
        return SecretKeySpec(encoded, KEY_ALGORITHM)
    }

    /**
     * Resolves the 256-bit master key from Android Keystore hardware (when running on an Android device)
     * or falls back to an in-memory 256-bit [SecretKeySpec] in headless JVM / Robolectric unit tests.
     */
    fun getOrCreateMasterKey(): SecretKey {
        cachedMasterKey?.let { return it }
        synchronized(this) {
            cachedMasterKey?.let { return it }
            val resolvedKey = try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
                val existingEntry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (existingEntry != null) {
                    isHardwareKeystoreBacked = true
                    existingEntry.secretKey
                } else {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        ANDROID_KEYSTORE_PROVIDER
                    )
                    val spec = KeyGenParameterSpec.Builder(
                        KEYSTORE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(KEY_SIZE_BITS)
                        .setRandomizedEncryptionRequired(false)
                        .build()
                    keyGenerator.init(spec)
                    isHardwareKeystoreBacked = true
                    keyGenerator.generateKey()
                }
            } catch (_: Throwable) {
                isHardwareKeystoreBacked = false
                SecretKeySpec(jvmFallbackKeyBytes, KEY_ALGORITHM)
            }
            cachedMasterKey = resolvedKey
            return resolvedKey
        }
    }

    fun activeKeyProviderDescription(): String {
        getOrCreateMasterKey()
        return if (isHardwareKeystoreBacked) {
            "AndroidKeyStore Hardware-Backed AES-256-GCM ($KEYSTORE_ALIAS)"
        } else {
            "JVM 256-bit AES-GCM SecretKeySpec (Unit Test / Sandbox Mode)"
        }
    }

    fun isEncryptedToken(value: String?): Boolean {
        return value != null && value.startsWith(ENVELOPE_PREFIX)
    }

    /**
     * Encrypts [plaintext] using AES-256-GCM with a fresh 12-byte random nonce and optional [associatedData] (AAD).
     * Packs `12-byte nonce + ciphertext + 16-byte authentication tag` into an `ENCv1:` Base64url string.
     */
    fun encrypt(
        plaintext: String,
        associatedData: String? = null,
        customKey: SecretKey? = null
    ): String {
        val secretKey = customKey ?: getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val requestedNonce = ByteArray(NONCE_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val actualNonce: ByteArray = try {
            cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey,
                GCMParameterSpec(AUTH_TAG_LENGTH_BITS, requestedNonce)
            )
            requestedNonce
        } catch (_: InvalidAlgorithmParameterException) {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher.iv ?: requestedNonce
        }

        if (!associatedData.isNullOrEmpty()) {
            cipher.updateAAD(associatedData.toByteArray(Charsets.UTF_8))
        }

        val ciphertextWithTag = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(actualNonce.size + ciphertextWithTag.size)
        System.arraycopy(actualNonce, 0, packed, 0, actualNonce.size)
        System.arraycopy(ciphertextWithTag, 0, packed, actualNonce.size, ciphertextWithTag.size)

        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(packed)
        return "$ENVELOPE_PREFIX$encoded"
    }

    /**
     * Decrypts an `ENCv1:` (or raw Base64url) AES-256-GCM token and verifies both the 128-bit authentication tag
     * and the bound [associatedData] (AAD) before releasing any plaintext.
     */
    fun decrypt(
        token: String,
        associatedData: String? = null,
        customKey: SecretKey? = null
    ): Result<String> = runCatching {
        val cleanB64 = token.removePrefix(ENVELOPE_PREFIX).trim()
        val raw = Base64.getUrlDecoder().decode(cleanB64)
        if (raw.size < NONCE_LENGTH_BYTES + AUTH_TAG_LENGTH_BYTES) {
            throw GeneralSecurityException(
                "Invalid AES-256-GCM payload length (${raw.size} bytes; minimum ${NONCE_LENGTH_BYTES + AUTH_TAG_LENGTH_BYTES})"
            )
        }

        val nonce = raw.copyOfRange(0, NONCE_LENGTH_BYTES)
        val ciphertextWithTag = raw.copyOfRange(NONCE_LENGTH_BYTES, raw.size)

        val secretKey = customKey ?: getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(AUTH_TAG_LENGTH_BITS, nonce)
        )

        if (!associatedData.isNullOrEmpty()) {
            cipher.updateAAD(associatedData.toByteArray(Charsets.UTF_8))
        }

        val plaintextBytes = cipher.doFinal(ciphertextWithTag)
        String(plaintextBytes, Charsets.UTF_8)
    }

    /**
     * Encrypts non-empty field values with [associatedData] context binding while preserving empty strings
     * and avoiding double-encryption of already-prefixed `ENCv1:` tokens.
     */
    fun encryptFieldIfNotEmpty(value: String, associatedData: String): String {
        if (value.isEmpty() || isEncryptedToken(value)) return value
        return encrypt(plaintext = value, associatedData = associatedData)
    }

    /**
     * Decrypts `ENCv1:` tokens bound to [associatedData], or returns unencrypted legacy strings as-is
     * for zero-downtime migration.
     */
    fun decryptFieldOrPlaintext(value: String, associatedData: String): String {
        if (value.isEmpty() || !isEncryptedToken(value)) return value
        return decrypt(token = value, associatedData = associatedData).getOrElse { value }
    }
}
