@file:OptIn(ExperimentalMaterial3Api::class)
package app.serfeli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import app.serfeli.ui.theme.Primary
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import app.serfeli.data.RouteCacheService
import app.serfeli.model.RouteHistoryItem
import app.serfeli.ui.theme.BackgroundCanvas
import app.serfeli.ui.components.BottomNavBar
import app.serfeli.ui.utils.LocalScrollHandler
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PFMScreen(
    onNavigateToRoute: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val routeService = remember { RouteCacheService.getInstance(context) }
    val history by routeService.history.collectAsState()
    val stats by routeService.lifetimeStats.collectAsState()

    val scrollState = rememberScrollState()
    val scrollHandler = app.serfeli.ui.utils.LocalScrollHandler.current
    
    // Refresh on appear (but give a moment for saves to complete)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500) // Wait 500ms for saves to complete
        routeService.refreshHistory()
    }
    
    // Listen for scroll to top (Tab 3)
    LaunchedEffect(Unit) {
        scrollHandler.scrollToTop.collect { tabIndex ->
            if (tabIndex == 3) {
                scrollState.animateScrollTo(0)
            }
        }
    }

    val featuredActivePlan = history.firstOrNull { it.status == "active" }
    android.util.Log.d("PFMScreen", "History size: ${history.size}, Active plan: ${featuredActivePlan?.id}")
    val recentHistory = history.take(3)

    Scaffold(
        containerColor = BackgroundCanvas,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(BackgroundCanvas)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.statusBars) // Handle status bar overlap if needed
                    .padding(horizontal = 16.dp, vertical = 12.dp) // iOS standard navbar padding
            ) {
                Text(
                    text = stringResource(app.serfeli.R.string.my_plans_title),
                    style = MaterialTheme.typography.headlineMedium, // Closer to iOS .title / .largeTitle
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp // Explicitly matching iOS Large Title default
                )
            }

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp), // Reduce top padding further
                verticalArrangement = Arrangement.spacedBy(10.dp) // Further reduced spacing (16 -> 10) for tighter layout
            ) {
                // 1. Active Plan
                featuredActivePlan?.let { item ->
                    ActivePlanCard(item = item, onContinue = {
                        onNavigateToRoute(item.id) 
                    })
                }

                // 2. Compact Stats
                StatsSummaryCard(
                    totalSaved = stats.totalSavings,
                    totalTrips = stats.totalTrips
                )

                // 3. Recent History
                if (recentHistory.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { // Reduced spacing (was 16.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(app.serfeli.R.string.recent_history_title),
                                style = MaterialTheme.typography.titleMedium, // Headline
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToHistory) {
                                Text(stringResource(app.serfeli.R.string.view_all), fontSize = 15.sp)
                            }
                        }

                        recentHistory.forEach { item ->
                            HistoryCard(item = item, onClick = {
                                if (item.status == "active") {
                                    onNavigateToRoute(item.id)
                                } else {
                                    onNavigateToRoute(item.id) 
                                }
                            })
                        }
                    }
                }
                
                // Bottom Padding for Nav Bar awareness
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ActivePlanCard(item: RouteHistoryItem, onContinue: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(20.dp), 
                spotColor = Color.Black.copy(alpha = 0.06f),
                ambientColor = Color.Transparent
            ) 
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gradient Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color.Blue, Color(0xFFA020F0)) // Blue -> Purple
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        stringResource(app.serfeli.R.string.current_focus),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(app.serfeli.R.string.active_shopping_plan),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${stringResource(app.serfeli.R.string.stores_count, item.route.stops.size)} • ${item.route.estTime.replace(" min", " " + stringResource(app.serfeli.R.string.min)).replace(" mins", " " + stringResource(app.serfeli.R.string.min))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Savings Badge
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)) // Green opacity 0.1
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        stringResource(app.serfeli.R.string.save_amount, "%.2f".format(item.route.totalSavings)) + " ₼",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853) // Green
                    )
                    Text(
                        stringResource(app.serfeli.R.string.estimated_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.1f))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary, // Solid Primary Blue
                    contentColor = Color.White // White Text
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(app.serfeli.R.string.continue_shopping), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
fun StatsSummaryCard(totalSaved: Double, totalTrips: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp, 
                shape = RoundedCornerShape(16.dp), 
                spotColor = Color.Black.copy(alpha = 0.05f),
                ambientColor = Color.Transparent
            )
    ) {
        Column {
            // Top: Savings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp) // Increased padding
            ) {
                Text(
                    text = "%.2f ₼".format(totalSaved),
                    fontSize = 28.sp, // Match iOS large title size inside card
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00C853)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(app.serfeli.R.string.total_saved_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Divider(color = Color.Gray.copy(alpha = 0.1f))
            
            // Bottom: Trips & Scouted
            Row(modifier = Modifier.padding(vertical = 12.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$totalTrips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(app.serfeli.R.string.trips_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp) // Taller divider
                        .background(Color.Gray.copy(alpha = 0.2f)) // Softer color
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${totalTrips * 12}", // Mock approx
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA020F0) // Purple
                    )
                    Text(
                        text = stringResource(app.serfeli.R.string.deals_scouted_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@Composable
fun HistoryCard(item: RouteHistoryItem, onClick: () -> Unit) {
    val dateStr = formatDate(item.date)
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp), // iOS corner radius
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // iOS uses shadow, not elevation. But 2dp is okay. Let's try 0 and add shadow border or custom shadow if needed. iOS shadow is very soft.
        // Actually, let's stick to small elevation but standard radius.
        // modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha=0.05f), spotColor = Color.Black.copy(alpha=0.05f)),
        // Compose shadow is tricky. Let's just use elevation 2.dp for now but higher radius.
        modifier = Modifier.fillMaxWidth().shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), clip = false),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93) // iOS Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                
                if (item.status == "completed") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle, // Filled circle
                            contentDescription = null,
                            tint = Color(0xFF16A34A), // iOS Green
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(app.serfeli.R.string.status_completed),
                            color = Color(0xFF16A34A),
                            style = MaterialTheme.typography.labelSmall, // Caption bold
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (item.status == "active") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart, // Or simple Cart
                            contentDescription = null,
                            tint = Primary, // Blue
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(app.serfeli.R.string.status_in_progress),
                            color = Primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = item.status.replaceFirstChar { it.uppercase() },
                        color = Color(0xFF8E8E93),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = stringResource(app.serfeli.R.string.stores_count, item.route.stops.size),
                        style = MaterialTheme.typography.titleMedium, // Headline
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.route.estTime.replace(" min", " " + stringResource(app.serfeli.R.string.min)).replace(" mins", " " + stringResource(app.serfeli.R.string.min)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(app.serfeli.R.string.saved_amount, item.route.totalSavings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A) // iOS Green
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Button (iOS parity)
            // Active: Blue Text, Blue BG opacity 0.1
            // Completed: Blue Text, Gray BG opacity 0.1? No, iOS code says:
            // .background(item.status == "active" ? Color.blue.opacity(0.1) : Color.gray.opacity(0.1))
            // .foregroundColor(item.status == "active" ? .blue : .blue) (Wait, actually looks like .blue for both usually, but let's check screenshot. Screenshot shows View Summary is Blue text on Grayish bg. Yes.)
            val btnTextColor = Primary
            val btnBgColor = if (item.status == "active") Primary.copy(alpha = 0.1f) else Color(0xFFF2F2F7) // SystemGray6 for completed
            val btnText = if (item.status == "active") stringResource(app.serfeli.R.string.continue_shopping) else stringResource(app.serfeli.R.string.view_summary)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(btnBgColor, RoundedCornerShape(10.dp)) // iOS radius 8-10
                    .padding(vertical = 10.dp), // iOS vertical 8
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = btnText,
                    color = btnTextColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
