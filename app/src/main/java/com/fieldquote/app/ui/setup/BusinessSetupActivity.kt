package com.fieldquote.app.ui.setup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.fieldquote.app.MainActivity
import com.fieldquote.app.data.models.BusinessProfile
import com.fieldquote.app.data.storage.BusinessProfileStorage
import com.fieldquote.app.services.FirebaseService
import com.fieldquote.app.ui.theme.FieldQuoteTheme
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class BusinessSetupActivity : ComponentActivity() {

    private lateinit var profileStorage: BusinessProfileStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profileStorage = BusinessProfileStorage.getInstance(this)

        // Check if setup is already complete
        if (profileStorage.isSetupComplete()) {
            navigateToMainActivity()
            return
        }

        setContent {
            FieldQuoteTheme {
                BusinessSetupScreen(
                    onSetupComplete = { profile ->
                        saveProfileAndContinue(profile)
                    }
                )
            }
        }

        // Initialize Firebase and get FCM token
        initializeFirebase()
    }

    private fun initializeFirebase() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            lifecycleScope.launch {
                profileStorage.updateFcmToken(token)
            }
        })
    }

    private fun saveProfileAndContinue(profile: BusinessProfile) {
        lifecycleScope.launch {
            try {
                // Get FCM token if available
                val currentToken = profileStorage.getBusinessProfile()?.fcmToken
                val profileWithToken = profile.copy(fcmToken = currentToken)

                // Save profile
                profileStorage.saveBusinessProfile(profileWithToken)

                // Register with server
                FirebaseService.registerBusinessWithServer(profileWithToken)

                // Navigate to main activity
                navigateToMainActivity()
            } catch (e: Exception) {
                Toast.makeText(
                    this@BusinessSetupActivity,
                    "Setup failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    onSetupComplete: (BusinessProfile) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var businessName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var defaultTaxRate by remember { mutableStateOf("") }

    // Notification preferences
    var enablePush by remember { mutableStateOf(true) }
    var enableEmail by remember { mutableStateOf(true) }
    var enableSms by remember { mutableStateOf(true) }
    var notificationEmail by remember { mutableStateOf("") }
    var notificationPhone by remember { mutableStateOf("") }

    // Server configuration
    var serverUrl by remember { mutableStateOf("https://quote-acceptance.yourserver.com") }
    var customServer by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Business Setup",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentStep + 1) / 3f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            when (currentStep) {
                0 -> BusinessInfoStep(
                    businessName = businessName,
                    onBusinessNameChange = { businessName = it },
                    ownerName = ownerName,
                    onOwnerNameChange = { ownerName = it },
                    email = email,
                    onEmailChange = { email = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    address = address,
                    onAddressChange = { address = it },
                    website = website,
                    onWebsiteChange = { website = it },
                    licenseNumber = licenseNumber,
                    onLicenseNumberChange = { licenseNumber = it },
                    taxId = taxId,
                    onTaxIdChange = { taxId = it },
                    defaultTaxRate = defaultTaxRate,
                    onDefaultTaxRateChange = { defaultTaxRate = it },
                    onNext = {
                        if (validateBusinessInfo(businessName, ownerName, email, phone)) {
                            currentStep = 1
                        } else {
                            Toast.makeText(
                                context,
                                "Please fill all required fields",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                1 -> NotificationPreferencesStep(
                    enablePush = enablePush,
                    onEnablePushChange = { enablePush = it },
                    enableEmail = enableEmail,
                    onEnableEmailChange = { enableEmail = it },
                    enableSms = enableSms,
                    onEnableSmsChange = { enableSms = it },
                    notificationEmail = notificationEmail,
                    onNotificationEmailChange = { notificationEmail = it },
                    notificationPhone = notificationPhone,
                    onNotificationPhoneChange = { notificationPhone = it },
                    onNext = { currentStep = 2 },
                    onBack = { currentStep = 0 }
                )

                2 -> ServerConfigStep(
                    serverUrl = serverUrl,
                    onServerUrlChange = { serverUrl = it },
                    customServer = customServer,
                    onCustomServerChange = { customServer = it },
                    onComplete = {
                        val profile = BusinessProfile(
                            businessName = businessName,
                            ownerName = ownerName,
                            email = email,
                            phone = phone,
                            address = address.ifBlank { null },
                            website = website.ifBlank { null },
                            licenseNumber = licenseNumber.ifBlank { null },
                            taxId = taxId.ifBlank { null },
                            defaultTaxRate = defaultTaxRate.toDoubleOrNull() ?: 0.0,
                            enablePushNotifications = enablePush,
                            enableEmailNotifications = enableEmail,
                            enableSmsNotifications = enableSms,
                            notificationEmail = notificationEmail.ifBlank { null },
                            notificationPhone = notificationPhone.ifBlank { null },
                            serverUrl = if (customServer) serverUrl else "https://quote-acceptance.yourserver.com"
                        )
                        onSetupComplete(profile)
                    },
                    onBack = { currentStep = 1 }
                )
            }
        }
    }
}

@Composable
fun BusinessInfoStep(
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    website: String,
    onWebsiteChange: (String) -> Unit,
    licenseNumber: String,
    onLicenseNumberChange: (String) -> Unit,
    taxId: String,
    onTaxIdChange: (String) -> Unit,
    defaultTaxRate: String,
    onDefaultTaxRateChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Business Information",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Tell us about your business. This information will appear on your quotes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Required fields
        OutlinedTextField(
            value = businessName,
            onValueChange = onBusinessNameChange,
            label = { Text("Business Name *") },
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = ownerName,
            onValueChange = onOwnerNameChange,
            label = { Text("Owner Name *") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email *") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone *") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Optional fields
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Business Address") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 2
        )

        OutlinedTextField(
            value = website,
            onValueChange = onWebsiteChange,
            label = { Text("Website") },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = licenseNumber,
            onValueChange = onLicenseNumberChange,
            label = { Text("License Number") },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = taxId,
            onValueChange = onTaxIdChange,
            label = { Text("Tax ID") },
            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = defaultTaxRate,
            onValueChange = onDefaultTaxRateChange,
            label = { Text("Default Tax Rate (%)") },
            leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Next", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun NotificationPreferencesStep(
    enablePush: Boolean,
    onEnablePushChange: (Boolean) -> Unit,
    enableEmail: Boolean,
    onEnableEmailChange: (Boolean) -> Unit,
    enableSms: Boolean,
    onEnableSmsChange: (Boolean) -> Unit,
    notificationEmail: String,
    onNotificationEmailChange: (String) -> Unit,
    notificationPhone: String,
    onNotificationPhoneChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Notification Preferences",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Choose how you want to be notified when quotes are accepted.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Push Notifications", fontWeight = FontWeight.Medium)
                            Text(
                                "Instant notifications on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = enablePush,
                        onCheckedChange = onEnablePushChange
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Email Notifications", fontWeight = FontWeight.Medium)
                            Text(
                                "Get notified via email",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = enableEmail,
                        onCheckedChange = onEnableEmailChange
                    )
                }

                if (enableEmail) {
                    OutlinedTextField(
                        value = notificationEmail,
                        onValueChange = onNotificationEmailChange,
                        label = { Text("Notification Email (optional)") },
                        placeholder = { Text("Leave blank to use primary email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("SMS Notifications", fontWeight = FontWeight.Medium)
                            Text(
                                "Get notified via text message",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = enableSms,
                        onCheckedChange = onEnableSmsChange
                    )
                }

                if (enableSms) {
                    OutlinedTextField(
                        value = notificationPhone,
                        onValueChange = onNotificationPhoneChange,
                        label = { Text("Notification Phone (optional)") },
                        placeholder = { Text("Leave blank to use primary phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun ServerConfigStep(
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    customServer: Boolean,
    onCustomServerChange: (Boolean) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Server Configuration",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Configure how quotes are shared and accepted.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (!customServer)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Use Default Server",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Recommended for most users",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RadioButton(
                    selected = !customServer,
                    onClick = { onCustomServerChange(false) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (customServer)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Use Custom Server",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "For self-hosted installations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = customServer,
                        onClick = { onCustomServerChange(true) }
                    )
                }

                if (customServer) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = onServerUrlChange,
                        label = { Text("Server URL") },
                        placeholder = { Text("https://your-server.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "You can change these settings later in the app preferences.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Complete Setup")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

private fun validateBusinessInfo(
    businessName: String,
    ownerName: String,
    email: String,
    phone: String
): Boolean {
    return businessName.isNotBlank() &&
            ownerName.isNotBlank() &&
            email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            phone.isNotBlank()
}