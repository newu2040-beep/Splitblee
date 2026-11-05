package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitBleeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveExpense_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SplitBleeRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createExpense(
                title = "Pizza Hut Dinner",
                category = "Food & Drink",
                place = "Pizza Hut, Thamel",
                notes = "Team dinner",
                groupId = null,
                groupName = null,
                currency = "USD",
                subtotalMinor = 7250L,
                taxPercent = 13,
                serviceChargeMinor = 0L,
                tipMinor = 0L,
                extraAmountMinor = 0L,
                discountMinor = 500L,
                splitMethod = "EQUAL",
                participantNames = listOf("Rahul Shah", "Aarav", "Priya", "Sita"),
                receiptUri = "content://media/picker/0/com.android.providers.media.photopicker/media/1001"
            )
        }
        assertTrue(createResult.isSuccess)
        val expenseId = createResult.getOrThrow()
        assertTrue(expenseId.isNotEmpty())

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateUserProfile(
                displayName = "Rahul Shah",
                age = 25,
                gender = "Male",
                address = "Thamel, Kathmandu",
                preferredLanguage = "en",
                defaultCurrency = "USD",
                theme = "Pastel Mint",
                themeMode = "Light",
                uiScaleMode = "Compact",
                photoUrl = "content://media/picker/0/com.android.providers.media.photopicker/media/1002",
                notificationsEnabled = true,
                locationSharingEnabled = true
            )
        }
        assertTrue(profileRes.isSuccess)

        val emittedExpenses = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExpenses(uid).first { list -> list.any { it.id == expenseId } }
        }
        assertTrue(emittedExpenses.any { it.id == expenseId && it.totalMinor == 7693L })
    }

    @Test
    fun getExpenseById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = SplitBleeRepository(firestore)
        val expenseId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createExpense(
                title = "Shopping Mall",
                category = "Shopping",
                place = "City Center",
                notes = null,
                groupId = null,
                groupName = null,
                currency = "USD",
                subtotalMinor = 12000L,
                taxPercent = 0,
                serviceChargeMinor = 0L,
                tipMinor = 0L,
                extraAmountMinor = 0L,
                discountMinor = 0L,
                splitMethod = "EQUAL",
                participantNames = listOf("Rahul Shah", "Aarav", "Priya")
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SplitBleeRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getExpenseById(expenseId)
        }
        assertTrue(result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeExpenses_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SplitBleeRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeExpenses("unauth_uid").first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@splitblee.test"
        const val BOB_EMAIL = "bob@splitblee.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
