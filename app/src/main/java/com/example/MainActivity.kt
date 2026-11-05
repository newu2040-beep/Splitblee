package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SplitBleeRepository
import com.example.model.AppLocalization
import com.example.model.ExpenseItem
import com.example.ui.SplitBleeViewModel
import com.example.ui.UiState
import com.example.ui.components.SplitBleeBottomBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FriendsMapScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.HistoryAnalyticsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NewSplitScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplitDetailsScreen
import com.example.ui.screens.signOutSplitBlee
import com.example.ui.theme.SplitBleeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitBleeRootApp()
        }
    }
}

@Composable
fun SplitBleeRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        // Screen 1: Login (Unauthenticated Gate — no Firestore ViewModels or listeners created)
        SplitBleeTheme(themeName = "Ocean Blue", themeMode = "Light") {
            LoginScreen(
                strings = AppLocalization.forLanguage("en"),
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        }
    } else {
        // Authenticated Session: Instantiate ViewModel scoped to authenticated user's UID
        val viewModel: SplitBleeViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    SplitBleeViewModel(
                        repository = SplitBleeRepository(db),
                        currentUserId = user.uid,
                        initialDisplayName = user.displayName ?: "Rahul Shah",
                        initialEmail = user.email ?: "rahul.shah@gmail.com",
                        initialPhotoUrl = user.photoUrl?.toString()
                    )
                }
            }
        )

        AuthenticatedSplitBleeApp(
            viewModel = viewModel,
            fallbackName = user.displayName ?: "Rahul Shah",
            fallbackEmail = user.email ?: "rahul.shah@gmail.com",
            onSignedOut = {
                currentUser = null
            }
        )
    }
}

