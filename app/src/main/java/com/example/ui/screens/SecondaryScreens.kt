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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import com.example.ui.theme.OceanBlueDarkText
import com.example.ui.theme.OceanBluePrimary

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
        customSharesMinor: List<Long>?
    ) -> Unit
) {
    BackHandler { onBack() }

    var title by remember { mutableStateOf("Pizza Hut Dinner") }
    var category by remember { mutableStateOf("Food & Drink") }
    var place by remember { mutableStateOf("Pizza Hut, Thamel") }
    var notes by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var selectedGroup by remember { mutableStateOf<ExpenseGroup?>(null) }

    var subtotalInput by remember { mutableStateOf("72.50") }
    var taxPercentInput by remember { mutableStateOf("13") }
    var serviceChargeInput by remember { mutableStateOf("0.00") }
    var tipInput by remember { mutableStateOf("0.00") }
    var extraInput by remember { mutableStateOf("0.00") }
    var discountInput by remember { mutableStateOf("5.00") }

    var splitMethod by remember { mutableStateOf("EQUAL") } // EQUAL, PERCENTAGE, EXACT, ITEMIZED
    val defaultParticipants = remember(userName, friends) {
        val base = mutableListOf(userName.ifBlank { "Rahul Shah" })
        friends.map { it.name }.forEach { if (it !in base && base.size < 4) base.add(it) }
        if (base.size == 1) {
            base.addAll(listOf("Aarav", "Priya", "Sita"))
        }
        base.joinToString(", ")
    }
    var participantsInput by remember(selectedGroup, defaultParticipants) {
        mutableStateOf(
            selectedGroup?.memberNames?.joinToString(", ")?.ifBlank { defaultParticipants }
                ?: defaultParticipants
        )
    }

    val participantNames = remember(participantsInput) {
        participantsInput.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("Rahul Shah", "Aarav") }
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
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    modifier = Modifier.size(42.dp)
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
                    text = "Create New Split",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(42.dp))
            }
        }

        // Step 1: Title, Category, Place, Currency
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("1. Expense Details", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_split_title_input")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Food & Drink", "Shopping", "Coffee", "Travel", "Household", "Events")) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = place,
                        onValueChange = { place = it },
                        label = { Text("Place / Venue") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("USD", "NPR", "INR", "PKR", "EUR", "GBP", "AED", "AUD", "JPY", "KWD")) { code ->
                            FilterChip(
                                selected = currency == code,
                                onClick = { currency = code },
                                label = { Text(code, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Step 2: Group or Participants
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("2. Group & Participants (${participantNames.size} people)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    if (groups.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedGroup == null,
                                    onClick = { selectedGroup = null },
                                    label = { Text("One-off Split", fontSize = 12.sp) }
                                )
                            }
                            items(groups, key = { it.id }) { grp ->
                                FilterChip(
                                    selected = selectedGroup?.id == grp.id,
                                    onClick = {
                                        selectedGroup = grp
                                        currency = grp.settlementCurrency
                                    },
                                    label = { Text(grp.name, fontSize = 12.sp) }
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
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("3. Bill Breakdown ($currency)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
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
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("4. Split Method & Exact Share Preview", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EQUAL", "PERCENTAGE", "EXACT", "ITEMIZED").forEach { method ->
                            FilterChip(
                                selected = splitMethod == method,
                                onClick = { splitMethod = method },
                                label = { Text(method, fontSize = 11.sp) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Final Reconciled Total:", fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = CurrencyCatalog.formatMinor(totalMinor, currency),
                            fontWeight = FontWeight.ExtraBold,
                            color = OceanBluePrimary,
                            fontSize = 17.sp
                        )
                    }

                    participantNames.forEachIndexed { idx, name ->
                        val shareMinor = previewSharesMinor.getOrElse(idx) { 0L }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontSize = 13.sp)
                            Text(
                                text = CurrencyCatalog.formatMinor(shareMinor, currency),
                                fontSize = 13.sp,
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
                    onSaveSplit(
                        title,
                        category,
                        place,
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
                        previewSharesMinor
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OceanBluePrimary),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_split_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Split (${CurrencyCatalog.formatMinor(totalMinor, currency)})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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

    var showCreateGroupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text("Groups", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Surface(
                    shape = CircleShape,
                    color = OceanBluePrimary,
                    modifier = Modifier.size(42.dp)
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
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            tint = OceanBluePrimary,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Groups Created Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Create a group with a dedicated settlement currency for trips, roommates, or dinners.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showCreateGroupDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBluePrimary)
                        ) {
                            Text("Create First Group")
                        }
                    }
                }
            }
        } else {
            items(groups, key = { it.id }) { group ->
                Surface(
                    onClick = onNewSplitClick,
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(group.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFDCE8FC))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Settlement: ${group.settlementCurrency}",
                                    color = OceanBlueDarkText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${group.memberCount} members: ${group.memberNames.joinToString(", ")}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.memberNames.take(4).forEachIndexed { idx, m ->
                                UserAvatarCircle(name = m, size = 32.dp, avatarIndex = idx)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("Thamel Weekend Crew") }
        var groupCurrency by remember { mutableStateOf(defaultCurrency) }
        var membersText by remember { mutableStateOf("${userName.ifBlank { "Rahul Shah" }}, Aarav, Priya, Sita") }

        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
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
                        onCreateGroup(groupName, groupCurrency, members)
                        showCreateGroupDialog = false
                    }
                ) {
                    Text("Create")
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
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text("History & Monthly Analytics", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.width(42.dp))
            }
        }

        // Monthly Overview & Settlement Progress Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Monthly Settlement Progress", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Expenses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyCatalog.formatMinor(totalSpentMinor, defaultCurrency),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Settled / Paid", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyCatalog.formatMinor(totalPaidMinor, defaultCurrency),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { settlementProgress },
                        color = OceanBluePrimary,
                        trackColor = Color(0xFFDCE8FC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = "${(settlementProgress * 100).toInt()}% settled across ${activeExpenses.size} active splits (${payments.size} immutable ledger entries)",
                        fontSize = 12.sp,
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
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        items(filteredExpenses, key = { it.id }) { exp ->
            Surface(
                onClick = { onOpenExpenseDetail(exp.id) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIconTile(category = exp.category, title = exp.title)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "${exp.expenseDate} • ${exp.place}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            CurrencyCatalog.formatMinor(exp.totalMinor, exp.currency),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
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
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
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
                            tint = OceanBluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${payment.payerName} → ${payment.payeeName} (${payment.expenseTitle})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "[${payment.entryType}] via ${payment.method}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyCatalog.formatMinor(payment.amountMinor, payment.currency),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (payment.entryType == "REVERSAL") Color(0xFFC81E1E) else Color(0xFF15803D)
                        )
                    }
                }
            }
        }
    }
}
