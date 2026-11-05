package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurrencyCatalog
import com.example.model.ExpenseItem
import com.example.model.LocalizedStrings
import com.example.model.ParticipantShareCodec
import com.example.ui.components.ExpenseHeaderIllustration
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.LocalActivePalette
import com.example.ui.theme.LocalAppDimens
import com.example.util.ExportHelper
import java.util.Locale

@Composable
fun SplitDetailsScreen(
    expense: ExpenseItem,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onUpdateExpenseInfo: (newTitle: String, newPlace: String, newNotes: String?, newReceiptUri: String?) -> Unit,
    onRecordPayment: (payerName: String, payeeName: String, amountMinor: Long, method: String, reference: String?, entryType: String) -> Unit,
    onNotifyStatus: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalActivePalette.current
    val dimens = LocalAppDimens.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }

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
            onUpdateExpenseInfo(expense.title, expense.place, expense.notes, uri.toString())
            onNotifyStatus("Receipt photo attached from gallery.")
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val bytes = pendingExportBytes
        if (uri != null && bytes != null) {
            ExportHelper.writeBytesToUri(context, uri, bytes)
                .onSuccess {
                    ExportHelper.sendRealTimeNotification(
                        context,
                        "SplitBlee High-Res Export Saved",
                        "Saved '${expense.title}' (${bytes.size / 1024} KB) to device storage."
                    )
                    onNotifyStatus("High-resolution receipt saved (${bytes.size / 1024} KB).")
                }
        }
    }

    val participantShares = remember(expense) {
        ParticipantShareCodec.decode(
            encoded = expense.participantSharesSummary,
            fallbackNames = expense.participantNames,
            totalMinor = expense.totalMinor,
            expenseStatus = expense.status
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.horizontalPadding),
        contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
    ) {
        // Top Navigation Bar matching Screen 4 mockup: `<` | `Split Details` | `...`
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
                        modifier = Modifier.testTag("split_details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = strings.splitDetails,
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
                        onClick = { showRecordPaymentDialog = true },
                        modifier = Modifier.testTag("split_details_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "Payment & Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Main Card containing Header Banner, Metadata, Avatars, Bill Summary, and Split Among
        item {
            Surface(
                shape = RoundedCornerShape(dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box {
                        ExpenseHeaderIllustration(
                            title = expense.title,
                            category = expense.category,
                            receiptUri = expense.receiptUri
                        )
                        // Gallery Photo Picker Button on Banner
                        Surface(
                            onClick = {
                                receiptPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .testTag("attach_receipt_photo_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "Attach Gallery Receipt",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (expense.receiptUri.isNullOrBlank()) "Gallery Photo" else "Change Photo",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (dimens.isCompact) 14.dp else 18.dp, vertical = if (dimens.isCompact) 12.dp else 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = expense.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = if (dimens.isCompact) 18.sp else 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                IconButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier.testTag("edit_expense_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Split",
                                        tint = palette.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Date",
                                tint = palette.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = expense.expenseDate,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Place",
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = expense.place,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!expense.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Note: ${expense.notes}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Participant Avatars Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val names = expense.participantNames.ifEmpty { listOf("You") }
                            names.take(4).forEachIndexed { idx, name ->
                                UserAvatarCircle(
                                    name = name,
                                    size = dimens.avatarSizeSmall,
                                    avatarIndex = idx,
                                    wearSunglasses = idx == 1 || idx == 2,
                                    modifier = Modifier.offset(x = ((-4) * idx).dp)
                                )
                            }
                            if (names.size > 4) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(dimens.avatarSizeSmall)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(1.dp, palette.primary.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${names.size - 4}",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = strings.billSummary,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = if (dimens.isCompact) 15.sp else 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                BillSummaryRow(
                                    label = strings.totalBill,
                                    value = CurrencyCatalog.formatMinor(expense.subtotalMinor, expense.currency)
                                )
                                BillSummaryRow(
                                    label = "Tax (${expense.taxPercent}%)",
                                    value = CurrencyCatalog.formatMinor(expense.taxMinor, expense.currency)
                                )
                                if (expense.serviceChargeMinor > 0L) {
                                    BillSummaryRow(
                                        label = "Service Charge",
                                        value = CurrencyCatalog.formatMinor(expense.serviceChargeMinor, expense.currency)
                                    )
                                }
                                if (expense.tipMinor > 0L) {
                                    BillSummaryRow(
                                        label = "Tip",
                                        value = CurrencyCatalog.formatMinor(expense.tipMinor, expense.currency)
                                    )
                                }
                                if (expense.extraAmountMinor > 0L) {
                                    BillSummaryRow(
                                        label = "Extra Amount",
                                        value = CurrencyCatalog.formatMinor(expense.extraAmountMinor, expense.currency)
                                    )
                                }
                                BillSummaryRow(
                                    label = strings.discount,
                                    value = "-${CurrencyCatalog.formatMinor(expense.discountMinor, expense.currency)}"
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.finalAmount,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = CurrencyCatalog.formatMinor(expense.totalMinor, expense.currency),
                                        color = palette.primary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.splitAmong,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${expense.participantCount} People • ${expense.splitMethod}",
                                color = palette.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            participantShares.forEach { share ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { showRecordPaymentDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = share.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Share: ${CurrencyCatalog.formatMinor(share.assignedMinor, expense.currency)} • Remaining: ${CurrencyCatalog.formatMinor(share.remainingMinor, expense.currency)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPillBadge(status = share.status)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons matching Screen 4 mockup + 4K / 8K Direct Save
        item {
            val isCurrentlyPaid = expense.status == "PAID"
            Button(
                onClick = {
                    if (isCurrentlyPaid) {
                        onRecordPayment(
                            "ALL_PARTICIPANTS",
                            "You",
                            expense.totalMinor,
                            "Ledger Reversal",
                            "Marked as unpaid",
                            "REVERSAL"
                        )
                        ExportHelper.sendRealTimeNotification(
                            context,
                            "Split Reversal Recorded",
                            "'${expense.title}' was marked as Unpaid in the ledger."
                        )
                    } else {
                        showRecordPaymentDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFDE8EC),
                    contentColor = Color(0xFFC81E1E)
                ),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.buttonHeight)
                    .testTag("mark_unpaid_or_record_button")
            ) {
                Icon(
                    imageVector = if (isCurrentlyPaid) Icons.Filled.Error else Icons.Filled.Payments,
                    contentDescription = null,
                    tint = Color(0xFFC81E1E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCurrentlyPaid) strings.markAsUnpaid else "${strings.recordPayment} / ${strings.markAsPaid}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4K & 8K High-Resolution Receipt Export Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        pendingExportBytes = ExportHelper.buildHighResPosterPngBytes(
                            scopeLabel = expense.title,
                            expenses = listOf(expense),
                            payments = emptyList(),
                            themeName = palette.name,
                            resolution = "4K UHD"
                        )
                        createDocumentLauncher.launch("SplitBlee_${expense.title.replace(" ", "_")}_4K.png")
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).testTag("export_4k_receipt_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save 4K PNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.primary)
                }

                OutlinedButton(
                    onClick = {
                        pendingExportBytes = ExportHelper.buildHighResPosterPngBytes(
                            scopeLabel = expense.title,
                            expenses = listOf(expense),
                            payments = emptyList(),
                            themeName = palette.name,
                            resolution = "8K UHD"
                        )
                        createDocumentLauncher.launch("SplitBlee_${expense.title.replace(" ", "_")}_8K.png")
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).testTag("export_8k_receipt_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save 8K PNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.primary)
                }
            }
        }

        // Deep Theme Button: "Share Details" matching Screen 4 mockup
        item {
            Button(
                onClick = {
                    val summaryText = buildString {
                        appendLine("SplitBlee — ${expense.title} (${palette.name} Theme)")
                        appendLine("Date: ${expense.expenseDate}")
                        appendLine("Place: ${expense.place}")
                        appendLine("Total Bill: ${CurrencyCatalog.formatMinor(expense.subtotalMinor, expense.currency)}")
                        appendLine("Tax (${expense.taxPercent}%): ${CurrencyCatalog.formatMinor(expense.taxMinor, expense.currency)}")
                        appendLine("Discount: -${CurrencyCatalog.formatMinor(expense.discountMinor, expense.currency)}")
                        appendLine("Final Amount: ${CurrencyCatalog.formatMinor(expense.totalMinor, expense.currency)}")
                        appendLine("Status: ${expense.status}")
                        appendLine("Participants (${expense.participantCount}):")
                        participantShares.forEach { s ->
                            appendLine("- ${s.name}: ${CurrencyCatalog.formatMinor(s.assignedMinor, expense.currency)} [${s.status}]")
                        }
                    }
                    ExportHelper.shareTextContent(context, "Split Details: ${expense.title}", summaryText)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.deep,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.buttonHeight)
                    .testTag("share_details_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.IosShare,
                    contentDescription = strings.shareDetails,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.shareDetails,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showEditDialog) {
        var titleInput by remember { mutableStateOf(expense.title) }
        var placeInput by remember { mutableStateOf(expense.place) }
        var notesInput by remember { mutableStateOf(expense.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Split Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Expense Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = placeInput,
                        onValueChange = { placeInput = it },
                        label = { Text("Place / Venue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateExpenseInfo(titleInput, placeInput, notesInput.ifBlank { null }, expense.receiptUri)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRecordPaymentDialog) {
        val unpaidParticipants = participantShares.filter { it.status != "PAID" }.ifEmpty { participantShares }
        var selectedPayer by remember { mutableStateOf(unpaidParticipants.firstOrNull()?.name ?: "Participant") }
        val defaultAmtMinor = participantShares.firstOrNull { it.name == selectedPayer }?.remainingMinor?.takeIf { it > 0 }
            ?: (expense.totalMinor / expense.participantCount.coerceAtLeast(1L))
        var amountText by remember(selectedPayer) {
            mutableStateOf(String.format(Locale.US, "%.2f", defaultAmtMinor / 100.0))
        }
        var method by remember { mutableStateOf("UPI") }
        var reference by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRecordPaymentDialog = false },
            title = { Text("Record Ledger Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Only a confirmed ledger entry updates status. Choose participant or settle all:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        participantShares.take(3).forEach { p ->
                            FilterChip(
                                selected = selectedPayer == p.name,
                                onClick = { selectedPayer = p.name },
                                label = { Text(p.name.substringBefore(" "), fontSize = 11.sp) }
                            )
                        }
                        FilterChip(
                            selected = selectedPayer == "ALL_PARTICIPANTS",
                            onClick = { selectedPayer = "ALL_PARTICIPANTS" },
                            label = { Text("All", fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (${expense.currency})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("UPI", "eSewa", "Cash", "Bank").forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("Reference / Note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minor = CurrencyCatalog.parseMajorToMinor(amountText, expense.currency)
                            .coerceAtLeast(100L)
                        onRecordPayment(
                            selectedPayer,
                            "You",
                            minor,
                            method,
                            reference.ifBlank { null },
                            "PAYMENT"
                        )
                        ExportHelper.sendRealTimeNotification(
                            context,
                            "Ledger Payment Confirmed",
                            "$selectedPayer paid ${CurrencyCatalog.formatMinor(minor, expense.currency)} via $method"
                        )
                        showRecordPaymentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm Ledger Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BillSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
