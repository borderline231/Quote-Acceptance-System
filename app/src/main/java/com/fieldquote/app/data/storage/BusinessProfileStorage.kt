package com.fieldquote.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fieldquote.app.data.models.BusinessProfile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Secure storage for business profile using encrypted SharedPreferences
 * Ensures business data is protected on the device
 */
class BusinessProfileStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "fieldquote_business_profile"
        private const val KEY_BUSINESS_PROFILE = "business_profile"
        private const val KEY_IS_SETUP_COMPLETE = "is_setup_complete"
        private const val KEY_ACTIVE_BUSINESS_ID = "active_business_id"

        @Volatile
        private var INSTANCE: BusinessProfileStorage? = null

        fun getInstance(context: Context): BusinessProfileStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BusinessProfileStorage(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val gson = Gson()
    private val prefs: SharedPreferences

    init {
        // Create or retrieve the master key for encryption
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize encrypted SharedPreferences
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save business profile securely
     */
    suspend fun saveBusinessProfile(profile: BusinessProfile) = withContext(Dispatchers.IO) {
        val json = gson.toJson(profile)
        prefs.edit()
            .putString(KEY_BUSINESS_PROFILE, json)
            .putBoolean(KEY_IS_SETUP_COMPLETE, true)
            .putString(KEY_ACTIVE_BUSINESS_ID, profile.businessId)
            .apply()
    }

    /**
     * Retrieve business profile
     */
    suspend fun getBusinessProfile(): BusinessProfile? = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_BUSINESS_PROFILE, null)
        json?.let {
            try {
                gson.fromJson(it, BusinessProfile::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Update specific fields of the business profile
     */
    suspend fun updateBusinessProfile(updates: (BusinessProfile) -> BusinessProfile) =
        withContext(Dispatchers.IO) {
            getBusinessProfile()?.let { currentProfile ->
                val updatedProfile = updates(currentProfile).copy(
                    updatedAt = System.currentTimeMillis()
                )
                saveBusinessProfile(updatedProfile)
                updatedProfile
            }
        }

    /**
     * Update FCM token for push notifications
     */
    suspend fun updateFcmToken(token: String) = withContext(Dispatchers.IO) {
        updateBusinessProfile { profile ->
            profile.copy(fcmToken = token)
        }
    }

    /**
     * Update notification preferences
     */
    suspend fun updateNotificationPreferences(
        enablePush: Boolean? = null,
        enableEmail: Boolean? = null,
        enableSms: Boolean? = null,
        notificationEmail: String? = null,
        notificationPhone: String? = null
    ) = withContext(Dispatchers.IO) {
        updateBusinessProfile { profile ->
            profile.copy(
                enablePushNotifications = enablePush ?: profile.enablePushNotifications,
                enableEmailNotifications = enableEmail ?: profile.enableEmailNotifications,
                enableSmsNotifications = enableSms ?: profile.enableSmsNotifications,
                notificationEmail = notificationEmail ?: profile.notificationEmail,
                notificationPhone = notificationPhone ?: profile.notificationPhone
            )
        }
    }

    /**
     * Check if business setup is complete
     */
    fun isSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_IS_SETUP_COMPLETE, false)
    }

    /**
     * Get active business ID
     */
    fun getActiveBusinessId(): String? {
        return prefs.getString(KEY_ACTIVE_BUSINESS_ID, null)
    }

    /**
     * Clear all business data (for logout/reset)
     */
    suspend fun clearBusinessData() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    /**
     * Update server configuration
     */
    suspend fun updateServerConfig(serverUrl: String, apiKey: String? = null) =
        withContext(Dispatchers.IO) {
            updateBusinessProfile { profile ->
                profile.copy(
                    serverUrl = serverUrl,
                    apiKey = apiKey
                )
            }
        }

    /**
     * Mark profile as synced with server
     */
    suspend fun markAsSynced() = withContext(Dispatchers.IO) {
        updateBusinessProfile { profile ->
            profile.copy(lastSyncedAt = System.currentTimeMillis())
        }
    }
}