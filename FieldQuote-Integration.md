# FieldQuote + Web Acceptance Integration

## Complete Workflow:

### 1. FieldQuote App (Android) - What You Have
- Field service providers create quotes
- Generate PDF with multiple tiers
- Share via SMS/WhatsApp/Email

### 2. Quote Acceptance Web System - What We Built
- Receives the PDF
- Shows it to client with Accept button
- Notifies FieldQuote when accepted

## Integration Points:

### Option A: Direct Integration (Recommended)
Add to FieldQuote's ShareHandler.kt:

```kotlin
fun shareWithAcceptanceTracking(pdfFile: File, quote: QuoteInvoice) {
    // 1. Upload PDF to your server
    val quoteId = uploadQuoteToServer(pdfFile, quote)

    // 2. Generate acceptance link
    val acceptanceLink = "https://your-domain.com/quote/$quoteId"

    // 3. Share link instead of PDF
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT,
            "View your quote: $acceptanceLink\n\n" +
            "Click to view and accept your quote online."
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Quote Link"))
}

private fun uploadQuoteToServer(pdfFile: File, quote: QuoteInvoice): String {
    // Upload PDF and quote data to your backend
    val response = ApiClient.uploadQuote(
        pdf = pdfFile,
        clientName = quote.clientName,
        clientPhone = quote.clientPhone,
        amount = quote.calculateTotal()
    )
    return response.quoteId
}
```

### Option B: Hybrid Approach
1. Share PDF normally via FieldQuote
2. Include acceptance link in the message:

```kotlin
fun sharePdfWithLink(pdfFile: File, quote: QuoteInvoice) {
    val quoteId = generateQuoteId()
    val acceptanceLink = "https://your-domain.com/accept/$quoteId"

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        putExtra(Intent.EXTRA_TEXT,
            "Please find your quote attached.\n\n" +
            "To accept this quote, click: $acceptanceLink"
        )
    }
}
```

## Notification Back to FieldQuote App:

### Add to FieldQuote - Push Notifications:

```kotlin
// Add Firebase Cloud Messaging
class NotificationReceiver : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "quote_accepted") {
            showNotification(
                title = "Quote Accepted! ✅",
                body = "${message.data["clientName"]} accepted quote ${message.data["quoteId"]}"
            )

            // Update local database
            updateQuoteStatus(message.data["quoteId"], "ACCEPTED")
        }
    }
}
```

### Add to Web Backend - Send Push to App:

```javascript
// When client accepts on web
async function notifyFieldQuoteApp(acceptanceData) {
    // Send push notification to your Android app
    await admin.messaging().send({
        token: userDeviceToken, // Your phone's FCM token
        notification: {
            title: 'Quote Accepted!',
            body: `${acceptanceData.clientName} accepted the quote`
        },
        data: {
            type: 'quote_accepted',
            quoteId: acceptanceData.quoteId,
            clientName: acceptanceData.clientName
        }
    });
}
```

## Benefits of Integration:

1. **Track Acceptance** - Know exactly when clients accept
2. **Professional** - Clients view quotes in browser, no PDF reader needed
3. **Analytics** - Track view times, acceptance rates
4. **Follow-up** - Auto-remind clients who haven't accepted
5. **Payment Ready** - Can add payment processing to acceptance page

## Quick Implementation:

1. Deploy the web acceptance system
2. Add one method to FieldQuote's ShareHandler
3. Get instant notifications when quotes are accepted
4. Track everything in your FieldQuote app

The systems work perfectly together - FieldQuote creates and sends, web system tracks acceptance!