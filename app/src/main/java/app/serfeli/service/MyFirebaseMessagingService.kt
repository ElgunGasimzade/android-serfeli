package app.serfeli.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "FCMService"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            // Display notification (System handles this automatically if app is in background, 
            // but for foreground you can build a NotificationCompat)
        }
    }

    private fun sendRegistrationToServer(token: String) {
        val sessionManager = app.serfeli.data.SessionManager(applicationContext)
        val userId = sessionManager.getUserId()
        val deviceId = sessionManager.getDeviceId()

        if (userId != null) {
            Log.d(TAG, "Attempting to sync token with backend: $token")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = app.serfeli.model.RegisterFcmTokenRequest(
                        userId = userId,
                        deviceId = deviceId,
                        fcmToken = token,
                        platform = "android"
                    )
                    app.serfeli.data.RetrofitClient.apiService.registerFcmToken(request)
                    Log.d(TAG, "Successfully synced token with backend")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync token with backend", e)
                }
            }
        } else {
            Log.d(TAG, "Cannot sync token: UserId is null (User not authenticated yet)")
        }
    }
}
