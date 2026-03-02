package app.serfeli.data

import android.content.Context
import app.serfeli.R

import app.serfeli.model.*
import app.serfeli.model.MockData // Explicit import just in case, though .* should cover it if in same package
// Wait, MockData is in app.serfeli.model package? Yes.
// RouteCacheService is in app.serfeli.data package.
// So import app.serfeli.model.* IS present.
// Let's verify MockData package declaration.
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Date

class RouteCacheService(private val context: Context) {

    private val apiService = RetrofitClient.apiService
    private val sessionManager = SessionManager(context)

    private val _history = MutableStateFlow<List<RouteHistoryItem>>(sessionManager.getHistory())
    val history: StateFlow<List<RouteHistoryItem>> = _history.asStateFlow()

    private val _lifetimeStats = MutableStateFlow(
        UserStats(
            totalTrips = sessionManager.getTotalTrips(),
            totalSavings = sessionManager.getTotalSavings()
        )
    )
    val lifetimeStats: StateFlow<UserStats> = _lifetimeStats.asStateFlow()

    // Removed smart caching - always fetch fresh data



    /**
     * Refresh history from backend API.
     * Always fetches fresh data from backend.
     */
    suspend fun refreshHistory(forceRefresh: Boolean = false) {
        val userId = sessionManager.getUserId() ?: return
        
        withContext(Dispatchers.IO) {
            try {
                val fetchedHistory = apiService.getPlans(userId)
                
                // Augment history with coordinates
                val augmentedHistory = fetchedHistory.map { historyItem ->
                    val augmentedRoute = historyItem.route.copy(
                        stops = historyItem.route.stops.map { stop ->
                            // Corrected package reference: MockData is in model package
                            val knownStore = MockData.stores.find { it.name.equals(stop.store, ignoreCase = true) }
                            
                            // Restore checked state from SessionManager
                            val checkedItems = sessionManager.getCheckedItems(historyItem.id)
                            
                            stop.copy(
                                lat = knownStore?.lat,
                                lon = knownStore?.lon,
                                items = stop.items?.map { item ->
                                    item.copy(checked = checkedItems.contains(item.id))
                                }
                            )
                        }
                    )
                    historyItem.copy(route = augmentedRoute)
                }

                val fetchedStats = apiService.getStats(userId)
                
                // Persist latest stats from backend
                sessionManager.saveTotalSavings(fetchedStats.totalSavings)
                sessionManager.saveTotalTrips(fetchedStats.totalTrips)
                
                withContext(Dispatchers.Main) {
                    _history.value = augmentedHistory
                    _lifetimeStats.value = fetchedStats
                    sessionManager.saveHistory(augmentedHistory.take(3))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveRoute(route: RouteDetails): String? {
        var userId = sessionManager.getUserId() ?: return null
        
        return withContext(Dispatchers.IO) {
            try {
                // Strict: Only save if we have valid backend userId
                val request = CreatePlanRequest(userId = userId, routeDetails = route, status = "active")
                val response = apiService.savePlan(request)
                
                // Success! Save to local history
                saveToLocalHistory(response.id, route)
                return@withContext response.id

            } catch (e: Exception) {
                e.printStackTrace()
                // If backend save fails, we return NULL.
                // We do NOT generate a local fake ID anymore.
                // The UI should show an error to the user.
                null
            }
        }
    }

    private suspend fun saveToLocalHistory(id: String, route: RouteDetails) {
        val newItem = RouteHistoryItem(
            id = id,
            route = route,
            date = java.util.Date(),
            status = "active"
        )
        
        val currentList = _history.value.toMutableList()
        currentList.add(0, newItem)
        
        withContext(Dispatchers.Main) {
            _history.value = currentList
        }
        android.util.Log.d("RouteCacheService", "Saved route: $id")
    }

    /**
     * Add item to active plan OR create new plan with this item.
     * Matches iOS APIService.addItemToActivePlan behavior.
     */
    suspend fun addItemToActivePlan(product: Product): String? {
        val userId = sessionManager.getUserId() ?: return null
        
        return withContext(Dispatchers.IO) {
            try {
                // Fetch latest plans from backend to find active plan
                val plans = apiService.getPlans(userId)
                val activePlan = plans.firstOrNull { it.status == "active" }
                
                if (activePlan != null) {
                    // Add to existing active plan
                    val request = AddItemToPlanRequest(
                        productId = product.id,
                        name = product.name,
                        brand = product.brand,
                        store = product.store,
                        price = product.price,
                        originalPrice = product.originalPrice,
                        imageUrl = product.imageUrl
                    )
                    apiService.addItemToPlan(activePlan.id, request)
                    
                    // Refresh history to get updated plan
                    refreshHistory()
                    
                    activePlan.id
                } else {
                    // Create new plan with this single item
                    val routeItem = RouteItem(
                        id = product.id,
                        name = product.name,
                        aisle = product.brand ?: "",
                        price = product.price,
                        savings = (product.originalPrice ?: product.price) - product.price,
                        checked = false
                    )
                    
                    val knownStore = MockData.stores.find { it.name.equals(product.store, ignoreCase = true) }

                    val store = RouteStore(
                        sequence = 1,
                        store = product.store ?: "Unknown Store",
                        distance = "0 km",
                        color = "#4A90E2",
                        lat = knownStore?.lat,
                        lon = knownStore?.lon,
                        items = listOf(routeItem)
                    )
                    
                    val route = RouteDetails(
                        totalSavings = (product.originalPrice ?: product.price) - product.price,
                        estTime = "5 " + context.getString(R.string.min),
                        stops = listOf(store)
                    )
                    
                    saveRoute(route)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun completePlan(id: String, checkedItems: Set<String>, newlyCheckedIds: Set<String> = checkedItems) {
        val currentList = _history.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index == -1) return

        val activeItem = currentList[index]
        
        // Filter route to only checked items -> NO, keep all items, just update checked state
        val newStops = activeItem.route.stops.map { stop ->
            stop.copy(items = stop.items?.map { it.copy(checked = checkedItems.contains(it.id)) })
        }

        // Stats savings
        val newStatsSavings = newStops.flatMap { it.items ?: emptyList() }.filter { newlyCheckedIds.contains(it.id) }.sumOf { it.savings }
        // Total Route savings
        val totalRouteSavings = newStops.flatMap { it.items ?: emptyList() }.filter { it.checked }.sumOf { it.savings }
        
        val finalRoute = activeItem.route.copy(
            stops = newStops,
            totalSavings = totalRouteSavings
        )

        withContext(Dispatchers.IO) {
            try {
                 val request = CompletePlanRequest(routeDetails = finalRoute)
                 apiService.completePlan(activeItem.id, request)
                 
                 if (newlyCheckedIds.isNotEmpty()) {
                     val statsReq = TripCompletionRequest(totalSavings = newStatsSavings, timeSpent = finalRoute.estTime, dealsScouted = newlyCheckedIds.size)
                     apiService.completeTrip(statsReq)
                 }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Update local
        val updatedItem = activeItem.copy(status = "completed", route = finalRoute)
        currentList[index] = updatedItem
        
        // ACCUMULATE SAVINGS
        val currentSavings = sessionManager.getTotalSavings()
        val formattedSavings = (currentSavings + newStatsSavings)
        sessionManager.saveTotalSavings(formattedSavings)

        withContext(Dispatchers.Main) {
            _history.value = currentList
            recalculateStats() // Update stats immediately
        }
    }

    suspend fun deletePlan(id: String) {
        val currentList = _history.value.toMutableList()
        currentList.removeAll { it.id == id }
        withContext(Dispatchers.Main) {
            _history.value = currentList
            recalculateStats() // Update stats immediately
        }
        
        withContext(Dispatchers.IO) {
            try {
                apiService.deletePlan(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun getLastActiveRoute(): RouteDetails? {
        return _history.value.firstOrNull { it.status == "active" }?.route
    }

    private fun recalculateStats() {
        // Backend or History-based Stats
        val currentHistory = _history.value
        val completedTrips = currentHistory.filter { it.status == "completed" }
        val totalTrips = completedTrips.size
        
        // PERSISTENT SAVINGS LOGIC
        // We take the MAX of (SessionManager, Calculated from History) to ensure it never drops on delete
        // But also respects backend if backend has MORE info (e.g. other devices)
        
        val calculatedSavings = completedTrips.sumOf { it.route.totalSavings }
        val storedSavings = sessionManager.getTotalSavings()
        
        val finalSavings = maxOf(calculatedSavings, storedSavings)
        
        // Update Session in case Calculated was higher (sync from server)
        if (calculatedSavings > storedSavings) {
            sessionManager.saveTotalSavings(calculatedSavings)
        }
        
        // Update Trips Persistence
        sessionManager.saveTotalTrips(totalTrips)
        
        _lifetimeStats.value = UserStats(
            totalTrips = totalTrips,
            totalSavings = finalSavings
        )
    }

    fun updateItemCheckState(planId: String, itemId: String, isChecked: Boolean) {
        val currentList = _history.value.toMutableList()
        val planIndex = currentList.indexOfFirst { it.id == planId }
        
        if (planIndex != -1) {
            val plan = currentList[planIndex]
            val newRoute = plan.route.copy(
                stops = plan.route.stops.map { stop ->
                    stop.copy(
                        items = stop.items?.map { item ->
                            if (item.id == itemId) {
                                item.copy(checked = isChecked)
                            } else {
                                item
                            }
                        }
                    )
                }
            )
            
            // Updates Memory
            currentList[planIndex] = plan.copy(route = newRoute)
            _history.value = currentList
            
            // Update Persistence
            val currentChecked = sessionManager.getCheckedItems(planId).toMutableSet()
            if (isChecked) {
                currentChecked.add(itemId)
            } else {
                currentChecked.remove(itemId)
            }
            sessionManager.saveCheckedItems(planId, currentChecked)
        }
    }
}
