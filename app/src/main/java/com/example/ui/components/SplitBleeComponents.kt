package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocalizedStrings
import com.example.ui.theme.OceanBlueDarkText
import com.example.ui.theme.OceanBluePrimary
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPartialBg
import com.example.ui.theme.StatusPartialText
import com.example.ui.theme.StatusUnpaidBg
import com.example.ui.theme.StatusUnpaidText

@Composable
fun OceanSailboatLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(72.dp)) {
        val w = size.width
        val h = size.height

        // Left deep-blue sail
        val leftSail = Path().apply {
            moveTo(w * 0.48f, h * 0.10f)
            lineTo(w * 0.48f, h * 0.68f)
            lineTo(w * 0.16f, h * 0.68f)
            close()
        }
        drawPath(
            path = leftSail,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2870F0), Color(0xFF144299))
            )
        )

        // Right lighter-blue sail
        val rightSail = Path().apply {
            moveTo(w * 0.53f, h * 0.22f)
            lineTo(w * 0.53f, h * 0.68f)
            lineTo(w * 0.84f, h * 0.68f)
            close()
        }
        drawPath(
            path = rightSail,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF66A6FF), Color(0xFF2468E8))
            )
        )

        // Hull
        val hull = Path().apply {
            moveTo(w * 0.14f, h * 0.73f)
            lineTo(w * 0.86f, h * 0.73f)
            lineTo(w * 0.75f, h * 0.86f)
            lineTo(w * 0.25f, h * 0.86f)
            close()
        }
        drawPath(path = hull, color = Color(0xFF102A63))

        // Gentle wave line beneath hull
        drawLine(
            color = Color(0xFF8AB8FF),
            start = Offset(w * 0.20f, h * 0.92f),
            end = Offset(w * 0.80f, h * 0.92f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun OceanHorizonIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(175.dp)) {
        val w = size.width
        val h = size.height

        // Soft sky-to-water gradient background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF3F7FD),
                    Color(0xFFDCEAFE),
                    Color(0xFFBBD7FB),
                    Color(0xFFEAF2FD)
                )
            )
        )

        // Distant soft mountain range
        val distantMountains = Path().apply {
            moveTo(0f, h * 0.56f)
            quadraticTo(w * 0.18f, h * 0.36f, w * 0.38f, h * 0.52f)
            quadraticTo(w * 0.62f, h * 0.34f, w * 0.82f, h * 0.48f)
            quadraticTo(w * 0.92f, h * 0.41f, w, h * 0.54f)
            lineTo(w, h * 0.62f)
            lineTo(0f, h * 0.62f)
            close()
        }
        drawPath(path = distantMountains, color = Color(0xFFC7DDFB))

        // Foreground island hills
        val nearHills = Path().apply {
            moveTo(0f, h * 0.62f)
            quadraticTo(w * 0.15f, h * 0.48f, w * 0.34f, h * 0.62f)
            lineTo(w * 0.66f, h * 0.62f)
            quadraticTo(w * 0.85f, h * 0.46f, w, h * 0.62f)
            close()
        }
        drawPath(path = nearHills, color = Color(0xFFA7C7F7))

        // Calm ocean waterband
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF8DB9F8), Color(0xFFD6E6FC), Color(0xFFEEF4FC)),
                startY = h * 0.62f,
                endY = h
            ),
            topLeft = Offset(0f, h * 0.62f),
            size = Size(w, h * 0.38f)
        )

        // Center sailboat on the horizon
        val cx = w * 0.5f
        val boatBaseY = h * 0.66f

        val mainSail = Path().apply {
            moveTo(cx - 2f, h * 0.14f)
            lineTo(cx - 2f, boatBaseY - 12f)
            lineTo(cx - 34f, boatBaseY - 12f)
            close()
        }
        drawPath(
            path = mainSail,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFDCEAFE))
            )
        )

        val jibSail = Path().apply {
            moveTo(cx + 3f, h * 0.24f)
            lineTo(cx + 3f, boatBaseY - 12f)
            lineTo(cx + 32f, boatBaseY - 12f)
            close()
        }
        drawPath(
            path = jibSail,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEAF2FF), Color(0xFF9EC5FA))
            )
        )

        // Mast
        drawLine(
            color = Color(0xFF102A63),
            start = Offset(cx, h * 0.12f),
            end = Offset(cx, boatBaseY - 6f),
            strokeWidth = 3f
        )

        // Hull
        val boatHull = Path().apply {
            moveTo(cx - 42f, boatBaseY - 8f)
            lineTo(cx + 42f, boatBaseY - 8f)
            lineTo(cx + 30f, boatBaseY + 5f)
            lineTo(cx - 30f, boatBaseY + 5f)
            close()
        }
        drawPath(path = boatHull, color = Color(0xFF102A63))

        // Water reflection ripples
        for (i in 1..4) {
            val ry = boatBaseY + 10f + i * 7f
            val halfSpan = (32f - i * 5f).coerceAtLeast(8f)
            drawLine(
                color = Color(0xFF1D5CE5).copy(alpha = 0.35f - i * 0.06f),
                start = Offset(cx - halfSpan, ry),
                end = Offset(cx + halfSpan, ry),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun UserAvatarCircle(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    avatarIndex: Int = 0,
    showOnlineDot: Boolean = false,
    wearSunglasses: Boolean = false
) {
    val bgPalette = listOf(
        listOf(Color(0xFFDCEAFE), Color(0xFFB6D4FE)),
        listOf(Color(0xFFE0E7FF), Color(0xFFC7D2FE)),
        listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0)),
        listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
        listOf(Color(0xFFFCE7F3), Color(0xFFFBCFE8))
    )
    val skinTones = listOf(
        Color(0xFFE0A378),
        Color(0xFFD4926A),
        Color(0xFFEAB68F),
        Color(0xFFC9825B)
    )
    val hairColors = listOf(
        Color(0xFF18181B),
        Color(0xFF27272A),
        Color(0xFF3F2314),
        Color(0xFF1E293B)
    )
    val idx = ( kotlin.math.abs(name.hashCode()) + avatarIndex ) % bgPalette.size
    val skin = skinTones[idx % skinTones.size]
    val hair = hairColors[idx % hairColors.size]
    val shirtColors = listOf(
        Color(0xFF1D5CE5),
        Color(0xFF0F295A),
        Color(0xFF0D9488),
        Color(0xFF4F46E5),
        Color(0xFF2563EB)
    )
    val shirt = shirtColors[idx % shirtColors.size]

    Box(modifier = modifier.size(size)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
        ) {
            val w = this.size.width
            val h = this.size.height
            drawRect(brush = Brush.verticalGradient(bgPalette[idx]))

            // Shoulders / collar
            drawArc(
                color = shirt,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.12f, h * 0.64f),
                size = Size(w * 0.76f, h * 0.65f)
            )

            // Neck
            drawRoundRect(
                color = skin,
                topLeft = Offset(w * 0.41f, h * 0.54f),
                size = Size(w * 0.18f, h * 0.16f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Head
            drawCircle(
                color = skin,
                radius = w * 0.23f,
                center = Offset(w * 0.5f, h * 0.42f)
            )

            // Hair top
            drawArc(
                color = hair,
                startAngle = 170f,
                sweepAngle = 200f,
                useCenter = true,
                topLeft = Offset(w * 0.25f, h * 0.15f),
                size = Size(w * 0.50f, h * 0.36f)
            )

            if (wearSunglasses || idx == 0) {
                // Cool dark sunglasses matching Rahul Shah avatar in mockup
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(w * 0.32f, h * 0.37f),
                    size = Size(w * 0.15f, h * 0.09f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(w * 0.53f, h * 0.37f),
                    size = Size(w * 0.15f, h * 0.09f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawLine(
                    color = Color(0xFF0F172A),
                    start = Offset(w * 0.47f, h * 0.40f),
                    end = Offset(w * 0.53f, h * 0.40f),
                    strokeWidth = 2.5f
                )
            } else {
                drawCircle(color = hair, radius = w * 0.025f, center = Offset(w * 0.41f, h * 0.41f))
                drawCircle(color = hair, radius = w * 0.025f, center = Offset(w * 0.59f, h * 0.41f))
            }

            // Friendly smile
            drawArc(
                color = Color(0xFF7C2D12),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.46f),
                size = Size(w * 0.16f, h * 0.08f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }

        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.26f).dp.coerceAtLeast(10.dp))
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun CategoryIconTile(category: String, title: String) {
    val lower = (category + " " + title).lowercase()
    val (bgColor, tintColor, icon) = when {
        "pizza" in lower || "food" in lower || "dinner" in lower || "restaurant" in lower ->
            Triple(Color(0xFFFFF1E6), Color(0xFFEA580C), Icons.Filled.LocalPizza)
        "shop" in lower || "mall" in lower || "store" in lower ->
            Triple(Color(0xFFF3E8FF), Color(0xFF9333EA), Icons.Filled.ShoppingBag)
        "coffee" in lower || "cafe" in lower || "tea" in lower || "drink" in lower ->
            Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Filled.Coffee)
        "travel" in lower || "trip" in lower || "cab" in lower || "flight" in lower ->
            Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Icons.Filled.DirectionsCar)
        else ->
            Triple(Color(0xFFE0EAFF), OceanBluePrimary, Icons.AutoMirrored.Filled.ReceiptLong)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = tintColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun StatusPillBadge(status: String) {
    val normalized = status.uppercase()
    val (bg, fg, label) = when (normalized) {
        "PAID" -> Triple(StatusPaidBg, StatusPaidText, "Paid")
        "PARTIALLY_PAID" -> Triple(StatusPartialBg, StatusPartialText, "Partial")
        "OVERPAID" -> Triple(Color(0xFFE0E7FF), Color(0xFF1D4ED8), "Overpaid")
        "CANCELLED" -> Triple(Color(0xFFE2E8F0), Color(0xFF475569), "Cancelled")
        else -> Triple(StatusUnpaidBg, StatusUnpaidText, "Unpaid")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ExpenseHeaderIllustration(title: String, category: String, modifier: Modifier = Modifier) {
    val isPizzaOrFood = (title + " " + category).lowercase().let {
        "pizza" in it || "food" in it || "dinner" in it || "lunch" in it
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (isPizzaOrFood) {
                // Warm rustic wood table + fresh artisan pizza illustration matching Screen 4 mockup
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF5C3A21), Color(0xFF8B5A2B), Color(0xFF4A2E1B))
                    )
                )
                val cx = w * 0.54f
                val cy = h * 0.52f
                val radius = h * 0.62f

                // Wooden pizza board
                drawCircle(color = Color(0xFFA47148), radius = radius * 1.08f, center = Offset(cx, cy))
                // Golden crust
                drawCircle(color = Color(0xFFE59838), radius = radius * 0.96f, center = Offset(cx, cy))
                // Tomato & melted mozzarella base
                drawCircle(color = Color(0xFFD9381E), radius = radius * 0.84f, center = Offset(cx, cy))
                drawCircle(color = Color(0xFFFBBF24), radius = radius * 0.78f, center = Offset(cx, cy))

                // Slice lines
                for (angle in listOf(0f, 45f, 90f, 135f)) {
                    val rad = Math.toRadians(angle.toDouble())
                    val dx = ( kotlin.math.cos(rad) * radius * 0.84f ).toFloat()
                    val dy = ( kotlin.math.sin(rad) * radius * 0.84f ).toFloat()
                    drawLine(
                        color = Color(0xFFB45309),
                        start = Offset(cx - dx, cy - dy),
                        end = Offset(cx + dx, cy + dy),
                        strokeWidth = 3f
                    )
                }

                // Toppings (tomatoes, basil leaves, olives)
                val offsets = listOf(
                    Offset(-0.35f, -0.25f), Offset(0.30f, -0.30f),
                    Offset(-0.25f, 0.32f), Offset(0.35f, 0.25f),
                    Offset(0.0f, -0.42f), Offset(-0.45f, 0.05f), Offset(0.42f, -0.05f)
                )
                offsets.forEachIndexed { i, off ->
                    drawCircle(
                        color = if (i % 2 == 0) Color(0xFFDC2626) else Color(0xFF15803D),
                        radius = 9f,
                        center = Offset(cx + off.x * radius, cy + off.y * radius)
                    )
                }
            } else {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF1D5CE5), Color(0xFF102A63))
                    )
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = h * 0.7f,
                    center = Offset(w * 0.8f, h * 0.2f)
                )
            }
        }
    }
}

