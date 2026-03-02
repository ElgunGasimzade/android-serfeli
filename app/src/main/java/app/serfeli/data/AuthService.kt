package app.serfeli.data

import android.content.Context
import app.serfeli.model.UpdateProfileRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthService(private val context: Context) {
    private val sessionManager = SessionManager(context)
    private val apiService = RetrofitClient.apiService

    suspend fun updateProfile(username: String?, email: String?, phone: String?): Boolean {
        return withContext(Dispatchers.IO) {
            var success = false
            // 1. Call Backend
            try {
                var userId = sessionManager.getUserId() ?: return@withContext false
                
                // If we are in fallback mode (guest_ prefix), try to get a real session first
                if (userId.startsWith("guest_")) {
                    try {
                        val session = sessionManager.createGuestSession()
                        userId = session.userId
                    } catch (e: Exception) {
                        // Failed to upgrade session, can't update profile on backend
                        e.printStackTrace()
                        return@withContext false
                    }
                }
                
                apiService.updateUserProfile(
                    UpdateProfileRequest(userId = userId, username = username, email = email, phone = phone)
                )
                success = true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // Propagate cancellation!
            } catch (e: Exception) {
                e.printStackTrace()
                // Keep success = false
            }

            // 2. Always update local for "Optimistic UI" IF we decide so, 
            // OR only on success to be strict.
            // User wants "Identical to iOS". iOS usually is optimistic or shows error.
            
            // We update local cache ONLY if backend success OR if we want optimistic updates.
            // Given the user report "profile information should never come from cache but it is",
            // we should probably NOT update cache if backend fails, to avoid showing false state.
            // BUT, if we are offline?
            // Let's stick to updating cache on SUCCESS for now to be safe and avoid "false" data.
            
            if (success) {
                sessionManager.saveUserInfo(
                    username = username ?: sessionManager.getUsername() ?: "Guest",
                    email = email ?: sessionManager.getEmail() ?: "",
                    phone = phone ?: sessionManager.getPhone() ?: ""
                )
            }
            
            // Return backend success status to let UI know if sync worked
            return@withContext success
        }
    }

    suspend fun fetchUserProfile(): UserProfile? {
         return withContext(Dispatchers.IO) {
             try {
                 var userId = sessionManager.getUserId() ?: return@withContext null
                 
                 // Retry login if fallback
                 if (userId.startsWith("guest_")) {
                     try {
                        val session = sessionManager.createGuestSession()
                        userId = session.userId
                     } catch (e: Exception) {
                        // Ignore, proceed with fallback cache logic below if API fails
                     }
                 }
                 
                 // "retrieve it always from db" - fetch latest
                 val response = apiService.getUserProfile(userId)
                 
                 // Cache it (even if user said "not cache", we need to store it somewhere to display it efficiently)
                 // But we prioritize the fresh data for the return value
                 sessionManager.saveUserInfo(
                     username = response.username,
                     email = response.email ?: "",
                     phone = response.phone ?: ""
                 )
                 
                 UserProfile(
                     username = response.username,
                     email = response.email,
                     phone = response.phone
                 )
             } catch (e: kotlinx.coroutines.CancellationException) {
                 throw e
             } catch (e: retrofit2.HttpException) {
                 if (e.code() == 404) {
                     // 404 means the user does not exist on server (e.g. DB reset or deleted)
                     // Critical: Clear local session to avoid "zombie" state
                     withContext(Dispatchers.Main) {
                         // Optional: could show toast, but here we just reset
                         // Log.e("AuthService", "User 404, resetting session")
                     }
                     sessionManager.clearSession()
                     val newSession = sessionManager.createGuestSession()
                     // Return the new guest profile
                     UserProfile(
                         username = "Guest",
                         email = null,
                         phone = null
                     )
                 } else {
                     e.printStackTrace()
                     getUser() // Fallback to cache for other errors (e.g. 500, network)
                 }
             } catch (e: Exception) {
                 e.printStackTrace()
                 // If API fails, fallback to cache or null
                 getUser()
             }
         }
    }

    fun getUser(): UserProfile? {
        val username = sessionManager.getUsername()
        if (username == null) return null
        return UserProfile(
            username = username,
            email = sessionManager.getEmail(),
            phone = sessionManager.getPhone()
        )
    }
}

data class UserProfile(
    val username: String,
    val email: String?,
    val phone: String?
)
