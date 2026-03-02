package app.serfeli.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDecoration
import coil.compose.AsyncImage
import app.serfeli.data.RetrofitClient
import app.serfeli.data.SessionManager
import app.serfeli.model.BrandGroup
import app.serfeli.model.BrandItem
import app.serfeli.model.BrandSelectionResponse
import app.serfeli.ui.theme.*
import app.serfeli.R

@Composable
fun BrandSelectionScreen(
    onNavigateBack: () -> Unit,
    onContinue: (List<Pair<String, BrandItem>>, List<String>) -> Unit, // Pass data back with Group Item Name
    preloadedGroups: List<BrandGroup>? = null,
    actionButtonText: String = stringResource(app.serfeli.R.string.start_shopping)
) {
    val context = LocalContext.current
    val apiService = remember { RetrofitClient.apiService }
    val sessionManager = remember { SessionManager(context) } // Keep for lastScanId
    
    var brandResponse by remember { mutableStateOf<BrandSelectionResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Store selected IDs (Source of Truth)
    val selectedItems = remember { mutableStateMapOf<String, String>() }
    val customNames = remember { mutableStateMapOf<String, String>() }
    var selectedStrategy by remember { mutableStateOf("savings") }

    // Helpers for total savings
    val totalSavings = remember(brandResponse, selectedItems.toMap()) {
        var total = 0.0
        brandResponse?.groups?.let { groups ->
            groups.forEach { group ->
            val selectedId = selectedItems[group.itemName]
            if (selectedId != null) {
                // Find option
                val option = group.options.find { it.id == selectedId }
                if (option != null) {
                     // Calculate savings: Original - Price
                     val original = option.originalPrice ?: 0.0
                     val price = option.price ?: 0.0
                     if (original > price) {
                         total += (original - price)
                     }
                }
            }
            }
        }
        total
    }
    
    val selectedCount = selectedItems.values.count { it != "SKIPPED" }

    // Location Logic handled by LocationService
    val locationService = app.serfeli.LocalLocationService.current
    val locationState by locationService.locationState.collectAsState()

    var retryTrigger by remember { mutableStateOf(0) }

    // Load Brands when location changes (or initially)
    LaunchedEffect(locationState, retryTrigger, preloadedGroups) {
        try {
            isLoading = true
            
            // Priority: Use Preloaded Groups if available
            if (preloadedGroups != null) {
                brandResponse = BrandSelectionResponse(groups = preloadedGroups)
            } else {
                val lastScanId = sessionManager.getLastScanId()
            
                // Pass location to API
                val response = apiService.getBrands(
                    scanId = lastScanId,
                    lat = locationState.lat,
                    lon = locationState.lon,
                    range = locationState.range
                )
                
                // Only update state if active
                brandResponse = response
            }
            
            // Initial Auto-Select (Default to first option or generic)
            brandResponse?.groups?.forEach { group ->
                if (group.status == "DEAL_FOUND" && group.options.isNotEmpty()) {
                    // Try to pick one that is "selected" by default or just the first one
                    val defaultOption = group.options.find { it.isSelected } ?: group.options.first()
                    if (selectedItems[group.itemName] == null) {
                         selectedItems[group.itemName] = defaultOption.id ?: group.itemName
                    }
                } else {
                    // Generic fallback
                    // We DO NOT set selectedItems here; we leave it as null so it falls into the "Unfound/Generic" parsing bucket.
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // Propagate cancellation
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Auto Select Logic
    fun performAutoSelect(strategy: String) {
        brandResponse?.groups?.forEach { group ->
            if (group.status == "DEAL_FOUND" && group.options.isNotEmpty()) {
                var best: BrandItem? = null
                when (strategy) {
                    "cheapest" -> best = group.options.minByOrNull { it.price ?: Double.MAX_VALUE }
                    "savings" -> best = group.options.maxByOrNull { it.savings }
                    "closest" -> best = group.options.minByOrNull { it.distance ?: Double.MAX_VALUE }
                }
                
                if (best != null) {
                    selectedItems[group.itemName] = best.id ?: best.brandName
                } else {
                     val defaultOpt = group.options.firstOrNull()
                     if (defaultOpt != null) {
                        selectedItems[group.itemName] = defaultOpt.id ?: defaultOpt.brandName
                     }
                }
            }
        }
    }

    Scaffold(
        bottomBar = { 
            BrandSelectionFooter(
                count = selectedCount + (brandResponse?.groups?.count { 
                    val sid = selectedItems[it.itemName]
                    val custom = customNames[it.itemName]
                    val isNotFound = it.options.isEmpty() || it.status != "DEAL_FOUND"
                    val hasComment = !custom.isNullOrBlank()
                    
                    if (sid == null || sid == "SKIPPED") {
                        if (sid == "SKIPPED") hasComment
                        else {
                             if (isNotFound) true // Always counted if not found and not explicitly skipped
                             else hasComment // If found but ignored, only count if commented
                        }
                    } else false
                } ?: 0),
                savings = totalSavings,
                isEnabled = (selectedCount + (brandResponse?.groups?.count { 
                    val sid = selectedItems[it.itemName]
                    val custom = customNames[it.itemName]
                    val isNotFound = it.options.isEmpty() || it.status != "DEAL_FOUND"
                    val hasComment = !custom.isNullOrBlank()
                    
                    if (sid == null || sid == "SKIPPED") {
                        if (sid == "SKIPPED") hasComment
                        else {
                             if (isNotFound) true // Always counted if not found and not explicitly skipped
                             else hasComment // If found but ignored, only count if commented
                        }
                    } else false
                } ?: 0)) > 0,
                actionText = actionButtonText,
                onContinue = {
                    val finalItems = mutableListOf<Pair<String, BrandItem>>()
                    val finalNames = mutableListOf<String>()
                    
                    for (group in brandResponse?.groups ?: emptyList()) {
                        val sid = selectedItems[group.itemName]
                        val custom = customNames[group.itemName]
                        
                        val isNotFound = group.options.isEmpty() || group.status != "DEAL_FOUND"
                        val hasComment = !custom.isNullOrBlank()
                        
                        if (sid == null || sid == "SKIPPED") {
                            if (sid == "SKIPPED") {
                                // user explicitly skipped. Only add if they wrote a generic comment
                                if (hasComment) {
                                    finalNames.add("${group.itemName} ($custom)")
                                }
                            } else {
                                // Not explicitly skipped (sid == null)
                                if (isNotFound) {
                                    if (hasComment) finalNames.add("${group.itemName} ($custom)")
                                    else finalNames.add(group.itemName)
                                } else {
                                    // Deal found, but not selected and not skipped explicitly.
                                    // Default to not adding it, unless they typed a comment.
                                    if (hasComment) {
                                         finalNames.add("${group.itemName} ($custom)")
                                    }
                                }
                            }
                        } else {
                            val deal = group.options.find { it.id == sid || it.brandName == sid }
                            if (deal != null) finalItems.add(Pair(group.itemName, deal))
                        }
                    }
                    
                    onContinue(finalItems, finalNames)
                }
            ) 
        },
        containerColor = BackgroundCanvas
    ) { paddingValues ->
        if (isLoading) {
             Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = Primary)
             }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp) // iOS standard padding 16
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         // Back Button
                         IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp).background(Color(0xFFF9FAFB), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = TextMain, modifier = Modifier.size(20.dp))
                        }
                        
                         Spacer(modifier = Modifier.width(16.dp))
                         
                         Text(stringResource(app.serfeli.R.string.review_items), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextMain)
                         
                         Spacer(modifier = Modifier.weight(1f))
                         
                         Box(
                            modifier = Modifier
                                .background(Color.Green.copy(alpha = 0.1f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            // Using SystemGreen for premium look
                            Text("$selectedCount/${brandResponse?.groups?.size ?: 0}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = app.serfeli.ui.theme.SystemGreen)
                        }
                    }
                }

                item {
                    Text(
                        stringResource(app.serfeli.R.string.review_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSub
                    )
                }
                
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            AutoSelectButton(
                                label = stringResource(app.serfeli.R.string.cheapest), 
                                icon = Icons.Default.Sell, 
                                color = app.serfeli.ui.theme.SystemGreen, // Premium Emerald
                                isSelected = selectedStrategy == "cheapest",
                                onClick = { 
                                    selectedStrategy = "cheapest"
                                    performAutoSelect("cheapest") 
                                }
                            )
                        }
                        item {
                            AutoSelectButton(
                                label = stringResource(app.serfeli.R.string.max_savings), 
                                icon = Icons.Default.Verified, 
                                color = app.serfeli.ui.theme.Primary, // Premium Blue
                                isSelected = selectedStrategy == "savings",
                                onClick = { 
                                    selectedStrategy = "savings"
                                    performAutoSelect("savings") 
                                }
                            )
                        }
                        item {
                            AutoSelectButton(
                                label = stringResource(app.serfeli.R.string.closest), 
                                icon = Icons.Default.LocationOn, 
                                color = Color(0xFFEA580C), // Orange
                                isSelected = selectedStrategy == "closest",
                                onClick = { 
                                    selectedStrategy = "closest"
                                    performAutoSelect("closest") 
                                }
                            )
                        }
                    }
                }
                
                brandResponse?.let { response ->
                    if (response.groups.isNullOrEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(stringResource(app.serfeli.R.string.nothing_found), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(app.serfeli.R.string.no_deals_available), style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)

                            }
                        }
                    } else {
                        response.groups?.forEach { group ->
                            item {
                                if (group.status == "DEAL_FOUND") {
                                    DealFoundCard(
                                        group = group,
                                        selectedId = selectedItems[group.itemName],
                                        customName = customNames[group.itemName] ?: "",
                                        onCustomNameChange = { newName -> 
                                            customNames[group.itemName] = newName 
                                            if (newName.isNotEmpty()) selectedItems[group.itemName] = "SKIPPED"
                                        },
                                        onSelect = { idOrName -> 
                                            selectedItems[group.itemName] = idOrName 
                                            customNames[group.itemName] = ""
                                        },
                                        onSkip = { 
                                            selectedItems[group.itemName] = "SKIPPED"
                                            customNames[group.itemName] = ""
                                        }
                                    )
                                } else {
                                    NoDealFoundCard(
                                        group = group,
                                        customName = customNames[group.itemName] ?: "",
                                        onCustomNameChange = { newName -> customNames[group.itemName] = newName },
                                        isSkipped = selectedItems[group.itemName] == "SKIPPED",
                                        onSkip = {
                                            if (selectedItems[group.itemName] == "SKIPPED") selectedItems.remove(group.itemName) 
                                            else selectedItems[group.itemName] = "SKIPPED"
                                            customNames[group.itemName] = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutoSelectButton(
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    color: Color, 
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(icon, null, tint = if (isSelected) Color.White else color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label, 
            color = if (isSelected) Color.White else color, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Bold
        )
    }
}



@Composable
fun BrandSelectionFooter(
    count: Int, 
    savings: Double, 
    isEnabled: Boolean, 
    actionText: String = stringResource(app.serfeli.R.string.start_shopping),
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color(0xFFF3F4F6))
            .padding(Dimens.PaddingLarge)
    ) {
        Button(
            onClick = onContinue,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) app.serfeli.ui.theme.SystemGreen else Color.Gray,
                disabledContainerColor = Color.Gray
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(actionText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (count > 0) {
                    Text(
                        stringResource(app.serfeli.R.string.items_ready_save, count, String.format("%.2f", savings)), 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Normal, 
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun DealFoundCard(
    group: BrandGroup,
    selectedId: String?,
    customName: String,
    onCustomNameChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onSkip: () -> Unit
) {
    // Parity with iOS: Header image, gradient, checks
    val brandOptions = group.options
    var isExpanded by remember { mutableStateOf(false) }
    
    // Logic: Is currently selected ID present in options?
    val isAnyOptionSelected = brandOptions.any { it.id == selectedId }
    val selectionIndex = if (isAnyOptionSelected) brandOptions.indexOfFirst { it.id == selectedId } else 0
    val displayImage = brandOptions.getOrNull(selectionIndex)?.logoUrl ?: brandOptions.firstOrNull()?.logoUrl ?: ""

    // Sorting for display: Selected first, then others
    val orderedOptions = remember(brandOptions, selectedId) {
        val selected = brandOptions.find { it.id == selectedId }
        val others = brandOptions.filter { it.id != selectedId }
        if (selected != null) listOf(selected) + others else brandOptions
    }

    val optionsToShow = if (isExpanded) orderedOptions else orderedOptions.take(1)

    Card(
        shape = RoundedCornerShape(20.dp), // iOS 20
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header Image with Overlay
            Box(modifier = Modifier.height(140.dp).fillMaxWidth()) {
                AsyncImage(
                    model = displayImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                )
                
                // Bottom Text "Best Value Found" (Simulated logic or just static if deal found)
                val activeOption = brandOptions.find { it.id == selectedId }
                if (activeOption != null && activeOption.savings > 0) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp)) // iOS Blue
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(app.serfeli.R.string.best_value_found), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (selectedId == "SKIPPED") { // Skipped
                     Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(app.serfeli.R.string.skipped), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, null, tint = Color(0xFF2563EB))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(group.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val itemDetailsText = if (group.itemDetails.equals("No deals found", ignoreCase = true)) {
                            stringResource(app.serfeli.R.string.no_deals_found_lowercase)
                        } else {
                            group.itemDetails
                        }
                        Text(itemDetailsText, style = MaterialTheme.typography.bodySmall, color = TextSub)
                    }
                }

                // Options List
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    optionsToShow.forEach { brand ->
                        BrandOptionRow(
                            item = brand,
                            isSelected = brand.id == selectedId || (brand.id == null && brand.brandName == selectedId),
                            onClick = { onSelect(brand.id ?: brand.brandName) }
                        )
                    }
                    
                    // Show More / Less
                    if (brandOptions.size > 1) {
                         Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF)) // Blue opacity 0.05
                                .clickable { isExpanded = !isExpanded }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isExpanded) stringResource(app.serfeli.R.string.show_less) else stringResource(app.serfeli.R.string.see_more_options, brandOptions.size - 1),
                                    color = Primary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    null,
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Custom Name Input
                    if (optionsToShow.size > 1 || selectedId == null || selectedId == "SKIPPED") {
                        OutlinedTextField(
                            value = if (customName == "SKIPPED") "" else customName,
                            onValueChange = onCustomNameChange,
                            placeholder = { Text(stringResource(R.string.or_enter_your_own_brand)) },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Skip Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSkip() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                         Text(
                             stringResource(R.string.skip_this_item_generic), 
                             color = Color.Gray, 
                             style = MaterialTheme.typography.bodyMedium
                         )
                    }
                }
            }
        }
    }
}

@Composable
fun BrandOptionRow(item: BrandItem, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Primary else Color(0xFFE5E7EB)
    val bgColor = if (isSelected) Primary.copy(alpha = 0.05f) else Color.White
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.logoUrl,
            contentDescription = null,
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(2.dp),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(item.brandName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(item.dealText, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            if (item.distance != null) {
                Text(
                    "${String.format("%.1f", item.distance)} km • ${item.estTime ?: ""}", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
             if (isSelected) {
                Icon(Icons.Filled.Check, null, tint = Primary, modifier = Modifier.size(24.dp))
            } else {
                Box(modifier = Modifier.size(24.dp).border(2.dp, Color(0xFFE5E7EB), CircleShape))
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (item.badge != null) {
                Text(
                    item.badge.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = app.serfeli.ui.theme.SystemGreen,
                    modifier = Modifier
                        .background(app.serfeli.ui.theme.SystemGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            
            Text("${String.format("%.2f", item.price ?: 0.0)} ₼", fontWeight = FontWeight.Bold)
            
            if (item.savings > 0) {
                Box(modifier = Modifier.background(app.serfeli.ui.theme.SystemGreen, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text("${stringResource(R.string.save_caps)} ${String.format("%.2f", item.savings)} ₼", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NoDealFoundCard(
    group: BrandGroup,
    customName: String,
    onCustomNameChange: (String) -> Unit,
    isSkipped: Boolean = false,
    onSkip: () -> Unit = {}
) {
     val borderColor = if (isSkipped) Color.Red.copy(alpha = 0.5f) else BorderSubtle
     val bgColor = if (isSkipped) Color.Red.copy(alpha = 0.05f) else Color.White

     Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
             Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFFFFF7ED), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Egg, null, tint = Color(0xFFEA580C))
                }
                 Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(group.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                    val itemDetailsText = if (group.itemDetails.equals("No deals found", ignoreCase = true)) {
                        stringResource(app.serfeli.R.string.no_deals_found_lowercase)
                    } else {
                        group.itemDetails
                    }
                    Text(itemDetailsText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.background(Color(0xFFF3F4F6), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(stringResource(app.serfeli.R.string.no_deals), color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
             
             // Simple info text
             Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(12.dp)) {
                 Icon(Icons.Outlined.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                 Spacer(modifier = Modifier.width(8.dp))
                 Text(stringResource(R.string.no_discounts_nearby), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
             }
             
             // Custom Brand Input
             OutlinedTextField(
                 value = if (isSkipped && customName.isBlank()) "" else customName,
                 onValueChange = { 
                     onCustomNameChange(it)
                     if (isSkipped && it.isNotBlank()) {
                         onSkip() // Unskip if they start typing
                     }
                 },
                 placeholder = { Text(stringResource(R.string.enter_brand_manually)) },
                 modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                 singleLine = true,
                 shape = RoundedCornerShape(8.dp)
             )
             
             Spacer(modifier = Modifier.height(12.dp))
             
             // Skip Button
             Button(
                 onClick = onSkip,
                 colors = ButtonDefaults.buttonColors(containerColor = if (isSkipped) Color.Red else Color.Red.copy(alpha = 0.1f)),
                 shape = RoundedCornerShape(12.dp),
                 modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                 contentPadding = PaddingValues(vertical = 12.dp)
             ) {
                 Text(
                     text = if (isSkipped) "Item Skipped - Tap to Undo" else stringResource(R.string.skip_this_item_generic), 
                     color = if (isSkipped) Color.White else Color.Red, 
                     fontWeight = FontWeight.Bold
                 )
             }
        }
    }
}
