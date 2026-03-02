package app.serfeli

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.serfeli.ui.components.BottomNavBar
import app.serfeli.ui.screens.HomeScreen
import app.serfeli.ui.screens.ProfileScreen
import app.serfeli.ui.screens.ShoppingRouteScreen
import app.serfeli.ui.screens.TripSummaryScreen
import app.serfeli.ui.screens.BrandSelectionScreen
import app.serfeli.ui.screens.ScanCaptureScreen
import app.serfeli.ui.screens.ShoppingPlanScreen
import app.serfeli.ui.screens.WatchlistScreen
import app.serfeli.ui.screens.PFMScreen
import app.serfeli.ui.screens.PlanHistoryScreen
import app.serfeli.ui.theme.DailyDealsTheme
import app.serfeli.ui.utils.ScrollHandler
import app.serfeli.ui.utils.LocalScrollHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import android.content.Context
import app.serfeli.ui.utils.LocaleHelper

object AppSession {
    var isFirstLaunch = true
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (AppSession.isFirstLaunch) {
            AppSession.isFirstLaunch = false
            val sessionManager = app.serfeli.data.SessionManager(this)
            sessionManager.saveShoppingList(emptyList())
            sessionManager.saveSelectedIds(emptyList())
            sessionManager.saveGenericItems(emptyList())
        }
        
        setContent {
            DailyDealsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    DailyDealsApp()
                }
            }
        }
    }
}


val LocalLocationService = androidx.compose.runtime.staticCompositionLocalOf<app.serfeli.data.LocationService> { 
    error("No LocationService provided") 
}

