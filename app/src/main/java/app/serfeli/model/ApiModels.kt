package app.serfeli.model

data class GuestLoginRequest(val deviceId: String)
data class AuthResponse(
    val token: String?,
    val user: AuthUser,
    val isNewUser: Boolean
)

data class AuthUser(
    val id: String,
    val deviceId: String,
    val email: String?,
    val phone: String?,
    val username: String?
)

data class HomeFeedResponse(
    val hero: Hero,
    val categories: List<Category>,
    val products: List<Product>
)
data class Hero(
    val title: String,
    val subtitle: String,
    val product: Product
)
data class Category(
    val id: String,
    val name: String,
    val selected: Boolean? = false
)
data class Product(
    val id: String,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val store: String? = null,
    val imageUrl: String,
    val price: Double,
    val originalPrice: Double? = null,
    val discountPercent: Int? = null,
    val badge: String? = null,
    val inStock: Boolean = true
)

data class ScanResponse(
    val scanId: String,
    val detectedItems: List<DetectedItem>
)
data class DetectedItem(
    val id: String,
    val name: String,
    val confidence: Double,
    val boundingBox: BoundingBox? = null,
    val dealAvailable: Boolean,
    val imageUrl: String? = null
)
data class BoundingBox(
    val x: Double, val y: Double, val w: Double, val h: Double
)

data class BrandSelectionResponse(
    val groups: List<BrandGroup>? = null
)
data class BrandGroup(
    val itemName: String,
    val itemDetails: String,
    val status: String,
    val options: List<BrandItem>
)
data class BrandItem(
    val id: String? = null, // Added ID
    val brandName: String,
    val logoUrl: String,
    val dealText: String,
    val savings: Double,
    val isSelected: Boolean,
    val price: Double? = null,
    val originalPrice: Double? = null,
    val badge: String? = null,
    val distance: Double? = null,
    val estTime: String? = null
)

data class OptimizeRequest(
    val items: List<String>,
    val ids: List<String>? = null // Added IDs
)
data class OptimizeResponse(
    val options: List<RouteOption>
)
data class RouteOption(
    val id: String,
    val type: String,
    val title: String,
    val totalSavings: Double,
    val totalDistance: String,
    val description: String? = null,
    val stops: List<RouteStopSummary>
)
data class RouteStopSummary(
    val store: String,
    val summary: String
)

data class RouteDetails(
    val totalSavings: Double,
    val estTime: String,
    val stops: List<RouteStore>
)
data class RouteStore(
    val sequence: Int,
    val store: String,
    val distance: String,
    val color: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val items: List<RouteItem>? = null
)
data class RouteItem(
    val id: String,
    val name: String,
    val aisle: String,
    val price: Double,
    val savings: Double,
    val checked: Boolean
)

data class TripCompletionRequest(
    val totalSavings: Double,
    val timeSpent: String,
    val dealsScouted: Int
)

data class TripCompletionResponse(
    val success: Boolean,
    val tripId: String
)

data class TripSummary(
    val totalSavings: Double,
    val timeSpent: String,
    val lifetimeEarnings: Double,
    val chartData: List<Double>,
    val dealsScouted: Int,
    val wagePerHour: Double,
    val tripId: String? = null // Added for PFM deduplication
)

data class WatchlistResponse(
    val items: List<WatchlistItem>,
    val popularEssentials: List<String>
)
data class WatchlistItem(
    val id: String,
    val name: String,
    val status: String,
    val subtitle: String,
    val badge: String? = null,
    val iconType: String,
    val isDealFound: Boolean = false
)


data class Store(
    val name: String,
    val lat: Double? = null,
    val lon: Double? = null
)

data class RouteHistoryItem(
    val id: String,
    var route: RouteDetails,
    val date: java.util.Date,
    var status: String // "active", "completed"
)

data class UserStats(
    val totalTrips: Int,
    val totalSavings: Double
)

data class DeviceLoginRequest(val deviceId: String)

data class CreatePlanRequest(
    val userId: String,
    val routeDetails: RouteDetails,
    val status: String
)

data class AddItemToPlanRequest(
    val productId: String,
    val name: String,
    val brand: String?,
    val store: String?,
    val price: Double,
    val originalPrice: Double?,
    val imageUrl: String?
)

data class CompletePlanRequest(
    val routeDetails: RouteDetails?
)

// MARK: - Family Models

data class Family(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdAt: String
)

data class FamilyMember(
    val id: String,
    val username: String,
    val email: String?,
    val role: String,
    val joinedAt: String
)

data class FamilyResponse(
    val family: Family?,
    val role: String?,
    val members: List<FamilyMember>?
)

data class ItemUser(
    val id: String,
    val username: String
)

data class FamilyShoppingItem(
    val id: String,
    val itemName: String,
    val quantity: Int,
    val status: String,
    val notes: String?,
    val brandName: String?,
    val storeName: String?,
    val listId: Int?,
    val price: Double?,
    val originalPrice: Double?,
    val productId: String?,
    val addedBy: ItemUser,
    val purchasedBy: ItemUser?,
    val purchasedAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class FamilyShoppingListResponse(
    val items: List<FamilyShoppingItem>
)

data class ShoppingList(
    val id: Int,
    val name: String,
    val createdAt: String,
    val pendingCount: Int,
    val totalCount: Int
)

data class ShoppingListsResponse(
    val lists: List<ShoppingList>
)

data class FamilyListItem(
    val id: String,
    val name: String,
    val inviteCode: String,
    val role: String,
    val createdAt: String,
    val memberCount: Int,
    val pendingItemsCount: Int
)

data class FamilyListResponse(
    val families: List<FamilyListItem>
)

data class CreateShoppingListRequest(
    val familyId: String,
    val userId: String,
    val name: String
)

data class AddFamilyShoppingItemRequest(
    val familyId: String,
    val userId: String,
    val itemName: String,
    val quantity: Int = 1,
    val notes: String? = null,
    val brandName: String? = null,
    val storeName: String? = null,
    val listId: Int? = null,
    val price: Double? = null,
    val originalPrice: Double? = null,
    val productId: String? = null
)

data class UpdateFamilyItemRequest(
    val userId: String,
    val quantity: Int? = null,
    val notes: String? = null,
    val status: String? = null,
    val brandName: String? = null,
    val storeName: String? = null,
    val price: Double? = null,
    val originalPrice: Double? = null,
    val productId: String? = null,
    val listId: Int? = null
)

data class CreateFamilyRequest(
    val familyName: String,
    val userId: String
)

data class JoinFamilyRequest(
    val inviteCode: String,
    val userId: String
)

data class AddToWatchlistRequest(
    val userId: String,
    val name: String
)

data class AddToWatchlistResponse(
    val success: Boolean,
    val id: String
)

data class BasicResponse(
    val success: Boolean? = true,
    val error: String? = null
)
