package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ExpenseGroup
import com.example.model.ExpenseItem
import com.example.model.FriendProfile
import com.example.model.ParticipantShareCodec
import com.example.model.ParticipantShareDetail
import com.example.model.PaymentEntry
import com.example.model.SplitCalculator
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SplitBleeRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfileInitialized(
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existing = existingSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) return@runCatching existing
            }
            val safeName = displayName.trim().ifEmpty { email.substringBefore("@").ifEmpty { "SplitBlee User" } }.take(100)
            val safeEmail = email.trim().ifEmpty { "user@splitblee.app" }.take(200)
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to safeName,
                "email" to safeEmail,
                "preferredLanguage" to "en",
                "defaultCurrency" to "USD",
                "theme" to "Ocean Blue",
                "themeMode" to "Light",
                "uiScaleMode" to "Auto",
                "notificationsEnabled" to true,
                "locationSharingEnabled" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!photoUrl.isNullOrBlank()) {
                payload["photoUrl"] = photoUrl.take(500)
            }
            docRef.set(payload).await()
            val createdSnap = docRef.get().await()
            createdSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = uid, userId = uid, displayName = safeName, email = safeEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        age: Int?,
        gender: String?,
        address: String?,
        preferredLanguage: String,
        defaultCurrency: String,
        theme: String,
        themeMode: String,
        uiScaleMode: String = "Auto",
        photoUrl: String? = null,
        notificationsEnabled: Boolean,
        locationSharingEnabled: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val safeScale = if (uiScaleMode in listOf("Auto", "Compact", "Standard", "Expanded")) uiScaleMode else "Auto"
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName.trim().ifEmpty { "SplitBlee User" }.take(100),
                "preferredLanguage" to preferredLanguage,
                "defaultCurrency" to defaultCurrency,
                "theme" to theme,
                "themeMode" to themeMode,
                "uiScaleMode" to safeScale,
                "notificationsEnabled" to notificationsEnabled,
                "locationSharingEnabled" to locationSharingEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (age != null && age in 13..120) {
                updates["age"] = age.toLong()
            }
            if (!gender.isNullOrBlank() && gender in listOf("Male", "Female")) {
                updates["gender"] = gender
            }
            if (!address.isNullOrBlank()) {
                updates["address"] = address.trim().take(250)
            }
            if (!photoUrl.isNullOrBlank()) {
                updates["photoUrl"] = photoUrl.trim().take(500)
            }
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(updates).await()
            } else {
                val createPayload = updates.toMutableMap().apply {
                    put("userId", uid)
                    put("email", (auth.currentUser?.email ?: "user@splitblee.app").take(200))
                    put("createdAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeExpenses(userId: String = requireUserId()): Flow<List<ExpenseItem>> = flow {
        val path = "expenses"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ExpenseItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserExpenses(): Result<List<ExpenseItem>> = runCatching {
        val uid = requireUserId()
        val path = "expenses"
        try {
            val snap = db.collection(path)
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snap.toObjects(ExpenseItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getExpenseById(expenseId: String): Result<ExpenseItem> = runCatching {
        val docRef = db.collection("expenses").document(expenseId)
        try {
            val snap = docRef.get().await()
            snap.toObject(ExpenseItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Expense not found")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createExpense(
        title: String,
        category: String,
        place: String,
        notes: String?,
        groupId: String?,
        groupName: String?,
        currency: String,
        subtotalMinor: Long,
        taxPercent: Int,
        serviceChargeMinor: Long,
        tipMinor: Long,
        extraAmountMinor: Long,
        discountMinor: Long,
        splitMethod: String,
        participantNames: List<String>,
        customSharesMinor: List<Long>? = null,
        receiptUri: String? = null,
        initialPaidByAll: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("expenses").document(docId)

        val safeNames = participantNames.map { it.trim().take(80) }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("You") }
            .take(20)
        val (taxMinor, totalMinor) = SplitCalculator.computeTotalMinor(
            subtotalMinor = subtotalMinor,
            taxPercent = taxPercent,
            serviceChargeMinor = serviceChargeMinor,
            tipMinor = tipMinor,
            extraAmountMinor = extraAmountMinor,
            discountMinor = discountMinor
        )
        val assignedList = if (customSharesMinor != null && customSharesMinor.size == safeNames.size && customSharesMinor.sum() == totalMinor) {
            customSharesMinor
        } else {
            SplitCalculator.calculateEqualShares(totalMinor, safeNames.size)
        }

        val shareDetails = safeNames.mapIndexed { idx, name ->
            val assigned = assignedList[idx]
            val isPaid = initialPaidByAll || idx == 0
            ParticipantShareDetail(
                name = name,
                assignedMinor = assigned,
                paidMinor = if (isPaid) assigned else 0L,
                status = if (isPaid) "PAID" else "UNPAID"
            )
        }

        val paidMinor = shareDetails.sumOf { it.paidMinor }
        val owedToYouMinor = shareDetails.drop(1).sumOf { it.remainingMinor }
        val status = if (initialPaidByAll || owedToYouMinor == 0L) "PAID" else SplitCalculator.deriveExpenseStatus(totalMinor, paidMinor)

        val now = Date()
        val expenseDateStr = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.US).format(now)
        val monthKeyStr = SimpleDateFormat("yyyy-MM", Locale.US).format(now)

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to title.trim().ifEmpty { "Shared Expense" }.take(120),
            "category" to category.trim().ifEmpty { "Food & Drink" }.take(60),
            "place" to place.trim().ifEmpty { "Local Venue" }.take(150),
            "expenseDate" to expenseDateStr,
            "monthKey" to monthKeyStr,
            "currency" to currency,
            "subtotalMinor" to subtotalMinor.coerceAtLeast(1L),
            "taxPercent" to taxPercent.coerceIn(0, 100).toLong(),
            "taxMinor" to taxMinor,
            "serviceChargeMinor" to serviceChargeMinor.coerceAtLeast(0L),
            "tipMinor" to tipMinor.coerceAtLeast(0L),
            "extraAmountMinor" to extraAmountMinor.coerceAtLeast(0L),
            "discountMinor" to discountMinor.coerceAtLeast(0L),
            "totalMinor" to totalMinor,
            "splitMethod" to splitMethod,
            "status" to status,
            "paidMinor" to paidMinor,
            "owedToYouMinor" to owedToYouMinor,
            "youOweMinor" to 0L,
            "participantCount" to safeNames.size.toLong(),
            "participantNames" to safeNames,
            "participantSharesSummary" to ParticipantShareCodec.encode(shareDetails),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!notes.isNullOrBlank()) payload["notes"] = notes.trim().take(500)
        if (!groupId.isNullOrBlank()) payload["groupId"] = groupId.trim().take(128)
        if (!groupName.isNullOrBlank()) payload["groupName"] = groupName.trim().take(100)
        if (!receiptUri.isNullOrBlank()) payload["receiptUri"] = receiptUri.trim().take(500)

        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateExpenseTitleAndNote(
        expense: ExpenseItem,
        newTitle: String,
        newPlace: String,
        newNotes: String?,
        newReceiptUri: String? = expense.receiptUri
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("expenses").document(expense.id)
        val updates = mutableMapOf<String, Any>(
            "title" to newTitle.trim().ifEmpty { expense.title }.take(120),
            "place" to newPlace.trim().ifEmpty { expense.place }.take(150),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!newNotes.isNullOrBlank()) {
            updates["notes"] = newNotes.trim().take(500)
        }
        if (!newReceiptUri.isNullOrBlank()) {
            updates["receiptUri"] = newReceiptUri.trim().take(500)
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordPaymentAndSyncExpense(
        expense: ExpenseItem,
        payerName: String,
        payeeName: String,
        amountMinor: Long,
        method: String,
        reference: String?,
        entryType: String = "PAYMENT"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val paymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val paymentRef = db.collection("payments").document(paymentId)
        val expenseRef = db.collection("expenses").document(expense.id)

        val safeAmount = amountMinor.coerceAtLeast(1L)
        val paymentPayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "expenseId" to expense.id,
            "expenseTitle" to expense.title.take(120),
            "payerName" to payerName.trim().ifEmpty { "Participant" }.take(100),
            "payeeName" to payeeName.trim().ifEmpty { "You" }.take(100),
            "amountMinor" to safeAmount,
            "currency" to expense.currency,
            "method" to method.trim().ifEmpty { "Manual" }.take(60),
            "entryType" to entryType,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!expense.groupId.isNullOrBlank()) paymentPayload["groupId"] = expense.groupId
        if (!reference.isNullOrBlank()) paymentPayload["reference"] = reference.trim().take(200)

        val existingShares = ParticipantShareCodec.decode(
            expense.participantSharesSummary,
            expense.participantNames,
            expense.totalMinor,
            expense.status
        )
        val updatedShares = existingShares.map { share ->
            if (share.name.equals(payerName, ignoreCase = true) || payerName == "ALL_PARTICIPANTS") {
                val newPaid = when (entryType) {
                    "REVERSAL" -> 0L
                    else -> if (payerName == "ALL_PARTICIPANTS") share.assignedMinor else (share.paidMinor + safeAmount)
                }
                val newStatus = SplitCalculator.deriveExpenseStatus(share.assignedMinor, newPaid)
                share.copy(paidMinor = newPaid, status = newStatus)
            } else {
                share
            }
        }

        val newTotalPaid = updatedShares.sumOf { it.paidMinor }
        val newOwedToYou = updatedShares.drop(1).sumOf { it.remainingMinor }
        val newExpenseStatus = when {
            entryType == "REVERSAL" && newTotalPaid <= (updatedShares.firstOrNull()?.assignedMinor ?: 0L) -> "UNPAID"
            newOwedToYou == 0L && newTotalPaid >= expense.totalMinor -> "PAID"
            else -> SplitCalculator.deriveExpenseStatus(expense.totalMinor, newTotalPaid)
        }

        try {
            paymentRef.set(paymentPayload).await()
            expenseRef.update(
                mapOf(
                    "paidMinor" to newTotalPaid,
                    "owedToYouMinor" to newOwedToYou,
                    "status" to newExpenseStatus,
                    "participantSharesSummary" to ParticipantShareCodec.encode(updatedShares),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            paymentId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, expenseRef.path)
            throw e
        }
    }

    fun observePayments(userId: String = requireUserId()): Flow<List<PaymentEntry>> = flow {
        val path = "payments"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PaymentEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeFriends(userId: String = requireUserId()): Flow<List<FriendProfile>> = flow {
        val path = "friends"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FriendProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addFriend(
        name: String,
        email: String?,
        age: Int?,
        gender: String,
        address: String?,
        locationSharing: Boolean,
        latitude: Double?,
        longitude: Double?,
        owesYouMinor: Long = 0L,
        youOweMinor: Long = 0L,
        splitsCount: Long = 0L,
        currency: String = "USD",
        avatarIndex: Int = 0
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "frd_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("friends").document(docId)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to name.trim().ifEmpty { "Friend" }.take(100),
            "gender" to if (gender in listOf("Male", "Female")) gender else "Male",
            "status" to "Accepted",
            "isOnline" to true,
            "locationSharing" to locationSharing,
            "owesYouMinor" to owesYouMinor.coerceAtLeast(0L),
            "youOweMinor" to youOweMinor.coerceAtLeast(0L),
            "splitsCount" to splitsCount.coerceAtLeast(0L),
            "currency" to currency,
            "avatarIndex" to avatarIndex.coerceIn(0, 20).toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!email.isNullOrBlank()) payload["email"] = email.trim().take(200)
        if (age != null && age in 13..120) payload["age"] = age.toLong()
        if (!address.isNullOrBlank()) payload["address"] = address.trim().take(250)
        if (locationSharing && latitude != null && longitude != null) {
            payload["latitude"] = latitude.coerceIn(-90.0, 90.0)
            payload["longitude"] = longitude.coerceIn(-180.0, 180.0)
        }
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFriendLocationSharing(
        friend: FriendProfile,
        enabled: Boolean,
        liveLat: Double? = null,
        liveLng: Double? = null,
        liveAddress: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("friends").document(friend.id)
        val updates = mutableMapOf<String, Any>(
            "locationSharing" to enabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (enabled && liveLat != null && liveLng != null) {
            updates["latitude"] = liveLat.coerceIn(-90.0, 90.0)
            updates["longitude"] = liveLng.coerceIn(-180.0, 180.0)
        }
        if (enabled && !liveAddress.isNullOrBlank()) {
            updates["address"] = liveAddress.trim().take(250)
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateFriendBalances(
        friend: FriendProfile,
        owesYouMinor: Long,
        youOweMinor: Long,
        splitsCount: Long
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("friends").document(friend.id)
        try {
            docRef.update(
                mapOf(
                    "owesYouMinor" to owesYouMinor.coerceAtLeast(0L),
                    "youOweMinor" to youOweMinor.coerceAtLeast(0L),
                    "splitsCount" to splitsCount.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFriendBlockStatus(friend: FriendProfile): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("friends").document(friend.id)
        val nextStatus = if (friend.status == "Blocked") "Accepted" else "Blocked"
        try {
            docRef.update(
                mapOf(
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeGroups(userId: String = requireUserId()): Flow<List<ExpenseGroup>> = flow {
        val path = "groups"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ExpenseGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createGroup(
        name: String,
        settlementCurrency: String,
        memberNames: List<String>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = "grp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("groups").document(docId)
        val safeMembers = memberNames.map { it.trim().take(80) }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("You") }
            .take(20)
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().ifEmpty { "Shared Group" }.take(100),
            "settlementCurrency" to settlementCurrency,
            "memberNames" to safeMembers,
            "memberCount" to safeMembers.size.toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