@Composable
fun SplitBleeBottomBar(
    currentTab: String,
    strings: LocalizedStrings,
    onSelectTab: (String) -> Unit,
    onNewSplitClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(10.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTabItem(
                label = strings.home,
                selected = currentTab == "dashboard",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_home",
                onClick = { onSelectTab("dashboard") }
            )
            BottomNavTabItem(
                label = strings.groups,
                selected = currentTab == "groups",
                selectedIcon = Icons.Filled.Group,
                unselectedIcon = Icons.Outlined.Group,
                testTag = "nav_groups",
                onClick = { onSelectTab("groups") }
            )

            // Central Ocean-Blue '+' Action Button matching Screen 2 mockup
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .offset(y = (-2).dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2B6FF7), OceanBluePrimary)
                        )
                    )
                    .clickable(onClick = onNewSplitClick)
                    .testTag("nav_new_split"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = strings.newSplit,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            BottomNavTabItem(
                label = strings.map,
                selected = currentTab == "map",
                selectedIcon = Icons.Filled.LocationOn,
                unselectedIcon = Icons.Outlined.LocationOn,
                testTag = "nav_map",
                onClick = { onSelectTab("map") }
            )
            BottomNavTabItem(
                label = strings.profile,
                selected = currentTab == "settings",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                testTag = "nav_profile",
                onClick = { onSelectTab("settings") }
            )
        }
    }
}

@Composable
private fun BottomNavTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val tint = if (selected) OceanBluePrimary else Color(0xFF64748B)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
