# 📄 Product Requirements Document (PRD)

**Product Name:** FieldQuote  
**Platform:** Android  
**Owner:** [Your Name / Business Name]  
**Created:** 2025-09-30  
**Version:** 1.0  
**Status:** Draft  

---

## 1. 🧭 Overview

**Goal:**  
To build an Android app that simplifies field service quoting and invoicing by allowing users to input service types, rates (per foot, flat, etc.), and generate professional multi-option quotes or single-option invoices as PDFs. The app also helps calculate potential earnings before finalizing any document.

**Primary Users:**  
- Field service providers (insulation, demolition, fencing, flooring, etc.)  
- Contractors and small business owners  
- Sales reps working on-site with clients  

---

## 2. 🎯 Key Objectives

- Let users select and enter service types and pricing.
- Allow earnings preview via dynamic calculations (e.g., $3/ft × 250 ft = $750).
- Support multiple pricing options per quote (e.g., Basic, Standard, Premium).
- Generate professional PDF documents (quotes/invoices).
- Share via SMS, WhatsApp, or email directly from app.
- Save client/job info locally for reuse.

---

## 3. 🔧 Features & Functionality

### 3.1 Service Entry Form
- Dropdown or search field to select service type
- Input fields:
  - Unit rate (e.g., $3/ft)
  - Quantity (e.g., 250 ft)
  - Description (optional)
- Auto-calculate subtotal per line

### 3.2 Multiple Quote Tiers (Optional)
- Add up to 3 pricing tiers (Basic, Standard, Premium)
- Each tier can have different services, discounts, or bundles
- Toggle to disable tiers and just create a single invoice

### 3.3 Quote/Invoice Summary
- Auto-calculate:
  - Total
  - Tax (configurable %)
  - Notes field (e.g., job ETA, warranty)
- Option to mark as “Quote” or “Invoice”
- Add client info (name, phone, job address)

### 3.4 PDF Generator
- Generates clean, branded PDF:
  - Business name/logo
  - Client details
  - Services listed by tier or invoice
  - Totals
  - Optional signature section
- Stored locally and optionally backed up via Google Drive (later phase)

### 3.5 Share & Print
- Share via:
  - Email (opens email client)
  - SMS, WhatsApp, etc. (Android Intent share)
- Save PDF to phone for offline use
- Print via Android Print Framework

---

## 4. 📲 UI/UX Design Notes

### Core Screens:
1. **Home** – List of previous quotes/invoices + “New Quote” button  
2. **Service Input** – Form to enter line items and pricing options  
3. **Preview & Review** – Shows summary and options before generation  
4. **Generated PDF View** – Share/Print/Save options  

### Design Style:
- Clean, card-based layout (Jetpack Compose)
- Responsive for small screens
- Use icons for actions (e.g., printer, share, copy)

---

## 5. 📈 Future Features (Phase 2+)

- Client database & CRM-lite
- Payment tracking & status tags (sent, signed, paid)
- Signature capture (touchscreen)
- Integrate QuickBooks / Stripe API
- Cloud sync with backup
- Inventory per service type

---

## 6. ⚙️ Technical Specs

| Area           | Stack / Library            |
|----------------|----------------------------|
| Language       | Kotlin                     |
| UI Toolkit     | Jetpack Compose            |
| PDF Engine     | iText / PdfDocument API    |
| Storage        | Room DB or SharedPreferences |
| Sharing        | Android Intents            |
| Optional Cloud | Firebase (later)           |

---

## 7. 📦 Data Model (Simplified)

```kotlin
data class ServiceLineItem(
  val serviceType: String,
  val unit: String,
  val quantity: Double,
  val rate: Double,
  val description: String?
)

data class QuoteTier(
  val title: String,
  val items: List<ServiceLineItem>
)

data class QuoteInvoice(
  val id: String,
  val type: String,
  val clientName: String,
  val clientPhone: String?,
  val jobAddress: String?,
  val tiers: List<QuoteTier>,
  val createdDate: Long,
  val notes: String?
)
```

---

## 8. 🧪 Acceptance Criteria

| Feature                         | Criteria |
|--------------------------------|----------|
| Quote creation                 | Can input at least 3 quote options per document |
| Invoice generation             | Can enter single-tier services and create invoice |
| PDF layout                     | Legible, branded, exportable PDF |
| Calculations                   | Accurate subtotal, tax, total |
| Sharing                        | Opens Android share sheet with file |
| Offline-first                  | Fully usable without internet |

---

## 9. 🚀 Launch Plan

1. **Prototype**  
   - Local-only PDF generator with basic input form  
2. **MVP Beta**  
   - Tiered quote support + invoice toggle  
   - Save/share locally  
3. **Public Release**  
   - Save previous quotes  
   - Print-friendly and brandable PDF  
   - App store publishing  
