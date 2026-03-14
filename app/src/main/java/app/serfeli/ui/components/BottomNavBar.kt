package app.serfeli.ui.components

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serfeli.ui.theme.SystemGray

@Composable
fun BottomNavBar(
    selectedItem: Int,
    onNavigate: (String) -> Unit,
    planCount: Int = 0
) {
    // iOS TabView tab items and routes
    val items = listOf(
        stringResource(app.serfeli.R.string.nav_home), // Home
        stringResource(app.serfeli.R.string.nav_watchlist), // Watchlist
        stringResource(app.serfeli.R.string.nav_shop), // Shop
        stringResource(app.serfeli.R.string.nav_group), // Group
        stringResource(app.serfeli.R.string.nav_plan) // Plan
    )
    val icons = listOf(
        Icons.Outlined.Home,           // iOS: house
        Icons.Outlined.Visibility,     // iOS: eye
        Icons.Outlined.Search,         // iOS: magnifyingglass
        Icons.Outlined.Person,         // iOS: person.3 (Using Person for now, Android has group icons too)
        Icons.Outlined.List            // iOS: list.bullet.clipboard
    )
    val routes = listOf("home", "watchlist", "scan_capture", "family_main", "pfm")

    // Custom "iOS-style" Tab Bar
    Surface(
        color = Color.White,
        shadowElevation = 8.dp, // Subtle shadow for depth
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Top Border (Separator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color(0xFFB2B2B2))
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 0.dp) // Adjust padding for height
                    .height(50.dp), // iOS standard tab bar height is ~49pts
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, label ->
                    val isSelected = selectedItem == index
                    val color = if (isSelected) Color(0xFF007AFF) else SystemGray
                    
                    BottomNavItem(
                        label = label,
                        icon = icons[index],
                        isSelected = isSelected,
                        color = color,
                        planCount = if (index == 4) planCount else 0,
                        onClick = {
                            // Always allow navigation to home (index 0) even if selected
                            // This allows returning from detail screens
                            if (!isSelected || index == 0) {
                                onNavigate(routes[index])
                            }
                        }
                    )
                }
            }
            // Add extra padding for valid touch area / safe area if needed, 
            // usually Scaffold handles system bars, but a little spacing helps visually
            Spacer(modifier = Modifier.height(4.dp)) 
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    planCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Remove ripple for iOS feel
            ) { onClick() }
            .padding(horizontal = 8.dp) // Touch target padding
            .fillMaxHeight()
    ) {
        BadgedBox(
            badge = {
                if (planCount > 0) {
                    Badge(
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = planCount.toString(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
