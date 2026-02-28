package com.example.dailydeals.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import com.example.dailydeals.model.GuestLoginRequest

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("daily_deals_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_TOKEN = "user_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_SHOPPING_LIST = "shopping_list"
        private const val KEY_LAST_SCAN_ID = "last_scan_id"
        private const val KEY_SELECTED_IDS = "selected_ids"
        private const val KEY_GENERIC_ITEMS = "generic_items"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_LOCATION_ENABLED = "location_enabled"
        private const val KEY_SEARCH_RANGE = "search_range"
    }

    fun saveSettings(language: String, locationEnabled: Boolean, searchRange: Float) {
        prefs.edit()
            .putString(KEY_LANGUAGE, language)
            .putBoolean(KEY_LOCATION_ENABLED, locationEnabled)
            .putFloat(KEY_SEARCH_RANGE, searchRange)
            .apply()
    }

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    fun isLocationEnabled(): Boolean = prefs.getBoolean(KEY_LOCATION_ENABLED, true)
    fun getSearchRange(): Float = prefs.getFloat(KEY_SEARCH_RANGE, 5f)

    fun saveUserInfo(username: String, email: String, phone: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHONE, phone)
            .apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getPhone(): String? = prefs.getString(KEY_PHONE, null)

    fun saveLastScanId(scanId: String) {
        prefs.edit().putString(KEY_LAST_SCAN_ID, scanId).apply()
    }

    fun getLastScanId(): String? {
        return prefs.getString(KEY_LAST_SCAN_ID, null)
    }

    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            // New Requirement: Append _android to identify platform
            deviceId = UUID.randomUUID().toString() + "_android"
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId!!
    }

    suspend fun createGuestSession(): GuestSession {
         var deviceId = getDeviceId()
         val apiService = RetrofitClient.apiService
         
         try {
             // 1. Try to login with existing device ID
             val response = apiService.deviceLogin(com.example.dailydeals.model.DeviceLoginRequest(deviceId))
             saveSession(response.token ?: "", response.user.id, response.isNewUser)
             return GuestSession(response.token, response.user.id, response.isNewUser)
         } catch (e: Exception) {
             e.printStackTrace()
             // 2. If login fails (e.g. User not found for this ID), User requested "create"
             // This implies our local ID is stale/unknown to backend.
             // We generate a FRESH ID with _android suffix and try again.
             
             try {
                // Generate new ID
                val newDeviceId = UUID.randomUUID().toString() + "_android"
                // Save it
                prefs.edit().putString(KEY_DEVICE_ID, newDeviceId).apply()
                
                // Try to create/login with NEW ID
                val response = apiService.deviceLogin(com.example.dailydeals.model.DeviceLoginRequest(newDeviceId))
                
                saveSession(response.token ?: "", response.user.id, response.isNewUser)
                return GuestSession(response.token, response.user.id, response.isNewUser)
             } catch (e2: Exception) {
                 e2.printStackTrace()
                 throw e2 // If even fresh creation fails, we have a real network/server issue.
             }
         }
    }

    private fun saveSession(token: String, userId: String, isGuest: Boolean) {
        prefs.edit()
            .putString(KEY_USER_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putBoolean(KEY_IS_GUEST, isGuest)
            .apply()
    }
    
    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_IS_GUEST)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getString(KEY_USER_TOKEN, null) != null
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    


    fun saveShoppingList(items: List<String>) {
        prefs.edit().putStringSet(KEY_SHOPPING_LIST, items.toSet()).apply()
    }

    fun getShoppingList(): List<String> {
        return prefs.getStringSet(KEY_SHOPPING_LIST, emptySet())?.toList() ?: emptyList()
    }

    fun saveSelectedIds(ids: List<String>) {
        prefs.edit().putStringSet(KEY_SELECTED_IDS, ids.toSet()).apply()
    }

    fun getSelectedIds(): List<String> {
        return prefs.getStringSet(KEY_SELECTED_IDS, emptySet())?.toList() ?: emptyList()
    }

    fun saveGenericItems(items: List<String>) {
        prefs.edit().putStringSet(KEY_GENERIC_ITEMS, items.toSet()).apply()
    }

    fun getGenericItems(): List<String> {
        return prefs.getStringSet(KEY_GENERIC_ITEMS, emptySet())?.toList() ?: emptyList()
    }

    private val KEY_WATCHLIST = "watchlist"

    fun saveWatchlist(items: List<String>) {
        prefs.edit().putStringSet(KEY_WATCHLIST, items.toSet()).apply()
    }

    fun getWatchlist(): List<String> {
        return prefs.getStringSet(KEY_WATCHLIST, emptySet())?.toList() ?: emptyList()
    }

    fun addToWatchlist(item: String) {
        val current = getWatchlist().toMutableList()
        if (!current.any { it.equals(item, ignoreCase = true) }) {
            current.add(item)
            saveWatchlist(current)
        }
    }

    fun removeFromWatchlist(item: String) {
        val current = getWatchlist().toMutableList()
        current.removeAll { it.equals(item, ignoreCase = true) }
        saveWatchlist(current)
    }

    fun saveCheckedItems(planId: String, items: Set<String>) {
        prefs.edit().putStringSet("checked_items_$planId", items).apply()
    }

    fun getCheckedItems(planId: String): Set<String> {
        return prefs.getStringSet("checked_items_$planId", emptySet()) ?: emptySet()
    }
    
    private val KEY_TOTAL_SAVINGS = "total_lifetime_savings"

    fun saveTotalSavings(amount: Double) {
        prefs.edit().putFloat(KEY_TOTAL_SAVINGS, amount.toFloat()).apply()
    }

    fun getTotalSavings(): Double {
        return prefs.getFloat(KEY_TOTAL_SAVINGS, 0f).toDouble()
    }

    private val KEY_TOTAL_TRIPS = "total_lifetime_trips"

    fun saveTotalTrips(count: Int) {
        prefs.edit().putInt(KEY_TOTAL_TRIPS, count).apply()
    }

    fun getTotalTrips(): Int {
        return prefs.getInt(KEY_TOTAL_TRIPS, 0)
    }

    private val KEY_HISTORY = "recent_route_history"
    private val gson = com.google.gson.Gson()

    fun saveHistory(items: List<com.example.dailydeals.model.RouteHistoryItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    fun getHistory(): List<com.example.dailydeals.model.RouteHistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : com.google.gson.reflect.TypeToken<List<com.example.dailydeals.model.RouteHistoryItem>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class GuestSession(
    val accessToken: String?,
    val userId: String,
    val isNewUser: Boolean
)
