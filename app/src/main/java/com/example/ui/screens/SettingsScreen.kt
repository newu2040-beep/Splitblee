package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLocalization
import com.example.model.CurrencyCatalog
import com.example.model.ExpenseItem
import com.example.model.LocalizedStrings
import com.example.model.PaymentEntry
import com.example.model.UserProfile
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.OceanBlueDarkText
import com.example.ui.theme.OceanBluePrimary
import com.example.util.ExportHelper

@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    fallbackEmail: String,
    expenses: List<ExpenseItem>,
    payments: List<PaymentEntry>,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onUpdatePreferences: (
        displayName: String,
        age: Int?,
        gender: String?,
        address: String?,
        language: String,
        currency: String,
        theme: String,
        themeMode: String,
        notificationsEnabled: Boolean,
        locationSharingEnabled: Boolean
    ) -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val currentName = (userProfile?.displayName?.ifBlank { fallbackName } ?: fallbackName).ifBlank { "Rahul Shah" }
    val currentEmail = (userProfile?.email?.ifBlank { fallbackEmail } ?: fallbackEmail).ifBlank { "rahul.shah@gmail.com" }
    val currentTheme = userProfile?.theme ?: "Ocean Blue"
    val currentThemeMode = userProfile?.themeMode ?: "Light"
    val currentLang = userProfile?.preferredLanguage ?: "en"
    val currentCurrency = userProfile?.defaultCurrency ?: "USD"
    val notificationsOn = userProfile?.notificationsEnabled ?: true
    val locationOn = userProfile?.locationSharingEnabled ?: false

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header Bar matching Screen 5 mockup: `<` | `Settings` | Gear Icon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = strings.settings,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("settings_gear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = strings.settings,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Profile Summary Header matching Screen 5 mockup
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatarCircle(
                    name = currentName,
                    size = 86.dp,
                    wearSunglasses = true
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentName,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = OceanBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = currentEmail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        onClick = { showEditProfileDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDCE8FC),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Text(
                            text = strings.editProfile,
                            color = OceanBlueDarkText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // App Preferences Section matching Screen 5 mockup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.appPreferences,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsPreferenceRow(
                            icon = Icons.Filled.Palette,
                            title = strings.theme,
                            value = currentTheme,
                            testTag = "settings_theme_row",
                            onClick = { showThemeDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        SettingsPreferenceRow(
                            icon = Icons.Filled.Language,
                            title = strings.language,
                            value = AppLocalization.languageDisplayName(currentLang),
                            testTag = "settings_language_row",
                            onClick = { showLanguageDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        SettingsPreferenceRow(
                            icon = Icons.Filled.AttachMoney,
                            title = strings.currency,
                            value = currentCurrency,
                            testTag = "settings_currency_row",
                            onClick = { showCurrencyDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsNone,
                                contentDescription = strings.notifications,
                                tint = OceanBlueDarkText,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = strings.notifications,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = notificationsOn,
                                onCheckedChange = { enabled ->
                                    onUpdatePreferences(
                                        currentName,
                                        userProfile?.age?.toInt(),
                                        userProfile?.gender,
                                        userProfile?.address,
                                        currentLang,
                                        currentCurrency,
                                        currentTheme,
                                        currentThemeMode,
                                        enabled,
                                        locationOn
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OceanBluePrimary
                                ),
                                modifier = Modifier.testTag("settings_notifications_switch")
                            )
                        }
                    }
                }
            }
        }

        // Data & Export Section matching Screen 5 mockup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.dataAndExport,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_export_details_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEEF4FC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = strings.exportDetails,
                                tint = OceanBlueDarkText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.exportDetails,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PDF • TXT • CSV",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // About Section matching Screen 5 mockup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.about,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsPreferenceRow(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = strings.helpAndSupport,
                            value = "",
                            testTag = "settings_help_row",
                            onClick = { showHelpDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        SettingsPreferenceRow(
                            icon = Icons.Filled.Info,
                            title = strings.aboutSplitBlee,
                            value = "",
                            testTag = "settings_about_row",
                            onClick = { showAboutDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        SettingsPreferenceRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Sign Out",
                            value = "",
                            testTag = "settings_sign_out_row",
                            onClick = onSignOut
                        )
                    }
                }
            }
        }

        // Footer matching Screen 5 mockup
        item {
            Text(
                text = "Made with \u2764\uFE0F by $currentName",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(currentName) }
        var ageInput by remember { mutableStateOf((userProfile?.age ?: 25L).toString()) }
        var genderInput by remember { mutableStateOf(userProfile?.gender ?: "Male") }
        var addressInput by remember { mutableStateOf(userProfile?.address ?: "Thamel, Kathmandu") }
        var locShare by remember { mutableStateOf(locationOn) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(strings.editProfile, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female").forEach { g ->
                            FilterChip(
                                selected = genderInput == g,
                                onClick = { genderInput = g },
                                label = { Text(g) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Optional Shared Place / Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Share my location pin on Map", fontSize = 13.sp)
                        Switch(checked = locShare, onCheckedChange = { locShare = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePreferences(
                            nameInput,
                            ageInput.toIntOrNull(),
                            genderInput,
                            addressInput,
                            currentLang,
                            currentCurrency,
                            currentTheme,
                            currentThemeMode,
                            notificationsOn,
                            locShare
                        )
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Theme Selector Dialog
    if (showThemeDialog) {
        val themes = listOf("Ocean Blue", "Mint", "Lavender", "Sand", "Graphite", "Rose")
        val modes = listOf("Light", "Dark", "System")
        var selectedTheme by remember { mutableStateOf(currentTheme) }
        var selectedMode by remember { mutableStateOf(currentThemeMode) }

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Appearance & Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Palette (Ocean Blue is Mockup Default):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    themes.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { t ->
                                FilterChip(
                                    selected = selectedTheme == t,
                                    onClick = { selectedTheme = t },
                                    label = { Text(t, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Display Mode:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { m ->
                            FilterChip(
                                selected = selectedMode == m,
                                onClick = { selectedMode = m },
                                label = { Text(m, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePreferences(
                            currentName,
                            userProfile?.age?.toInt(),
                            userProfile?.gender,
                            userProfile?.address,
                            currentLang,
                            currentCurrency,
                            selectedTheme,
                            selectedMode,
                            notificationsOn,
                            locationOn
                        )
                        showThemeDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Language Selector Dialog (Immediate switch without logout!)
    if (showLanguageDialog) {
        val langs = listOf(
            "en" to "English (Default)",
            "ne" to "Nepali — नेपाली (Devanagari)",
            "hi" to "Hindi — हिन्दी (Devanagari)"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    langs.forEach { (code, label) ->
                        Surface(
                            onClick = {
                                onUpdatePreferences(
                                    currentName,
                                    userProfile?.age?.toInt(),
                                    userProfile?.gender,
                                    userProfile?.address,
                                    code,
                                    currentCurrency,
                                    currentTheme,
                                    currentThemeMode,
                                    notificationsOn,
                                    locationOn
                                )
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentLang == code) Color(0xFFDCE8FC) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (currentLang == code) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Searchable ISO 4217 Currency Selector Dialog
    if (showCurrencyDialog) {
        var query by remember { mutableStateOf("") }
        val filtered = CurrencyCatalog.supported.filter {
            query.isBlank() ||
                it.code.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Default Currency (ISO 4217)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search NPR, INR, PKR, USD, EUR...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filtered, key = { it.code }) { spec ->
                            Surface(
                                onClick = {
                                    onUpdatePreferences(
                                        currentName,
                                        userProfile?.age?.toInt(),
                                        userProfile?.gender,
                                        userProfile?.address,
                                        currentLang,
                                        spec.code,
                                        currentTheme,
                                        currentThemeMode,
                                        notificationsOn,
                                        locationOn
                                    )
                                    showCurrencyDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentCurrency == spec.code) Color(0xFFDCE8FC) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${spec.code} — ${spec.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(spec.symbol.trim(), fontWeight = FontWeight.Bold, color = OceanBluePrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Data & Export Dialog (Scope Selection + PDF / TXT / CSV Export)
    if (showExportDialog) {
        var scopeSelection by remember { mutableStateOf("All Records") }
        val scopedExpenses = when (scopeSelection) {
            "Paid Only" -> expenses.filter { it.status == "PAID" }
            "Unpaid Only" -> expenses.filter { it.status != "PAID" }
            else -> expenses
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Data & Export (PDF • TXT • CSV)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Scope preview: ${scopedExpenses.size} expenses & ${payments.size} ledger payments",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All Records", "Paid Only", "Unpaid Only").forEach { s ->
                            FilterChip(
                                selected = scopeSelection == s,
                                onClick = { scopeSelection = s },
                                label = { Text(s, fontSize = 11.sp) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val pdfBytes = ExportHelper.buildPdfSummaryBytes(scopeSelection, scopedExpenses, payments)
                            val txtReport = ExportHelper.buildTxtReport(
                                "$scopeSelection (PDF Generated: ${pdfBytes.size} bytes)",
                                scopedExpenses,
                                payments
                            )
                            ExportHelper.shareTextContent(context, "SplitBlee PDF/Report ($scopeSelection)", txtReport)
                            showExportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export PDF & Summary Report")
                    }
                    OutlinedButton(
                        onClick = {
                            val txt = ExportHelper.buildTxtReport(scopeSelection, scopedExpenses, payments)
                            ExportHelper.shareTextContent(context, "SplitBlee TXT Export", txt)
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export Plain-Text (TXT — UTF-8)")
                    }
                    OutlinedButton(
                        onClick = {
                            val csv = ExportHelper.buildCsvReport(scopedExpenses)
                            ExportHelper.shareTextContent(context, "SplitBlee CSV Export", csv)
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export Spreadsheet (CSV — UTF-8)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(strings.helpAndSupport, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "• Create Equal, Percentage, Exact, or Item-based splits with deterministic minor-unit rounding.\n" +
                        "• Record payments in Split Details to update statuses in real time.\n" +
                        "• Export PDF, TXT, or CSV reports anytime from Settings → Data & Export."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About SplitBlee v1.0", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "SplitBlee — Split. Share. Enjoy.\n\n" +
                        "Built with Ocean-Blue minimal mobile-first design, real-time Cloud Firestore synchronization, ISO 4217 multi-currency minor-unit precision, and privacy-first friend map controls."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsPreferenceRow(
    icon: ImageVector,
    title: String,
    value: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = OceanBlueDarkText,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
