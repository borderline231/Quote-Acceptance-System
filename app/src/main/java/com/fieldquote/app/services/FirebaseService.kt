package com.fieldquote.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fieldquote.app.MainActivity
import com.fieldquote.app.R
import com.fieldquote.app.data.models.BusinessProfile
import com.fieldquote.app.data.storage.BusinessProfileStorage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Firebase Cloud Messaging service for handling push notifications
 * Receives notifications when quotes are accepted
 */
class FirebaseService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "fieldquote_notifications"
        private const val CHANNEL_NAME = "FieldQuote Notifications"
        private const val NOTIFICATION_ID = 1001

        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        /**
         * Register business with server and subscribe to notifications
         */
        suspend fun registerBusinessWithServer(profile: BusinessProfile) {
            try {
                val json = JSONObject().apply {
                    put("businessId", profile.businessId)
                    put("businessName", profile.businessName)
                    put("ownerName", profile.ownerName)
                    put("email", profile.email)
                    put("phone", profile.phone)
                    put("fcmToken", profile.fcmToken)
                    put("enablePushNotifications", profile.enablePushNotifications)
                    put("enableEmailNotifications", profile.enableEmailNotifications)
                    put("enableSmsNotifications", profile.enableSmsNotifications)
                    put("notificationEmail", profile.getNotificationEmailAddress())
                    put("notificationPhone", profile.getNotificationPhoneNumber())
                }

                val request = Request.Builder()
                    .url("${profile.serverUrl}/api/register-business")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to register business: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Log error but don't fail the app
            }
        }

        /**
         * Update FCM token on server
         */
        suspend fun updateFcmTokenOnServer(businessId: String, token: String, serverUrl: String) {
            try {
                val json = JSONObject().apply {
                    put("businessId", businessId)
                    put("fcmToken", token)
                }

                val request = Request.Builder()
                    .url("$serverUrl/api/update-fcm-token")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    // Silent failure - token update is not critical
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private lateinit var profileStorage: BusinessProfileStorage

    override fun onCreate() {
        super.onCreate()
        profileStorage = BusinessProfileStorage.getInstance(this)
        createNotificationChannel()
    }

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Update token in local storage
        CoroutineScope(Dispatchers.IO).launch {
            profileStorage.updateFcmToken(token)

            // Update token on server if business is registered
            profileStorage.getBusinessProfile()?.let { profile ->
                updateFcmTokenOnServer(
                    profile.businessId,
                    token,
                    profile.serverUrl
                )
            }
        }
    }

    /**
     * Handle incoming FCM messages
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains data payload
        remoteMessage.data.isNotEmpty().let {
            handleQuoteAcceptanceNotification(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            showNotification(
                it.title ?: "FieldQuote",
                it.body ?: "You have a new notification"
            )
        }
    }

    /**
     * Process quote acceptance notification
     */
    private fun handleQuoteAcceptanceNotification(data: Map<String, String>) {
        val quoteId = data["quoteId"]
        val clientName = data["clientName"]
        val totalAmount = data["totalAmount"]
        val acceptedAt = data["acceptedAt"]
        val notificationType = data["type"]

        when (notificationType) {
            "quote_accepted" -> {
                val title = "Quote Accepted!"
                val message = "$clientName has accepted your quote for $$totalAmount"
                showNotificationWithAction(title, message, quoteId)
            }
            "quote_viewed" -> {
                val title = "Quote Viewed"
                val message = "$clientName is viewing your quote"
                showNotification(title, message)
            }
            "reminder" -> {
                val title = "Quote Reminder"
                val message = data["message"] ?: "You have pending quotes"
                showNotification(title, message)
            }
            else -> {
                // Handle other notification types
                data["title"]?.let { title ->
                    data["body"]?.let { body ->
                        showNotification(title, body)
                    }
                }
            }
        }

        // Store notification in local database for history
        saveNotificationToHistory(data)
    }

    /**
     * Show notification with action button
     */
    private fun showNotificationWithAction(title: String, message: String, quoteId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent for when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("quoteId", quoteId)
            putExtra("fromNotification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_view,
                "View Details",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show simple notification
     */
    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for accepted quotes and important updates"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Save notification to local history
     */
    private fun saveNotificationToHistory(data: Map<String, String>) {
        CoroutineScope(Dispatchers.IO).launch {
            // TODO: Implement local database storage for notification history
            // This would typically use Room database
        }
    }
}