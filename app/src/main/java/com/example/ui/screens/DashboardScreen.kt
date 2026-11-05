package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.LocalActivePalette
import com.example.ui.theme.LocalAppDimens

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    photoUri: String?,
    expensesState: UiState<List<ExpenseItem>>,
    strings: LocalizedStrings,
    onOpenNewSplit: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExpenseDetail: (String) -> Unit,
    onToggleCompactMode: () -> Unit,
    onCyclePastelTheme: () -> Unit
) {
    val palette = LocalActivePalette.current
    val dimens = LocalAppDimens.current
    val displayCurrency = userProfile?.defaultCurrency ?: "USD"
    val fullName = (userProfile?.displayName?.ifBlank { fallbackName } ?: fallbackName).ifBlank { "SplitBlee User" }
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
            .padding(horizontal = dimens.horizontalPadding),
        contentPadding = PaddingValues(top = 10.dp, bottom = dimens.bottomBarClearance),
        verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
    ) {
        // Top Header Bar: Menu icon + Quick Theme/Compact Pills + User Avatar with online dot
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

                // Quick Interactive Pills for Pastel Theme & Compact Display Mode
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onCyclePastelTheme,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("dashboard_quick_theme_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = "Cycle Theme",
                                tint = palette.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = palette.name,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleCompactMode,
                        shape = RoundedCornerShape(50),
                        color = if (dimens.isCompact) palette.primary else MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        modifier = Modifier.testTag("dashboard_compact_mode_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AspectRatio,
                                contentDescription = "Compact Mode",
                                tint = if (dimens.isCompact) Color.White else palette.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (dimens.isCompact) "Compact" else dimens.uiScaleMode,
                                color = if (dimens.isCompact) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                UserAvatarCircle(
                    name = fullName,
                    size = dimens.avatarSizeMedium,
                    showOnlineDot = true,
                    wearSunglasses = true,
                    photoUri = photoUri,
                    modifier = Modifier
                        .clickable { onOpenSettings() }
                        .testTag("dashboard_avatar_button")
                )
            }
        }

        // Greeting Section matching Screen 2 mockup
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "${strings.hello}\n$firstName \uD83D\uDC4B",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = if (dimens.isCompact) 24.sp else 30.sp,
                    lineHeight = if (dimens.isCompact) 29.sp else 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = strings.dashboardSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (dimens.isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Prominent Total Balance Card styled dynamically by active Pastel / Ocean Theme
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(dimens.cardRadius))
                    .clip(RoundedCornerShape(dimens.cardRadius))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                palette.primary,
                                palette.deep
                            )
                        )
                    )
                    .clickable { onOpenHistory() }
                    .padding(if (dimens.isCompact) 16.dp else 20.dp)
                    .testTag("total_balance_card")
            ) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(if (dimens.isCompact) 115.dp else 150.dp)
                        .height(if (dimens.isCompact) 48.dp else 64.dp)
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
                        color = Color.White.copy(alpha = 0.50f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.totalBalance,
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = if (dimens.isCompact) 13.sp else 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "● Live Sync",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(if (dimens.isCompact) 30.dp else 34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = strings.history,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = CurrencyCatalog.formatMinor(displayBalanceMinor, displayCurrency),
                        color = Color.White,
                        fontSize = if (dimens.isCompact) 28.sp else 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (activeExpenses.isNotEmpty()) {
                            "↑ Owed to you: ${CurrencyCatalog.formatMinor(totalOwedToYouMinor, displayCurrency)} • You owe: ${CurrencyCatalog.formatMinor(totalYouOweMinor, displayCurrency)}"
                        } else {
                            "↑ Real-time Cloud Firestore ledger connected • ${activeExpenses.size} active splits"
                        },
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = if (dimens.isCompact) 11.sp else 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4 Quick Action Buttons (New Split, Groups, History, Map) matching Screen 2 mockup
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (dimens.isCompact) 8.dp else 12.dp)
            ) {
                QuickActionTile(
                    label = strings.newSplit,
                    icon = Icons.Filled.AddCircleOutline,
                    accentColor = palette.primary,
                    tileHeight = dimens.quickTileHeight,
                    testTag = "quick_action_new_split",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenNewSplit
                )
                QuickActionTile(
                    label = strings.groups,
                    icon = Icons.Outlined.Group,
                    accentColor = palette.primary,
                    tileHeight = dimens.quickTileHeight,
                    testTag = "quick_action_groups",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenGroups
                )
                QuickActionTile(
                    label = strings.history,
                    icon = Icons.Filled.History,
                    accentColor = palette.primary,
                    tileHeight = dimens.quickTileHeight,
                    testTag = "quick_action_history",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHistory
                )
                QuickActionTile(
                    label = strings.map,
                    icon = Icons.Outlined.LocationOn,
                    accentColor = palette.primary,
                    tileHeight = dimens.quickTileHeight,
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
                    fontSize = if (dimens.isCompact) 16.sp else 18.sp,
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
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = strings.seeAll,
                        tint = palette.primary,
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
                        CircularProgressIndicator(color = palette.primary)
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
                            accentColor = palette.primary,
                            onCreateSplit = onOpenNewSplit
                        )
                    }
                } else {
                    items(activeExpenses.take(15), key = { it.id }) { expense ->
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
    accentColor: Color,
    tileHeight: androidx.compose.ui.unit.Dp,
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
            .height(tileHeight)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExpenseRowCard(
    expense: ExpenseItem,
    onClick: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val shortDate = expense.expenseDate.substringBefore(",").ifBlank { expense.expenseDate }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(dimens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_item_${expense.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (dimens.isCompact) 11.dp else 14.dp, vertical = if (dimens.isCompact) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconTile(category = expense.category, title = expense.title)

            Spacer(modifier = Modifier.width(if (dimens.isCompact) 10.dp else 14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (dimens.isCompact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$shortDate • ${expense.participantCount} people • ${expense.place}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (dimens.isCompact) 11.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyCatalog.formatMinor(expense.totalMinor, expense.currency),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (dimens.isCompact) 14.sp else 16.sp,
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
    accentColor: Color,
    onCreateSplit: () -> Unit
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
                text = "Your Real-Time Ledger is Ready",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Create your first bill split with friends. Every split, receipt photo, and settlement syncs live with Cloud Firestore.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onCreateSplit,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("empty_create_split_button")
            ) {
                Text("Create New Split", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
