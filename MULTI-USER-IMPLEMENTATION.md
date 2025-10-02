# FieldQuote Multi-User Implementation Guide

## Complete Implementation Summary

This implementation adds comprehensive multi-user support to FieldQuote, allowing multiple businesses to use the same infrastructure while maintaining complete isolation and privacy.

## Features Implemented

### 1. Android App Components

#### Business Profile Management
- **Location**: `app/src/main/java/com/fieldquote/app/data/models/BusinessProfile.kt`
- **Storage**: `app/src/main/java/com/fieldquote/app/data/storage/BusinessProfileStorage.kt`
- Secure encrypted storage for business information
- Support for multiple notification preferences per business
- Complete business isolation

#### Business Setup Flow
- **Location**: `app/src/main/java/com/fieldquote/app/ui/setup/BusinessSetupActivity.kt`
- First-time setup wizard for new businesses
- Captures business information, notification preferences, and server configuration
- Beautiful Material Design 3 UI with step-by-step process

#### Enhanced Quote Sharing
- **Location**: `app/src/main/java/com/fieldquote/app/share/EnhancedShareHandlerV2.kt`
- Includes business information with every quote upload
- Branded quote sharing messages
- Business-specific tracking and analytics

#### Push Notifications
- **Location**: `app/src/main/java/com/fieldquote/app/services/FirebaseService.kt`
- Firebase Cloud Messaging integration
- Real-time notifications when quotes are accepted
- Business-specific notification routing

### 2. Server Components

#### Multi-User Server
- **Location**: `quote-acceptance-system/multi-user-server.js`
- Complete business isolation
- API key authentication per business
- Multi-channel notification system

#### Key Features:
- **Business Registration**: Each business gets unique ID and API key
- **Quote Isolation**: Quotes are strictly isolated by business ID
- **Notification Routing**: Notifications sent only to the correct business
- **Analytics Dashboard**: Per-business analytics and reporting

#### Notification Channels Supported:
1. **Push Notifications** (Firebase Cloud Messaging)
2. **Email Notifications** (SMTP)
3. **SMS Notifications** (Twilio)
4. **Webhook Notifications** (Custom integrations)

## Setup Instructions

### Android App Setup

1. **Update build.gradle.kts**:
```kotlin
// Already configured in app/build.gradle.kts
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

2. **Add google-services.json**:
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Create a new project or select existing
   - Add Android app with your package name
   - Download `google-services.json`
   - Place in `app/` directory

3. **Update AndroidManifest.xml**:
```xml
<!-- Add required permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Add Firebase service -->
<service
    android:name="com.fieldquote.app.services.FirebaseService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<!-- Add BusinessSetupActivity -->
<activity
    android:name="com.fieldquote.app.ui.setup.BusinessSetupActivity"
    android:exported="false" />
