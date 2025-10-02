package com.fieldquote.app.data.models

import java.util.UUID

/**
 * Represents a business profile for multi-user support
 * Each business using FieldQuote will have their own profile
 */
data class BusinessProfile(
    val businessId: String = UUID.randomUUID().toString(),
    val businessName: String,
    val ownerName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val website: String? = null,
    val logoUrl: String? = null,
    val taxId: String? = null,
    val licenseNumber: String? = null,

    // Notification preferences
    val enablePushNotifications: Boolean = true,
    val enableEmailNotifications: Boolean = true,
    val enableSmsNotifications: Boolean = true,
    val notificationEmail: String? = null, // Can be different from primary email
    val notificationPhone: String? = null, // Can be different from primary phone

    // Firebase Cloud Messaging token for push notifications
    val fcmToken: String? = null,

    // Server configuration
    val serverUrl: String = "https://quote-acceptance.yourserver.com",
    val apiKey: String? = null, // For authenticated API calls

    // Business settings
    val defaultTaxRate: Double = 0.0,
    val currency: String = "USD",
    val quoteValidityDays: Int = 30,
    val termsAndConditions: String? = null,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null
) {
    /**
     * Check if business profile is complete for operations
     */
    fun isComplete(): Boolean {
        return businessName.isNotBlank() &&
                ownerName.isNotBlank() &&
                email.isNotBlank() &&
                phone.isNotBlank()
    }

    /**
     * Get display name for the business
     */
    fun getDisplayName(): String {
        return businessName.ifBlank { ownerName }
    }

    /**
     * Get notification email (falls back to primary email)
     */
    fun getNotificationEmailAddress(): String {
        return notificationEmail?.takeIf { it.isNotBlank() } ?: email
    }

    /**
     * Get notification phone (falls back to primary phone)
     */
    fun getNotificationPhoneNumber(): String {
        return notificationPhone?.takeIf { it.isNotBlank() } ?: phone
    }
}