from PyPDF2 import PdfReader, PdfWriter
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import letter
import io

def create_pdf_with_submit_button(output_path, webhook_url):
    """Create a PDF form that submits to a webhook"""

    # Create a PDF with form fields using ReportLab
    packet = io.BytesIO()
    c = canvas.Canvas(packet, pagesize=letter)

    # Add form with JavaScript action
    form = c.acroForm

    # Add submit button
    c.drawString(100, 700, "Agreement Terms Here...")

    # Create submit button that posts to webhook
    form.button(
        name='acceptButton',
        tooltip='Click to Accept',
        x=250, y=200, width=100, height=40,
        buttonStyle='beveled',
        borderWidth=2,
        fillColor='green',
        textColor='white',
        forceBorder=True
    )

    # Add JavaScript action to button
    js_code = f"""
    var doc = this;
    var now = new Date();
    var data = {{
        accepted: true,
        timestamp: now.toISOString(),
        documentName: doc.documentFileName
    }};

    // Submit form data to webhook
    this.submitForm({{
        cURL: "{webhook_url}",
        cSubmitAs: "FDF",
        oJavaScript: {{
            Before: 'app.alert("Sending acceptance confirmation...");',
            After: 'app.alert("Thank you! Your acceptance has been recorded.");'
        }}
    }});
    """

    c.save()
    packet.seek(0)

    return packet

# Webhook receiver (Flask)
from flask import Flask, request, jsonify
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

@app.route('/pdf-webhook', methods=['POST'])
def handle_pdf_submission():
    """Receive and process PDF form submissions"""

    try:
        # Parse FDF data
        content_type = request.headers.get('Content-Type')

        if 'fdf' in content_type.lower():
            # Parse FDF format
            fdf_data = request.data
            # Extract acceptance info from FDF

            # Send notifications
            send_notification_email()
            send_slack_notification()
            update_database()

            logging.info(f"Acceptance received at {datetime.now()}")

            return jsonify({"status": "accepted"}), 200

    except Exception as e:
        logging.error(f"Error processing submission: {e}")
        return jsonify({"error": str(e)}), 500

def send_notification_email():
    """Send email when document is accepted"""
    # Implementation for email notification
    pass

def send_slack_notification():
    """Send Slack notification when document is accepted"""
    import requests
    webhook_url = "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
    requests.post(webhook_url, json={
        "text": "📄 Document accepted!",
        "attachments": [{
            "color": "good",
            "fields": [{
                "title": "Status",
                "value": "Client Accepted",
                "short": True
            }, {
                "title": "Timestamp",
                "value": str(datetime.now()),
                "short": True
            }]
        }]
    })

def update_database():
    """Update database with acceptance record"""
    # Database update logic
    pass

if __name__ == '__main__':
    # Create the PDF
    create_pdf_with_submit_button("accept_form.pdf", "https://your-server.com/pdf-webhook")

    # Run webhook server
    # app.run(host='0.0.0.0', port=5000)