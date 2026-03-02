package app.serfeli.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serfeli.ui.components.BottomNavBar
import app.serfeli.ui.theme.*
import app.serfeli.ui.utils.LocalScrollHandler
import app.serfeli.data.RetrofitClient
import app.serfeli.data.SessionManager
import app.serfeli.model.WatchlistItem
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WatchlistScreen(
    onNavigate: (String) -> Unit,
    onNavigateToSelection: ((List<app.serfeli.model.BrandGroup>, String, () -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val apiService = remember { RetrofitClient.apiService }
    val sessionManager = remember { SessionManager(context) }
    
    // Location Logic
    val locationService = app.serfeli.LocalLocationService.current
    val locationState by locationService.locationState.collectAsState()

    var watchlistItems by remember { mutableStateOf<List<WatchlistItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isAdding by remember { mutableStateOf(false) } // Feedback state
    var isEditing by remember { mutableStateOf(false) }
    var allBrandGroups by remember { mutableStateOf<List<app.serfeli.model.BrandGroup>>(emptyList()) } // CACHE
    
    // Search State
    var searchQuery by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Scroll Handler (Tab 1)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollHandler = LocalScrollHandler.current
    LaunchedEffect(Unit) {
        scrollHandler.scrollToTop.collect { tabIndex ->
            if (tabIndex == 1) {
                listState.animateScrollToItem(0)
            }
        }
    }

    // Loader Function
    fun loadAllData() {
        scope.launch {
            isLoading = true
            try {
                // 1. Fetch API Watchlist
                val userId = sessionManager.getUserId()
                var apiItems: List<app.serfeli.model.WatchlistItem> = emptyList()
                if (userId != null) {
                    try {
                        val response = apiService.getWatchlist(userId)
                        apiItems = response.items ?: emptyList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // 2. Fetch Local Watchlist (fallback/merge)
                val localNames = sessionManager.getWatchlist()
                val mergedNames = (localNames + apiItems.map { it.name }).distinctBy { it.lowercase() }

                // 3. Scan Flow Simulation
                val tempScanId = UUID.randomUUID().toString()
                val scanItems = mergedNames.map { name ->
                    app.serfeli.model.DetectedItem(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        confidence = 1.0,
                        dealAvailable = true
                    )
                }
                
                val allGroups = if (scanItems.isNotEmpty()) {
                    try {
                        apiService.confirmScan(tempScanId, scanItems)
                        val brandResponse = apiService.getBrands(
                            scanId = tempScanId,
                            lat = locationState.lat,
                            lon = locationState.lon,
                            range = locationState.range
                        )
                        brandResponse.groups ?: emptyList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                
                allBrandGroups = allGroups

                // 4. Enrich Items
                val enrichedItems = mergedNames.map { name ->
                    val matchingGroup = allGroups.find { group -> 
                        group.itemName.equals(name, ignoreCase = true) || 
                        group.itemName.contains(name, ignoreCase = true) || 
                        name.contains(group.itemName, ignoreCase = true)
                    }

                    val (status, badge) = if (matchingGroup != null && matchingGroup.options.isNotEmpty()) {
                        val count = matchingGroup.options.size
                        val maxDiscount = matchingGroup.options.mapNotNull { 
                             it.badge?.replace("-", "")?.replace("%", "")?.toIntOrNull()
                        }.maxOrNull()
                        
                        val badgeText = if ((maxDiscount ?: 0) > 0) "-$maxDiscount%" else null
                        val foundText = context.getString(app.serfeli.R.string.deals_found_suffix)
                        Pair("$count $foundText", badgeText) 
                    } else {
                        Pair(context.getString(app.serfeli.R.string.no_active_deals), null)
                    }

                    val apiItem = apiItems.find { it.name.equals(name, ignoreCase = true) }

                    WatchlistItem(
                        id = apiItem?.id ?: UUID.randomUUID().toString(), // Use DB ID if available
                        name = name,
                        iconType = getIconTypeForName(name),
                        status = status,
                        subtitle = if (status.contains("Found") || status.contains("Tapıldı")) context.getString(app.serfeli.R.string.tap_to_view) else context.getString(app.serfeli.R.string.waiting_deals),
                        badge = badge,
                        isDealFound = status.contains("Found") || status.contains("Tapıldı")
                    )
                }
                
                watchlistItems = enrichedItems

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    // Initial Load
    LaunchedEffect(Unit) {
        loadAllData()
    }
    
    // Reload when location state changes (enabled/disabled or location updates)
    LaunchedEffect(locationState) {
        // Skip the very first render (handled by Unit effect above)
        if (watchlistItems.isNotEmpty() || isLoading) {
            loadAllData()
        }
    }
    
    // Search Suggestions (Like iOS)
    LaunchedEffect(searchQuery.text) {
        if (searchQuery.text.length >= 2) {
            isSearching = true
            kotlinx.coroutines.delay(300) // Debounce
            if (searchQuery.text.length >= 2) { // Check again after delay
                try {
                    suggestions = apiService.searchKeywords(searchQuery.text)
                } catch (e: Exception) {
                    e.printStackTrace()
                    suggestions = emptyList()
                }
            }
            isSearching = false
        } else {
            suggestions = emptyList()
            isSearching = false
        }
    }

    fun handleAdd(name: String) {
        if (name.isBlank()) return
        scope.launch {
            isAdding = true
            sessionManager.addToWatchlist(name)
            
            val userId = sessionManager.getUserId()
            if (userId != null) {
                try {
                    val req = app.serfeli.model.AddToWatchlistRequest(userId = userId, name = name)
                    apiService.addToWatchlist(req)
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Optimistic Update
            val newItem = WatchlistItem(
                id = UUID.randomUUID().toString(),
                name = name,
                iconType = getIconTypeForName(name),
                status = context.getString(app.serfeli.R.string.watching_prices),
                subtitle = context.getString(app.serfeli.R.string.waiting_deals),
                isDealFound = false
            )
            watchlistItems = watchlistItems + newItem
            
            searchQuery = androidx.compose.ui.text.input.TextFieldValue("")
            suggestions = emptyList()
            isAdding = false
            
            // Background Fetch
            loadAllData()
        }
    }
    
    fun handleDelete(item: WatchlistItem) {
        scope.launch {
            // Optimistic local update
            watchlistItems = watchlistItems.filter { it.id != item.id }
            sessionManager.removeFromWatchlist(item.name)
            
            val userId = sessionManager.getUserId()
            if (userId != null) {
                try {
                    apiService.removeFromWatchlist(item.id, userId)
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // We removed locally optimistically, so no need to block UI. Just fetch background.
            loadAllData()
        }
    }

    Scaffold(
        containerColor = BackgroundCanvas,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom 
            ) {
                Text(
                    androidx.compose.ui.res.stringResource(app.serfeli.R.string.watchlist_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                TextButton(
                    onClick = { isEditing = !isEditing },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (isEditing) androidx.compose.ui.res.stringResource(app.serfeli.R.string.done) 
                        else androidx.compose.ui.res.stringResource(app.serfeli.R.string.edit), 
                        color = Color(0xFF059669), fontSize = 17.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Search Bar with Suggestions
                Column(modifier = Modifier.zIndex(2f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, null, tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).height(24.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = { handleAdd(searchQuery.text) }
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.text.isEmpty()) {
                                        Text(androidx.compose.ui.res.stringResource(app.serfeli.R.string.watchlist_add_hint), color = Color.Gray.copy(alpha = 0.6f))
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        if (searchQuery.text.isNotEmpty()) {
                            Button(
                                onClick = { handleAdd(searchQuery.text) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                if (isAdding) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(androidx.compose.ui.res.stringResource(app.serfeli.R.string.add_button), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    // Suggestions Dropdown (Like iOS)
                    AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 10.dp,
                            color = Color.White
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                suggestions.take(5).forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                searchQuery = androidx.compose.ui.text.input.TextFieldValue(
                                                    text = suggestion,
                                                    selection = androidx.compose.ui.text.TextRange(suggestion.length)
                                                )
                                                suggestions = emptyList()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Search, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(suggestion, modifier = Modifier.weight(1f), fontSize = 16.sp)
                                        Icon(Icons.Filled.ArrowForward, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                    }
                                    if (suggestion != suggestions.take(5).last()) {
                                        Divider(modifier = Modifier.padding(start = 44.dp), color = Color.Gray.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading && watchlistItems.isEmpty()) { 
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else if (watchlistItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp), 
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(androidx.compose.ui.res.stringResource(app.serfeli.R.string.watchlist_empty), style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                        Text(androidx.compose.ui.res.stringResource(app.serfeli.R.string.watchlist_empty_subtitle), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(watchlistItems, key = { it.id }) { item ->
                            if (isEditing) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItemPlacement(), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { handleDelete(item) },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.RemoveCircle, 
                                            contentDescription = androidx.compose.ui.res.stringResource(app.serfeli.R.string.delete), 
                                            tint = Color.Red,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Box(modifier = Modifier.weight(1f)) {
                                        WatchlistItemCard(item = item, onClick = {
                                            val matchingGroup = allBrandGroups.find { group -> 
                                                group.itemName.equals(item.name, ignoreCase = true) || 
                                                group.itemName.contains(item.name, ignoreCase = true) || 
                                                item.name.contains(group.itemName, ignoreCase = true)
                                            }
                                            
                                            if (matchingGroup != null) {
                                                onNavigateToSelection?.invoke(listOf(matchingGroup), "Add to Plan", {})
                                            }
                                        })
                                    }
                                }
                            } else {
                                WatchlistItemCard(item = item, onClick = {
                                    val matchingGroup = allBrandGroups.find { group -> 
                                        group.itemName.equals(item.name, ignoreCase = true) || 
                                        group.itemName.contains(item.name, ignoreCase = true) || 
                                        item.name.contains(group.itemName, ignoreCase = true)
                                    }
                                    
                                    if (matchingGroup != null) {
                                        onNavigateToSelection?.invoke(listOf(matchingGroup), "Add to Plan", {})
                                    }
                                })
                            }
                        }
                    }
                }
            }
            
            if (isAdding) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                     CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

private fun getIconTypeForName(name: String): String {
    return when {
         name.contains("milk", ignoreCase = true) -> "MILK"
         name.contains("egg", ignoreCase = true) -> "EGG"
         name.contains("paper", ignoreCase = true) -> "PAPER"
         else -> "CART"
    }
}

@Composable
fun WatchlistItemCard(item: app.serfeli.model.WatchlistItem, onClick: () -> Unit) {
    val isDealFound = item.isDealFound
    val bgColor = if (isDealFound) Color(0xFFD1FAE5) else Color.Gray.copy(alpha = 0.1f) // Green / Gray
    val contentColor = if (isDealFound) Color(0xFF059669) else Color.Gray
    val borderColor = if (isDealFound) Color(0xFFD1FAE5) else Color.Gray.copy(alpha = 0.2f)

    val icon = when (item.iconType) {
        "DRINK", "MILK" -> Icons.Outlined.LocalDrink
        "EGG" -> Icons.Outlined.Egg
        "PAPER", "TOWELS" -> Icons.Outlined.List
        else -> Icons.Outlined.ShoppingCart
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDealFound) {
                        Icon(Icons.Filled.CheckCircle, null, tint = contentColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (item.badge != null) {
                 Text(
                    text = item.badge,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFFBE123C), RoundedCornerShape(100))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            } else if (!isDealFound) {
                Icon(
                     Icons.Filled.Search,
                     null, 
                     tint = Color.Gray.copy(alpha = 0.5f), 
                     modifier = Modifier.size(14.dp)
                )
            }
        }
        
        if (isDealFound) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = 1.dp)
            ) {
                 Box(
                     modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(Color(0xFF059669), RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                        .align(Alignment.CenterStart)
                 )
            }
        }
    }
}
