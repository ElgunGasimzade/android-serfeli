package com.example.dailydeals.data

import com.example.dailydeals.model.*

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

interface ApiService {


    @GET("api/v1/home/feed")
    suspend fun getHomeFeed(
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("limit") limit: Int = 20,
        @retrofit2.http.Query("sort") sort: String? = null,
        @retrofit2.http.Query("store") store: String? = null,
        @retrofit2.http.Query("lat") lat: Double? = null,
        @retrofit2.http.Query("lon") lon: Double? = null,
        @retrofit2.http.Query("range") range: Double? = null
    ): HomeFeedResponse

    @GET("api/v1/search")
    suspend fun searchProducts(
        @retrofit2.http.Query("q") query: String,
        @retrofit2.http.Query("lat") lat: Double? = null,
        @retrofit2.http.Query("lon") lon: Double? = null,
        @retrofit2.http.Query("range") range: Double? = null
    ): SearchResponse

    @GET("api/v1/keywords/search")
    suspend fun searchKeywords(
        @retrofit2.http.Query("q") query: String
    ): List<String>

    @GET("api/v1/stores")
    suspend fun getAvailableStores(
        @retrofit2.http.Query("lat") lat: Double? = null,
        @retrofit2.http.Query("lon") lon: Double? = null,
        @retrofit2.http.Query("range") range: Double? = null
    ): List<Store>

    @Multipart
    @POST("api/v1/scan/process")
    suspend fun processScan(@Part file: okhttp3.MultipartBody.Part?): ScanResponse

    @POST("api/v1/scan/{scanId}/confirm")
    suspend fun confirmScan(@Path("scanId") scanId: String, @Body items: List<DetectedItem>): ScanResponse

    @GET("api/v1/deals/brands")
    suspend fun getBrands(
        @retrofit2.http.Query("scanId") scanId: String? = null,
        @retrofit2.http.Query("lat") lat: Double? = null,
        @retrofit2.http.Query("lon") lon: Double? = null,
        @retrofit2.http.Query("range") range: Double? = null
    ): BrandSelectionResponse

    @POST("api/v1/planning/optimize")
    suspend fun optimizePlan(@Body request: OptimizeRequest): OptimizeResponse

    @GET("api/v1/planning/route/{optionId}")
    suspend fun getRoute(@Path("optionId") optionId: String): RouteDetails

    @POST("api/v1/trips")
    suspend fun completeTrip(@Body request: TripCompletionRequest): TripCompletionResponse

    @GET("api/v1/trips/last")
    suspend fun getLastTrip(): TripSummary

    @GET("api/v1/watchlist")
    suspend fun getWatchlist(@retrofit2.http.Query("userId") userId: String): WatchlistResponse

    @POST("api/v1/watchlist")
    suspend fun addToWatchlist(@Body request: AddToWatchlistRequest): AddToWatchlistResponse

    @DELETE("api/v1/watchlist/{itemId}")
    suspend fun removeFromWatchlist(
        @Path("itemId") itemId: String,
        @retrofit2.http.Query("userId") userId: String
    ): BasicResponse

    // PFM / Planning Endpoints (Renamed to 'plans' to match iOS)
    @GET("api/v1/plans/{userId}")
    suspend fun getPlans(@Path("userId") userId: String): List<RouteHistoryItem>

    @GET("api/v1/plans/{userId}/stats")
    suspend fun getStats(@Path("userId") userId: String): UserStats

    @POST("api/v1/plans")
    suspend fun savePlan(@Body request: CreatePlanRequest): SavePlanResponse

    @POST("api/v1/plans/{planId}/items")
    suspend fun addItemToPlan(@Path("planId") planId: String, @Body request: AddItemToPlanRequest)

    @PUT("api/v1/plans/{planId}/complete")
    suspend fun completePlan(@Path("planId") planId: String, @Body request: CompletePlanRequest): TripCompletionResponse

    @DELETE("api/v1/plans/{planId}")
    suspend fun deletePlan(@Path("planId") planId: String)

    // Auth
    @POST("api/v1/auth/device-login")
    suspend fun deviceLogin(@Body request: DeviceLoginRequest): AuthResponse

    @GET("api/v1/auth/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): UserProfileResponse

    @PUT("api/v1/auth/profile")
    suspend fun updateUserProfile(@Body request: UpdateProfileRequest): AuthResponse

    // --- Family Management ---
    @POST("api/v1/family/create")
    suspend fun createFamily(@Body request: CreateFamilyRequest): FamilyResponse

    @POST("api/v1/family/join")
    suspend fun joinFamily(@Body request: JoinFamilyRequest): FamilyResponse

    @GET("api/v1/family/list")
    suspend fun listFamilies(@retrofit2.http.Query("userId") userId: String): FamilyListResponse

    @GET("api/v1/family/my-family")
    suspend fun getMyFamily(@retrofit2.http.Query("userId") userId: String): FamilyResponse

    @POST("api/v1/family/leave")
    suspend fun leaveFamily(@Body request: Map<String, String>): BasicResponse

    // --- Family Shopping List ---
    @POST("api/v1/family/shopping-lists/create")
    suspend fun createShoppingList(@Body request: CreateShoppingListRequest): BasicResponse

    @GET("api/v1/family/shopping-lists")
    suspend fun getShoppingLists(@retrofit2.http.Query("familyId") familyId: String): ShoppingListsResponse

    @DELETE("api/v1/family/shopping-lists/{listId}")
    suspend fun deleteShoppingList(@Path("listId") listId: Int): BasicResponse

    @GET("api/v1/family/shopping-list")
    suspend fun getShoppingList(
        @retrofit2.http.Query("familyId") familyId: String,
        @retrofit2.http.Query("listId") listId: Int? = null
    ): FamilyShoppingListResponse

    @POST("api/v1/family/shopping-list/add")
    suspend fun addToShoppingList(@Body request: AddFamilyShoppingItemRequest): FamilyShoppingItem

    @PUT("api/v1/family/shopping-list/{itemId}")
    suspend fun updateShoppingListItem(
        @Path("itemId") itemId: String,
        @Body request: UpdateFamilyItemRequest
    ): BasicResponse

    @DELETE("api/v1/family/shopping-list/{itemId}")
    suspend fun deleteShoppingListItem(
        @Path("itemId") itemId: String,
        @retrofit2.http.Query("userId") userId: String
    ): BasicResponse
}

data class SearchResponse(
    val results: List<Product>,
    val count: Int
)

data class SavePlanResponse(
    val id: String
)
