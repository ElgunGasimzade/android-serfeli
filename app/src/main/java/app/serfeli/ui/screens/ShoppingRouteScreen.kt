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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serfeli.data.RouteCacheService
import app.serfeli.model.*
import app.serfeli.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingRouteScreen(
    planId: String, // This is the PLAN ID (firebase/mongo ID), not routeOptionId
    onNavigateBack: () -> Unit,
    onCompleteTrip: () -> Unit
) {
    // Map Sheet State
    var showMapSheet by remember { mutableStateOf(false) }
    var selectedStopForMap by remember { mutableStateOf<RouteStore?>(null) }
    val mapSheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val routeService = remember { RouteCacheService(context) }
    val scope = rememberCoroutineScope()
    
    // State
    // Reactive State from Service
    val history by routeService.history.collectAsState()
    
    // Find Plan reactively
    val currentPlan = history.find { it.id == planId }
    val routeDetails = currentPlan?.route
    
    // Loading state
    var isLoading by remember { mutableStateOf(true) }

    // Initial Load Logic (Only triggers fetch if needed)
    LaunchedEffect(planId) {
        android.util.Log.d("ShoppingRoute", "Looking for plan: $planId")
        if (currentPlan == null) {
            isLoading = true
             routeService.refreshHistory(forceRefresh = true)
             isLoading = false
        } else {
             isLoading = false
        }
    }

    // Savings Calculation (Reactive)
    val totalSavings = routeDetails?.let { details ->
        var sum = 0.0
        details.stops.forEach { stop ->
            stop.items?.forEach { item ->
                if (item.checked) { // Use item.checked directly
                    sum += item.savings
                }
            }
        }
        sum
    } ?: 0.0
    
    val totalItemsCount = routeDetails?.stops?.sumOf { it.items?.size ?: 0 } ?: 0
    val checkedCount = routeDetails?.stops?.sumOf { stop -> 
        stop.items?.count { it.checked } ?: 0 
    } ?: 0

    var hasCapturedInitialState by remember { mutableStateOf(false) }
    var initiallyCheckedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    LaunchedEffect(routeDetails) {
        if (routeDetails != null && !hasCapturedInitialState) {
            initiallyCheckedIds = routeDetails.stops.flatMap { it.items ?: emptyList() }.filter { it.checked }.map { it.id }.toSet()
            hasCapturedInitialState = true
        }
    }
    
    val currentCheckedIds = routeDetails?.stops?.flatMap { stop -> 
        stop.items?.filter { it.checked }?.map { it.id } ?: emptyList()
    }?.toSet() ?: emptySet()
    
    val newlyCheckedIds = currentCheckedIds.subtract(initiallyCheckedIds)

    // Complete Logic
    fun completeTrip() {
        scope.launch {
            val sessionManager = app.serfeli.data.SessionManager(context)
            sessionManager.saveShoppingList(emptyList())
            sessionManager.saveSelectedIds(emptyList())
            sessionManager.saveGenericItems(emptyList())
            
            if (currentCheckedIds.isNotEmpty() || currentPlan?.status == "completed") {
                routeService.completePlan(planId, currentCheckedIds, newlyCheckedIds)
            } else {
                routeService.deletePlan(planId)
            }
            onCompleteTrip()
        }
    }

    Scaffold(
        topBar = { 
            RouteHeader(
                onNavigateBack = onNavigateBack, 
                totalSavings = totalSavings, 
                routeDetails = routeDetails,
                onMarkAll = {
                    val allChecked = checkedCount == totalItemsCount
                    routeDetails?.stops?.flatMap { it.items ?: emptyList() }?.forEach { item ->
                        routeService.updateItemCheckState(planId, item.id, !allChecked)
                    }
                }
            ) 
        },
        bottomBar = { 
            val allItemsAreInitiallyChecked = totalItemsCount > 0 && initiallyCheckedIds.size == totalItemsCount
            if (!allItemsAreInitiallyChecked) {
                RouteFooter(onComplete = { completeTrip() }) 
            }
        },
        containerColor = BackgroundCanvas
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (routeDetails != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Draw Connector Line Logic
                // We can use a Box with a line drawn behind the content
                Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { // Try to measure height? 
                    // Drawing a line across the whole column is tricky without exact coordinates of nodes.
                    // BUT, iOS does it by putting the line in a ZStack behind the VStcak.
                    // Here we can draw a line from top to bottom of this container.
                    if ((routeDetails?.stops?.size ?: 0) > 1) {
                         Box(
                             modifier = Modifier
                                 .fillMaxHeight()
                                 .padding(start = 20.dp) // Approximate center of the left badges (40dp wide)
                                 .width(2.dp)
                                 .background(Color.Gray.copy(alpha = 0.3f))
                         )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                         routeDetails!!.stops.forEach { stop ->
                            StopCard(
                                stop = stop,
                                // Use item.checked directly from data model
                                onItemCheckedChange = { id, checked -> 
                                    routeService.updateItemCheckState(planId, id, checked) 
                                },
                                onNavigateToMap = { 
                                    selectedStopForMap = stop
                                    showMapSheet = true
                                }
                            )
                        }
                    }
                }
            }
            
            if (showMapSheet && selectedStopForMap != null) {
                val stop = selectedStopForMap!!
                app.serfeli.ui.components.MapActionSheet(
                    storeName = stop.store,
                    lat = stop.lat,
                    lon = stop.lon,
                    onDismissRequest = { showMapSheet = false },
                    sheetState = mapSheetState
                )
            }
        } else {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(app.serfeli.R.string.route_not_found), color = Color.Gray)
            }
        }
    }
}

