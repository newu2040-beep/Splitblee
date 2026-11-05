package com.example.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurrencyCatalog
import com.example.model.ExpenseGroup
import com.example.model.ExpenseItem
import com.example.model.FriendProfile
import com.example.model.PaymentEntry
import com.example.model.SplitCalculator
import com.example.ui.components.CategoryIconTile
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.LocalActivePalette
import com.example.ui.theme.LocalAppDimens
import com.example.util.RealTimeLocationAndMediaHelper

@Composable
fun NewSplitScreen(
    defaultCurrency: String,
    groups: List<ExpenseGroup>,
    friends: List<FriendProfile>,
    userName: String,
    onBack: () -> Unit,
    onSaveSplit: (
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
        customSharesMinor: List<Long>?,
        receiptUri: String?
    ) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalActivePalette.current
    val dimens = LocalAppDimens.current

    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food & Drink") }
    var place by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var selectedGroup by remember { mutableStateOf<ExpenseGroup?>(null) }
    var receiptUri by remember { mutableStateOf<String?>(null) }

    var subtotalInput by remember { mutableStateOf("24.00") }
    var taxPercentInput by remember { mutableStateOf("13") }
    var serviceChargeInput by remember { mutableStateOf("0.00") }
    var tipInput by remember { mutableStateOf("0.00") }
    var extraInput by remember { mutableStateOf("0.00") }
    var discountInput by remember { mutableStateOf("0.00") }

    var locationPermissionGranted by remember {
        mutableStateOf(RealTimeLocationAndMediaHelper.hasLocationPermission(context))
    }
    val liveLocation by RealTimeLocationAndMediaHelper.rememberLiveGpsLocation(context, locationPermissionGranted)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = granted
    }

    val receiptPhotoPicker = rememberLauncherForActivityResult(
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
            receiptUri = uri.toString()
        }
    }

    val receiptDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            receiptUri = uri.toString()
        }
    }

    val receiptBitmap = remember(receiptUri) {
        RealTimeLocationAndMediaHelper.loadImageBitmapFromUri(context, receiptUri)
    }

    var splitMethod by remember { mutableStateOf("EQUAL") } // EQUAL, PERCENTAGE, EXACT, ITEMIZED
    val defaultParticipants = remember(userName, friends) {
        val base = mutableListOf(userName.ifBlank { "Me" })
        friends.map { it.name }.forEach { if (it !in base && base.size < 6) base.add(it) }
        base.joinToString(", ")
    }
    var participantsInput by remember(selectedGroup, defaultParticipants) {
        mutableStateOf(
            selectedGroup?.memberNames?.joinToString(", ")?.ifBlank { defaultParticipants }
                ?: defaultParticipants
        )
    }

    val participantNames = remember(participantsInput, userName) {
        participantsInput.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(userName.ifBlank { "Me" }) }
            .take(20)
    }

    val subtotalMinor = CurrencyCatalog.parseMajorToMinor(subtotalInput, currency).coerceAtLeast(100L)
    val taxPct = taxPercentInput.toIntOrNull()?.coerceIn(0, 100) ?: 0
    val serviceMinor = CurrencyCatalog.parseMajorToMinor(serviceChargeInput, currency)
    val tipMinor = CurrencyCatalog.parseMajorToMinor(tipInput, currency)
    val extraMinor = CurrencyCatalog.parseMajorToMinor(extraInput, currency)
    val discountMinor = CurrencyCatalog.parseMajorToMinor(discountInput, currency)

    val (taxMinor, totalMinor) = SplitCalculator.computeTotalMinor(
        subtotalMinor = subtotalMinor,
        taxPercent = taxPct,
        serviceChargeMinor = serviceMinor,
        tipMinor = tipMinor,
        extraAmountMinor = extraMinor,
        discountMinor = discountMinor
    )

    val previewSharesMinor = remember(totalMinor, participantNames, splitMethod) {
        when (splitMethod) {
            "PERCENTAGE" -> {
                val basePct = 100 / participantNames.size
                val remPct = 100 % participantNames.size
                val pcts = List(participantNames.size) { idx -> if (idx < remPct) basePct + 1 else basePct }
                SplitCalculator.calculatePercentageShares(totalMinor, pcts)
            }
            else -> SplitCalculator.calculateEqualShares(totalMinor, participantNames.size)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.horizontalPadding),
        contentPadding = PaddingValues(top = dimens.verticalSpacing, bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.verticalSpacing)
    ) {
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
                    modifier = Modifier.size(dimens.topBarIconSize)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("new_split_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    text = "Create Real-Time Split",
                    fontSize = (18f * dimens.fontScale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(dimens.topBarIconSize))
            }
        }

        // Step 1: Title, Category, Live GPS Venue, Receipt Photo, Currency
        item {
            Surface(
                shape = RoundedCornerShape(dimens.cardCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Expense Details & Receipt",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (15f * dimens.fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title (e.g., Dinner, Groceries)") },
                        placeholder = { Text("Enter split title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_split_title_input")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Food & Drink", "Shopping", "Coffee", "Travel", "Household", "Events")) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = (12f * dimens.fontScale).sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.accentChipBg,
                                    selectedLabelColor = palette.deepNavy
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = place,
                            onValueChange = { place = it },
                            label = { Text("Place / Venue") },
                            placeholder = {
                                Text(
                                    liveLocation?.addressLabel ?: "Restaurant, store, or city"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                if (!locationPermissionGranted) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    liveLocation?.let { place = it.addressLabel }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = palette.accentChipBg,
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("use_live_gps_place_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = "Use Live GPS",
                                    tint = palette.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GPS",
                                    fontSize = (11f * dimens.fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.deepNavy
                                )
                            }
                        }
                    }

                    // Gallery Receipt Photo & File Attachment Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                receiptPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_split_gallery_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (receiptBitmap != null) "Change Photo" else "Gallery Receipt",
                                fontSize = (11.5f * dimens.fontScale).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                receiptDocPicker.launch(arrayOf("image/*", "application/pdf"))
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_split_file_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Attach File",
                                fontSize = (11.5f * dimens.fontScale).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (receiptBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        ) {
                            Image(
                                bitmap = receiptBitmap,
                                contentDescription = "Attached Receipt Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(bottomStart = 10.dp),
                                color = palette.deepNavy.copy(alpha = 0.8f),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = "Receipt Attached ✓",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("USD", "NPR", "INR", "PKR", "EUR", "GBP", "AED", "AUD", "JPY", "KWD")) { code ->
                            FilterChip(
                                selected = currency == code,
                                onClick = { currency = code },
                                label = { Text(code, fontSize = (12f * dimens.fontScale).sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.accentChipBg,
                                    selectedLabelColor = palette.deepNavy
                                )
                            )
                        }
                    }
                }
            }
        }

        // Step 2: Group or Participants
        item {
            Surface(
                shape = RoundedCornerShape(dimens.cardCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. Group & Participants (${participantNames.size} people)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (15f * dimens.fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (groups.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedGroup == null,
                                    onClick = { selectedGroup = null },
                                    label = { Text("One-off Split", fontSize = (12f * dimens.fontScale).sp) }
                                )
                            }
                            items(groups, key = { it.id }) { grp ->
                                FilterChip(
                                    selected = selectedGroup?.id == grp.id,
                                    onClick = {
                                        selectedGroup = grp
                                        currency = grp.settlementCurrency
                                    },
                                    label = { Text(grp.name, fontSize = (12f * dimens.fontScale).sp) }
                                )
                            }
                        }
                    }
                    if (friends.isNotEmpty()) {
                        Text(
                            text = "Tap a friend to toggle in this split:",
                            fontSize = (11.5f * dimens.fontScale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(friends, key = { it.id }) { friend ->
                                val isIncluded = participantNames.any { it.equals(friend.name, ignoreCase = true) }
                                FilterChip(
                                    selected = isIncluded,
                                    onClick = {
                                        val currentList = participantNames.toMutableList()
                                        if (isIncluded && currentList.size > 1) {
                                            currentList.removeAll { it.equals(friend.name, ignoreCase = true) }
                                        } else if (!isIncluded) {
                                            currentList.add(friend.name)
                                        }
                                        participantsInput = currentList.joinToString(", ")
                                    },
                                    label = { Text(friend.name, fontSize = (11.5f * dimens.fontScale).sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.accentChipBg,
                                        selectedLabelColor = palette.deepNavy
                                    )
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = participantsInput,
                        onValueChange = { participantsInput = it },
                        label = { Text("Participants (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Step 3: Bill Breakdown (Subtotal, Tax %, Service Charge, Tip, Extra, Discount)
        item {
            Surface(
                shape = RoundedCornerShape(dimens.cardCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. Bill Breakdown ($currency)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (15f * dimens.fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = subtotalInput,
                            onValueChange = { subtotalInput = it },
                            label = { Text("Subtotal") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = taxPercentInput,
                            onValueChange = { taxPercentInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tax %") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = it },
                            label = { Text("Discount") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tipInput,
                            onValueChange = { tipInput = it },
                            label = { Text("Tip") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = serviceChargeInput,
                            onValueChange = { serviceChargeInput = it },
                            label = { Text("Service Fee") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = extraInput,
                            onValueChange = { extraInput = it },
                            label = { Text("Extra Amount") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Step 4: Split Method & Reconciled Share Preview
        item {
            Surface(
                shape = RoundedCornerShape(dimens.cardCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "4. Split Method & Exact Share Preview",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (15f * dimens.fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("EQUAL", "PERCENTAGE", "EXACT", "ITEMIZED")) { method ->
                            FilterChip(
                                selected = splitMethod == method,
                                onClick = { splitMethod = method },
                                label = { Text(method, fontSize = (11f * dimens.fontScale).sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.accentChipBg,
                                    selectedLabelColor = palette.deepNavy
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tax ($taxPct%):",
                            fontSize = (12.5f * dimens.fontScale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyCatalog.formatMinor(taxMinor, currency),
                            fontSize = (12.5f * dimens.fontScale).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Final Reconciled Total:", fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = CurrencyCatalog.formatMinor(totalMinor, currency),
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.primary,
                            fontSize = (17f * dimens.fontScale).sp
                        )
                    }

                    participantNames.forEachIndexed { idx, name ->
                        val shareMinor = previewSharesMinor.getOrElse(idx) { 0L }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontSize = (13f * dimens.fontScale).sp)
                            Text(
                                text = CurrencyCatalog.formatMinor(shareMinor, currency),
                                fontSize = (13f * dimens.fontScale).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val effectiveTitle = title.trim().ifBlank { "$category Split" }
                    val effectivePlace = place.trim().ifBlank {
                        liveLocation?.addressLabel ?: "Shared Venue"
                    }
                    onSaveSplit(
                        effectiveTitle,
                        category,
                        effectivePlace,
                        notes.ifBlank { null },
                        selectedGroup?.id,
                        selectedGroup?.name,
                        currency,
                        subtotalInput,
                        taxPct,
                        serviceChargeInput,
                        tipInput,
                        extraInput,
                        discountInput,
                        splitMethod,
                        participantNames,
                        previewSharesMinor,
                        receiptUri
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                shape = RoundedCornerShape(dimens.buttonHeight / 2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.buttonHeight)
                    .testTag("save_split_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Live Split (${CurrencyCatalog.formatMinor(totalMinor, currency)})",
                    fontSize = (15.5f * dimens.fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GroupsScreen(
    groups: List<ExpenseGroup>,
    defaultCurrency: String,
    userName: String,
    onBack: () -> Unit,
    onCreateGroup: (name: String, currency: String, members: List<String>) -> Unit,
    onNewSplitClick: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalActivePalette.current
    val dimens = LocalAppDimens.current
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.horizontalPadding),
        contentPadding = PaddingValues(top = dimens.verticalSpacing, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.verticalSpacing)
    ) {
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
                    modifier = Modifier.size(dimens.topBarIconSize)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = "Groups",
                    fontSize = (18f * dimens.fontScale).sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    shape = CircleShape,
                    color = palette.primary,
                    modifier = Modifier.size(dimens.topBarIconSize)
                ) {
                    IconButton(
                        onClick = { showCreateGroupDialog = true },
                        modifier = Modifier.testTag("add_group_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Group", tint = Color.White)
                    }
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(dimens.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(dimens.cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Groups Created Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = (16f * dimens.fontScale).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a real-time group with a dedicated settlement currency for trips, roommates, or dinners.",
                            fontSize = (12.5f * dimens.fontScale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showCreateGroupDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                        ) {
                            Text("Create First Group", color = Color.White)
                        }
                    }
                }
            }
        } else {
            items(groups, key = { it.id }) { group ->
                Surface(
                    onClick = onNewSplitClick,
                    shape = RoundedCornerShape(dimens.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(dimens.cardPadding)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.name,
                                fontSize = (16.5f * dimens.fontScale).sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(palette.accentChipBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Settlement: ${group.settlementCurrency}",
                                    color = palette.deepNavy,
                                    fontSize = (11f * dimens.fontScale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${group.memberCount} members: ${group.memberNames.joinToString(", ")}",
                            fontSize = (12.5f * dimens.fontScale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.memberNames.take(5).forEachIndexed { idx, m ->
                                UserAvatarCircle(name = m, size = 32.dp, avatarIndex = idx)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupCurrency by remember { mutableStateOf(defaultCurrency) }
        var membersText by remember { mutableStateOf(userName.ifBlank { "Me" }) }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Real-Time Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("e.g., Weekend Trip, Apartment") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("USD", "NPR", "INR", "PKR", "EUR", "GBP")) { c ->
                            FilterChip(
                                selected = groupCurrency == c,
                                onClick = { groupCurrency = c },
                                label = { Text(c, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = membersText,
                        onValueChange = { membersText = it },
                        label = { Text("Members (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val members = membersText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onCreateGroup(groupName.ifBlank { "Shared Group" }, groupCurrency, members)
                        showCreateGroupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryAnalyticsScreen(
    expenses: List<ExpenseItem>,
    payments: List<PaymentEntry>,
    defaultCurrency: String,
    onBack: () -> Unit,
    onOpenExpenseDetail: (String) -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalActivePalette.current
    val dimens = LocalAppDimens.current

    var statusFilter by remember { mutableStateOf("ALL") }
    val activeExpenses = expenses.filter { it.status != "CANCELLED" }
    val filteredExpenses = remember(activeExpenses, statusFilter) {
        when (statusFilter) {
            "PAID" -> activeExpenses.filter { it.status == "PAID" }
            "UNPAID" -> activeExpenses.filter { it.status != "PAID" }
            else -> activeExpenses
        }
    }

    val totalSpentMinor = activeExpenses.sumOf {
        CurrencyCatalog.convertMinorEstimate(it.totalMinor, it.currency, defaultCurrency)
    }
    val totalPaidMinor = activeExpenses.sumOf {
        CurrencyCatalog.convertMinorEstimate(it.paidMinor, it.currency, defaultCurrency)
    }
    val settlementProgress = if (totalSpentMinor > 0L) {
        (totalPaidMinor.toFloat() / totalSpentMinor.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.horizontalPadding),
        contentPadding = PaddingValues(top = dimens.verticalSpacing, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.verticalSpacing)
    ) {
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
                    modifier = Modifier.size(dimens.topBarIconSize)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = "History & Monthly Analytics",
                    fontSize = (18f * dimens.fontScale).sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(dimens.topBarIconSize))
            }
        }

        // Monthly Overview & Settlement Progress Card
        item {
            Surface(
                shape = RoundedCornerShape(dimens.cardCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(dimens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Real-Time Settlement Progress",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (15.5f * dimens.fontScale).sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Expenses",
                                fontSize = (12f * dimens.fontScale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyCatalog.formatMinor(totalSpentMinor, defaultCurrency),
                                fontSize = (18f * dimens.fontScale).sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Settled / Paid",
                                fontSize = (12f * dimens.fontScale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyCatalog.formatMinor(totalPaidMinor, defaultCurrency),
                                fontSize = (18f * dimens.fontScale).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { settlementProgress },
                        color = palette.primary,
                        trackColor = palette.accentChipBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = "${(settlementProgress * 100).toInt()}% settled across ${activeExpenses.size} active splits (${payments.size} immutable ledger entries)",
                        fontSize = (11.5f * dimens.fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Status Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "All Splits", "PAID" to "Paid", "UNPAID" to "Unpaid / Partial").forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label, fontSize = (12f * dimens.fontScale).sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.accentChipBg,
                            selectedLabelColor = palette.deepNavy
                        )
                    )
                }
            }
        }

        items(filteredExpenses, key = { it.id }) { exp ->
            Surface(
                onClick = { onOpenExpenseDetail(exp.id) },
                shape = RoundedCornerShape(dimens.cardCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(dimens.cardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIconTile(
                        category = exp.category,
                        title = exp.title
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exp.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = (14.5f * dimens.fontScale).sp
                        )
                        Text(
                            text = "${exp.expenseDate} • ${exp.place}",
                            fontSize = (11.5f * dimens.fontScale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyCatalog.formatMinor(exp.totalMinor, exp.currency),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (14.5f * dimens.fontScale).sp
                        )
                        StatusPillBadge(status = exp.status)
                    }
                }
            }
        }

        if (payments.isNotEmpty()) {
            item {
                Text(
                    text = "Immutable Payment Ledger",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (15.5f * dimens.fontScale).sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(payments.take(15), key = { it.id }) { payment ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${payment.payerName} → ${payment.payeeName} (${payment.expenseTitle})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (12.5f * dimens.fontScale).sp
                            )
                            Text(
                                text = "[${payment.entryType}] via ${payment.method}",
                                fontSize = (11f * dimens.fontScale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyCatalog.formatMinor(payment.amountMinor, payment.currency),
                            fontWeight = FontWeight.Bold,
                            fontSize = (13.5f * dimens.fontScale).sp,
                            color = if (payment.entryType == "REVERSAL") Color(0xFFC81E1E) else Color(0xFF15803D)
                        )
                    }
                }
            }
        }
    }
}
