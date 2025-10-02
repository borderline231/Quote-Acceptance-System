package com.fieldquote.app.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.fieldquote.app.data.models.BusinessProfile
import com.fieldquote.app.data.models.QuoteWithDetails
import com.fieldquote.app.data.storage.BusinessProfileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Enhanced share handler with multi-user business support
 * Handles quote sharing with business isolation and tracking
 */
class EnhancedShareHandlerV2(private val context: Context) {

    private val client = OkHttpClient()
    private val profileStorage = BusinessProfileStorage.getInstance(context)

    /**
     * Share quote with online acceptance tracking and business identification
     * Each business's quotes are isolated on the server
     */
    suspend fun shareQuoteWithAcceptanceTracking(
        pdfFile: File,
        quote: QuoteWithDetails,
        recipientPhone: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            // Get business profile - required for multi-user support
            val businessProfile = profileStorage.getBusinessProfile()
                ?: throw Exception("Business profile not configured")

            // Upload the PDF and quote details to server with business info
            val quoteId = uploadQuoteToServer(pdfFile, quote, businessProfile)

            // Generate the acceptance link
            val acceptanceLink = "${businessProfile.serverUrl}/quote/$quoteId"

            // Create share message with business branding
            val message = buildShareMessage(
                businessProfile,
                quote,
                acceptanceLink
            )

            withContext(Dispatchers.Main) {
                // Share via SMS if phone number provided
                if (!recipientPhone.isNullOrEmpty()) {
                    shareViaSMS(message, recipientPhone)
                } else {
                    // Generic share dialog
                    shareViaIntent(message, businessProfile.businessName)
                }
            }

            // Track quote sharing for analytics
            trackQuoteShared(quoteId, businessProfile.businessId)

        } catch (e: Exception) {
            // Fallback to regular PDF sharing if server upload fails
            withContext(Dispatchers.Main) {
                ShareHandler(context).sharePdfFile(pdfFile)
            }
        }
    }

    /**
     * Upload PDF and quote details to server with business information
     */
    private suspend fun uploadQuoteToServer(
        pdfFile: File,
        quote: QuoteWithDetails,
        businessProfile: BusinessProfile
    ): String {
        val quoteId = UUID.randomUUID().toString()

        // Build multipart request with PDF, quote data, and business info
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            // Quote identification
            .addFormDataPart("quoteId", quoteId)

            // Business information for multi-user support
            .addFormDataPart("businessId", businessProfile.businessId)
            .addFormDataPart("businessName", businessProfile.businessName)
            .addFormDataPart("businessEmail", businessProfile.email)
            .addFormDataPart("businessPhone", businessProfile.phone)
            .addFormDataPart("businessAddress", businessProfile.address ?: "")
            .addFormDataPart("businessWebsite", businessProfile.website ?: "")
            .addFormDataPart("businessOwner", businessProfile.ownerName)

            // Client information
            .addFormDataPart("clientName", quote.quote.clientName)
            .addFormDataPart("clientPhone", quote.quote.clientPhone ?: "")
            .addFormDataPart("clientEmail", quote.quote.clientEmail ?: "")
            .addFormDataPart("jobAddress", quote.quote.jobAddress ?: "")

            // Quote details
            .addFormDataPart("totalAmount", quote.calculateGrandTotal().toString())
            .addFormDataPart("quoteDate", System.currentTimeMillis().toString())
            .addFormDataPart("validUntil",
                (System.currentTimeMillis() + (businessProfile.quoteValidityDays * 24 * 60 * 60 * 1000)).toString()
            )

            // Notification preferences
            .addFormDataPart("notificationPreferences", JSONObject().apply {
                put("enablePush", businessProfile.enablePushNotifications)
                put("enableEmail", businessProfile.enableEmailNotifications)
                put("enableSms", businessProfile.enableSmsNotifications)
                put("notificationEmail", businessProfile.getNotificationEmailAddress())
                put("notificationPhone", businessProfile.getNotificationPhoneNumber())
                put("fcmToken", businessProfile.fcmToken ?: "")
            }.toString())

            // Detailed quote structure
            .addFormDataPart("quoteDetails", createQuoteDetailsJson(quote, businessProfile))

            // PDF file
            .addFormDataPart(
                "pdf",
                pdfFile.name,
                pdfFile.asRequestBody("application/pdf".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("${businessProfile.serverUrl}/api/upload-quote")
            .post(requestBody)
            .apply {
                // Add API key if configured for authenticated requests
                businessProfile.apiKey?.let {
                    addHeader("X-API-Key", it)
                }
            }
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Failed to upload quote: ${response.code}")
        }

        // Parse response to get confirmed quote ID
        val responseBody = response.body?.string()
        val responseJson = responseBody?.let { JSONObject(it) }

        return responseJson?.optString("quoteId") ?: quoteId
    }

    /**
     * Create detailed quote JSON with all tiers and items
     */
    private fun createQuoteDetailsJson(
        quote: QuoteWithDetails,
        businessProfile: BusinessProfile
    ): String {
        return JSONObject().apply {
            // Business branding
            put("businessInfo", JSONObject().apply {
                put("name", businessProfile.businessName)
                put("owner", businessProfile.ownerName)
                put("email", businessProfile.email)
                put("phone", businessProfile.phone)
                put("website", businessProfile.website ?: "")
                put("licenseNumber", businessProfile.licenseNumber ?: "")
                put("taxId", businessProfile.taxId ?: "")
            })

            // Quote tiers and items
            put("tiers", JSONArray().apply {
                quote.tiers.forEach { tier ->
                    put(JSONObject().apply {
                        put("title", tier.title)
                        put("description", tier.description ?: "")
                        put("total", tier.calculateTotal())
                        put("items", JSONArray().apply {
                            tier.items.forEach { item ->
                                put(JSONObject().apply {
                                    put("serviceType", item.serviceType)
                                    put("quantity", item.quantity)
                                    put("rate", item.rate)
                                    put("unit", item.unit)
                                    put("description", item.description ?: "")
                                    put("subtotal", item.quantity * item.rate)
                                })
                            }
                        })
                    })
                }
            })

            // Financial details
            put("subtotal", quote.calculateSubtotal())
            put("taxRate", quote.taxRate)
            put("taxAmount", quote.calculateTax())
            put("grandTotal", quote.calculateGrandTotal())
            put("currency", businessProfile.currency)

            // Additional information
            put("notes", quote.quote.notes ?: "")
            put("termsAndConditions", businessProfile.termsAndConditions ?: "")
            put("validityDays", businessProfile.quoteValidityDays)
        }.toString()
    }

    /**
     * Build branded share message
     */
    private fun buildShareMessage(
        businessProfile: BusinessProfile,
        quote: QuoteWithDetails,
        acceptanceLink: String
    ): String {
        return buildString {
            appendLine("📋 Quote from ${businessProfile.businessName}")
            appendLine()
            appendLine("Dear ${quote.quote.clientName},")
            appendLine()
            appendLine("Your personalized quote is ready for review:")
            appendLine("Total: $${quote.calculateGrandTotal()}")

            if (quote.quote.jobAddress?.isNotBlank() == true) {
                appendLine("Location: ${quote.quote.jobAddress}")
            }

            appendLine()
            appendLine("✅ View & Accept Quote:")
            appendLine(acceptanceLink)
            appendLine()
            appendLine("Click the link to:")
            appendLine("• View detailed pricing")
            appendLine("• See all service options")
            appendLine("• Accept with one click")
            appendLine()

            if (businessProfile.quoteValidityDays > 0) {
                appendLine("This quote is valid for ${businessProfile.quoteValidityDays} days.")
            }

            appendLine()
            appendLine("Questions? Contact us:")

            if (businessProfile.phone.isNotBlank()) {
                appendLine("📞 ${businessProfile.phone}")
            }

            if (businessProfile.email.isNotBlank()) {
                appendLine("✉️ ${businessProfile.email}")
            }

            if (businessProfile.website?.isNotBlank() == true) {
                appendLine("🌐 ${businessProfile.website}")
            }

            appendLine()
            appendLine("Thank you for your business!")
            appendLine("- ${businessProfile.ownerName}")
            appendLine(businessProfile.businessName)
        }
    }

    /**
     * Share via SMS with formatted message
     */
    private fun shareViaSMS(message: String, phoneNumber: String) {
        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("sms:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            // Fallback to generic share
            shareViaIntent(message, "Quote")
        }
    }

    /**
     * Share via generic intent
     */
    private fun shareViaIntent(message: String, businessName: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, "Your Quote from $businessName")
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Quote")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    /**
     * Track quote sharing for analytics
     */
    private suspend fun trackQuoteShared(quoteId: String, businessId: String) {
        try {
            val json = JSONObject().apply {
                put("quoteId", quoteId)
                put("businessId", businessId)
                put("sharedAt", System.currentTimeMillis())
                put("method", "link")
            }

            val profile = profileStorage.getBusinessProfile()
            profile?.let {
                val request = Request.Builder()
                    .url("${it.serverUrl}/api/track-quote-shared")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .apply {
                        it.apiKey?.let { key ->
                            addHeader("X-API-Key", key)
                        }
                    }
                    .build()

                client.newCall(request).execute()
            }
        } catch (e: Exception) {
            // Silent failure - tracking is not critical
        }
    }

    /**
     * Check acceptance status of a quote
     */
    suspend fun checkQuoteStatus(quoteId: String): QuoteStatus = withContext(Dispatchers.IO) {
        try {
            val profile = profileStorage.getBusinessProfile()
                ?: return@withContext QuoteStatus(accepted = false)

            val request = Request.Builder()
                .url("${profile.serverUrl}/api/quote-status/$quoteId")
                .get()
                .apply {
                    profile.apiKey?.let {
                        addHeader("X-API-Key", it)
                    }
                    // Include business ID for verification
                    addHeader("X-Business-Id", profile.businessId)
                }
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                QuoteStatus(
                    accepted = json.getBoolean("accepted"),
                    viewedAt = json.optString("viewedAt"),
                    acceptedAt = json.optString("acceptedAt"),
                    clientName = json.optString("clientName"),
                    clientEmail = json.optString("clientEmail"),
                    totalAmount = json.optDouble("totalAmount", 0.0)
                )
            } else {
                QuoteStatus(accepted = false)
            }
        } catch (e: Exception) {
            QuoteStatus(accepted = false)
        }
    }
}

/**
 * Enhanced quote status with additional information
 */
data class QuoteStatus(
    val accepted: Boolean,
    val viewedAt: String? = null,
    val acceptedAt: String? = null,
    val clientName: String? = null,
    val clientEmail: String? = null,
    val totalAmount: Double? = null
)