@Composable
private fun AuthenticatedSplitBleeApp(
    viewModel: SplitBleeViewModel,
    fallbackName: String,
    fallbackEmail: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
    val friendsState by viewModel.friendsState.collectAsStateWithLifecycle()
    val groupsState by viewModel.groupsState.collectAsStateWithLifecycle()
    val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
    val friends = (friendsState as? UiState.Success)?.data ?: emptyList()
    val groups = (groupsState as? UiState.Success)?.data ?: emptyList()
    val payments = (paymentsState as? UiState.Success)?.data ?: emptyList()

    val themeName = userProfile?.theme ?: "Ocean Blue"
    val themeMode = userProfile?.themeMode ?: "Light"
    val langCode = userProfile?.preferredLanguage ?: "en"
    val defaultCurrency = userProfile?.defaultCurrency ?: "USD"
    val strings = remember(langCode) { AppLocalization.forLanguage(langCode) }

    // Screen Navigation State following the 5-Screen Approved Mockup Sequence:
    // "dashboard" (Screen 2), "map" (Screen 3), "split_details" (Screen 4), "settings" (Screen 5),
    // plus secondary sheets/pages "new_split", "groups", "history"
    var currentScreen by rememberSaveable { mutableStateOf("dashboard") }
    var selectedExpenseId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val showBottomBar = currentScreen in listOf("dashboard", "map", "settings", "groups", "history")

    SplitBleeTheme(themeName = themeName, themeMode = themeMode) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    SplitBleeBottomBar(
                        currentTab = currentScreen,
                        strings = strings,
                        onSelectTab = { dest -> currentScreen = dest },
                        onNewSplitClick = { currentScreen = "new_split" }
                    )
                }
            }
        ) { innerPadding ->
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        "dashboard" -> {
                            DashboardScreen(
                                userProfile = userProfile,
                                fallbackName = fallbackName,
                                expensesState = expensesState,
                                strings = strings,
                                onOpenNewSplit = { currentScreen = "new_split" },
                                onOpenGroups = { currentScreen = "groups" },
                                onOpenHistory = { currentScreen = "history" },
                                onOpenMap = { currentScreen = "map" },
                                onOpenSettings = { currentScreen = "settings" },
                                onOpenExpenseDetail = { expenseId ->
                                    selectedExpenseId = expenseId
                                    currentScreen = "split_details"
                                },
                                onSeedStarterData = {
                                    seedReferenceMockupData(viewModel, defaultCurrency)
                                }
                            )
                        }

                        "map" -> {
                            FriendsMapScreen(
                                userProfile = userProfile,
                                friendsState = friendsState,
                                onBack = { currentScreen = "dashboard" },
                                onAddFriend = { name, email, age, gender, address, locShare, curr, avatarIdx ->
                                    viewModel.addFriend(name, email, age, gender, address, locShare, curr, avatarIdx)
                                },
                                onToggleFriendLocation = { friend, enabled ->
                                    viewModel.toggleFriendLocation(friend, enabled)
                                },
                                onToggleFriendBlocked = { friend ->
                                    viewModel.toggleFriendBlocked(friend)
                                },
                                onSeedStarterFriends = {
                                    seedReferenceFriends(viewModel, defaultCurrency)
                                }
                            )
                        }

                        "split_details" -> {
                            val activeExpense: ExpenseItem? = expenses.firstOrNull { it.id == selectedExpenseId }
                                ?: expenses.firstOrNull()
                            if (activeExpense != null) {
                                SplitDetailsScreen(
                                    expense = activeExpense,
                                    strings = strings,
                                    onBack = { currentScreen = "dashboard" },
                                    onUpdateExpenseInfo = { newTitle, newPlace, newNotes ->
                                        viewModel.updateExpenseInfo(activeExpense, newTitle, newPlace, newNotes)
                                    },
                                    onRecordPayment = { payer, payee, amtMinor, method, ref, entryType ->
                                        viewModel.recordPayment(
                                            expense = activeExpense,
                                            payerName = payer,
                                            payeeName = payee,
                                            amountMinor = amtMinor,
                                            method = method,
                                            reference = ref,
                                            entryType = entryType
                                        )
                                    }
                                )
                            } else {
                                currentScreen = "dashboard"
                            }
                        }

                        "settings" -> {
                            SettingsScreen(
                                userProfile = userProfile,
                                fallbackName = fallbackName,
                                fallbackEmail = fallbackEmail,
                                expenses = expenses,
                                payments = payments,
                                strings = strings,
                                onBack = { currentScreen = "dashboard" },
                                onUpdatePreferences = { name, age, gender, addr, lang, curr, thm, mode, notif, loc ->
                                    viewModel.updateProfileAndPreferences(
                                        current = userProfile,
                                        displayName = name,
                                        age = age,
                                        gender = gender,
                                        address = addr,
                                        preferredLanguage = lang,
                                        defaultCurrency = curr,
                                        theme = thm,
                                        themeMode = mode,
                                        notificationsEnabled = notif,
                                        locationSharingEnabled = loc
                                    )
                                },
                                onSignOut = {
                                    signOutSplitBlee(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = onSignedOut,
                                        scope = scope
                                    )
                                }
                            )
                        }

                        "new_split" -> {
                            NewSplitScreen(
                                defaultCurrency = defaultCurrency,
                                groups = groups,
                                friends = friends,
                                userName = userProfile?.displayName ?: fallbackName,
                                onBack = { currentScreen = "dashboard" },
                                onSaveSplit = { title, cat, place, notes, grpId, grpName, curr, sub, tax, srv, tip, ext, disc, method, names, shares ->
                                    viewModel.createSplit(
                                        title = title,
                                        category = cat,
                                        place = place,
                                        notes = notes,
                                        groupId = grpId,
                                        groupName = grpName,
                                        currency = curr,
                                        subtotalInput = sub,
                                        taxPercent = tax,
                                        serviceChargeInput = srv,
                                        tipInput = tip,
                                        extraInput = ext,
                                        discountInput = disc,
                                        splitMethod = method,
                                        participantNames = names,
                                        customSharesMinor = shares,
                                        onCreated = { createdId ->
                                            selectedExpenseId = createdId
                                            currentScreen = "split_details"
                                        }
                                    )
                                }
                            )
                        }

                        "groups" -> {
                            GroupsScreen(
                                groups = groups,
                                defaultCurrency = defaultCurrency,
                                userName = userProfile?.displayName ?: fallbackName,
                                onBack = { currentScreen = "dashboard" },
                                onCreateGroup = { name, curr, members ->
                                    viewModel.createGroup(name, curr, members)
                                },
                                onNewSplitClick = { currentScreen = "new_split" }
                            )
                        }

                        "history" -> {
                            HistoryAnalyticsScreen(
                                expenses = expenses,
                                payments = payments,
                                defaultCurrency = defaultCurrency,
                                onBack = { currentScreen = "dashboard" },
                                onOpenExpenseDetail = { expenseId ->
                                    selectedExpenseId = expenseId
                                    currentScreen = "split_details"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun seedReferenceFriends(viewModel: SplitBleeViewModel, currency: String) {
    viewModel.addFriend(
        name = "Rahul Shah",
        email = "rahul.shah@gmail.com",
        age = 25,
        gender = "Male",
        address = "Thamel, Kathmandu",
        locationSharing = true,
        currency = currency,
        avatarIndex = 0
    )
    viewModel.addFriend(
        name = "Priya Sharma",
        email = "priya@splitblee.app",
        age = 24,
        gender = "Female",
        address = "Lazimpat, Kathmandu",
        locationSharing = true,
        currency = currency,
        avatarIndex = 1
    )
    viewModel.addFriend(
        name = "Aarav Thapa",
        email = "aarav@splitblee.app",
        age = 26,
        gender = "Male",
        address = "Jhamsikhel, Lalitpur",
        locationSharing = true,
        currency = currency,
        avatarIndex = 2
    )
}

private fun seedReferenceMockupData(viewModel: SplitBleeViewModel, currency: String) {
    seedReferenceFriends(viewModel, currency)
    viewModel.createGroup(
        name = "Thamel Dinner Crew",
        currency = currency,
        members = listOf("Rahul Shah", "Aarav Thapa", "Priya Sharma", "Sita Gurung")
    )
    viewModel.createSplit(
        title = "Pizza Hut Dinner",
        category = "Food & Drink",
        place = "Pizza Hut, Thamel",
        notes = "Wood-fired pizza dinner",
        groupId = null,
        groupName = "Thamel Dinner Crew",
        currency = currency,
        subtotalInput = "72.50",
        taxPercent = 13,
        serviceChargeInput = "0.00",
        tipInput = "0.00",
        extraInput = "0.00",
        discountInput = "5.00",
        splitMethod = "EQUAL",
        participantNames = listOf("Rahul Shah", "Aarav Thapa", "Priya Sharma", "Sita Gurung")
    )
    viewModel.createSplit(
        title = "Shopping Mall",
        category = "Shopping",
        place = "City Center Mall",
        notes = "Weekend shopping",
        groupId = null,
        groupName = null,
        currency = currency,
        subtotalInput = "120.00",
        taxPercent = 0,
        serviceChargeInput = "0.00",
        tipInput = "0.00",
        extraInput = "0.00",
        discountInput = "0.00",
        splitMethod = "EQUAL",
        participantNames = listOf("Rahul Shah", "Aarav Thapa", "Priya Sharma")
    )
    viewModel.createSplit(
        title = "Coffee Break",
        category = "Coffee",
        place = "Himalayan Java",
        notes = "Morning espresso",
        groupId = null,
        groupName = null,
        currency = currency,
        subtotalInput = "36.00",
        taxPercent = 0,
        serviceChargeInput = "0.00",
        tipInput = "0.00",
        extraInput = "0.00",
        discountInput = "0.00",
        splitMethod = "EQUAL",
        participantNames = listOf("Rahul Shah", "Priya Sharma")
    )
}
