package app.serfeli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serfeli.ui.theme.Primary
import app.serfeli.R
import app.serfeli.ui.theme.BackgroundCanvas
import app.serfeli.ui.theme.Dimens
import app.serfeli.data.RouteCacheService
import kotlinx.coroutines.launch

@Composable
fun ShoppingPlanScreen(
    onPlanSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val apiService = remember { app.serfeli.data.RetrofitClient.apiService }
    val sessionManager = remember { app.serfeli.data.SessionManager(context) }
    var routeOptions by remember { mutableStateOf<List<app.serfeli.model.RouteOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val routeService = remember { app.serfeli.data.RouteCacheService(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Retrieve the user's finalized shopping list from Brand Selection
        val savedItems = sessionManager.getGenericItems()
        val savedIds = sessionManager.getSelectedIds()
        try {
            val response = apiService.optimizePlan(app.serfeli.model.OptimizeRequest(savedItems, savedIds))
            routeOptions = response.options
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCanvas)
            .verticalScroll(rememberScrollState())
            .padding(Dimens.PaddingLarge)
    ) {
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
        
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
             IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(32.dp).background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(Dimens.PaddingSmall)) // Space between back button and text
            Column {
                Text(
                    text = stringResource(app.serfeli.R.string.we_found_ways, routeOptions.size),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(app.serfeli.R.string.choose_option),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.PaddingSmall)
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.PaddingLarge)) // Space after header block

        if (isLoading) {
             Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = Primary)
             }
        } else if (routeOptions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(app.serfeli.R.string.no_routes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            
            routeOptions.forEachIndexed { index, option ->
                val isMaxSavings = option.type.equals("max_savings", ignoreCase = true) || option.type.equals("MAX_SAVINGS", ignoreCase = true)
                
                OptionCard(
                    title = if (isMaxSavings) stringResource(app.serfeli.R.string.option_a) else stringResource(app.serfeli.R.string.option_b),
                    savings = "${"%.2f".format(option.totalSavings)} ₼",
                    headerIcon = if (isMaxSavings) Icons.Outlined.Savings else Icons.Outlined.AccessTime,
                    headerIconColor = if (isMaxSavings) Color(0xFF34C759) else Color(0xFF007AFF),
                    headerBgColor = if (isMaxSavings) Color(0xFF34C759).copy(alpha = 0.1f) else Color(0xFF007AFF).copy(alpha = 0.1f),
                    badge = if (isMaxSavings) stringResource(app.serfeli.R.string.max_savings).uppercase() else null,
                    badgeColor = if (isMaxSavings) Color(0xFF34C759) else null,
                    borderColor = if (isMaxSavings) Color(0xFF34C759) else Color(0xFFE5E7EB),
                    description = run {
                        val desc = option.description ?: ""
                        when {
                            desc == "Save more by visiting multiple stores." -> stringResource(app.serfeli.R.string.save_more_desc)
                            desc == "No single store has these items." -> stringResource(app.serfeli.R.string.no_single_store)
                            desc.startsWith("Get everything at ") -> {
                                val store = desc.removePrefix("Get everything at ").removeSuffix(".")
                                "${stringResource(app.serfeli.R.string.get_everything_prefix)} $store"
                            }
                            else -> desc
                        }
                    },
                    content = {
                        if (option.stops.size > 1) {
                            RouteVisual(
                                stops = option.stops.map { RouteStop(it.store, it.summary) }
                            )
                        } else {
                            val stop = option.stops.firstOrNull()
                             SingleStopVisual(
                                name = stop?.store ?: stringResource(app.serfeli.R.string.store_default),
                                detail = stop?.summary ?: stringResource(app.serfeli.R.string.details_unavailable)
                            )
                        }
                    },
                    distance = option.totalDistance,
                    buttonText = stringResource(app.serfeli.R.string.select_route),
                    onSelect = { 
                        scope.launch {
                            try {
                                isLoading = true
                                var details: app.serfeli.model.RouteDetails? = null
                                try {
                                    details = apiService.getRoute(option.id)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                                
                                // Fallback logic construction
                                if (details == null) {
                                     val fallbackStops = option.stops.mapIndexed { index, stopSummary ->
                                        app.serfeli.model.RouteStore(
                                            sequence = index + 1,
                                            store = stopSummary.store,
                                            distance = context.getString(app.serfeli.R.string.unknown),
                                            color = "#007AFF",
                                            items = emptyList()
                                        )
                                    }
                                    details = app.serfeli.model.RouteDetails(
                                        totalSavings = option.totalSavings,
                                        estTime = context.getString(app.serfeli.R.string.unknown),
                                        stops = fallbackStops
                                    )
                                }


                                val planId = routeService.saveRoute(details!!)
                                if (planId != null) {
                                    // Navigate to specific route while resetting stack (handled in MainActivity)
                                    onPlanSelected(planId)
                                    isLoading = false
                                } else {
                                    isLoading = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isLoading = false
                            }
                        }
                    },
                    isPrimary = isMaxSavings
                )
                
                Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
            }
        }
    }
    
    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}




                


@Composable
fun OptionCard(
    title: String,
    savings: String,
    headerIcon: ImageVector,
    headerIconColor: Color,
    headerBgColor: Color,
    badge: String? = null,
    badgeColor: Color? = null,
    borderColor: Color = Color(0xFFE5E7EB),
    description: String? = null,
    content: @Composable () -> Unit,
    distance: String,
    buttonText: String,
    onSelect: () -> Unit,
    isPrimary: Boolean
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp) // Space for shadow
            .clickable { onSelect() }, // Make whole card clickable
        shape = RoundedCornerShape(Dimens.CornerRadiusExtraLarge),
        color = Color.White,
        shadowElevation = 6.dp, // iOS shadow radius 10, y 5 ~ 6dp android
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box {
            Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.save_money_format, savings), 
                        // Actually line 80 set `savings` var. Line 152 uses it. `savings` var has "AZN X.XX". So "Save AZN X.XX".
                        // Wait, line 80 `savings = ...`. 
                        // The snippet `text = "Save $savings"` uses string interpolation of the `savings` variable.
                        // If `savings` is "AZN 10.00", then text becomes "Save AZN 10.00".
                        // So I don't need to change the $ there if it's the variable reference, BUT...
                        // If the code was `text = "Save $${savings}"` then I'd change it. 
                        // The grep said: `text = "Save $savings",`. This looks like kotlin variable interpolation `$savings`.
                        // So checking context: `val savings = ...` (Line 80).
                        // So I should leave line 152 UNLESS it has a literal $.
                        // Grep said: `text = "Save $savings",`. This is just using the variable.
                        // Let's check `TripSummary` instead which usually has literal $.
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(headerBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        headerIcon,
                        contentDescription = null,
                        tint = headerIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Content (Route or Single Stop)
            content()

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .drawBehind {
                        drawLine(
                            color = Color(0xFFF3F4F6),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = distance,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPrimary) Color(0xFF34C759) else Color.White,
                        contentColor = if (isPrimary) Color.White else MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (!isPrimary) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)) else null,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        if (badge != null && badgeColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 12.dp))
                    .background(badgeColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
        }
    }
}

data class RouteStop(val name: String, val detail: String)

@Composable
fun RouteVisual(stops: List<RouteStop>) {
    Column(
        modifier = Modifier
            .padding(start = 10.dp) // Space for the line
            .drawBehind {
                // Solid line matching iOS (pathEffect removed)
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(0f, 10.dp.toPx()),
                    end = Offset(0f, size.height - 10.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
    ) {
        stops.forEachIndexed { index, stop ->
            Row(
                modifier = Modifier.padding(bottom = if (index < stops.size - 1) 24.dp else 0.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Dot matching iOS (Black fill, White stroke)
                Box(
                    modifier = Modifier
                        .offset(x = (-10).dp) // Align center of dot with line
                        .size(12.dp) // w-3 h-3
                        .background(Color.Black, CircleShape) // Changed to Color.Black
                        .border(2.dp, Color.White, CircleShape) // Changed border to 2.dp
                )
                
                Column(modifier = Modifier.padding(start = 8.dp).offset(y = (-4).dp)) {
                    Text(
                        text = stop.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stop.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SingleStopVisual(name: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB)) // Gray-50
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Storefront,
                contentDescription = null,
                tint = Color(0xFF9CA3AF), // Gray-400
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
