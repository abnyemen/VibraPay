package com.example.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.OfficialAccountProfile
import com.example.data.WalletCurrency
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID
import kotlin.random.Random

class VibraPayCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerAndLookupCloudAccount_authenticatedUser_returnsQuadrupleName() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = VibraPayCloudRepository(firestore)

        val randomAccountId = Random.nextInt(100000, 999999).toString()
        val randomPhone = "77" + Random.nextInt(1000000, 9999999).toString()
        val profile = OfficialAccountProfile(
            accountId = randomAccountId,
            firstName = "أحمد",
            fatherName = "علي",
            grandfatherName = "صالح",
            familyName = "الشامي",
            countryDialCode = "+967",
            countryNameAr = "اليمن",
            countryFlag = "🇾🇪",
            phoneNumberLocal = randomPhone,
            fullPhoneDigits = "967$randomPhone",
            email = ALICE_EMAIL,
            passwordPlainOrHash = "123456",
            balanceYer = 250000.0,
            balanceSar = 5000.0,
            balanceUsd = 1500.0
        )

        val regResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerOrSyncCloudAccount(profile)
        }
        assertTrue(regResult.isSuccess)

        // Lookup by 6-digit numeric Account ID
        val lookedUpById = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.lookupRecipientInCloud(randomAccountId).getOrThrow()
        }
        assertNotNull(lookedUpById)
        assertEquals("أحمد علي صالح الشامي", lookedUpById?.fullQuadName)

        // Lookup by Phone Number
        val lookedUpByPhone = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.lookupRecipientInCloud(randomPhone).getOrThrow()
        }
        assertNotNull(lookedUpByPhone)
        assertEquals(randomAccountId, lookedUpByPhone?.accountId)
    }

    @Test
    fun executeAtomicOnlineTransfer_deductsSenderAndCreditsRecipient() = runBlocking {
        signInTestUser(BOB_EMAIL)
        val bobRepo = VibraPayCloudRepository(firestore)
        val bobAccountId = Random.nextInt(100000, 999999).toString()
        val bobPhone = "50" + Random.nextInt(1000000, 9999999).toString()
        val bobProfile = OfficialAccountProfile(
            accountId = bobAccountId,
            firstName = "خالد",
            fatherName = "محمد",
            grandfatherName = "عبدالله",
            familyName = "القحطاني",
            countryDialCode = "+966",
            countryNameAr = "المملكة العربية السعودية",
            countryFlag = "🇸🇦",
            phoneNumberLocal = bobPhone,
            fullPhoneDigits = "966$bobPhone",
            email = BOB_EMAIL,
            passwordPlainOrHash = "123456",
            balanceYer = 300000.0,
            balanceSar = 1000.0,
            balanceUsd = 2000.0
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.registerOrSyncCloudAccount(bobProfile).getOrThrow()
        }

        // Switch to Alice and execute an atomic transfer of 250 SAR to Bob
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = VibraPayCloudRepository(firestore)
        val aliceAccountId = Random.nextInt(100000, 999999).toString()
        val alicePhone = "77" + Random.nextInt(1000000, 9999999).toString()
        val aliceProfile = OfficialAccountProfile(
            accountId = aliceAccountId,
            firstName = "أحمد",
            fatherName = "علي",
            grandfatherName = "صالح",
            familyName = "الشامي",
            countryDialCode = "+967",
            countryNameAr = "اليمن",
            countryFlag = "🇾🇪",
            phoneNumberLocal = alicePhone,
            fullPhoneDigits = "967$alicePhone",
            email = ALICE_EMAIL,
            passwordPlainOrHash = "123456",
            balanceYer = 250000.0,
            balanceSar = 2000.0,
            balanceUsd = 1500.0
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerOrSyncCloudAccount(aliceProfile).getOrThrow()
        }

        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val txResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.executeAtomicOnlineTransfer(
                transferId = txId,
                ottId = "778899",
                senderAccount = aliceProfile,
                recipientAccount = bobProfile,
                amount = 250.0,
                currency = WalletCurrency.SAR
            )
        }
        assertTrue(txResult.isSuccess)

        // Verify Alice's SAR balance was debited (2000 -> 1750) and Bob's SAR balance was credited (1000 -> 1250)
        val updatedAlice = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.lookupRecipientInCloud(aliceAccountId).getOrThrow()
        }
        val updatedBob = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.lookupRecipientInCloud(bobAccountId).getOrThrow()
        }
        assertEquals(1750.0, updatedAlice?.balanceSar ?: 0.0, 0.01)
        assertEquals(1250.0, updatedBob?.balanceSar ?: 0.0, 0.01)

        // Observe participant transfers stream
        val transfers = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeParticipantTransfers(aliceUid).first { list ->
                list.any { it.transferId == txId }
            }
        }
        assertTrue(transfers.any { it.transferId == txId })
    }

    @Test
    fun getUserPrivateProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = VibraPayCloudRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveUserPrivateProfile(
                accountId = "584920",
                email = ALICE_EMAIL,
                fullQuadName = "أحمد علي صالح الشامي"
            ).getOrThrow()
        }

        // Bob tries to read Alice's private PII profile -> MUST fail with PERMISSION_DENIED
        signInTestUser(BOB_EMAIL)
        val bobRepo = VibraPayCloudRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getUserPrivateProfile(aliceUid).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected FirebaseFirestoreException but got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun observeParticipantTransfers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = VibraPayCloudRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeParticipantTransfers("unauth_uid_123").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@vibrapay.test"
        const val BOB_EMAIL = "bob@vibrapay.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
