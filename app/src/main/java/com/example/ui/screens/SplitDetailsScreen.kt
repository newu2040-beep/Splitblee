package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.ui.theme.OceanBlueDarkText
import com.example.ui.theme.OceanBlueDeepNavy
import com.example.ui.theme.OceanBluePrimary
import com.example.util.ExportHelper

@Composable
fun SplitDetailsScreen(
    expense: ExpenseItem,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onUpdateExpenseInfo: (newTitle: String, newPlace: String, newNotes: String?) -> Unit,
    onRecordPayment: (payerName: String, payeeName: String, amountMinor: Long, method: String, reference: String?, entryType: String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }

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
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    modifier = Modifier.size(42.dp)
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
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Artisan Pizza / Category Banner matching Screen 4 mockup
                    ExpenseHeaderIllustration(
                        title = expense.title,
                        category = expense.category
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        // Title + Edit Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = expense.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEEF4FC),
                                modifier = Modifier.size(34.dp)
                            ) {
                                IconButton(
                                    onClick = { showEditDialog = true },
                                    modifier = Modifier.testTag("edit_expense_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Split",
                                        tint = OceanBlueDarkText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date & Time Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Date",
                                tint = OceanBluePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = expense.expenseDate,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // Location / Place Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Place",
                                tint = OceanBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = expense.place,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!expense.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Note: ${expense.notes}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Participant Avatars Row with +N Badge matching Screen 4 mockup
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val names = expense.participantNames.ifEmpty { listOf("Rahul Shah", "Aarav", "Priya", "Sita") }
                            names.take(4).forEachIndexed { idx, name ->
                                UserAvatarCircle(
                                    name = name,
                                    size = 42.dp,
                                    avatarIndex = idx,
                                    wearSunglasses = idx == 1 || idx == 2,
                                    modifier = Modifier.offset(x = ((-4) * idx).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEF4FC))
                                    .border(1.dp, Color(0xFFD5E3F7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (names.size > 4) "+${names.size - 4}" else "+1",
                                    color = OceanBlueDarkText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Bill Summary Header
                        Text(
                            text = strings.billSummary,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bill Summary Card matching Screen 4 mockup
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Split Among Header matching Screen 4 mockup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.splitAmong,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${expense.participantCount} People • ${expense.splitMethod}",
                                color = OceanBluePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Avatars row under Split Among
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            participantShares.take(4).forEachIndexed { idx, share ->
                                UserAvatarCircle(
                                    name = share.name,
                                    size = 42.dp,
                                    avatarIndex = idx,
                                    wearSunglasses = idx == 1 || idx == 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Per-Participant Share Breakdown (Assigned, Paid, Remaining, Status)
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

        // Action Buttons matching Screen 4 mockup:
        // 1. Soft Red/Pink Button: "Mark as Unpaid" (or if Unpaid, "Mark as Paid" + "Record Payment")
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
                    .height(50.dp)
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Deep Navy Button: "Share Details" matching Screen 4 mockup
        item {
            Button(
                onClick = {
                    val summaryText = buildString {
                        appendLine("SplitBlee — ${expense.title}")
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
                    containerColor = OceanBlueDeepNavy,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Edit Expense Dialog
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
                        onUpdateExpenseInfo(titleInput, placeInput, notesInput.ifBlank { null })
                        showEditDialog = false
                    }
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

    // Record Confirmed Payment Ledger Entry Dialog
    if (showRecordPaymentDialog) {
        val unpaidParticipants = participantShares.filter { it.status != "PAID" }.ifEmpty { participantShares }
        var selectedPayer by remember { mutableStateOf(unpaidParticipants.firstOrNull()?.name ?: "Participant") }
        val defaultAmtMinor = participantShares.firstOrNull { it.name == selectedPayer }?.remainingMinor?.takeIf { it > 0 }
            ?: (expense.totalMinor / expense.participantCount.coerceAtLeast(1L))
        var amountText by remember(selectedPayer) {
            mutableStateOf(String.format( java.util.Locale.US, "%.2f", defaultAmtMinor / 100.0 ))
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
                        showRecordPaymentDialog = false
                    }
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
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
