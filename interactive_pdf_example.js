// JavaScript for PDF Accept Button (Adobe Acrobat)
// This code would be added to a button's action in Adobe Acrobat

// Method 1: Submit to a web endpoint
this.submitForm({
    cURL: "https://your-server.com/api/accept-notification",
    cSubmitAs: "FDF", // or "HTML", "XML", "PDF"
    cCharset: "utf-8"
});

// Method 2: Send email notification
this.mailDoc({
    bUI: false,
    cTo: "notifications@company.com",
    cSubject: "Client Accepted - " + this.documentFileName,
    cMsg: "The client has accepted the document at " + new Date()
});

// Method 3: Open a URL to trigger notification
app.launchURL("https://your-server.com/accepted?doc=" + this.documentFileName);