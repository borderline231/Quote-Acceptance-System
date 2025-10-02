# ⚙️ Technical Specification: FieldQuote App

**App Name:** FieldQuote  
**Platform:** Android  
**Language:** Kotlin  
**UI Toolkit:** Jetpack Compose  
**Storage:** Room Database (for client/job info) or SharedPreferences (simplified)  
**PDF Generator:** iText (preferred) or Android PdfDocument API  
**Sharing:** Android Intent System  
**Cloud (Future):** Firebase for backup and sync  
**Created:** 2025-09-30  

---

## 1. 📲 Modules Overview

### 1.1 QuoteBuilder Module
- Handles service input, calculations, quote tiers, and PDF export
- Manages logic for toggling between Quote and Invoice mode

### 1.2 Storage Module
- Local storage using Room DB or SharedPreferences
- Handles saving/retrieving clients, jobs, previous quotes

### 1.3 PDFGenerator Module
- Uses iText/PdfDocument API to build structured, branded documents
- Supports multi-tier quotes and single-tier invoices
- Automatically includes totals, tax, and optional signature section

### 1.4 ShareModule
- Integrates with Android’s `Intent.ACTION_SEND` to share files via:
  - Email
  - WhatsApp
  - SMS
  - Google Drive, etc.

---

## 2. 📐 Data Models

### 2.1 ServiceLineItem.kt

```kotlin
data class ServiceLineItem(
    val serviceType: String,
    val unit: String,
    val quantity: Double,
    val rate: Double,
    val description: String?
)
```

### 2.2 QuoteTier.kt

```kotlin
data class QuoteTier(
    val title: String, // e.g., Basic, Standard, Premium
    val items: List<ServiceLineItem>
)
```

### 2.3 QuoteInvoice.kt

```kotlin
data class QuoteInvoice(
    val id: String,
    val type: String, // "quote" or "invoice"
    val clientName: String,
    val clientPhone: String?,
    val jobAddress: String?,
    val tiers: List<QuoteTier>,
    val createdDate: Long,
    val notes: String?
)
```

---

## 3. 🔢 Calculations

### Subtotal:
`subtotal = quantity × rate`

### Total per tier:
`sum(subtotal of all items in tier)`

### Tax (if enabled):
`total_with_tax = total × (1 + taxRate)`

### Earnings Preview (before finalizing):
Real-time display of projected earnings based on:
- Selected tier
- Quantity × unit price

---

## 4. 🧪 Testing Strategy

| Feature                     | Test Cases |
|----------------------------|------------|
| Input Validation           | Empty fields, zero/negative numbers, invalid characters |
| Quote Tier Calculations    | 1, 2, or 3 tiers with varied services and rates |
| PDF Layout                 | Proper layout, readable text, branding, multi-tier display |
| File Sharing               | Can share via at least 3 messaging platforms |
| Offline Support            | Full function with no internet |
| Storage                    | Saved quotes persist between app sessions |
| UX                         | Forms usable on small and large screens |

---

## 5. 🔄 API Hooks (Future Roadmap)

| Purpose          | API (Optional Phase)        |
|------------------|-----------------------------|
| Cloud Sync       | Firebase Firestore          |
| Email Automation | SendGrid or SMTP Gateway    |
| CRM Export       | QuickBooks / Stripe Sync    |
| Signature Upload | Firebase Storage or Supabase|

---

## 6. 🧰 Permissions Required

- `WRITE_EXTERNAL_STORAGE` – to save PDF files (Android <10)
- `READ/WRITE_MEDIA_DOCUMENTS` – scoped storage for Android 11+
- `INTERNET` – (for future Firebase or email integration)
- `READ_CONTACTS` – optional for importing client info

---

## 7. 🧑‍💻 Development Tools

- Android Studio Giraffe+
- Kotlin 1.9+
- Jetpack Compose Compiler
- iText v8+ or PdfDocument
- Room DB + Hilt (if using DI)
- Material Icons, Fonts, Themes

---

## 8. 🗓️ Dev Milestones

| Phase            | Tasks |
|------------------|-------|
| Prototype        | Form input, earnings calc, single PDF output |
| MVP              | Multi-tier support, sharing, local storage |
| Beta             | PDF branding, error handling, validation |
| v1.0 Release     | UI polish, print support, basic CRM list |
| v2.0+            | Cloud sync, API integrations, email flows |

---

## 9. 📁 File Structure (Example)

```
FieldQuote/
├── data/
│   └── models/
│       ├── QuoteInvoice.kt
│       ├── QuoteTier.kt
│       └── ServiceLineItem.kt
├── ui/
│   ├── HomeScreen.kt
│   ├── QuoteInputScreen.kt
│   ├── PreviewScreen.kt
├── pdf/
│   └── PdfGenerator.kt
├── share/
│   └── ShareHandler.kt
├── storage/
│   └── LocalStore.kt
└── MainActivity.kt
```
