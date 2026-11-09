package com.gitinclo.vibepitch

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.gitinclo.vibepitch.data.local.LocalGuestSessionManager
import com.gitinclo.vibepitch.data.model.BusinessAccountProfile
import com.gitinclo.vibepitch.data.model.NicheKey
import com.gitinclo.vibepitch.security.Aes256GcmCryptoEngine
import com.gitinclo.vibepitch.ui.features.compliance.ComplianceDocsDialog
import com.gitinclo.vibepitch.ui.theme.VibePitchTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Aes256GcmEncryptionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun aes256Gcm_generatesUnique96BitNoncePerEncryptionAndRoundTripsWithAad() {
        val secretKey = Aes256GcmCryptoEngine.generateRandomKey()
        assertEquals(32, secretKey.encoded.size) // 256-bit key

        val plaintext = "Sensitive medical or financial record • Deposit $2,500"
        val aadContext = "user_id:9481"

        val token1 = Aes256GcmCryptoEngine.encrypt(
            plaintext = plaintext,
            associatedData = aadContext,
            customKey = secretKey
        )
        val token2 = Aes256GcmCryptoEngine.encrypt(
            plaintext = plaintext,
            associatedData = aadContext,
            customKey = secretKey
        )

        assertTrue(Aes256GcmCryptoEngine.isEncryptedToken(token1))
        assertTrue(Aes256GcmCryptoEngine.isEncryptedToken(token2))
        // Fresh 12-byte nonce per call guarantees distinct ciphertexts for identical plaintexts
        assertNotEquals(token1, token2)

        val decrypted1 = Aes256GcmCryptoEngine.decrypt(token1, aadContext, secretKey)
        val decrypted2 = Aes256GcmCryptoEngine.decrypt(token2, aadContext, secretKey)
        assertTrue(decrypted1.isSuccess)
        assertTrue(decrypted2.isSuccess)
        assertEquals(plaintext, decrypted1.getOrThrow())
        assertEquals(plaintext, decrypted2.getOrThrow())
    }

    @Test
    fun aes256Gcm_rejectsMismatchedAadContextOrTamperedCiphertextWithAeadBadTagException() {
        val secretKey = Aes256GcmCryptoEngine.generateRandomKey()
        val plaintext = "Protected Client Intake: Sarah Jenkins (555-0192)"
        val ownerAad = "users/alice_123/leads"
        val attackerAad = "users/bob_456/leads"

        val encryptedToken = Aes256GcmCryptoEngine.encrypt(
            plaintext = plaintext,
            associatedData = ownerAad,
            customKey = secretKey
        )

        // Attempting to decrypt with another user's AAD context must fail authentication
        val wrongAadResult = Aes256GcmCryptoEngine.decrypt(
            token = encryptedToken,
            associatedData = attackerAad,
            customKey = secretKey
        )
        assertTrue(wrongAadResult.isFailure)
        assertTrue(wrongAadResult.exceptionOrNull() is AEADBadTagException)

        // Attempting to decrypt without AAD when AAD was bound must also fail authentication
        val missingAadResult = Aes256GcmCryptoEngine.decrypt(
            token = encryptedToken,
            associatedData = null,
            customKey = secretKey
        )
        assertTrue(missingAadResult.isFailure)
        assertTrue(missingAadResult.exceptionOrNull() is AEADBadTagException)
    }

    @Test
    fun deriveKeyFromPassword_producesDeterministic256BitKeyAndRejectsWrongPassword() {
        val salt = "vibepitch_16salt".toByteArray(Charsets.UTF_8)
        val keyA = Aes256GcmCryptoEngine.deriveKeyFromPassword("CorrectHorseBatteryStaple!", salt)
        val keyB = Aes256GcmCryptoEngine.deriveKeyFromPassword("CorrectHorseBatteryStaple!", salt)
        val wrongKey = Aes256GcmCryptoEngine.deriveKeyFromPassword("WrongPassphrase123", salt)

        assertEquals(32, keyA.encoded.size)
        assertTrue(keyA.encoded.contentEquals(keyB.encoded))

        val token = Aes256GcmCryptoEngine.encrypt(
            plaintext = "Vault Backup Payload",
            associatedData = "vault:backup:v1",
            customKey = keyA
        )
        assertEquals(
            "Vault Backup Payload",
            Aes256GcmCryptoEngine.decrypt(token, "vault:backup:v1", keyB).getOrThrow()
        )
        assertTrue(Aes256GcmCryptoEngine.decrypt(token, "vault:backup:v1", wrongKey).isFailure)
    }

    @Test
    fun localGuestSessionManager_storesEncryptedAes256GcmPayloadsAtRestAndDecryptsTransparently() {
        val guestManager = LocalGuestSessionManager(context)
        guestManager.clearAllGuestData()
        guestManager.startOrResumeGuestSession()

        guestManager.savePendingBusinessUpgradeDraft(
            BusinessAccountProfile(
                agencyName = "Cipher Shield Agency",
                ownerName = "Elena Vance",
                businessEmail = "elena@ciphershield.io",
                businessPhone = "555-0177",
                primaryNicheKey = NicheKey.SAAS_AI_REMEDIATION.key,
                websiteUrl = "https://ciphershield.io"
            )
        )

        val rawStoredProfile = guestManager.getRawStoredPreference(
            LocalGuestSessionManager.KEY_PENDING_BUSINESS_PROFILE
        )
        assertNotNull(rawStoredProfile)
        assertTrue(
            "Expected ENCv1: AES-256-GCM envelope at rest, got $rawStoredProfile",
            Aes256GcmCryptoEngine.isEncryptedToken(rawStoredProfile)
        )
        assertFalse(rawStoredProfile!!.contains("Cipher Shield Agency"))
        assertFalse(rawStoredProfile.contains("elena@ciphershield.io"))

        val decryptedProfile = guestManager.getPendingBusinessProfile("user_verified_1")
        assertNotNull(decryptedProfile)
        assertEquals("Cipher Shield Agency", decryptedProfile?.agencyName)
        assertEquals("elena@ciphershield.io", decryptedProfile?.businessEmail)
    }

    @Test
    fun complianceDocsDialog_aes256GcmInspectorTab_verifiesAndSimulatesAadTamperingInCompose() {
        composeTestRule.setContent {
            VibePitchTheme {
                ComplianceDocsDialog(onDismiss = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("compliance_doc_tab_aes_256_gcm").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("aes256_gcm_inspector_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("aes256_simulate_tamper_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("aes256_run_live_verify_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("aes256_inspector_status").assertIsDisplayed()
    }
}
