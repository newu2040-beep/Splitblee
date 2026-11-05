package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurrencyCatalog
import com.example.model.ExpenseItem
import com.example.model.LocalizedStrings
import com.example.model.UserProfile
import com.example.ui.UiState
import com.example.ui.components.CategoryIconTile
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.OceanBlueDarkText
import com.example.ui.theme.OceanBluePrimary
import com.example.ui.theme.OceanBlueSecondaryText

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    expensesState: UiState<List<ExpenseItem>>,
    strings: LocalizedStrings,
    onOpenNewSplit: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExpenseDetail: (String) -> Unit,
    onSeedStarterData: () -> Unit
) {
    val displayCurrency = userProfile?.defaultCurrency ?: "USD"
    val fullName = (userProfile?.displayName?.ifBlank { fallbackName } ?: fallbackName).ifBlank { "Rahul" }
    val firstName = fullName.trim().substringBefore(" ")

    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
    val activeExpenses = expenses.filter { it.status != "CANCELLED" }

    val totalVolumeMinor = activeExpenses.sumOf {
        CurrencyCatalog.convertMinorEstimate(it.totalMinor, it.currency, displayCurrency)
    }
    val totalOwedToYouMinor = activeExpenses.sumOf {
        CurrencyCatalog.convertMinorEstimate(it.owedToYouMinor, it.currency, displayCurrency)
    }
    val totalYouOweMinor = activeExpenses.sumOf {
        CurrencyCatalog.convertMinorEstimate(it.youOweMinor, it.currency, displayCurrency)
    }
    val displayBalanceMinor = if (totalVolumeMinor > 0L) totalVolumeMinor else 0L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header Bar: Menu icon + User Avatar with online dot
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
                        onClick = onOpenHistory,
                        modifier = Modifier.testTag("dashboard_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                UserAvatarCircle(
                    name = fullName,
                    size = 44.dp,
                    showOnlineDot = true,
                    wearSunglasses = true,
                    modifier = Modifier
                        .clickable { onOpenSettings() }
                        .testTag("dashboard_avatar_button")
                )
            }
        }

        // Greeting Section matching Screen 2 mockup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${strings.hello}\n$firstName \uD83D\uDC4B",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = strings.dashboardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Prominent Total Balance Ocean-Blue Card matching Screen 2 mockup
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2B6EF6),
                                Color(0xFF1A52C4),
                                Color(0xFF123684)
                            )
                        )
                    )
                    .clickable { onOpenHistory() }
                    .padding(20.dp)
                    .testTag("total_balance_card")
            ) {
                // Smooth decorative trend wave in bottom-right of card
                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(150.dp)
                        .height(64.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val wavePath = Path().apply {
                        moveTo(0f, h * 0.85f)
                        quadraticTo(w * 0.18f, h * 0.45f, w * 0.34f, h * 0.68f)
                        quadraticTo(w * 0.52f, h * 0.90f, w * 0.68f, h * 0.35f)
                        quadraticTo(w * 0.84f, h * 0.10f, w, h * 0.42f)
                    }
                    drawPath(
                        path = wavePath,
                        color = Color.White.copy(alpha = 0.55f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.totalBalance,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = strings.history,
                                tint = OceanBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = CurrencyCatalog.formatMinor(displayBalanceMinor, displayCurrency),
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (activeExpenses.isNotEmpty()) {
                                "↑ Owed: ${CurrencyCatalog.formatMinor(totalOwedToYouMinor, displayCurrency)} • You owe: ${CurrencyCatalog.formatMinor(totalYouOweMinor, displayCurrency)}"
                            } else {
                                "↑ Real-time ledger ready • 0 splits this month"
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4 Quick Action Buttons (New Split, Groups, History, Map) matching Screen 2 mockup
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    label = strings.newSplit,
                    icon = Icons.Filled.AddCircleOutline,
                    testTag = "quick_action_new_split",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenNewSplit
                )
                QuickActionTile(
                    label = strings.groups,
                    icon = Icons.Outlined.Group,
                    testTag = "quick_action_groups",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenGroups
                )
                QuickActionTile(
                    label = strings.history,
                    icon = Icons.Filled.History,
                    testTag = "quick_action_history",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHistory
                )
                QuickActionTile(
                    label = strings.map,
                    icon = Icons.Outlined.LocationOn,
                    testTag = "quick_action_map",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMap
                )
            }
        }

        // Recent Splits Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentSplits,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenHistory() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("see_all_splits_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.seeAll,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = strings.seeAll,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Recent Splits Content
        when (expensesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OceanBluePrimary)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = expensesState.message,
                            color = Color(0xFFB91C1C),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (activeExpenses.isEmpty()) {
                    item {
                        EmptySplitsCard(
                            onCreateSplit = onOpenNewSplit,
                            onSeedStarterData = onSeedStarterData
                        )
                    }
                } else {
                    items(activeExpenses.take(10), key = { it.id }) { expense ->
                        ExpenseRowCard(
                            expense = expense,
                            onClick = { onOpenExpenseDetail(expense.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
            .height(78.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = OceanBluePrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExpenseRowCard(
    expense: ExpenseItem,
    onClick: () -> Unit
) {
    val shortDate = expense.expenseDate.substringBefore(",").ifBlank { expense.expenseDate }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_item_${expense.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconTile(category = expense.category, title = expense.title)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$shortDate • ${expense.participantCount} people",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyCatalog.formatMinor(expense.totalMinor, expense.currency),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPillBadge(status = expense.status)
            }
        }
    }
}

@Composable
private fun EmptySplitsCard(
    onCreateSplit: () -> Unit,
    onSeedStarterData: () -> Unit
) {
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
            Text(
                text = "No Splits Recorded Yet",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Start a real split with your friends, or populate your Firestore ledger with the reference splits (Pizza Dinner, Shopping Mall, Coffee Break) to explore!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCreateSplit,
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBluePrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("empty_create_split_button")
                ) {
                    Text("Create Split", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onSeedStarterData,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("seed_starter_splits_button")
                ) {
                    Text("Load Reference Splits", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
