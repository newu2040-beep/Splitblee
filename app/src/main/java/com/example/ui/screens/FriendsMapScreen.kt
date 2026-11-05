package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurrencyCatalog
import com.example.model.FriendProfile
import com.example.model.UserProfile
import com.example.ui.UiState
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.LocalActivePalette
import com.example.ui.theme.LocalAppDimens
import com.example.util.RealTimeLocationAndMediaHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsMapScreen(
    userProfile: UserProfile?,
    friendsState: UiState<List<FriendProfile>>,
    onBack: () -> Unit,
    onAddFriend: (
        name: String,
        email: String?,
        age: Int?,
        gender: String,
        address: String?,
        locationSharing: Boolean,
        latitude: Double?,
        longitude: Double?,
        currency: String,
        avatarIndex: Int
    ) -> Unit,
    onToggleFriendLocation: (FriendProfile, Boolean, Double?, Double?, String?) -> Unit,
    onToggleFriendBlocked: (FriendProfile) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val palette = LocalActivePalette.current
    val dimens = LocalAppDimens.current

    var isMapView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember {
        mutableStateOf(RealTimeLocationAndMediaHelper.hasLocationPermission(context))
    }
    var locationStatusBanner by remember { mutableStateOf<String?>(null) }

    val liveGpsState by RealTimeLocationAndMediaHelper.rememberLiveGpsLocation(
        context = context,
        enabled = locationPermissionGranted
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = granted
        locationStatusBanner = if (granted) {
            "Live GPS receiver active. Explicit friend pin sharing remains controlled per friend."
        } else {
            "Location permission denied. List view works without GPS access."
        }
    }

    val allFriends = (friendsState as? UiState.Success)?.data ?: emptyList()
    val filteredFriends = remember(allFriends, searchQuery) {
        if (searchQuery.isBlank()) allFriends
        else allFriends.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                (it.address?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    var selectedFriendId by remember(allFriends) {
        mutableStateOf(allFriends.firstOrNull()?.id)
    }
    val selectedFriend = allFriends.firstOrNull { it.id == selectedFriendId } ?: allFriends.firstOrNull()
    val displayCurrency = userProfile?.defaultCurrency ?: "USD"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.horizontalPadding, vertical = 6.dp)
    ) {
        // Top Header Bar matching Screen 3 mockup: Back `<`, `Map | List` segmented pill, Add Friend button
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
                    modifier = Modifier.testTag("map_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Segmented Map | List Switch matching Screen 3 mockup
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.height(dimens.headerIconSize)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isMapView) palette.primary else Color.Transparent)
                            .clickable { isMapView = true }
                            .padding(horizontal = if (dimens.isCompact) 18.dp else 24.dp, vertical = 5.dp)
                            .testTag("toggle_map_view"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Map",
                            color = if (isMapView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (!isMapView) palette.primary else Color.Transparent)
                            .clickable { isMapView = false }
                            .padding(horizontal = if (dimens.isCompact) 18.dp else 24.dp, vertical = 5.dp)
                            .testTag("toggle_list_view"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "List",
                            color = if (!isMapView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(dimens.headerIconSize)
            ) {
                IconButton(
                    onClick = { showAddFriendDialog = true },
                    modifier = Modifier.testTag("map_add_friend_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Add Friend",
                        tint = palette.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live GPS Telemetry Banner
        val gpsText = liveGpsState?.let { gps ->
            "● Live GPS (${gps.provider}): ${String.format(Locale.US, "%.5f, %.5f", gps.latitude, gps.longitude)} (±${gps.accuracyMeters.toInt()}m) • ${gps.addressLabel}"
        } ?: locationStatusBanner

        if (!gpsText.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Text(
                    text = gpsText,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (isMapView) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(dimens.cardRadius))
                    .background(palette.lightContainer.copy(alpha = 0.55f))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    drawRect(color = palette.lightBackground)

                    val park1 = Path().apply {
                        moveTo(w * 0.65f, h * 0.05f)
                        lineTo(w * 0.95f, h * 0.12f)
                        lineTo(w * 0.88f, h * 0.35f)
                        lineTo(w * 0.60f, h * 0.26f)
                        close()
                    }
                    drawPath(park1, color = Color(0xFFD6F0DF))

                    val park2 = Path().apply {
                        moveTo(w * 0.05f, h * 0.50f)
                        lineTo(w * 0.35f, h * 0.56f)
                        lineTo(w * 0.28f, h * 0.82f)
                        lineTo(0f, h * 0.75f)
                        close()
                    }
                    drawPath(park2, color = Color(0xFFD6F0DF))

                    val river = Path().apply {
                        moveTo(w * 0.18f, 0f)
                        quadraticTo(w * 0.42f, h * 0.38f, w * 0.78f, h * 0.85f)
                    }
                    drawPath(
                        path = river,
                        color = palette.lightContainer,
                        style = Stroke(width = 34f, cap = StrokeCap.Round)
                    )

                    val roadLines = listOf(
                        Offset(0f, h * 0.22f) to Offset(w, h * 0.48f),
                        Offset(0f, h * 0.65f) to Offset(w, h * 0.32f),
                        Offset(w * 0.32f, 0f) to Offset(w * 0.12f, h),
                        Offset(w * 0.74f, 0f) to Offset(w * 0.54f, h),
                        Offset(w * 0.50f, 0f) to Offset(w * 0.88f, h * 0.7f)
                    )
                    roadLines.forEach { (start, end) ->
                        drawLine(
                            color = Color.White.copy(alpha = 0.88f),
                            start = start,
                            end = end,
                            strokeWidth = 13f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Surrounding Friend Pins (only for friends with locationSharing == true)
                val sharingFriends = allFriends.filter { it.locationSharing && it.status != "Blocked" }
                val pinPositions = listOf(
                    Offset(0.18f, 0.20f),
                    Offset(0.74f, 0.22f),
                    Offset(0.18f, 0.58f),
                    Offset(0.72f, 0.54f)
                )

                sharingFriends.filter { it.id != selectedFriend?.id }.take(4).forEachIndexed { index, friend ->
                    val pos = pinPositions[index % pinPositions.size]
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (pos.x * 230).dp,
                                y = (pos.y * 190).dp
                            )
                            .clickable { selectedFriendId = friend.id }
                            .testTag("map_pin_${friend.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatarCircle(
                            name = friend.name,
                            size = if (dimens.isCompact) 44.dp else 52.dp,
                            avatarIndex = friend.avatarIndex.toInt(),
                            showOnlineDot = friend.isOnline
                        )
                    }
                }

                // Central Highlighted Friend Pin matching Screen 3 mockup
                if (selectedFriend != null) {
                    val centralSize = if (dimens.isCompact) 74.dp else 88.dp
                    val innerAvatar = if (dimens.isCompact) 62.dp else 74.dp
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-12).dp)
                            .clickable { showProfileSheet = true }
                            .testTag("map_central_friend_pin"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(centralSize)
                                .shadow(12.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(palette.primary, palette.deep)
                                    )
                                )
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatarCircle(
                                name = selectedFriend.name,
                                size = innerAvatar,
                                avatarIndex = selectedFriend.avatarIndex.toInt(),
                                wearSunglasses = true
                            )
                        }
                        Canvas(modifier = Modifier.size(width = 22.dp, height = 12.dp)) {
                            val triangle = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(size.width / 2f, size.height)
                                close()
                            }
                            drawPath(triangle, color = palette.primary)
                        }
                    }
                }

                // Floating Live GPS Button on bottom-right of map
                Surface(
                    onClick = {
                        if (!locationPermissionGranted) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            val gps = liveGpsState
                            locationStatusBanner = if (gps != null) {
                                "Live GPS locked: ${String.format(Locale.US, "%.5f, %.5f", gps.latitude, gps.longitude)} (${gps.addressLabel})"
                            } else {
                                "Waiting for hardware GPS fix from LocationManager..."
                            }
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(if (dimens.isCompact) 40.dp else 46.dp)
                        .testTag("map_my_location_fab")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.NearMe,
                            contentDescription = "Request Live GPS Location",
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Compact Friend Detail Sheet Card matching Screen 3 mockup
            Surface(
                shape = RoundedCornerShape(dimens.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimens.bottomBarClearance - 16.dp)
                    .testTag("friend_detail_card")
            ) {
                if (selectedFriend == null) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Friends in Network Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add friends to track real-time split balances and opt-in live GPS pins.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddFriendDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Real Friend")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (dimens.isCompact) 14.dp else 18.dp, vertical = if (dimens.isCompact) 11.dp else 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProfileSheet = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatarCircle(
                                name = selectedFriend.name,
                                size = if (dimens.isCompact) 48.dp else 58.dp,
                                avatarIndex = selectedFriend.avatarIndex.toInt(),
                                wearSunglasses = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedFriend.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = if (dimens.isCompact) 16.sp else 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Age: ${selectedFriend.age ?: 25} • ${selectedFriend.gender}" +
                                        (selectedFriend.address?.let { " • $it" } ?: ""),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selectedFriend.locationSharing) Color(0xFFDDF6E8)
                                            else Color(0xFFE2E8F0)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 2.dp)
                                ) {
                                    val coordSuffix = if (selectedFriend.locationSharing && selectedFriend.latitude != null && selectedFriend.longitude != null) {
                                        String.format(Locale.US, " (%.3f, %.3f)", selectedFriend.latitude, selectedFriend.longitude)
                                    } else ""
                                    Text(
                                        text = if (selectedFriend.locationSharing) "● Live Pin$coordSuffix" else "● Location Private",
                                        color = if (selectedFriend.locationSharing) Color(0xFF15803D) else Color(0xFF475569),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // 3-Column Real-Time Balances Row: Owes you | You owe | Splits
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = CurrencyCatalog.formatMinor(
                                        selectedFriend.owesYouMinor,
                                        selectedFriend.currency.ifBlank { displayCurrency }
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = if (dimens.isCompact) 15.sp else 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Owes you",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(28.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = CurrencyCatalog.formatMinor(
                                        selectedFriend.youOweMinor,
                                        selectedFriend.currency.ifBlank { displayCurrency }
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = if (dimens.isCompact) 15.sp else 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "You owe",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(28.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${selectedFriend.splitsCount}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = if (dimens.isCompact) 15.sp else 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Splits",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showProfileSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (dimens.isCompact) 40.dp else 46.dp)
                                .testTag("view_profile_button")
                        ) {
                            Text(
                                text = "View Profile",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (allFriends.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allFriends.take(4).forEachIndexed { idx, friend ->
                                    UserAvatarCircle(
                                        name = friend.name,
                                        size = dimens.avatarSizeSmall,
                                        avatarIndex = idx + 1,
                                        modifier = Modifier
                                            .offset(x = ((-5) * idx).dp)
                                            .clickable { selectedFriendId = friend.id }
                                    )
                                }
                                if (allFriends.size > 4) {
                                    Box(
                                        modifier = Modifier
                                            .size(dimens.avatarSizeSmall)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .border(2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${allFriends.size - 4}",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search friends by name or shared place...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    IconButton(onClick = { showAddFriendDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Friend")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("friends_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = dimens.bottomBarClearance),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFriends, key = { it.id }) { friend ->
                    Surface(
                        onClick = {
                            selectedFriendId = friend.id
                            showProfileSheet = true
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatarCircle(
                                name = friend.name,
                                size = dimens.avatarSizeMedium,
                                avatarIndex = friend.avatarIndex.toInt(),
                                showOnlineDot = friend.isOnline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Age: ${friend.age ?: 25} • ${friend.gender} • ${if (friend.locationSharing) (friend.address ?: "Live pin active") else "Location private"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Owes: ${CurrencyCatalog.formatMinor(friend.owesYouMinor, friend.currency)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "Splits: ${friend.splitsCount}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileSheet && selectedFriend != null) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatarCircle(
                        name = selectedFriend.name,
                        size = 56.dp,
                        avatarIndex = selectedFriend.avatarIndex.toInt(),
                        wearSunglasses = true
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(selectedFriend.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "Age: ${selectedFriend.age ?: 25} • Gender: ${selectedFriend.gender} • Status: ${selectedFriend.status}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!selectedFriend.address.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedFriend.address,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Explicit Live GPS Pin Sharing", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Location is private by default. Syncs live coordinates when enabled.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = selectedFriend.locationSharing,
                        onCheckedChange = { enabled ->
                            val gps = liveGpsState
                            onToggleFriendLocation(
                                selectedFriend,
                                enabled,
                                gps?.latitude,
                                gps?.longitude,
                                gps?.addressLabel
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onToggleFriendBlocked(selectedFriend)
                            showProfileSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (selectedFriend.status == "Blocked") "Unblock User" else "Block User")
                    }
                    Button(
                        onClick = { showProfileSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAddFriendDialog) {
        val gps = liveGpsState
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var ageText by remember { mutableStateOf("25") }
        var gender by remember { mutableStateOf("Male") }
        var address by remember(gps) { mutableStateOf(gps?.addressLabel ?: "") }
        var shareLocation by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Add Real-Time Friend", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Friend Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Age") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier.weight(1.4f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Male", "Female").forEach { g ->
                                FilterChip(
                                    selected = gender == g,
                                    onClick = { gender = g },
                                    label = { Text(g, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Live Venue") },
                        placeholder = { Text(gps?.addressLabel ?: "Enter city or tap GPS on map") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = shareLocation,
                            onCheckedChange = { shareLocation = it }
                        )
                        Text(
                            text = "Consent to share live GPS pin on Friends Map",
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddFriend(
                                name.trim(),
                                email.trim().ifEmpty { null },
                                ageText.toIntOrNull(),
                                gender,
                                address.trim().ifEmpty { gps?.addressLabel },
                                shareLocation,
                                if (shareLocation) gps?.latitude else null,
                                if (shareLocation) gps?.longitude else null,
                                displayCurrency,
                                allFriends.size % 5
                            )
                            showAddFriendDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text("Save Friend")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
