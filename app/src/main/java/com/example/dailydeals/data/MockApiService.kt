package com.example.dailydeals.data

import com.example.dailydeals.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class MockApiService {
    suspend fun getHomeFeed(
        page: Int = 1,
        limit: Int = 20,
        sortBy: String? = null,
        storeFilter: String? = null
    ): HomeFeedResponse {
        // Simulate network delay
        kotlinx.coroutines.delay(500)
        return MockData.homeFeed
    }
    
    suspend fun getAvailableStores(): List<Store> {
        return MockData.stores.map { 
             Store(name = it.name!!, lat = 0.0, lon = 0.0)
        }
    }
    
    suspend fun searchProducts(query: String): SearchResponse {
        kotlinx.coroutines.delay(300)
         if (query.isBlank()) return SearchResponse(emptyList(), 0)
        val filtered = MockData.products.filter { 
            it.name.contains(query, ignoreCase = true) || 
            (it.brand?.contains(query, ignoreCase = true) == true)
        }
        return SearchResponse(filtered, filtered.size)
    }

    suspend fun getBrands(scanId: String? = null): BrandSelectionResponse {
        return RetrofitClient.apiService.getBrands(scanId)
    }

    suspend fun processScan(base64: String): ScanResponse {
        val requestBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), base64)
        val part = okhttp3.MultipartBody.Part.createFormData("file", "scan.jpg", requestBody)
        return RetrofitClient.apiService.processScan(part)
    }

    suspend fun confirmScanItems(scanId: String, items: List<DetectedItem>): ScanResponse {
        return RetrofitClient.apiService.confirmScan(scanId, items)
    }

    suspend fun optimizePlan(items: List<String>, ids: List<String>? = null): OptimizeResponse {
        return RetrofitClient.apiService.optimizePlan(OptimizeRequest(items, ids))
    }

    suspend fun getRouteOptions(): OptimizeResponse {
        return optimizePlan(emptyList()) 
    }

    suspend fun getRoute(optionId: String): RouteDetails {
        return RetrofitClient.apiService.getRoute(optionId)
    }

    suspend fun completeTrip(request: TripCompletionRequest): TripCompletionResponse {
        return RetrofitClient.apiService.completeTrip(request)
    }

    suspend fun getTripSummary(): TripSummary {
        return RetrofitClient.apiService.getLastTrip()
    }

    suspend fun getWatchlist(userId: String): WatchlistResponse {
        return RetrofitClient.apiService.getWatchlist(userId)
    }

    suspend fun addToWatchlist(request: AddToWatchlistRequest): AddToWatchlistResponse {
        return RetrofitClient.apiService.addToWatchlist(request)
    }

    suspend fun removeFromWatchlist(itemId: String, userId: String): BasicResponse {
        return RetrofitClient.apiService.removeFromWatchlist(itemId, userId)
    }
}


