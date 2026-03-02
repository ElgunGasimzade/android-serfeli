@file:OptIn(ExperimentalMaterial3Api::class)
package app.serfeli.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import app.serfeli.R
import app.serfeli.data.RetrofitClient
import app.serfeli.model.*
import app.serfeli.ui.components.HeroSection
import app.serfeli.ui.components.ProductCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// import com.google.android.gms.location.LocationServices

// Update SortOption to use resource IDs
enum class SortOption(val id: String, val displayNameRes: Int) {
    DiscountPct("discount_pct", R.string.sort_discount),
    PriceAsc("price_asc", R.string.sort_price_asc),
    PriceDesc("price_desc", R.string.sort_price_desc),
    DiscountVal("discount_val", R.string.sort_savings)
}

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit
) {
    // ... (rest of the code unchanged until UI) ...
    val context = LocalContext.current
    val apiService = remember { RetrofitClient.apiService }
    val scope = rememberCoroutineScope()
    
    // Location Service
    val locationService = app.serfeli.LocalLocationService.current
    val locationState by locationService.locationState.collectAsState()

    // Data State
    var homeFeed by remember { mutableStateOf<HomeFeedResponse?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var availableStores by remember { mutableStateOf<List<Store>>(emptyList()) }
    
    // UI State
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // Sort & Filter State
    var selectedSort by remember { mutableStateOf(SortOption.DiscountPct) }
    var selectedStore by remember { mutableStateOf<String?>(null) }
    
    // Menu States (Dropdowns)
    var showSortMenu by remember { mutableStateOf(false) }
    var showStoreMenu by remember { mutableStateOf(false) }
    
    // Pagination State
    var currentPage by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var canLoadMore by remember { mutableStateOf(true) }
    
    // Scrolls
    val listState = rememberLazyGridState()
    
    // Error State
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Helper to load feed
    fun loadFeed(reload: Boolean = false) {
        if (isLoading && !reload) return
        if (!canLoadMore && !reload) return
        
        scope.launch {
            isLoading = true
            errorMessage = null // Reset error
            try {
                if (reload) {
                    currentPage = 1
                    canLoadMore = true
                }
                
                val response = apiService.getHomeFeed(
                    page = currentPage,
                    limit = 20,
                    sort = selectedSort.id,
                    store = selectedStore,
                    lat = locationState.lat,
                    lon = locationState.lon,
                    range = locationState.range
                )
                
                // 1. EXTRACT STORES (Client-Side Fallback since endpoint is 404)
                // We accumulate unique stores from the loaded products using name only
                val storesFromResponse = response.products.mapNotNull { it.store }.distinct().map { Store(name = it) }
                val currentStoreNames = availableStores.map { it.name }.toSet()
                val newStores = storesFromResponse.filter { !currentStoreNames.contains(it.name) }
                if (newStores.isNotEmpty()) {
                    availableStores = (availableStores + newStores).distinctBy { it.name }
                }

                // 2. FILTERING (Client-side fallback)
                var fetchedProducts = if (selectedStore != null) {
                    response.products.filter { it.store == selectedStore }
                } else {
                    response.products
                }
                
                // 3. SORTING (Client-side fallback/override)
                fetchedProducts = when (selectedSort) {
                    SortOption.PriceAsc -> fetchedProducts.sortedBy { it.price }
                    SortOption.PriceDesc -> fetchedProducts.sortedByDescending { it.price }
                    SortOption.DiscountPct -> fetchedProducts.sortedByDescending { it.discountPercent }
                    SortOption.DiscountVal -> fetchedProducts.sortedByDescending { (it.originalPrice ?: 0.0) - it.price }
                }

                if (reload) {
                    homeFeed = response
                    products = fetchedProducts
                } else {
                    products = products + fetchedProducts
                }
                
                if (response.products.isEmpty()) {
                    canLoadMore = false
                } else {
                    currentPage++
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Unable to connect. Please check your network."
            } finally {
                isLoading = false
            }
        }
    }

    // Scroll Handler
    val scrollHandler = app.serfeli.ui.utils.LocalScrollHandler.current
    LaunchedEffect(Unit) {
        scrollHandler.scrollToTop.collect { tabIndex ->
            if (tabIndex == 0) { // Home Tab
                listState.animateScrollToItem(0)
                loadFeed(reload = true)
            }
        }
    }

    // Initial Load & Location Logic
    LaunchedEffect(Unit) {
        // 1. Start fetching stores (independent of location)
        launch {
            try {
                availableStores = apiService.getAvailableStores(
                    lat = locationState.lat,
                    lon = locationState.lon,
                    range = locationState.range
                )
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        // 2. Initial Load
        loadFeed(reload = true)
    }
    
    // Sort/Filter Change OR Location Change
    LaunchedEffect(selectedSort, selectedStore, locationState) {
        delay(500) // Debounce rapid updates (e.g. driving)
        loadFeed(reload = true)
    }

    // Client-side Search (Robust Fallback)
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            delay(300) 
            try {
                // Try API first
                searchResults = apiService.searchProducts(
                    query = searchQuery,
                    lat = locationState.lat,
                    lon = locationState.lon,
                    range = locationState.range
                ).results
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: Client-side filter of loaded products
                searchResults = products.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) || 
                    (it.store?.contains(searchQuery, ignoreCase = true) == true)
                }
            } finally {
                isSearching = false
            }
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    // Pagination Check
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 4
        }
    }
    
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && canLoadMore && searchQuery.isEmpty()) {
            loadFeed(reload = false)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7) // SystemGray6
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp), // iOS 20pt horizontal padding
                contentPadding = PaddingValues(bottom = 100.dp), // Space for bottom bar
                horizontalArrangement = Arrangement.spacedBy(16.dp), // iOS grid spacing 16pt
                verticalArrangement = Arrangement.spacedBy(16.dp)  // iOS VStack spacing 16-20pt
            ) {
            // Custom Header (Title + Logo)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp), // iOS header padding
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Daily Deals Logo",
                        modifier = Modifier
                            .size(36.dp) // Slightly smaller icon
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.home_title),
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, letterSpacing = (-0.5).sp),
                        fontWeight = FontWeight.Black, // Heavier weight matches iOS "Large Title"
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onNavigate("profile") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile",
                            tint = Color.Gray // Changed from Blue to Gray
                        )
                    }
                }
            }

            // Search Bar
            item(span = { GridItemSpan(maxLineSpan) }) { 
                HomeSearchBar(
                    searchQuery = searchQuery, 
                    onSearchQueryChange = { searchQuery = it }
                ) 
            }
            
            // Sort & Filter Row
            if (!isSearching && (products.isNotEmpty() || isLoading)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // SORT BUTTON (Dropdown Menu)
                        Box {
                            FilterButton(
                                text = if (selectedSort == SortOption.DiscountPct) 
                                    androidx.compose.ui.res.stringResource(R.string.sort) 
                                else androidx.compose.ui.res.stringResource(selectedSort.displayNameRes),
                                icon = when(selectedSort) {
                                    SortOption.PriceAsc -> Icons.Default.ArrowUpward
                                    SortOption.PriceDesc -> Icons.Default.ArrowDownward
                                    else -> Icons.Default.Sort 
                                },
                                isActive = selectedSort != SortOption.DiscountPct,
                                onClick = { showSortMenu = true }
                            )
                            
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier
                                    .background(Color.White)
                                    .width(220.dp) // Set width to match screenshot roughly
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(androidx.compose.ui.res.stringResource(option.displayNameRes)) },
                                        onClick = {
                                            selectedSort = option
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (selectedSort == option) {
                                            { Icon(Icons.Default.Check, null, tint = Color(0xFF007AFF)) }
                                        } else null
                                    )
                                }
                            }
                        }
                        
                        // STORE FILTER BUTTON (Dropdown Menu, Scrollable)
                        Box {
                            FilterButton(
                                text = selectedStore ?: androidx.compose.ui.res.stringResource(R.string.all_stores),
                                icon = Icons.Default.FilterList,
                                isActive = selectedStore != null,
                                onClick = { showStoreMenu = true }
                            )

                            DropdownMenu(
                                expanded = showStoreMenu,
                                onDismissRequest = { showStoreMenu = false },
                                modifier = Modifier
                                    .background(Color.White)
                                    .width(250.dp) 
                                    .heightIn(max = 400.dp) // Limit height, allow scroll
                            ) {
                                // Default "All Stores" option
                                DropdownMenuItem(
                                    text = { Text(androidx.compose.ui.res.stringResource(R.string.all_stores)) },
                                    onClick = {
                                        selectedStore = null
                                        showStoreMenu = false
                                    },
                                    leadingIcon = if (selectedStore == null) {
                                        { Icon(Icons.Default.Check, null, tint = Color(0xFF007AFF)) }
                                    } else null
                                )
                                Divider()
                                
                                // Scrollable list of stores
                                // Compose DropdownMenu implements scroll internally if content exceeds bounds,
                                // but explicit column/scroll can ensure behavior
                                availableStores.forEach { store ->
                                    DropdownMenuItem(
                                        text = { Text(store.name) },
                                        onClick = {
                                            selectedStore = store.name
                                            showStoreMenu = false
                                        },
                                        leadingIcon = if (selectedStore == store.name) {
                                            { Icon(Icons.Default.Check, null, tint = Color(0xFF007AFF)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // SEARCH RESULTS
            if (searchQuery.isNotBlank()) {
                // Search Results Mode
                if (isSearching) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF007AFF))
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                         Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(androidx.compose.ui.res.stringResource(R.string.no_items_found), color = Color.Gray)
                        }
                    }
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                         Text(
                             androidx.compose.ui.res.stringResource(R.string.search_results_title), 
                             modifier = Modifier.padding(vertical = 8.dp),
                             style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Bold
                         ) 
                    }
                    items(items = searchResults, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onClick = {
                                LocalProductStore.selected = product
                                onNavigate("product_detail/${product.id}") 
                            }
                        )
                    }
                }
            } else {
                // NORMAL FEED MODE with DYNAMIC HERO
                
                // Determine Hero vs Grid items dynamically from current 'products' list
                // This ensures hero changes when we sort/filter
                val heroProduct = if (products.isNotEmpty()) products.first() else null
                val gridProducts = if (products.isNotEmpty()) products.drop(1) else emptyList()

                // HERO SECTION
                heroProduct?.let { hero ->
                    item(span = { GridItemSpan(maxLineSpan) }) { 
                        // Wrap product in Hero model
                        HeroSection(
                            hero = Hero(
                                androidx.compose.ui.res.stringResource(R.string.hero_title), 
                                androidx.compose.ui.res.stringResource(R.string.hero_subtitle), 
                                hero
                            ),
                            onClick = {
                                LocalProductStore.selected = hero
                                onNavigate("product_detail/${hero.id}")
                            }
                        ) 
                    }
                }

                if (products.isEmpty() && isLoading) {
                     item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(50.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF007AFF))
                        }
                    }
                } else if (products.isEmpty() && !isLoading) {
                     item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage ?: androidx.compose.ui.res.stringResource(R.string.load_error), 
                                color = Color.Red, 
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = { loadFeed(reload = true) }) {
                                Text(androidx.compose.ui.res.stringResource(R.string.retry))
                            }
                        }
                    }
                } else {
                    // Render Grid items (minus hero)
                    items(
                        items = gridProducts,
                        key = { it.id }
                    ) { product ->
                        ProductCard(
                            product = product,
                            onClick = { 
                                LocalProductStore.selected = product
                                onNavigate("product_detail/${product.id}") 
                            }
                        )
                    }
                }
                
                // Loading Footer
                if (isLoading && products.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                         Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF007AFF))
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun FilterButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp)) // iOS 8pt
            .background(if (isActive) Color(0xFF007AFF).copy(alpha = 0.1f) else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp) // iOS padding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color(0xFF007AFF) else Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = if (isActive) Color(0xFF007AFF) else Color.Black,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun HomeSearchBar(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)) // iOS Search Bar Radius ~10pt
            .background(Color(0xFFE3E3E8)) // Slightly darker gray for search field on light background
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Outlined.Search, 
            null, 
            tint = Color(0xFF8E8E93), 
            modifier = Modifier.size(20.dp)
        )
        
        androidx.compose.foundation.text.BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = Color.Black, 
                fontSize = 17.sp
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (searchQuery.isEmpty()) {
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.search_hint), 
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF8E8E93), 
                            fontSize = 17.sp
                        )
                    )
                }
                innerTextField()
            }
        )
        
        if (searchQuery.isNotEmpty()) {
            Icon(
                Icons.Filled.Close, 
                null, 
                tint = Color(0xFF8E8E93),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onSearchQueryChange("") }
            )
        }
    }
}


