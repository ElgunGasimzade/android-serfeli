package app.serfeli.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serfeli.data.RouteCacheService
import app.serfeli.ui.theme.BackgroundCanvas
import app.serfeli.ui.theme.Primary

@Composable
fun TripSummaryScreen(
    onNavigateToPlans: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Use RouteCacheService for single source of truth
    val routeService = remember { app.serfeli.data.RouteCacheService.getInstance(context) }
    val history by routeService.history.collectAsState()
    val stats by routeService.lifetimeStats.collectAsState()
    
    // Find the most recent completed trip
    val lastCompletedTrip = remember(history) {
        history.filter { it.status == "completed" }.maxByOrNull { it.date.time }
    }
    
    // If we just finished a trip, it should be the top one.
    // If not found (e.g. empty), show generic or loading?
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
    ) {
        if (lastCompletedTrip != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                        .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                        .background(Color.White)
                        .padding(top = 64.dp, bottom = 40.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)), // Green-100
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Celebration,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Text(
                            text = stringResource(app.serfeli.R.string.total_savings),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )
                        
                        Text(
                            text = "${"%.2f".format(lastCompletedTrip.route.totalSavings)} ₼",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 56.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        
                        Text(
                            text = stringResource(app.serfeli.R.string.spent_shopping, lastCompletedTrip.route.estTime.replace(" min", " " + stringResource(app.serfeli.R.string.min)).replace(" mins", " " + stringResource(app.serfeli.R.string.min))), // Using estTime as proxy for now
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.padding(24.dp)) {
                    // Lifetime Stats Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(app.serfeli.R.string.lifetime_earnings),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = Color(0xFFDCFCE7), // Green-100
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(app.serfeli.R.string.all_time),
                                        color = Color(0xFF16A34A),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.padding(vertical = 24.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${"%.2f".format(stats.totalSavings)} ₼",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(app.serfeli.R.string.saved_total),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            
                            // Bar Chart
                            // Mock Data for visual parity
                            val chartData = listOf(0.3, 0.5, 0.4, 0.7, 0.5, 0.8, 0.6, 0.9)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                chartData.forEach { fraction ->
                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .fillMaxHeight()
                                            .background(Color(0xFFE5E7EB), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(fraction.toFloat()) // Cast to Float
                                                .background(Color(0xFF16A34A), RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val months = java.text.DateFormatSymbols.getInstance().shortMonths.take(8)
                                months.forEach { month ->
                                    Text(
                                        text = month,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Impact Metrics
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Deals Scouted (Mock logic: trips * 12)
                        ImpactMetricCard(
                            modifier = Modifier.weight(1f),
                            value = "${stats.totalTrips * 12}",
                            label = stringResource(app.serfeli.R.string.deals_scouted),
                            subLabel = stringResource(app.serfeli.R.string.deals_scouted_sub),
                            icon = Icons.Outlined.Radar,
                            iconColor = Color(0xFF9333EA), // Purple-600
                            iconBg = Color(0xFFFAF5FF) // Purple-50
                        )
                        
                        // Wage (Mock logic: Savings / Time? Or just mock)
                        // iOS uses "Wage" = "Value earned vs time". Let's use totalSavigns / (totalTrips * 0.5 hours)
                        val totalTrips = if (stats.totalTrips > 0) stats.totalTrips else 1
                        val hours = totalTrips * 0.5
                        val wage = if (hours > 0) stats.totalSavings / hours else 0.0
                        
                        ImpactMetricCard(
                            modifier = Modifier.weight(1f),
                            value = "${"%.2f".format(wage)} ₼/hr",
                            label = stringResource(app.serfeli.R.string.your_wage),
                            subLabel = stringResource(app.serfeli.R.string.wage_sub),
                            icon = Icons.Outlined.Timer,
                            iconColor = Color(0xFF2563EB), // Blue-600
                            iconBg = Color(0xFFEFF6FF) // Blue-50
                        )
                    }
                }
            }
        } else {
             // Loading state or empty state
             Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator()
             }
        }
        
        // Footer (Always show button to leave)
        Box(
            modifier = Modifier
                .padding(24.dp)
        ) {
            Button(
                onClick = onNavigateToPlans,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Text(
                    text = stringResource(app.serfeli.R.string.go_to_plans),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ImpactMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    subLabel: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF), // Gray-400
                fontSize = 10.sp,
                lineHeight = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