```

### Server Setup

1. **Install Dependencies**:
```bash
cd quote-acceptance-system
npm install
```

2. **Configure Environment**:
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. **Firebase Setup**:
   - Go to Firebase Console → Project Settings → Service Accounts
   - Generate new private key
   - Save as `firebase-service-account.json` in server directory
   - Update `FIREBASE_SERVICE_ACCOUNT_PATH` in `.env`

4. **Email Configuration**:
```env
EMAIL_SERVICE=gmail
EMAIL_USER=your-email@gmail.com
EMAIL_PASS=your-app-specific-password  # Generate from Google Account settings
```

5. **SMS Configuration (Optional)**:
```env
TWILIO_ACCOUNT_SID=your_account_sid
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890
```

6. **Start Server**:
```bash
npm start
# or for development
npm run dev
```

## How It Works

### Business Registration Flow

1. **First Launch**: When a new user opens FieldQuote, they're directed to `BusinessSetupActivity`
2. **Business Info**: User enters business name, contact info, and preferences
3. **Registration**: App registers with server and receives unique API key
4. **FCM Token**: Firebase token is obtained and stored for push notifications
5. **Ready**: User can now create and share quotes

### Quote Sharing Flow

1. **Create Quote**: Business creates quote in FieldQuote app
2. **Upload**: Quote uploaded with business ID and notification preferences
3. **Share Link**: Unique link generated and shared with client
4. **Client Views**: Server tracks views and notifies business
5. **Client Accepts**: Business receives multi-channel notifications

### Notification Flow

When a quote is accepted:
1. **Server Processing**: Server identifies the business that owns the quote
2. **Push Notification**: Sent via Firebase to business's device
3. **Email Notification**: Sent to business's notification email
4. **SMS Notification**: Sent to business's notification phone (if configured)
5. **Webhook**: Triggered for custom integrations (if configured)

## Privacy & Security

### Data Isolation
- Each business's data is completely isolated
- API key required for accessing business-specific endpoints
- Quotes can only be viewed by clients with the direct link
- Business owners can only see their own quotes

### Encryption
- Business profiles encrypted on device using Android Keystore
- HTTPS required for all server communication
- API keys stored securely

### Authentication
- API key authentication for business operations
- Business ID verification for all requests
- Rate limiting to prevent abuse

## API Endpoints

### Public Endpoints
- `POST /api/register-business` - Register new business
- `GET /quote/:quoteId` - View quote (for clients)
- `POST /api/accept-quote` - Accept quote (for clients)

### Authenticated Endpoints (Require API Key)
- `POST /api/upload-quote` - Upload new quote
- `GET /api/quote-status/:quoteId` - Check quote status
- `GET /api/business/:businessId/quotes` - List all quotes
- `GET /api/business/:businessId/analytics` - Get analytics
- `POST /api/update-fcm-token` - Update push token
- `POST /api/track-quote-shared` - Track sharing events

## Testing

### Test Business Registration
1. Clear app data
2. Launch app
3. Complete setup wizard
4. Verify business registered on server

### Test Quote Acceptance Notifications
1. Create and share a quote
2. Open quote link in browser
3. Accept the quote
4. Verify notifications received:
   - Push notification on device
   - Email in inbox
   - SMS (if configured)

### Test Multi-Business Isolation
1. Register two different businesses
2. Create quotes for each
3. Verify each business only sees their own quotes
4. Verify notifications go to correct business

## Troubleshooting

### Push Notifications Not Working
- Verify `google-services.json` is in place
- Check Firebase Console for correct setup
- Ensure notification permissions granted on device
- Check server logs for FCM errors

### Email Notifications Not Working
- Verify SMTP credentials in `.env`
- For Gmail, use app-specific password
- Check spam folder
- Review server logs for email errors

### Business Not Registering
- Check network connectivity
- Verify server URL in app configuration
- Check server logs for registration errors
- Ensure all required fields completed

## Production Deployment

### Server Deployment
1. Use environment variables for all sensitive configuration
2. Enable HTTPS with SSL certificate
3. Set up database (MongoDB/PostgreSQL) for production
4. Configure proper logging and monitoring
5. Set up backup strategy for uploaded PDFs

### App Deployment
1. Update server URL to production endpoint
2. Generate signed APK/AAB
3. Test thoroughly on multiple devices
4. Submit to Google Play Store

### Recommended Services
- **Hosting**: AWS EC2, Google Cloud, DigitalOcean
- **Database**: MongoDB Atlas, PostgreSQL on RDS
- **File Storage**: AWS S3, Google Cloud Storage
- **Email**: SendGrid, AWS SES, Mailgun
- **SMS**: Twilio, AWS SNS
- **Monitoring**: Datadog, New Relic, Sentry

## Future Enhancements

### Planned Features
1. **Web Dashboard**: Web interface for businesses to manage quotes
2. **Team Support**: Multiple users per business
3. **Quote Templates**: Reusable quote templates
4. **Payment Integration**: Accept payments through quotes
5. **Advanced Analytics**: Detailed business intelligence
6. **Offline Support**: Work offline and sync when connected
7. **Quote Versioning**: Track quote revisions
8. **Customer Portal**: Dedicated portal for repeat customers

### Scalability Considerations
- Implement caching layer (Redis)
- Use CDN for static assets
- Implement queue system for notifications
- Add load balancing for high traffic
- Consider microservices architecture

## Support

For issues or questions:
1. Check server logs: `npm run logs`
2. Check app logs: `adb logcat | grep FieldQuote`
3. Review this documentation
4. Check environment configuration
5. Verify all services are running

## License

This implementation is provided as-is for use with FieldQuote.
Ensure compliance with all third-party service terms of service.