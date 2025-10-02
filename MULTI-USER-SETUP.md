# 🔄 Multi-User Support for FieldQuote

## How It Works for Multiple Business Users:

When **Business A** sends quote → **Business A** gets notified when accepted
When **Business B** sends quote → **Business B** gets notified when accepted

## Required Changes:

### 1. FieldQuote App - Add User Registration:

```kotlin
// Add to FieldQuote app
data class BusinessProfile(
    val businessId: String = UUID.randomUUID().toString(),
    val businessName: String,
    val email: String,
    val phone: String,
    val notificationToken: String? = null // Firebase token
)

// Save business profile locally
fun saveBusinessProfile(profile: BusinessProfile) {
    sharedPreferences.edit()
        .putString("business_id", profile.businessId)
        .putString("business_email", profile.email)
        .putString("business_phone", profile.phone)
        .apply()
}
```

### 2. When Uploading Quote - Include Business Info:

```kotlin
// In EnhancedShareHandler.kt
private suspend fun uploadQuoteToServer(pdfFile: File, quote: QuoteWithDetails): String {
    val businessProfile = getBusinessProfile() // Get from SharedPreferences

    val requestBody = MultipartBody.Builder()
        .addFormDataPart("businessId", businessProfile.businessId)
        .addFormDataPart("businessEmail", businessProfile.email)
        .addFormDataPart("businessPhone", businessProfile.phone)
        .addFormDataPart("notificationToken", getFirebaseToken())
        // ... rest of quote data
        .build()
}
```

### 3. Server Changes - Track Who Sent Each Quote:

```javascript
// enhanced-server.js modification
app.post('/api/upload-quote', upload.single('pdf'), async (req, res) => {
    const {
        businessId,
        businessEmail,
        businessPhone,
        notificationToken,
        // ... other fields
    } = req.body;

    // Store with business info
    quoteDatabase.set(quoteId, {
        // ... quote data
        business: {
            id: businessId,
            email: businessEmail,
            phone: businessPhone,
            notificationToken
        }
    });
});

// When quote is accepted, notify the RIGHT business
async function sendNotifications(quote) {
    // Send to the business that created this quote
    if (quote.business.notificationToken) {
        // Push notification to their phone
        await sendPushNotification(quote.business.notificationToken, {
            title: '✅ Quote Accepted!',
            body: `${quote.clientName} accepted your quote`
        });
    }

    // Email to their business email
    if (quote.business.email) {
        await sendEmail(quote.business.email, 'Quote Accepted', ...);
    }

    // SMS to their business phone
    if (quote.business.phone) {
        await sendSMS(quote.business.phone, 'Quote accepted!');
    }
}
```

## Two Deployment Options:

### Option A: Shared Server (Recommended for Start)
- **One server** handles all businesses
- Each business gets unique IDs
- Server routes notifications to right business
- **Cost:** FREE (one deployment)
- **Setup:** What we built above

### Option B: Each Business Deploys Own Server
- Each user deploys their own copy
- Complete isolation between businesses
- **Cost:** FREE per business
- **Setup:** Each user needs to:
  1. Fork the code
  2. Deploy to Render/Glitch
  3. Configure their own URL in app

## Simplest Implementation:

### 1. Add to FieldQuote First-Time Setup:
```kotlin
class SetupActivity : AppCompatActivity() {
    fun setupBusiness() {
        // Ask for business info on first launch
        showDialog {
            inputBusinessName()
            inputEmail()
            inputPhone()

            // Generate unique ID
            val businessId = UUID.randomUUID().toString()

            // Save locally
            saveBusinessProfile(...)

            // Register for push notifications
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                saveNotificationToken(token)
            }
        }
    }
}
```

### 2. Server Auto-Routes Notifications:
```javascript
// No changes needed by users - server handles routing
quoteAccepted(quote) {
    // Automatically notifies the right business
    notifyBusiness(quote.business.id)
}
```

## What Each User Sees:

1. **Downloads FieldQuote** from Play Store
2. **Sets up business profile** (one-time)
3. **Creates & sends quotes** normally
4. **Gets notifications** only for THEIR quotes

## Database Structure:

```json
{
  "quotes": {
    "quote-123": {
      "clientName": "John Smith",
      "amount": 2500,
      "business": {
        "id": "biz-abc",
        "name": "Joe's Plumbing",
        "email": "joe@plumbing.com",
        "notificationToken": "firebase-token-xyz"
      }
    },
    "quote-456": {
      "clientName": "Jane Doe",
      "amount": 1200,
      "business": {
        "id": "biz-def",
        "name": "Mary's Electric",
        "email": "mary@electric.com",
        "notificationToken": "firebase-token-789"
      }
    }
  }
}
```

## Privacy & Security:

- Each business only sees their own quotes
- Client data is isolated per business
- Notifications only go to quote creator
- Optional: Add authentication for extra security

## To Enable Multi-User:

1. **Update FieldQuote** - Add business profile setup
2. **Update server** - Track business per quote
3. **Deploy once** - All users share the server
4. **Each user** gets their own notifications

This way, anyone who downloads FieldQuote can use it for their business and get notifications for their own quotes!