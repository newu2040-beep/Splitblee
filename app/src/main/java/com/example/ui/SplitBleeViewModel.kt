package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SplitBleeRepository
import com.example.model.CurrencyCatalog
import com.example.model.ExpenseGroup
import com.example.model.ExpenseItem
import com.example.model.FriendProfile
import com.example.model.PaymentEntry
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class SplitBleeViewModel(
    private val repository: SplitBleeRepository,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String,
    private val initialPhotoUrl: String?
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val expensesState: StateFlow<UiState<List<ExpenseItem>>> = repository.observeExpenses(currentUserId)
        .map<List<ExpenseItem>, UiState<List<ExpenseItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing expenses", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load expenses"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val friendsState: StateFlow<UiState<List<FriendProfile>>> = repository.observeFriends(currentUserId)
        .map<List<FriendProfile>, UiState<List<FriendProfile>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing friends", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load friends"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val groupsState: StateFlow<UiState<List<ExpenseGroup>>> = repository.observeGroups(currentUserId)
        .map<List<ExpenseGroup>, UiState<List<ExpenseGroup>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing groups", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load groups"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val paymentsState: StateFlow<UiState<List<PaymentEntry>>> = repository.observePayments(currentUserId)
        .map<List<PaymentEntry>, UiState<List<PaymentEntry>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing payments", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load payments"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfileInitialized(
                displayName = initialDisplayName,
                email = initialEmail,
                photoUrl = initialPhotoUrl
            )
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun createSplit(
        title: String,
        category: String,
        place: String,
        notes: String?,
        groupId: String?,
        groupName: String?,
        currency: String,
        subtotalInput: String,
        taxPercent: Int,
        serviceChargeInput: String,
        tipInput: String,
        extraInput: String,
        discountInput: String,
        splitMethod: String,
        participantNames: List<String>,
        customSharesMinor: List<Long>? = null,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val subtotalMinor = CurrencyCatalog.parseMajorToMinor(subtotalInput, currency).coerceAtLeast(100L)
            val serviceChargeMinor = CurrencyCatalog.parseMajorToMinor(serviceChargeInput, currency)
            val tipMinor = CurrencyCatalog.parseMajorToMinor(tipInput, currency)
            val extraMinor = CurrencyCatalog.parseMajorToMinor(extraInput, currency)
            val discountMinor = CurrencyCatalog.parseMajorToMinor(discountInput, currency)

            val res = repository.createExpense(
                title = title,
                category = category,
                place = place,
                notes = notes,
                groupId = groupId,
                groupName = groupName,
                currency = currency,
                subtotalMinor = subtotalMinor,
                taxPercent = taxPercent,
                serviceChargeMinor = serviceChargeMinor,
                tipMinor = tipMinor,
                extraAmountMinor = extraMinor,
                discountMinor = discountMinor,
                splitMethod = splitMethod,
                participantNames = participantNames,
                customSharesMinor = customSharesMinor
            )
            _isBusy.value = false
            res.onSuccess { id ->
                _statusBanner.value = "Split '${title.ifBlank { "Shared Expense" }}' saved in real time."
                onCreated(id)
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to create split"
            }
        }
    }

    fun updateExpenseInfo(
        expense: ExpenseItem,
        newTitle: String,
        newPlace: String,
        newNotes: String?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateExpenseTitleAndNote(expense, newTitle, newPlace, newNotes)
            _isBusy.value = false
            res.onSuccess {
                _statusBanner.value = "Split details updated."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to update split"
            }
        }
    }

    fun recordPayment(
        expense: ExpenseItem,
        payerName: String,
        payeeName: String,
        amountMinor: Long,
        method: String,
        reference: String?,
        entryType: String = "PAYMENT"
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.recordPaymentAndSyncExpense(
                expense = expense,
                payerName = payerName,
                payeeName = payeeName,
                amountMinor = amountMinor,
                method = method,
                reference = reference,
                entryType = entryType
            )
            _isBusy.value = false
            res.onSuccess {
                _statusBanner.value = if (entryType == "REVERSAL") {
                    "Ledger reversal recorded; split marked as Unpaid."
                } else {
                    "Payment recorded in immutable ledger."
                }
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to record payment"
            }
        }
    }

    fun addFriend(
        name: String,
        email: String?,
        age: Int?,
        gender: String,
        address: String?,
        locationSharing: Boolean,
        currency: String,
        avatarIndex: Int
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val lat = if (locationSharing) 27.714 + ((avatarIndex % 5) * 0.004) else null
            val lng = if (locationSharing) 85.312 + ((avatarIndex % 5) * 0.004) else null
            val res = repository.addFriend(
                name = name,
                email = email,
                age = age,
                gender = gender,
                address = address,
                locationSharing = locationSharing,
                latitude = lat,
                longitude = lng,
                currency = currency,
                avatarIndex = avatarIndex
            )
            _isBusy.value = false
            res.onSuccess {
                _statusBanner.value = "Friend '$name' added."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to add friend"
            }
        }
    }

    fun toggleFriendLocation(friend: FriendProfile, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleFriendLocationSharing(friend, enabled)
                .onSuccess {
                    _statusBanner.value = if (enabled) {
                        "Location sharing enabled for ${friend.name}."
                    } else {
                        "Location sharing revoked for ${friend.name}."
                    }
                }
        }
    }

    fun toggleFriendBlocked(friend: FriendProfile) {
        viewModelScope.launch {
            repository.toggleFriendBlockStatus(friend)
                .onSuccess {
                    _statusBanner.value = "Updated block status for ${friend.name}."
                }
        }
    }

    fun createGroup(name: String, currency: String, members: List<String>) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createGroup(name, currency, members)
            _isBusy.value = false
            res.onSuccess {
                _statusBanner.value = "Group '$name' created."
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to create group"
            }
        }
    }

    fun updateProfileAndPreferences(
        current: UserProfile?,
        displayName: String = current?.displayName ?: initialDisplayName.ifBlank { "SplitBlee User" },
        age: Int? = current?.age?.toInt(),
        gender: String? = current?.gender,
        address: String? = current?.address,
        preferredLanguage: String = current?.preferredLanguage ?: "en",
        defaultCurrency: String = current?.defaultCurrency ?: "USD",
        theme: String = current?.theme ?: "Ocean Blue",
        themeMode: String = current?.themeMode ?: "Light",
        notificationsEnabled: Boolean = current?.notificationsEnabled ?: true,
        locationSharingEnabled: Boolean = current?.locationSharingEnabled ?: false
    ) {
        viewModelScope.launch {
            repository.updateUserProfile(
                displayName = displayName,
                age = age,
                gender = gender,
                address = address,
                preferredLanguage = preferredLanguage,
                defaultCurrency = defaultCurrency,
                theme = theme,
                themeMode = themeMode,
                notificationsEnabled = notificationsEnabled,
                locationSharingEnabled = locationSharingEnabled
            ).onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to save settings"
            }
        }
    }

    private companion object {
        const val TAG = "SplitBleeVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