@Composable
fun DailyDealsApp() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { app.serfeli.data.SessionManager(context) }
    val locationService = remember { app.serfeli.data.LocationService(context, sessionManager) }
    val scrollHandler = remember { ScrollHandler() }
    
    // Cleanup on dispose
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            locationService.cleanup()
        }
    }
    
    // Check permissions on resume/start
    androidx.compose.runtime.LaunchedEffect(Unit) {
        locationService.checkForPermissions()
    }
    
    // Location Permission Launcher
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                      permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
        // Update LocationService or SessionManager if needed, though they check on resume usually.
        // We mainly want to force the prompt.
        if (granted) {
             locationService.checkForPermissions()
        }
    }

    // App Launch Initialization
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!sessionManager.isLoggedIn()) {
             // Force create guest session if not logged in
             sessionManager.createGuestSession()
        }
        
        // Request Permissions on Launch if not granted
        val fineLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineLocation != android.content.pm.PackageManager.PERMISSION_GRANTED && 
            coarseLocation != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Determine which tab is selected based on current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // User requested Bottom Bar on ALL screens
    val showBottomBar = true 
    
    // helper for navigation items
    val onNavigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // State for preloaded data (Watchlist -> BrandSelection)
    var preloadedBrandGroups by remember { androidx.compose.runtime.mutableStateOf<List<app.serfeli.model.BrandGroup>?>(null) }
    var brandSelectionActionText by remember { androidx.compose.runtime.mutableStateOf("Start Shopping") }

    // RouteCache for "Add to Plan" logic
    val routeCacheService = remember { app.serfeli.data.RouteCacheService(context) }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(
        LocalLocationService provides locationService,
        LocalScrollHandler provides scrollHandler
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    // Determine selected index
                    val selectedIndex = when {
                        currentRoute == "home" -> 0
                        currentRoute == "watchlist" -> 1
                        currentRoute == "scan_capture" -> 2
                        currentRoute?.startsWith("brand_selection") == true -> {
                             // Context-aware selection: Watchlist vs Shop
                             if (brandSelectionActionText == "Add to Plan") 1 else 2
                        }
                        currentRoute == "shopping_plan" -> 2 // Shop flow
                        
                        currentRoute == "family_main" || currentRoute?.startsWith("family_lists") == true || currentRoute?.startsWith("family_shopping_list") == true || currentRoute?.startsWith("brand_selection_family") == true || currentRoute?.startsWith("add_items_family_list") == true -> 3 // Group Flow
                        
                        currentRoute == "pfm" -> 4
                        currentRoute?.startsWith("shopping_route") == true -> 4
                        currentRoute == "plan_history" -> 4 
                        
                        else -> 0 
                    }
                    
                    // Show NavBar
                    BottomNavBar(
                        selectedItem = selectedIndex,
                        onNavigate = { route ->
                            if (route == "pfm" && currentRoute == "pfm") {
                                 // Scroll to top or refresh?
                            } else {
                                onNavigateToTab(route) 
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    onNavigate = { route ->
                        if (route.startsWith("product_detail")) {
                            navController.navigate(route)
                        } else {
                            onNavigateToTab(route)
                        }
                    }
                )
            }
            
            composable("watchlist") {
                WatchlistScreen(
                    onNavigate = { route -> onNavigateToTab(route) },
                    onNavigateToSelection = { groups, actionText, _ ->
                        preloadedBrandGroups = groups
                        brandSelectionActionText = actionText
                        navController.navigate("brand_selection")
                    }
                )
            }
            
            composable("scan_capture") {
                ScanCaptureScreen(
                    onNavigateToDeals = {
                         // Clear any preloaded data from Watchlist so we load fresh scan results
                         preloadedBrandGroups = null
                         brandSelectionActionText = "Start Shopping"
                         navController.navigate("brand_selection")
                    },
                    onNavigate = { route -> onNavigateToTab(route) }
                )
            }
            
            composable("pfm") {
                PFMScreen(
                    onNavigateToRoute = { routeId ->
                         navController.navigate("shopping_route/$routeId")
                    },
                    onNavigateToHistory = {
                        navController.navigate("plan_history")
                    },
                    onNavigate = { route -> onNavigateToTab(route) }
                )
            }
            
            composable("profile") {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigate = { route -> onNavigateToTab(route) }
                )
            }

            composable("family_main") {
                app.serfeli.ui.screens.FamilyMainScreen(
                    onNavigateToLists = { familyId, familyName ->
                        navController.navigate("family_lists/$familyId/$familyName")
                    },
                    sessionManager = sessionManager
                )
            }

            composable(
                route = "family_lists/{familyId}/{familyName}",
                arguments = listOf(
                    navArgument("familyId") { type = NavType.StringType },
                    navArgument("familyName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val familyId = backStackEntry.arguments?.getString("familyId") ?: ""
                val familyName = backStackEntry.arguments?.getString("familyName") ?: ""
                app.serfeli.ui.screens.FamilyListsScreen(
                    familyId = familyId,
                    familyName = familyName,
                    sessionManager = sessionManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToList = { fId, listId ->
                        // This will navigate to FamilyShoppingListScreen which we still need to create
                        navController.navigate("family_shopping_list/$fId/$listId/$familyName")
                    }
                )
            }
            
            composable(
                route = "family_shopping_list/{familyId}/{listId}/{familyName}",
                arguments = listOf(
                    navArgument("familyId") { type = NavType.StringType },
                    navArgument("listId") { type = NavType.IntType },
                    navArgument("familyName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val familyId = backStackEntry.arguments?.getString("familyId") ?: ""
                val listId = backStackEntry.arguments?.getInt("listId") ?: -1
                val familyName = backStackEntry.arguments?.getString("familyName") ?: ""
                app.serfeli.ui.screens.FamilyShoppingListScreen(
                    familyId = familyId,
                    listId = listId,
                    familyName = familyName,
                    sessionManager = sessionManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddItems = { fId, lId ->
                        navController.navigate("add_items_family/$fId/$lId")
                    },
                    onNavigateToShoppingPlan = {
                        onNavigateToTab("shopping_plan")
                    }
                )
            }
            
            composable(
                route = "add_items_family/{familyId}/{listId}",
                arguments = listOf(
                    navArgument("familyId") { type = NavType.StringType },
                    navArgument("listId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val familyId = backStackEntry.arguments?.getString("familyId") ?: ""
                val listId = backStackEntry.arguments?.getInt("listId") ?: -1
                
                app.serfeli.ui.screens.AddItemsToFamilyListScreen(
                    familyId = familyId,
                    listId = listId,
                    sessionManager = sessionManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBrandSelection = {
                        navController.navigate("brand_selection_family/$familyId/$listId")
                    }
                )
            }
            
            composable(
                route = "brand_selection_family/{familyId}/{listId}",
                arguments = listOf(
                    navArgument("familyId") { type = NavType.StringType },
                    navArgument("listId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val familyId = backStackEntry.arguments?.getString("familyId") ?: ""
                val listId = backStackEntry.arguments?.getInt("listId") ?: -1
                val scope = rememberCoroutineScope()
                var isSubmitting by remember { mutableStateOf(false) }
                val apiService = app.serfeli.data.RetrofitClient.apiService
                
                app.serfeli.ui.screens.BrandSelectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    actionButtonText = stringResource(R.string.add_to_list),
                    onContinue = { items, names ->
                        if (isSubmitting) return@BrandSelectionScreen
                        scope.launch {
                            isSubmitting = true
                            try {
                                val userId = sessionManager.getUserId() ?: ""
                                
                                // Submit found deals
                                for ((groupName, item) in items) {
                                    apiService.addToShoppingList(
                                        app.serfeli.model.AddFamilyShoppingItemRequest(
                                            familyId = familyId,
                                            userId = userId,
                                            itemName = groupName,
                                            brandName = item.brandName,
                                            storeName = item.dealText,
                                            listId = listId,
                                            price = item.price,
                                            originalPrice = item.originalPrice,
                                            productId = item.id
                                        )
                                    )
                                }
                                
                                // Submit un-found / skipped generic items
                                for (name in names) {
                                    apiService.addToShoppingList(
                                        app.serfeli.model.AddFamilyShoppingItemRequest(
                                            familyId = familyId,
                                            userId = userId,
                                            itemName = name,
                                            listId = listId
                                        )
                                    )
                                }
                                
                                // Navigate back twice to return to FamilyShoppingListScreen
                                navController.popBackStack("family_shopping_list", inclusive = false)
                                // In compose nav, you can just popBackStack twice if that's the stack:
                                // To be safe, popping passing the route of family_shopping_list would have to be exact,
                                // but we mapped route dynamically. A safer way is popBackStack() twice.
                                navController.popBackStack()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                )
            }

            composable("brand_selection") {
                BrandSelectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onContinue = { items, names -> 
                        if (brandSelectionActionText == "Add to Plan") {
                             // "Add to Plan" Logic: Append to active plan
                             scope.launch {
                                 // 1. Add Brand Items
                                 items.forEach { (groupName, brandItem) ->
                                     val storeName = brandItem.dealText.removePrefix("at ").trim()
                                     val product = app.serfeli.model.Product(
                                         id = brandItem.id ?: java.util.UUID.randomUUID().toString(),
                                         name = brandItem.brandName,
                                         brand = brandItem.brandName,
                                         store = storeName,
                                         price = brandItem.price ?: 0.0,
                                         originalPrice = brandItem.originalPrice,
                                         imageUrl = brandItem.logoUrl,
                                         category = "Groceries", // Default
                                         inStock = true,
                                         discountPercent = null // Calculated by backend or service
                                     )
                                     routeCacheService.addItemToActivePlan(product)
                                 }
                                 
                                 // 2. Add Generic Items
                                 names.forEach { name ->
                                     val product = app.serfeli.model.Product(
                                         id = java.util.UUID.randomUUID().toString(),
                                         name = name,
                                         brand = "Generic",
                                         store = null,
                                         price = 0.0,
                                         originalPrice = null,
                                         imageUrl = "",
                                         category = "Groceries",
                                         inStock = true
                                     )
                                     routeCacheService.addItemToActivePlan(product)
                                 }
                                 
                                 // Return to Watchlist
                                 navController.popBackStack()
                             }
                        } else {
                             // "Start Shopping" Logic: Overwrite/Save Session for new Plan
                             val ids = items.mapNotNull { it.second.id }
                             sessionManager.saveSelectedIds(ids)
                             sessionManager.saveGenericItems(names) // Generics
                             
                             navController.navigate("shopping_plan")
                        }
                    },
                    preloadedGroups = preloadedBrandGroups,
                    actionButtonText = brandSelectionActionText
                )
            }
            
            composable("shopping_plan") {
                ShoppingPlanScreen(
                    onPlanSelected = { routeIdOrToken ->
                        if (routeIdOrToken == "SWITCH_TO_PFM") {
                            navController.navigate("pfm") {
                                popUpTo("home") { inclusive = false }
                            }
                        } else {
                            navController.navigate("pfm") {
                                popUpTo("home") { inclusive = false }
                            }
                            navController.navigate("shopping_route/$routeIdOrToken")
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
             composable(
                route = "shopping_route/{routeId}",
                arguments = listOf(navArgument("routeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
                ShoppingRouteScreen(
                    planId = routeId,
                    onNavigateBack = { navController.popBackStack() },
                    onCompleteTrip = {
                        navController.navigate("pfm") {
                             popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }
            
            composable("plan_history") {
                PlanHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { routeId ->
                         navController.navigate("shopping_route/$routeId")
                    }
                )
            }
            
            composable(
                route = "product_detail/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                app.serfeli.ui.screens.ProductDetailScreen(
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
    }
}