@Composable
fun RouteHeader(
    onNavigateBack: () -> Unit, 
    totalSavings: Double, 
    routeDetails: RouteDetails?,
    onMarkAll: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding()
        ) {
            // Row 1: Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = stringResource(app.serfeli.R.string.back), 
                        tint = Color.Black, 
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Row 2: Title (Left) and Actions (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // Center vertically so title and buttons align
            ) {
                // Title and Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(app.serfeli.R.string.active_trip), 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    val stopCount = routeDetails?.stops?.size ?: 0
                    val estTime = routeDetails?.estTime ?: "--"
                    Text(
                        "${stringResource(app.serfeli.R.string.stops_count, stopCount)} • ${estTime.replace(" min", " " + stringResource(app.serfeli.R.string.min)).replace(" mins", " " + stringResource(app.serfeli.R.string.min))}", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color(0xFF8E8E93) // iOS Gray
                    )
                }

                // Actions (Savings + Mark All)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Savings Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(50)) // Green Light
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "%.2f ₼ %s".format(totalSavings, stringResource(app.serfeli.R.string.savings_badge)), 
                            color = Color(0xFF16A34A),  // Green
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Mark All Button
                    IconButton(
                        onClick = onMarkAll,
                        modifier = Modifier
                            .background(Color(0xFFEFF6FF), CircleShape) // Blue Light
                            .size(32.dp)
                    ) {
                        Icon(Icons.Filled.List, null, tint = Primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StopCard(
    stop: RouteStore,
    // Removed checkedStates map, using Stop data directly if possible or callback
    onItemCheckedChange: (String, Boolean) -> Unit,
    onNavigateToMap: () -> Unit
) {
    val storeColor = try {
        Color(android.graphics.Color.parseColor(stop.color))
    } catch (e: Exception) {
        Primary
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9)) // iOS gray.opacity(0.05)
                    .padding(16.dp), // iOS standard padding
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically // Center vertically
            ) {
                // Left Side: Badge + Store Info
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f) // Take available space, pushing button to right
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFEFF6FF), CircleShape), // Blue Light (matches iOS)
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stop.sequence.toString(),
                            fontWeight = FontWeight.Bold,
                            color = Primary // Blue
                        )
                    }
                    Column {
                        Text(
                            stop.store, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            // maxLines = 1, // Removed to allow wrapping
                            // overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis 
                        )
                        if (stop.store != "Other items" && stop.store != "Digər məhsullar") {
                            Text(
                                stringResource(app.serfeli.R.string.distance_away, stop.distance) + " • " + stringResource(app.serfeli.R.string.items_count, stop.items?.size ?: 0), 
                                style = MaterialTheme.typography.bodySmall, 
                                color = TextSub
                            )
                        } else {
                            Text(
                                stringResource(app.serfeli.R.string.items_count, stop.items?.size ?: 0), 
                                style = MaterialTheme.typography.bodySmall, 
                                color = TextSub
                            )
                        }
                    }
                }
                
                if (stop.store != "Other items" && stop.store != "Digər məhsullar") {
                    Spacer(modifier = Modifier.width(8.dp)) // Spacing between text and button
                    
                    Button(
                         onClick = onNavigateToMap,
                         colors = ButtonDefaults.buttonColors(
                             containerColor = Color(0xFFEFF6FF), // Blue Light
                             contentColor = Primary // Blue
                         ),
                         contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                         shape = RoundedCornerShape(10.dp), // iOS corner radius
                         modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Outlined.Navigation, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(app.serfeli.R.string.navigate), style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Divider(color = Color(0xFFF3F4F6))
            
            // Items
            Column {
                stop.items?.forEach { item ->
                    ShoppingItemRow(
                        item = item,
                        isChecked = item.checked, // Use model property
                        onCheckedChange = { isChecked -> onItemCheckedChange(item.id, isChecked) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: RouteItem,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp), // iOS standard item padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Custom Checkbox
            if (isChecked) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
            } else {
                 Icon(
                    Icons.Default.CropSquare, // Square equivalent
                    null, 
                    tint = Color.Gray, 
                    modifier = Modifier.size(24.dp)
                )
            }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Medium,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                color = if (isChecked) TextSub else MaterialTheme.colorScheme.onSurface,
                maxLines = 3, // Increased from 2 to 3 to show full name
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (item.price > 0.0) {
                Text(
                    "${if (item.aisle == "General") stringResource(app.serfeli.R.string.general) else item.aisle} • ${String.format("%.2f", item.price)} ₼", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = TextSub,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Text(
                    if (item.aisle == "General") stringResource(app.serfeli.R.string.general) else item.aisle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = TextSub,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        
        if (item.savings > 0.0) {
            Text(stringResource(app.serfeli.R.string.save_amount, "%.2f".format(item.savings)) + " ₼", style = MaterialTheme.typography.labelSmall, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RouteFooter(onComplete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color(0xFFF3F4F6))
            .padding(24.dp)
    ) {
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)), // Green
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(app.serfeli.R.string.complete_trip), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}
