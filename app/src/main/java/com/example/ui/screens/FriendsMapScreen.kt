package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CurrencyCatalog
import com.example.model.FriendProfile
import com.example.model.UserProfile
import com.example.ui.UiState
import com.example.ui.components.UserAvatarCircle
import com.example.ui.theme.OceanBlueDarkText
import com.example.ui.theme.OceanBluePrimary
import com.example.ui.theme.OceanBlueSecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsMapScreen(
    userProfile: UserProfile?,
    friendsState: UiState<List<FriendProfile>>,
    onBack: () -> Unit,
    onAddFriend: (name: String, email: String?, age: Int?, gender: String, address: String?, locationSharing: Boolean, currency: String, avatarIndex: Int) -> Unit,
    onToggleFriendLocation: (FriendProfile, Boolean) -> Unit,
    onToggleFriendBlocked: (FriendProfile) -> Unit,
    onSeedStarterFriends: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var isMapView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var locationStatusBanner by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = granted
        locationStatusBanner = if (granted) {
            "Location permission granted. Explicit sharing remains controlled per friend."
        } else {
            "Location permission denied. Switching to List view works without location permission."
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header Bar matching Screen 3 mockup: Back `<`, `Map | List` segmented pill, Filter/Add button
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
                modifier = Modifier.height(42.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isMapView) OceanBluePrimary else Color.Transparent)
                            .clickable { isMapView = true }
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                            .testTag("toggle_map_view"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Map",
                            color = if (isMapView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (!isMapView) OceanBluePrimary else Color.Transparent)
                            .clickable { isMapView = false }
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                            .testTag("toggle_list_view"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "List",
                            color = if (!isMapView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(
                    onClick = { showAddFriendDialog = true },
                    modifier = Modifier.testTag("map_add_friend_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Add or Filter Friends",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (locationStatusBanner != null) {
            Surface(
                color = Color(0xFFE0ECFF),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = locationStatusBanner ?: "",
                    color = OceanBlueDarkText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        if (isMapView) {
            // Map + Bottom Compact Friend Card matching Screen 3 mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFE4EFFC))
            ) {
                // Custom City Map Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Soft map base
                    drawRect(color = Color(0xFFE7F0FA))

                    // Green park areas
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

                    // River channel
                    val river = Path().apply {
                        moveTo(w * 0.18f, 0f)
                        quadraticTo(w * 0.42f, h * 0.38f, w * 0.78f, h * 0.85f)
                    }
                    drawPath(
                        path = river,
                        color = Color(0xFFB9D7FA),
                        style = Stroke(width = 36f, cap = StrokeCap.Round)
                    )

                    // Major and minor roads
                    val roadLines = listOf(
                        Offset(0f, h * 0.22f) to Offset(w, h * 0.48f),
                        Offset(0f, h * 0.65f) to Offset(w, h * 0.32f),
                        Offset(w * 0.32f, 0f) to Offset(w * 0.12f, h),
                        Offset(w * 0.74f, 0f) to Offset(w * 0.54f, h),
                        Offset(w * 0.50f, 0f) to Offset(w * 0.88f, h * 0.7f)
                    )
                    roadLines.forEach { (start, end) ->
                        drawLine(
                            color = Color.White.copy(alpha = 0.85f),
                            start = start,
                            end = end,
                            strokeWidth = 14f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Surrounding Friend Pins (only for friends with locationSharing == true)
                val sharingFriends = allFriends.filter { it.locationSharing && it.status != "Blocked" }
                val pinPositions = listOf(
                    Offset(0.22f, 0.24f),
                    Offset(0.76f, 0.26f),
                    Offset(0.20f, 0.62f),
                    Offset(0.74f, 0.56f)
                )

                sharingFriends.filter { it.id != selectedFriend?.id }.take(4).forEachIndexed { index, friend ->
                    val pos = pinPositions[index % pinPositions.size]
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (pos.x * 260).dp,
                                y = (pos.y * 230).dp
                            )
                            .clickable { selectedFriendId = friend.id }
                            .testTag("map_pin_${friend.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatarCircle(
                            name = friend.name,
                            size = 54.dp,
                            avatarIndex = friend.avatarIndex.toInt(),
                            showOnlineDot = friend.isOnline
                        )
                    }
                }

                // Central Highlighted Friend Pin matching Screen 3 mockup
                if (selectedFriend != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-16).dp)
                            .clickable { showProfileSheet = true }
                            .testTag("map_central_friend_pin"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .shadow(12.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF3B82F6), OceanBluePrimary)
                                    )
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatarCircle(
                                name = selectedFriend.name,
                                size = 78.dp,
                                avatarIndex = selectedFriend.avatarIndex.toInt(),
                                wearSunglasses = true
                            )
                        }
                        // Teardrop pointer triangle
                        Canvas(modifier = Modifier.size(width = 24.dp, height = 14.dp)) {
                            val triangle = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(size.width / 2f, size.height)
                                close()
                            }
                            drawPath(triangle, color = OceanBluePrimary)
                        }
                    }
                }

                // Floating My-Location Paper Plane Button on bottom-right of map
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
                            locationStatusBanner = "Centered on Thamel, Kathmandu • Explicit location sharing active."
                        }
                    },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(46.dp)
                        .testTag("map_my_location_fab")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.NearMe,
                            contentDescription = "Request Location",
                            tint = OceanBluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Compact Friend Detail Sheet Card matching Screen 3 mockup
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 86.dp)
                    .testTag("friend_detail_card")
            ) {
                if (selectedFriend == null) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Friends Added Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add friends or load reference friends to view balances and opt-in map pins.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showAddFriendDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBluePrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Friend")
                            }
                            OutlinedButton(
                                onClick = onSeedStarterFriends,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Load Sample Friends")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        // Top Row: Avatar + Name + Verified + Age/Gender + Online Pill + Chevron
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProfileSheet = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatarCircle(
                                name = selectedFriend.name,
                                size = 62.dp,
                                avatarIndex = selectedFriend.avatarIndex.toInt(),
                                wearSunglasses = true
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedFriend.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = "Age: ${selectedFriend.age ?: 25} • ${selectedFriend.gender}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selectedFriend.locationSharing) Color(0xFFDDF6E8)
                                            else Color(0xFFE2E8F0)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (selectedFriend.locationSharing) "● Online" else "● Location Private",
                                        color = if (selectedFriend.locationSharing) Color(0xFF15803D) else Color(0xFF475569),
                                        fontSize = 11.sp,
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

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 3-Column Balances Row: Owes you | You owe | Splits
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
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Owes you",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(32.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = CurrencyCatalog.formatMinor(
                                        selectedFriend.youOweMinor,
                                        selectedFriend.currency.ifBlank { displayCurrency }
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "You owe",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            VerticalDivider(
                                modifier = Modifier.height(32.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${selectedFriend.splitsCount}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Splits",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Ocean-Blue Button: View Profile
                        Button(
                            onClick = { showProfileSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBluePrimary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("view_profile_button")
                        ) {
                            Text(
                                text = "View Profile",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Overlapping Friend Avatars Row at bottom of card matching Screen 3 mockup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allFriends.take(3).forEachIndexed { idx, friend ->
                                UserAvatarCircle(
                                    name = friend.name,
                                    size = 36.dp,
                                    avatarIndex = idx + 1,
                                    modifier = Modifier
                                        .offset(x = ((-6) * idx).dp)
                                        .clickable { selectedFriendId = friend.id }
                                )
                            }
                            if (allFriends.size > 3) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F0FE))
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${allFriends.size - 3}",
                                        color = OceanBlueDarkText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Searchable List View (Works even when Location Permission is denied!)
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

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp),
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
                                size = 48.dp,
                                avatarIndex = friend.avatarIndex.toInt(),
                                showOnlineDot = friend.isOnline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Age: ${friend.age ?: 25} • ${friend.gender} • ${if (friend.locationSharing) (friend.address ?: "Sharing location") else "Location private"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Owes: ${CurrencyCatalog.formatMinor(friend.owesYouMinor, friend.currency)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "You owe: ${CurrencyCatalog.formatMinor(friend.youOweMinor, friend.currency)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC81E1E)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Friend Detail & Privacy Consent Sheet
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
                                    tint = OceanBluePrimary,
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
                        Text("Explicit Location Pin Sharing", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Location is private by default. Revoke anytime.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = selectedFriend.locationSharing,
                        onCheckedChange = { enabled ->
                            onToggleFriendLocation(selectedFriend, enabled)
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
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBluePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add Friend Dialog (Male / Female gender constraint & explicit location consent)
    if (showAddFriendDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var ageText by remember { mutableStateOf("25") }
        var gender by remember { mutableStateOf("Male") }
        var address by remember { mutableStateOf("Thamel, Kathmandu") }
        var shareLocation by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = { Text("Add Friend", fontWeight = FontWeight.Bold) },
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
                        label = { Text("Shared Place / Address (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = shareLocation,
                            onCheckedChange = { shareLocation = it }
                        )
                        Text(
                            text = "Consent to share pin on Friends Map (private by default)",
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
                                address.trim().ifEmpty { null },
                                shareLocation,
                                displayCurrency,
                                allFriends.size % 5
                            )
                            showAddFriendDialog = false
                        }
                    }
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
