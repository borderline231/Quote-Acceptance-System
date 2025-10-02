import uuid
from datetime import datetime, timedelta
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import letter
from reportlab.lib.colors import HexColor
import qrcode
from io import BytesIO
import base64

class FrictionlessPDFAcceptance:
    def __init__(self, base_url="https://your-domain.com"):
        self.base_url = base_url

    def create_pdf_with_one_click_accept(self,
                                        content,
                                        client_name,
                                        output_path="agreement.pdf"):
        """Create PDF with the simplest possible acceptance process"""

        # Generate unique, secure acceptance link
        doc_id = str(uuid.uuid4())

        # Create short, memorable link
        accept_url = f"{self.base_url}/a/{doc_id[:8]}"

        # Create PDF
        c = canvas.Canvas(output_path, pagesize=letter)
        width, height = letter

        # Header
        c.setFont("Helvetica-Bold", 16)
        c.drawString(50, height - 50, f"Agreement for {client_name}")

        # Content
        c.setFont("Helvetica", 11)
        y = height - 100
        for line in content.split('\n'):
            c.drawString(50, y, line)
            y -= 15

        # Large, clear acceptance section
        y = 250

        # Box around acceptance area
        c.setStrokeColor(HexColor("#2563eb"))
        c.setLineWidth(2)
        c.roundRect(40, y-100, width-80, 180, 10, stroke=1, fill=0)

        # Title
        c.setFont("Helvetica-Bold", 14)
        c.setFillColor(HexColor("#1e40af"))
        c.drawCentredString(width/2, y+50, "TO ACCEPT THIS AGREEMENT:")

        # Method 1: Click button (desktop)
        c.setFont("Helvetica", 12)
        c.setFillColor(HexColor("#374151"))
        c.drawString(60, y+20, "Option 1: Click the button below")

        # Create large, obvious button
        button_x, button_y, button_w, button_h = 180, y-20, 230, 45
        c.setFillColor(HexColor("#10b981"))  # Green
        c.roundRect(button_x, button_y, button_w, button_h, 5, stroke=0, fill=1)

        # Button text
        c.setFillColor(HexColor("#ffffff"))
        c.setFont("Helvetica-Bold", 16)
        c.drawCentredString(button_x + button_w/2, button_y + 15, "✓ I ACCEPT")

        # Make button clickable
        c.linkURL(accept_url, (button_x, button_y, button_x + button_w, button_y + button_h))

        # Method 2: QR code (mobile)
        c.setFont("Helvetica", 12)
        c.setFillColor(HexColor("#374151"))
        c.drawString(60, y-50, "Option 2: Scan with your phone")

        # Generate QR code
        qr = qrcode.QRCode(box_size=3, border=1)
        qr.add_data(accept_url)
        qr.make(fit=True)

        img = qr.make_image(fill_color="black", back_color="white")
        img_buffer = BytesIO()
        img.save(img_buffer, format='PNG')
        img_buffer.seek(0)

        # Add QR to PDF
        c.drawInlineImage(img_buffer, 450, y-85, width=80, height=80)

        # Footer with expiry info
        c.setFont("Helvetica", 9)
        c.setFillColor(HexColor("#6b7280"))
        c.drawCentredString(width/2, 50,
                          f"This acceptance link expires in 7 days. Document ID: {doc_id[:8]}")

        c.save()

        return doc_id, accept_url

    def create_simple_acceptance_page(self):
        """Create the simplest possible acceptance web page"""

        html_template = '''
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Document Acceptance</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 20px;
            padding: 40px;
            max-width: 500px;
            width: 100%;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            text-align: center;
        }
        .icon {
            font-size: 60px;
            margin-bottom: 20px;
        }
        h1 {
            color: #1a202c;
            margin-bottom: 10px;
            font-size: 28px;
        }
        .doc-info {
            color: #718096;
            margin-bottom: 30px;
            font-size: 14px;
        }
        .confirm-btn {
            background: #48bb78;
            color: white;
            border: none;
            padding: 18px 60px;
            font-size: 18px;
            font-weight: bold;
            border-radius: 10px;
            cursor: pointer;
            transition: all 0.3s;
            width: 100%;
            max-width: 300px;
        }
        .confirm-btn:hover {
            background: #38a169;
            transform: translateY(-2px);
            box-shadow: 0 10px 20px rgba(72, 187, 120, 0.3);
        }
        .confirm-btn:active {
            transform: translateY(0);
        }
        .loading {
            display: none;
            color: #718096;
            margin-top: 20px;
        }
        .success {
            display: none;
            color: #48bb78;
            margin-top: 20px;
            font-size: 18px;
        }
        .success .icon { color: #48bb78; }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">📄</div>
        <h1>Accept Agreement?</h1>
        <p class="doc-info">Document ID: <strong>{{ doc_id }}</strong></p>
        <p class="doc-info">For: <strong>{{ client_name }}</strong></p>

        <button class="confirm-btn" onclick="acceptDocument()">
            ✓ Yes, I Accept
        </button>

        <div class="loading">
            <p>⏳ Processing...</p>
        </div>

        <div class="success">
            <div class="icon">✅</div>
            <p><strong>Successfully Accepted!</strong></p>
            <p style="margin-top: 10px; color: #718096; font-size: 14px;">
                You can close this window
            </p>
        </div>
    </div>

    <script>
        async function acceptDocument() {
            const btn = document.querySelector('.confirm-btn');
            const loading = document.querySelector('.loading');
            const success = document.querySelector('.success');

            // Hide button, show loading
            btn.style.display = 'none';
            loading.style.display = 'block';

            try {
                // Send acceptance
                const response = await fetch('/api/accept', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({
                        doc_id: '{{ doc_id }}',
                        timestamp: new Date().toISOString(),
                        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
                    })
                });

                if (response.ok) {
                    // Show success
                    loading.style.display = 'none';
                    success.style.display = 'block';

                    // Auto-close after 3 seconds (mobile friendly)
                    setTimeout(() => {
                        if (window.history.length > 1) {
                            window.history.back();
                        } else {
                            window.close();
                        }
                    }, 3000);
                }
            } catch (error) {
                alert('Error accepting document. Please try again.');
                btn.style.display = 'block';
                loading.style.display = 'none';
            }
        }
    </script>
</body>
</html>
        '''

        return html_template

    def create_backend_handler(self):
        """Minimal backend to handle acceptance"""

        from flask import Flask, request, jsonify, render_template_string

        app = Flask(__name__)

        @app.route('/a/<doc_id>')
        def accept_page(doc_id):
            # Verify doc_id exists and isn't expired
            # Get client info from database

            return render_template_string(
                self.create_simple_acceptance_page(),
                doc_id=doc_id,
                client_name="John Smith"  # From database
            )

        @app.route('/api/accept', methods=['POST'])
        def process_acceptance():
            data = request.json

            # Record acceptance
            acceptance_record = {
                'doc_id': data['doc_id'],
                'timestamp': data['timestamp'],
                'ip_address': request.remote_addr,
                'user_agent': request.headers.get('User-Agent'),
                'timezone': data.get('timezone')
            }

            # Save to database
            # save_to_database(acceptance_record)

            # Send instant notifications
            self.send_instant_notification(acceptance_record)

            return jsonify({'status': 'accepted'}), 200

        return app

    def send_instant_notification(self, acceptance_data):
        """Send immediate notification via multiple channels"""

        # 1. Email (fastest)
        import smtplib
        from email.message import EmailMessage

        msg = EmailMessage()
        msg['Subject'] = f'✅ Document {acceptance_data["doc_id"]} Accepted!'
        msg.set_content(f'Accepted at: {acceptance_data["timestamp"]}')

        # 2. Push notification (if using service like Pusher)
        import requests
        requests.post('https://api.pusher.com/apps/YOUR_APP/events',
            json={'channel': 'notifications', 'name': 'doc_accepted', 'data': acceptance_data}
        )

        # 3. SMS (for critical documents)
        # twilio_client.messages.create(...)

        # 4. Webhook to your system
        requests.post('https://your-system.com/webhook',
            json=acceptance_data,
            timeout=5
        )

# Usage
system = FrictionlessPDFAcceptance(base_url="https://accept.yourcompany.com")
doc_id, accept_url = system.create_pdf_with_one_click_accept(
    content="Terms and conditions here...",
    client_name="John Smith",
    output_path="agreement.pdf"
)

print(f"PDF created with acceptance URL: {accept_url}")
# When client clicks, they see a simple page with one button, click it, done!