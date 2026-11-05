package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
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
import com.example.ui.theme.LocalActivePalette
import com.example.ui.theme.LocalAppDimens
import com.example.ui.theme.SplitBleePalettes
import com.example.util.ExportHelper
import com.example.util.RealTimeLocationAndMediaHelper

@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    fallbackEmail: String,
    activeTheme: String,
    activeThemeMode: String,
    activeUiScaleMode: String,
    activePhotoUri: String?,
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
        uiScaleMode: String,
        photoUrl: String?,
        notificationsEnabled: Boolean,
        locationSharingEnabled: Boolean
    ) -> Unit,
    onNotifyStatus: (String) -> Unit = {},
    onSignOut: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalActivePalette.current
    val dimens = LocalAppDimens.current

    val currentName = (userProfile?.displayName?.ifBlank { fallbackName } ?: fallbackName).ifBlank { "SplitBlee User" }
    val currentEmail = (userProfile?.email?.ifBlank { fallbackEmail } ?: fallbackEmail).ifBlank { "user@splitblee.app" }
    val currentTheme = activeTheme
    val currentThemeMode = activeThemeMode
    val currentScaleMode = activeUiScaleMode
    val currentPhotoUri = activePhotoUri
    val currentLang = userProfile?.preferredLanguage ?: "en"
    val currentCurrency = userProfile?.defaultCurrency ?: "USD"
    val notificationsOn = userProfile?.notificationsEnabled ?: true
    val locationOn = userProfile?.locationSharingEnabled ?: false

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showScaleDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var hasLocPerm by remember { mutableStateOf(RealTimeLocationAndMediaHelper.hasLocationPermission(context)) }
    var hasCamPerm by remember { mutableStateOf(RealTimeLocationAndMediaHelper.hasCameraPermission(context)) }
    var hasNotifPerm by remember { mutableStateOf(RealTimeLocationAndMediaHelper.hasNotificationPermission(context)) }

    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingExportLabel by remember { mutableStateOf("4K UHD Report") }

    // 1. All-Permissions Runtime Request Launcher (Location + Camera + Notifications)
    val allPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocPerm = RealTimeLocationAndMediaHelper.hasLocationPermission(context)
        hasCamPerm = RealTimeLocationAndMediaHelper.hasCameraPermission(context)
        hasNotifPerm = RealTimeLocationAndMediaHelper.hasNotificationPermission(context)
        val grantedCount = results.values.count { it }
        onNotifyStatus("Permissions updated ($grantedCount granted). Gallery & SAF file access ready.")
    }

    // 2. Android System Gallery Photo Picker (Zero-permission compliant full gallery access)
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            onUpdatePreferences(
                currentName,
                userProfile?.age?.toInt(),
                userProfile?.gender,
                userProfile?.address,
                currentLang,
                currentCurrency,
                currentTheme,
                currentThemeMode,
                currentScaleMode,
                uri.toString(),
                notificationsOn,
                locationOn
            )
            onNotifyStatus("Profile photo updated from gallery in real time.")
        }
    }

    // 3. System File Open Picker (Full File / Document Access)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onNotifyStatus("Opened file: ${uri.lastPathSegment ?: uri.toString()}")
        }
    }

    // 4. System File Save Picker for 4K / 8K PNG, PDF, CSV, TXT Exports
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val bytes = pendingExportBytes
        if (uri != null && bytes != null) {
            ExportHelper.writeBytesToUri(context, uri, bytes)
                .onSuccess {
                    ExportHelper.sendRealTimeNotification(
                        context,
                        "SplitBlee $pendingExportLabel Saved",
                        "Saved ${bytes.size / 1024} KB directly to device files."
                    )
                    onNotifyStatus("Saved $pendingExportLabel (${bytes.size / 1024} KB) to device storage.")
                }
                .onFailure { err ->
                    onNotifyStatus(err.localizedMessage ?: "Failed to save file")
                }
        }
    }

    val liveGps by RealTimeLocationAndMediaHelper.rememberLiveGpsLocation(context, hasLocPerm && locationOn)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.horizontalPadding),
        contentPadding = PaddingValues(top = 8.dp, bottom = dimens.bottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
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
                    modifier = Modifier.size(dimens.headerIconSize)
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
                    fontSize = if (dimens.isCompact) 16.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(dimens.headerIconSize)
                ) {
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("settings_gear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = strings.settings,
                            tint = palette.primary
                        )
                    }
                }
            }
        }

        // Profile Summary Header matching Screen 5 mockup with Gallery Photo Picker
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.clickable {
                        profilePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    UserAvatarCircle(
                        name = currentName,
                        size = dimens.avatarSizeLarge,
                        wearSunglasses = true,
                        photoUri = currentPhotoUri
                    )
                    Surface(
                        shape = CircleShape,
                        color = palette.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = "Pick Gallery Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentName,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = if (dimens.isCompact) 18.sp else 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = currentEmail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Text(
                                text = strings.editProfile,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            onClick = {
                                profilePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.testTag("pick_gallery_avatar_button")
                        ) {
                            Text(
                                text = "Gallery Photo",
                                color = palette.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Instant Pastel Theme Swatch Strip (1-tap real-time switching!)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pastel & Signature Themes",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Active: $currentTheme ($currentThemeMode)",
                        color = palette.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SplitBleePalettes.all, key = { it.name }) { itemPalette ->
                        val isSelected = itemPalette.name.equals(currentTheme, ignoreCase = true)
                        Surface(
                            onClick = {
                                onUpdatePreferences(
                                    currentName,
                                    userProfile?.age?.toInt(),
                                    userProfile?.gender,
                                    userProfile?.address,
                                    currentLang,
                                    currentCurrency,
                                    itemPalette.name,
                                    currentThemeMode,
                                    currentScaleMode,
                                    currentPhotoUri,
                                    notificationsOn,
                                    locationOn
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) itemPalette.lightContainer else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 4.dp else 1.dp,
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) itemPalette.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("theme_swatch_${itemPalette.name.replace(" ", "_").lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    itemPalette.previewSwatches.forEach { swatchColor ->
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(1.dp, Color.White, CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = itemPalette.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) itemPalette.deep else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // App Preferences Section matching Screen 5 mockup + Compact Display Mode
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.appPreferences,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = RoundedCornerShape(dimens.cardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsPreferenceRow(
                            icon = Icons.Filled.Palette,
                            title = strings.theme,
                            value = "$currentTheme • $currentThemeMode",
                            accentColor = palette.primary,
                            testTag = "settings_theme_row",
                            onClick = { showThemeDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        SettingsPreferenceRow(
                            icon = Icons.Filled.AspectRatio,
                            title = "Display Scaling & Compact Mode",
                            value = if (dimens.isCompact && currentScaleMode == "Auto") "Auto (Compact)" else currentScaleMode,
                            accentColor = palette.primary,
                            testTag = "settings_compact_scale_row",
                            onClick = { showScaleDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        SettingsPreferenceRow(
                            icon = Icons.Filled.Language,
                            title = strings.language,
                            value = AppLocalization.languageDisplayName(currentLang),
                            accentColor = palette.primary,
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
                            accentColor = palette.primary,
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
                                tint = palette.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = strings.notifications,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = notificationsOn,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                        allPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                    }
                                    onUpdatePreferences(
                                        currentName,
                                        userProfile?.age?.toInt(),
                                        userProfile?.gender,
                                        userProfile?.address,
                                        currentLang,
                                        currentCurrency,
                                        currentTheme,
                                        currentThemeMode,
                                        currentScaleMode,
                                        currentPhotoUri,
                                        enabled,
                                        locationOn
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.primary
                                ),
                                modifier = Modifier.testTag("settings_notifications_switch")
                            )
                        }
                    }
                }
            }
        }

        // Permissions, Gallery & File Access Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Permissions, Gallery & File Access",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = RoundedCornerShape(dimens.cardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsPreferenceRow(
                            icon = Icons.Filled.Security,
                            title = "Request All Device Permissions",
                            value = if (hasLocPerm && hasCamPerm && hasNotifPerm) "All Granted" else "Grant Now",
                            accentColor = palette.primary,
                            testTag = "settings_permissions_row",
                            onClick = { showPermissionsDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        SettingsPreferenceRow(
                            icon = Icons.Filled.AddPhotoAlternate,
                            title = "Gallery Media Picker",
                            value = "Select Photo",
                            accentColor = palette.primary,
                            testTag = "settings_gallery_picker_row",
                            onClick = {
                                profilePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        SettingsPreferenceRow(
                            icon = Icons.Filled.FolderOpen,
                            title = "Device Files & Document Browser",
                            value = "Browse Files",
                            accentColor = palette.primary,
                            testTag = "settings_browse_files_row",
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }
                }
            }
        }

        // Data & Export Section matching Screen 5 mockup + 4K / 8K UHD Resolution Export
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.dataAndExport,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(dimens.cardRadius),
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HighQuality,
                                contentDescription = strings.exportDetails,
                                tint = palette.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.exportDetails} (4K & 8K UHD)",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "4K UHD • 8K UHD • PDF • PNG • TXT • CSV",
                                color = palette.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.about,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = RoundedCornerShape(dimens.cardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsPreferenceRow(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = strings.helpAndSupport,
                            value = "",
                            accentColor = palette.primary,
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
                            accentColor = palette.primary,
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
                            accentColor = palette.primary,
                            testTag = "settings_sign_out_row",
                            onClick = onSignOut
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Made with \u2764\uFE0F by $currentName",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(currentName) }
        var ageInput by remember { mutableStateOf((userProfile?.age ?: 25L).toString()) }
        var genderInput by remember { mutableStateOf(userProfile?.gender ?: "Male") }
        var addressInput by remember { mutableStateOf(userProfile?.address ?: liveGps?.addressLabel ?: "") }
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
                        label = { Text("Shared Place / Live Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Share my live GPS pin on Map", fontSize = 13.sp)
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
                            currentScaleMode,
                            currentPhotoUri,
                            notificationsOn,
                            locShare
                        )
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
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

    // Theme Studio Dialog (Pastel Themes + Classic Themes + Light/Dark/System)
    if (showThemeDialog) {
        val modes = listOf("Light", "Dark", "System")
        var selectedTheme by remember { mutableStateOf(currentTheme) }
        var selectedMode by remember { mutableStateOf(currentThemeMode) }

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Pastel & Signature Themes", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                    Text("Select Pastel or Classic Palette:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SplitBleePalettes.all, key = { it.name }) { p ->
                            val isChosen = selectedTheme == p.name
                            Surface(
                                onClick = { selectedTheme = p.name },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) p.lightContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        p.previewSwatches.forEach { sw ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 4.dp)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(sw)
                                                    .border(1.dp, Color.White, CircleShape)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (p.isPastel) "${p.name} (Pastel)" else p.name,
                                            fontWeight = if (isChosen) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isChosen) p.deep else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = p.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
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
                            currentScaleMode,
                            currentPhotoUri,
                            notificationsOn,
                            locationOn
                        )
                        showThemeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text("Apply Live")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Compact Mode & Phone Display Scaling Dialog
    if (showScaleDialog) {
        val scaleOptions = listOf(
            "Auto" to "Auto-Fit Phone Display (Automatically scales to screen width & height)",
            "Compact" to "Compact Mode (Optimized for small phones — resizes entire UI, cards & text)",
            "Standard" to "Standard Mode (Default 100% Material 3 density)",
            "Expanded" to "Expanded Mode (Larger typography & touch targets)"
        )
        AlertDialog(
            onDismissRequest = { showScaleDialog = false },
            title = { Text("Display Size & Compact Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scaleOptions.forEach { (modeKey, desc) ->
                        val active = currentScaleMode == modeKey
                        Surface(
                            onClick = {
                                onUpdatePreferences(
                                    currentName,
                                    userProfile?.age?.toInt(),
                                    userProfile?.gender,
                                    userProfile?.address,
                                    currentLang,
                                    currentCurrency,
                                    currentTheme,
                                    currentThemeMode,
                                    modeKey,
                                    currentPhotoUri,
                                    notificationsOn,
                                    locationOn
                                )
                                showScaleDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = modeKey,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (active) palette.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScaleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Permissions & System Access Dialog
    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = { Text("Permissions & System Access", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Grant hardware & system permissions for real-time GPS pins, camera receipt capture, notifications, gallery photos, and file exports:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PermissionStatusLine("Fine & Coarse GPS Location", hasLocPerm)
                    PermissionStatusLine("Camera Receipt Capture", hasCamPerm)
                    PermissionStatusLine("Real-Time Notifications", hasNotifPerm)
                    PermissionStatusLine("Android Gallery Photo Picker", true)
                    PermissionStatusLine("Storage Access Framework (4K/8K Files)", true)

                    Button(
                        onClick = {
                            val perms = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            allPermissionsLauncher.launch(perms.toTypedArray())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request All Runtime Permissions")
                    }

                    OutlinedButton(
                        onClick = {
                            showPermissionsDialog = false
                            profilePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Gallery Photo Picker")
                    }

                    OutlinedButton(
                        onClick = {
                            showPermissionsDialog = false
                            openDocumentLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Full File & Document Picker")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Language Selector Dialog
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
                                    currentScaleMode,
                                    currentPhotoUri,
                                    notificationsOn,
                                    locationOn
                                )
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentLang == code) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
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
                                        currentScaleMode,
                                        currentPhotoUri,
                                        notificationsOn,
                                        locationOn
                                    )
                                    showCurrencyDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (currentCurrency == spec.code) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${spec.code} — ${spec.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(spec.symbol.trim(), fontWeight = FontWeight.Bold, color = palette.primary)
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

    // 4K & 8K UHD Data & Export Dialog
    if (showExportDialog) {
        var scopeSelection by remember { mutableStateOf("All Records") }
        var resolutionSelection by remember { mutableStateOf("4K UHD") }
        val scopedExpenses = when (scopeSelection) {
            "Paid Only" -> expenses.filter { it.status == "PAID" }
            "Unpaid Only" -> expenses.filter { it.status != "PAID" }
            else -> expenses
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("4K & 8K UHD Data Export", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Theme: $currentTheme • Scope: ${scopedExpenses.size} splits & ${payments.size} payments",
                        fontSize = 12.sp,
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
                    Text("Export Resolution:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("4K UHD", "8K UHD", "Standard").forEach { resOpt ->
                            FilterChip(
                                selected = resolutionSelection == resOpt,
                                onClick = { resolutionSelection = resOpt },
                                label = { Text(resOpt, fontSize = 11.sp) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            pendingExportLabel = "$resolutionSelection Poster PNG"
                            pendingExportBytes = ExportHelper.buildHighResPosterPngBytes(
                                scopeLabel = scopeSelection,
                                expenses = scopedExpenses,
                                payments = payments,
                                themeName = currentTheme,
                                resolution = resolutionSelection
                            )
                            val tag = resolutionSelection.substringBefore(" ")
                            showExportDialog = false
                            saveDocumentLauncher.launch("SplitBlee_${currentTheme.replace(" ", "_")}_${tag}_Report.png")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        modifier = Modifier.fillMaxWidth().testTag("export_high_res_png_button")
                    ) {
                        Text("Save $resolutionSelection Visual Poster (PNG)")
                    }

                    Button(
                        onClick = {
                            pendingExportLabel = "$resolutionSelection Vector PDF"
                            pendingExportBytes = ExportHelper.buildPdfSummaryBytes(
                                scopeLabel = scopeSelection,
                                expenses = scopedExpenses,
                                payments = payments,
                                themeName = currentTheme,
                                resolution = resolutionSelection
                            )
                            val tag = resolutionSelection.substringBefore(" ")
                            showExportDialog = false
                            saveDocumentLauncher.launch("SplitBlee_${currentTheme.replace(" ", "_")}_${tag}_Report.pdf")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.deep),
                        modifier = Modifier.fillMaxWidth().testTag("export_high_res_pdf_button")
                    ) {
                        Text("Save $resolutionSelection Vector Report (PDF)")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val txt = ExportHelper.buildTxtReport(
                                    scopeSelection,
                                    scopedExpenses,
                                    payments,
                                    currentTheme,
                                    resolutionSelection
                                )
                                ExportHelper.shareTextContent(context, "SplitBlee TXT Export ($resolutionSelection)", txt)
                                showExportDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share TXT", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val csv = ExportHelper.buildCsvReport(scopedExpenses)
                                ExportHelper.shareTextContent(context, "SplitBlee CSV Export", csv)
                                showExportDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share CSV", fontSize = 12.sp)
                        }
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
                    "• Pastel & Classic Themes: Switch instantaneously from the Settings swatch bar or Dashboard header.\n" +
                        "• Compact Mode: Automatically scales typography, cards, and spacing for small phone screens.\n" +
                        "• 4K & 8K UHD Exports: Save high-resolution themed PNG posters and vector PDFs directly to device files."
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
            title = { Text("About SplitBlee v2.0", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "SplitBlee — Split. Share. Enjoy.\n\n" +
                        "Real-time Cloud Firestore financial ledger with Pastel & Ocean themes, responsive Compact Mode for small displays, live GPS telemetry, gallery receipt picker, and 4K/8K UHD export."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PermissionStatusLine(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            text = if (granted) "● Active" else "○ Request",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (granted) Color(0xFF15803D) else Color(0xFFB45309)
        )
    }
}

@Composable
private fun SettingsPreferenceRow(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (dimens.isCompact) 11.dp else 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (dimens.isCompact) 13.sp else 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
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
